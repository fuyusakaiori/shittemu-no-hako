package com.org.simpleframework.aop;

import java.lang.reflect.Method;

/**
 * <h3>需要织入的方法</h3>
 */
public interface MethodMatcher {

    boolean matches(Method method, Class<?> clazz);

}
