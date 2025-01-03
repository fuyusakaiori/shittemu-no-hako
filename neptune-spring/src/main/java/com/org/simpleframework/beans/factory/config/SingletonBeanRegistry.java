package com.org.simpleframework.beans.factory.config;

/**
 * <h2>单例对象的注册中心</h2>
 * <h3>注: Registry 才是实际负责管理原始对象的, 对内使用的容器; BeanFactory 则是对外使用的容器</h3>
 * <h3>注: 返回的是原始的对象, 不是封装后的 BeanDefinition 对象</h3>
 */
public interface SingletonBeanRegistry {

    /**
     * <h3>注册中心注册原始的对象</h3>
     * @param beanName 对象名称
     * @param beanObject 原始对象
     */
    void registerSingleton(String beanName, Object beanObject);

    /**
     * <h3>根据对象名字从注册中心获取原始对象</h3>
     */
    Object getSingleton(String beanName);



    /**
     * <h3>原始对象是否存在</h3>
     */
    boolean containsSingleton(String beanName);

    /**
     * <h3>获取所有原始对象的名字</h3>
     */
    String[] getSingletonNames();
}
