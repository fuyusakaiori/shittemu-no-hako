package com.org.simpleframework.beans.factory;

/**
 * <h2>可以感知容器</h2>
 */
public interface BeanFactoryAware extends Aware{

    void setBeanFactory(BeanFactory beanFactory);
}
