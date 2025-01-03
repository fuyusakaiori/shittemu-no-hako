package com.org.simpleframework.aop.framework.adapter;

import com.org.simpleframework.aop.MethodBeforeAdvice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * <h2>前置通知拦截器</h2>
 * <h3>注: 实际上是实现的环绕通知接口, 然后调用方法前置通知拦截器, 本质还是环绕通知</h3>
 */
public class MethodBeforeAdviceInterceptor implements MethodInterceptor {

    private final MethodBeforeAdvice beforeAdvice;

    public MethodBeforeAdviceInterceptor(MethodBeforeAdvice beforeAdvice) {
        this.beforeAdvice = beforeAdvice;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 1. 执行方法前置通知
        this.beforeAdvice.before(invocation.getMethod(), invocation.getThis(), invocation.getArguments());
        // 2. 执行原始方法
        return invocation.proceed();
    }
}
