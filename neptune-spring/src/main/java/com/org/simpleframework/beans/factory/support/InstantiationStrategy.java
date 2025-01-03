package com.org.simpleframework.beans.factory.support;

import com.org.simpleframework.beans.exception.BeansException;
import com.org.simpleframework.beans.factory.BeanFactory;
import com.org.simpleframework.beans.factory.config.BeanDefinition;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * <h2>实例化策略</h2>
 * <h3>1. 主要分为两种策略</h3>
 * <h3>1.1 构造方法创建: 分为有参构造和无参构造, {@code spring} 默认是无参构造创建</h3>
 * <h3>注: 不过在接口中这两个方法被整合成一个方法了</h3>
 * <h3>1.2 工厂方法创建: 分为工厂静态方法创建和工厂实例创建, 分为两个不同的方法</h3>
 * <h3>注: 前者是指定 {@code factory-method}, 后者是指定 {@code factory-bean}</h3>
 * <h3>2. 这个接口共有两个实现类</h3>
 * <h3>2.1 SimpleInstantiationStrategy 主要负责采用反射创建实例</h3>
 * <h3>2.2 CglibSubclassingInstantiationStrategy </h3>
 * <h3>注: 这里暂时仅实现采用无参构造方法创建实例的方式, 原本的简易容器也是这么实现的</h3>
 */
public interface InstantiationStrategy {

    /**
     * <h3>无参构造创建 Bean 实例</h3>
     * @param beanDefinition BeanDefinition 实例
     * @param beanName Bean 名字
     * @param owner 父容器 => 主要用于 CGLIB 生成子类使用的
     * @return Bean 实例
     */
    Object instantiate(BeanDefinition beanDefinition, String beanName, BeanFactory owner) throws BeansException;

    /**
     * <h3>有参构造创建 Bean 实例</h3>
     * @param beanDefinition BeanDefinition 实例
     * @param beanName Bean 名字
     * @param owner 父容器 => 主要用于 CGLIB 生成子类使用的
     * @param constructor 构造器
     * @param args 参数
     * @return Bean 实例
     */
    Object instantiate(BeanDefinition beanDefinition, String beanName, BeanFactory owner,
                       Constructor<?> constructor, Object... args) throws BeansException;

    /**
     * <h3>工厂方法创建实例</h3>
     * <h3>注: 不实现</h3>
     * @param beanDefinition BeanDefinition 实例
     * @param beanName Bean 名字
     * @param owner 父容器 => 主要用于 CGLIB 生成子类使用的
     * @param factoryBean 工厂实例
     * @param factoryMethod 工厂方法（静态）
     * @param args 参数
     * @return Bean 实例
     */
    Object instantiate(BeanDefinition beanDefinition, String beanName, BeanFactory owner,
                       Object factoryBean, Method factoryMethod, Object... args) throws BeansException;

}
