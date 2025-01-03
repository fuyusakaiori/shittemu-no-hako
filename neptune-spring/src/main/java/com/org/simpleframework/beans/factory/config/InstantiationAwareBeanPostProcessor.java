package com.org.simpleframework.beans.factory.config;

import com.org.simpleframework.beans.PropertyValue;
import com.org.simpleframework.beans.PropertyValues;
import com.org.simpleframework.beans.exception.BeansException;

/**
 * <h3>后置处理器</h3>
 * <h3>注: Bean 实例属性注入完成后才会执行的方法</h3>
 */
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

    /**
     * <h3>在执行初始化方法之前调用</h3>
     * @param beanClass Bean Class 对象
     * @param beanName Bean 名字
     * @return Bean 实例
     */
    Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException;

    /**
     * <h3>在执行初始化方法之后执行</h3>
     * @param bean Bean 实例
     * @param beanName Bean 名字
     * @return 是否处理成功
     */
     boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException;

    /**
     * <h3>在注入属性之前处理需要使用的属性</h3>
     * @param propertyValues 属性集合
     * @param bean Bean 实例
     * @param beanName Bean 名字
     * @return 处理后的属性集合
     */
    PropertyValues postProcessPropertyValues(PropertyValues propertyValues, Object bean, String beanName) throws BeansException;

}
