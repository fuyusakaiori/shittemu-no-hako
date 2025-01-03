package com.org.simpleframework.aop.framework;

import com.org.simpleframework.aop.AdvisedSupport;

/**
 * <h2>默认创建代理类的工厂</h2>
 * <h3>注: 这里和 Spring 实现有些区别</h3>
 */
public class DefaultAopProxyFactory implements AopProxyFactory {

    private final AdvisedSupport advisedSupport;

    public DefaultAopProxyFactory(AdvisedSupport advisedSupport) {
        this.advisedSupport = advisedSupport;
    }

    public Object getProxy(){
        return createAopProxy(advisedSupport).getProxy();
    }

    @Override
    public AopProxy createAopProxy(AdvisedSupport config) {
        if (config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)){
            // 1. 如果目标对象本身是接口, 那么也采用 JDK 代理
            if (config.getTargetSource().getTargetClass().isInterface())
                return new JdkDynamicAopProxy(config);
            // 2. 否则就采用 CGLIB 代理
            return new CglibAopProxy(config);
        }else{
            return new JdkDynamicAopProxy(config);
        }
    }

    private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
        return config.getInterfaces().size() == 0;
    }
}
