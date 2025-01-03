package com.org.simpleframework.beans.factory.config;

/**
 * <h3>原始对象级别的后置处理器</h3>
 * <h3>注: 主要用于 AOP</h3>
 */
public interface BeanPostProcessor {

    /**
     * <h3>Bean 实例创建前调用的方法</h3>
     */
     Object postProcessBeforeInitialization(String beanName, Object bean);

    /**
     * <h3>Bean 实例创建后调用的方法</h3>
     */
    Object postProcessAfterInitialization(String beanName, Object bean);

}
