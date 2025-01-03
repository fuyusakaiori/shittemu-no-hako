package com.org.simpleframework.beans.factory.config;

/**
 * <h2>提供对 BeanDefinition 修改的方法</h2>
 */
public interface BeanFactoryPostProcessor
{

    /**
     * <h3>提供修改 BeanDefinition 实例的机会</h3>
     * @param beanFactory 内部容器
     */
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory);

}
