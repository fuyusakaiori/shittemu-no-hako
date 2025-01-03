package com.org.simpleframework.aop.framework.adapter;

import com.org.simpleframework.aop.MethodAfterAdvice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class MethodAfterAdviceInterceptor implements MethodInterceptor {

    private final MethodAfterAdvice afterAdvice;

    public MethodAfterAdviceInterceptor(MethodAfterAdvice afterAdvice) {
        this.afterAdvice = afterAdvice;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object result = invocation.proceed();
        this.afterAdvice.after(invocation.getMethod(), invocation.getThis(), invocation.getArguments());
        return result;
    }
}
