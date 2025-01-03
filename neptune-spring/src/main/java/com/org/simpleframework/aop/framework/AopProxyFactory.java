package com.org.simpleframework.aop.framework;

import com.org.simpleframework.aop.AdvisedSupport;

/**
 * <h2>创建 AOP 代理类的工厂</h2>
 */
public interface AopProxyFactory {

    /**
     * <h3>创建 AOP 代理类</h3>
     */
    AopProxy createAopProxy(AdvisedSupport config);

}
