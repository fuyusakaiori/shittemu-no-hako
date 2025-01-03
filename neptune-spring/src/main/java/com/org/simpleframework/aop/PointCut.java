package com.org.simpleframework.aop;

/**
 * <h2>匹配一组需要增强的方法</h2>
 */
public interface PointCut {

    ClassFilter getClassFilter();

    MethodMatcher getMethodMatcher();

}
