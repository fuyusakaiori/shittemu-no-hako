package com.org.simpleframework.beans.factory.support;

import com.org.simpleframework.beans.factory.config.BeanDefinition;

/**
 * <h2>注册中心</h2>
 * <h3>注: 注册的本质就是将 BeanDefinition 实例和名字作为键值对存储在哈希表中</h3>
 */
public interface BeanDefinitionRegistry {

    /**
     * <h3>在注册中心中注册 BeanDefinition 实例</h3>
     * @param beanName 对象的名称
     * @param beanDefinition 描述对象的实例
     */
    void registerBeanDefinition(String beanName, BeanDefinition beanDefinition);

    /**
     * <h3>根据名字从注册中心获取相应的 BeanDefinition 实例</h3>
     * @param beanName 对象的名字
     * @return BeanDefinition 实例
     */
    BeanDefinition getBeanDefinition(String beanName);

    /**
     * <h3>注册中心是否包含相应的 BeanDefinition 实例</h3>
     * @param beanName 对象的名字
     */
    boolean containsBeanDefinition(String beanName);

    /**
     * <h3>获取所有 BeanDefinition 实例的名字</h3>
     */
    String[] getBeanDefinitionsNames();

    int getBeanDefinitionCount();

}
