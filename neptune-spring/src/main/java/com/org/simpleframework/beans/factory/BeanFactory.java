package com.org.simpleframework.beans.factory;

import com.org.simpleframework.beans.exception.BeansException;

/**
 * <h2>提供访问容器的方法</h2>
 * <h3>1. IOC 容器需要创建相应的 BeanDefinition 实例</h3>
 * <h3>2. 然后负责将 BeanDefinition 实例注册到 Registry 中</h3>
 */
public interface BeanFactory {

    /**
     * <h3>根据对象的名称查找对象</h3>
     * @param beanName 对象的名称
     * @return 对象
     * @throws BeansException 找不到对象的异常
     */
    Object getBean(String beanName) throws BeansException;

    /**
     * <h3>根据对象的名称和对象的类型查找</h3>
     * @param beanName 对象的名称
     * @param requiredType 对象的类型
     * @return 对象
     * @throws BeansException 找不到对象的异常
     */
    <T> T getBean(String beanName, Class<T> requiredType) throws BeansException;

    /**
     * <h3>根据对象的类型查找</h3>
     * <h3>注: 此前基于注解实现的仅仅只能够提供按照对象类型去查询</h3>
     * @param requiredType 对象的类型
     * @return 对象
     * @throws BeansException 找不到对象的异常
     */
    <T> T getBean(Class<T> requiredType) throws BeansException;

    /**
     * <h3>仅支持按照对象的名字查询是否存在相应的对象</h3>
     * <h3>注: 不可能按照类型去查找对象是否存在, 因为相同类型的对象可能存在多个</h3>
     * @param beanName 对象的名字
     * @return 是否存在对象
     */
    boolean containsBean(String beanName);

    /**
     * TODO <h3> 判断对象是否是单例 </h3>
     * @param beanName 对象名称
     * @return 是否为单例类
     */
    boolean isSingleton(String beanName);

    boolean isPrototype(String beanName);

}
