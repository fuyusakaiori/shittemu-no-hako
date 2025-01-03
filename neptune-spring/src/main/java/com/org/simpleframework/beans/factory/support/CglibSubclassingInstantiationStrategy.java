package com.org.simpleframework.beans.factory.support;

import com.org.simpleframework.beans.exception.BeansException;
import com.org.simpleframework.beans.factory.BeanFactory;
import com.org.simpleframework.beans.factory.config.BeanDefinition;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * <h2>CGLIB 动态创建实例</h2>
 * <h3>1. 因为对象中存在方法需要替换, 所以会动态地生成子类</h3>
 * <h3>2. 这个子类中就包含了需要替换的方法, 然后再利用这个子类生成实例对象</h3>
 * <h3>注: 如果配置了 {@code look-up} 和 {@code replace-method} 才会使用</h3>
 */
public class CglibSubclassingInstantiationStrategy implements InstantiationStrategy
{
    @Override
    public Object instantiate(BeanDefinition beanDefinition, String beanName, BeanFactory owner) throws BeansException
    {
        return null;
    }

    @Override
    public Object instantiate(BeanDefinition beanDefinition, String beanName, BeanFactory owner, Constructor<?> constructor, Object... args) throws BeansException
    {
        return null;
    }

    @Override
    public Object instantiate(BeanDefinition beanDefinition, String beanName, BeanFactory owner, Object factoryBean, Method factoryMethod, Object... args) throws BeansException
    {
        return null;
    }
}
