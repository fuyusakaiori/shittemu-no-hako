package com.org.simpleframework.beans.factory.config;

import cn.hutool.core.bean.BeanException;

/**
 * <h2>负责调用后置处理器</h2>
 * <h3>注: 而不是直接让容器调用</h3>
 */
public interface AutowireCapableBeanFactory {

    default Object applyBeanPostProcessorBeforeInitialization(Object bean, String beanName) throws BeanException {
        return bean;
    }

    default Object applyBeanPostProcessorAfterInitialization(Object bean, String beanName) throws BeanException{
        return bean;
    }
}
