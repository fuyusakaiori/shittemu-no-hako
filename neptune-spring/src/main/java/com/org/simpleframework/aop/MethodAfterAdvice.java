package com.org.simpleframework.aop;


import java.lang.reflect.Method;

/**
 * <h2>方法后置通知</h2>
 */
public interface MethodAfterAdvice extends AfterAdvice {

    void after(Method method, Object target, Object... args) throws Throwable;
}
