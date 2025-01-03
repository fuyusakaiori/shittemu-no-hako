package com.org.simpleframework.beans.factory.config;

import com.org.simpleframework.beans.exception.BeansException;

/**
 * <h2>扩展后置处理器的功能</h2>
 */
public interface SmartInstantiationBeanPostProcessor extends InstantiationAwareBeanPostProcessor {

    /**
     * <h3>返回二级缓存中或者三级缓存创建的没有填充属性的 Bean 实例</h3>
     * <h3>注: 这个方法实际实现依靠需要代理类, 但是现在暂时没有实现代理, 所以直接返回传入的原对象就可以了</h3>
     * @param beanName Bean 名字
     * @param bean Bean 实例
     * @return 被包装之后的 Bean 实例
     */
    default Object getEarlyBeanReference(String beanName, Object bean) throws BeansException {
        return bean;
    }
}
