package com.org.simpleframework.aop;

/**
 * <h2>需要织入的类</h2>
 */
public interface ClassFilter {

    boolean matches(Class<?> clazz);

}
