package com.org.simpleframework.beans.factory.support;

import com.org.simpleframework.beans.exception.BeansException;
import com.org.simpleframework.beans.factory.BeanFactory;
import com.org.simpleframework.beans.factory.config.BeanDefinition;
import com.org.simpleframework.util.Assert;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <h3>简单实例化策略</h3>
 */
public class SimpleInstantiationStrategy implements InstantiationStrategy {

    private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES;
    // TODO 不支持设置浮点数的默认值
    static {
        // 创建新的哈希表
        // 将其转变为不可以修改的哈希表
        DEFAULT_TYPE_VALUES = Map.of(boolean.class, false,
                byte.class, (byte) 0,
                short.class, (short) 0,
                int.class, 0,
                long.class, (long) 0);
    }

    @Override
    public Object instantiate(BeanDefinition beanDefinition, String beanName, BeanFactory owner) throws BeansException {
        Class<?> clazz = beanDefinition.getBeanClass();
        // 注: 如果 BeanDefinition 实例中没有相应的 Class 对象肯定是创建不了的
        if (clazz == null)
            throw new IllegalArgumentException(beanName);
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            // 注: 默认私有构造器也是可以创建对象
            constructor.setAccessible(true);
            return constructor.newInstance();
        }
        catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new  BeansException(e.getMessage());
        }
    }

    /**
     * <h3>有参构造创建 Bean 实例</h3>
     */
    @Override
    public Object instantiate(BeanDefinition beanDefinition, String beanName, BeanFactory owner,
                              Constructor<?> constructor, Object... args) throws BeansException {
        try
        {
            // 1. 获取构造器所有参数的类型
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            // 2. 验证构造器的参数数量是否小于传入的参数数量
            Assert.isTrue(args.length <= parameterTypes.length);
            // 3. 然后开始创建实例
            Object[] argsDefaultWithValues = new Object[args.length];
            for (int index = 0; index < args.length; index++) {
                if (args[index] == null){
                    // 如果没有值, 那么就看是不是默认值
                    Class<?> parameterType = parameterTypes[index];
                    argsDefaultWithValues[index] = (parameterType.isPrimitive() ? DEFAULT_TYPE_VALUES.get(parameterType) : null);
                }else{
                    // 如果有值, 那么直接赋值进去就行
                    argsDefaultWithValues[index] = args[index];
                }
            }
            return constructor.newInstance(argsDefaultWithValues);
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new BeansException(e.getMessage());
        }
    }

    @Override
    public Object instantiate(BeanDefinition beanDefinition, String beanName, BeanFactory owner,
                              Object factoryBean, Method factoryMethod, Object... args) throws BeansException {
        return null;
    }
}
