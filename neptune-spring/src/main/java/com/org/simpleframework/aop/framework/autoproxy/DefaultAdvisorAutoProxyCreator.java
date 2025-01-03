package com.org.simpleframework.aop.framework.autoproxy;


import cn.hutool.core.collection.ConcurrentHashSet;
import com.org.simpleframework.aop.*;
import com.org.simpleframework.aop.aspectj.AspectJExpressionPointCutAdvisor;
import com.org.simpleframework.aop.framework.DefaultAopProxyFactory;
import com.org.simpleframework.beans.PropertyValues;
import com.org.simpleframework.beans.exception.BeansException;
import com.org.simpleframework.beans.factory.BeanFactory;
import com.org.simpleframework.beans.factory.BeanFactoryAware;
import com.org.simpleframework.beans.factory.config.ConfigurableListableBeanFactory;
import com.org.simpleframework.beans.factory.config.SmartInstantiationBeanPostProcessor;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h2>负责获取所有低级切面</h2>
 * <h3>注: 需要在创建实例的过程被调用, 就需要实现后置处理器接口</h3>
 */
public class DefaultAdvisorAutoProxyCreator implements BeanFactoryAware, SmartInstantiationBeanPostProcessor {

    private ConfigurableListableBeanFactory beanFactory;

    private final Set<Object> earlyProxyReferences = new ConcurrentHashSet<>();

    /**
     * <h3>负责获取所有的低级切面, 然后创建相应的代理类</h3>
     * @return 代理类
     */
    protected Object wrapIfNecessary(Object bean, String beanName){
        // TODO
        if(isInfrastructureClass(bean.getClass()))
            return bean;

        // 1. 获取容器进而获取所有的切面对象
        Collection<AspectJExpressionPointCutAdvisor> advisors = beanFactory.getBeansOfType(AspectJExpressionPointCutAdvisor.class).values();
        for (AspectJExpressionPointCutAdvisor advisor : advisors) {
            ClassFilter classFilter = advisor.getPointCut().getClassFilter();
            if (classFilter.matches(bean.getClass())){
                TargetSource target = new TargetSource(bean);
                MethodMatcher methodMatcher = advisor.getPointCut().getMethodMatcher();
                Advice advice = advisor.getAdvice();
                AdvisedSupport advisorSupport = new AdvisedSupport();
                advisorSupport.setTargetSource(target);
                advisorSupport.setMethodMatcher(methodMatcher);
                if (advice instanceof MethodInterceptor)
                    advisorSupport.setMethodInterceptor((MethodInterceptor) advice);
                return new DefaultAopProxyFactory(advisorSupport).getProxy();
            }
        }
        return bean;
    }

    /**
     * <h3>代理的目标对象不可以是通知, 切点, 切面的实现类</h3>
     */
    private boolean isInfrastructureClass(Class<?> clazz) {
        return Advice.class.isAssignableFrom(clazz)
                       || Advisor.class.isAssignableFrom(clazz)
                       || PointCut.class.isAssignableFrom(clazz);

    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory)){
            throw new IllegalArgumentException();
        }
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    /**
     * <h3>提前暴露引用时调用: 提前动态代理</h3>
     * @param beanName Bean 名字
     * @param bean Bean 实例
     * @return 代理对象
     */
    @Override
    public Object getEarlyBeanReference(String beanName, Object bean) throws BeansException {
        // 1. 记录被代理的对象
        earlyProxyReferences.add(beanName);
        // 2. 创建代理类
        return wrapIfNecessary(bean, beanName);
    }


    @Override
    public Object postProcessBeforeInitialization(String beanName, Object bean) {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(String beanName, Object bean) {
        // 1. 如果集合中没有当前的目标对象, 那么就是可以代理的
        synchronized (this.earlyProxyReferences){
            if (!this.earlyProxyReferences.contains(beanName)){
                return wrapIfNecessary(bean, beanName);
            }
        }
        // 2. 如果集合中已经有需要代理的目标对象, 那么就直接返回
        return bean;
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        return null;
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException
    {
        return true;
    }

    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues propertyValues, Object bean, String beanName) throws BeansException {
        return propertyValues;
    }
}
