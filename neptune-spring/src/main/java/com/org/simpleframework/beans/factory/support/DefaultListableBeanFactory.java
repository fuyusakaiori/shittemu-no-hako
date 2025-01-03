package com.org.simpleframework.beans.factory.support;

import com.org.simpleframework.beans.exception.BeanDefinitionException;
import com.org.simpleframework.beans.factory.config.BeanDefinition;
import com.org.simpleframework.beans.factory.config.ConfigurableListableBeanFactory;
import com.org.simpleframework.util.Assert;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h2>对内使用的容器实例</h2>
 * <h3>1. 提供注册、移除、获取 BeanDefinition 实例的方法</h3>
 * <h3>2. 提供注册、移除、获取 Bean 实例的方法（继承而来的）</h3>
 * <h3>注：对外的高级容器都会创建这个简单的容器来负责加载 BeanDefinition、Bean 实例</h3>
 */
@Slf4j
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
        implements BeanDefinitionRegistry, ConfigurableListableBeanFactory
{

    /**
     * <h3>选择 ConcurrentHashMap 而不是 HashMap => 确保并发安全</h3>
     */
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    //=================================== BeanDefinition ===================================

    /**
     * <h3>1. 先检查容器中是否已经存在相应的 BeanDefinition => 直接简单粗暴覆盖掉</h3>
     * <h3>注: 源码中需要经过各种权限验证才可以决定是否覆盖</h3>
     * <h3>2. 然后检查是否存在其余的线程正在注册 BeanDefinition </h3>
     * <h3>注: 源码中发现有其余线程在注册时候需要增量更新, 这里就简单模仿源码上锁</h3>
     * @param beanName 对象的名称
     * @param beanDefinition 描述对象的实例
     */
    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        Assert.notNull(beanDefinition, beanName);
        log.debug("正在向注册中心加载\t" + beanName + "\t的 BeanDefinition 实例");
        // 简单上个锁, 因为暂时不明白是怎样判断到有 Bean 实例创建的
        synchronized (this.beanDefinitionMap){
            beanDefinitionMap.put(beanName, beanDefinition);
        }
    }

    /**
     * <h3>获取 BeanDefinition 实例: 源码的逻辑页非常简单</h3>
     * @param beanName 对象的名字
     */
    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        Assert.notNull(beanName);
        BeanDefinition beanDefinition = this.beanDefinitionMap.get(beanName);
        if (beanDefinition == null)
            throw new BeanDefinitionException(beanName);
        return beanDefinition;
    }

    /**
     * <h3>是否包含相应的 BeanDefinition 实例</h3>
     * @param beanName 对象的名字
     * @return
     */
    @Override
    public boolean containsBeanDefinition(String beanName) {
        Assert.notNull(beanName);
        return this.beanDefinitionMap.containsKey(beanName);
    }

    /**
     * <h3>获取所有 BeanDefinition 实例的名字</h3>
     */
    @Override
    public String[] getBeanDefinitionsNames() {
        Set<String> beanDefinitionsName = this.beanDefinitionMap.keySet();
        return beanDefinitionsName.toArray(new String[0]);
    }

    @Override
    public int getBeanDefinitionCount() {
        return this.beanDefinitionMap.size();
    }

    //=================================== Bean ===================================

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getBeansOfType(Class<T> type) {
        Assert.notNull(type, "");
        Map<String, T> beans = new HashMap<>();
        this.beanDefinitionMap.forEach((beanName, beanDefinition) -> {
            Class<?> beanClass = beanDefinition.getBeanClass();
            if (type.isAssignableFrom(beanClass))
                beans.put(beanName, (T) getBean(beanName));
        });
        return beans;
    }

    @Override
    public void preInstanceSingleBean() {
        this.beanDefinitionMap.forEach((beanName, beanDefinition) -> {
            // 注: 为什么不直接将 BeanDefinition 作为参数传入啊?
            if (beanDefinition.isSingleton())
                getBean(beanName, beanDefinition.getBeanClass());
        });
    }
}
