package com.org.simpleframework.aop;

import java.lang.reflect.Method;

/**
 * <h2>方法前置通知</h2>
 */
public interface MethodBeforeAdvice extends BeforeAdvice {

    /**
     * <h3>方法前置通知</h3>
     * @param method 方法
     * @param target 目标对象
     * @param args 方法参数
     */
    void before(Method method, Object target, Object... args) throws Throwable;

}
