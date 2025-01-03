package com.org.simpleframework.aop;

import com.org.simpleframework.util.Assert;
import org.aopalliance.intercept.MethodInterceptor;

import java.util.ArrayList;
import java.util.List;

public class AdvisedSupport {

    /**
     * <h3>是否需要采用 CGLIB 动态代理</h3>
     */
    private boolean proxyTargetClass = false;

    /**
     * <h3>需要代理的对象</h3>
     */
    private TargetSource targetSource;

    /**
     * <h3>用于查找类中是否有对应的方法</h3>
     */
    private MethodMatcher methodMatcher;

    /**
     * <h3>拦截器的逻辑是要自己实现的</h3>
     */
    private MethodInterceptor methodInterceptor;

    /**
     * <h3>目标对象实现的接口</h3>
     */
    List<Class<?>> interfaces = new ArrayList<>();

    public MethodInterceptor getMethodInterceptor() {
        return methodInterceptor;
    }

    public void setMethodInterceptor(MethodInterceptor methodInterceptor) {
        this.methodInterceptor = methodInterceptor;
    }

    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }

    public void setMethodMatcher(MethodMatcher methodMatcher) {
        this.methodMatcher = methodMatcher;
    }

    public TargetSource getTargetSource() {
        return targetSource;
    }

    public void setTargetSource(TargetSource targetSource) {
        this.targetSource = targetSource;
    }

    public boolean isProxyTargetClass() {
        return proxyTargetClass;
    }

    public void setProxyTargetClass(boolean proxyTargetClass) {
        this.proxyTargetClass = proxyTargetClass;
    }

    public List<Class<?>> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(Class<?>... interfaces) {
        // 1. 清空之前接口
        this.interfaces.clear();
        // 2. 添加接口
        for (Class<?> clazz : interfaces) {
            addInterface(clazz);
        }
    }

    public void addInterface(Class<?> clazz) {
        // 1. 如果不是接口直接退出
        if (!clazz.isInterface())
            throw new IllegalArgumentException();
        // 2. 如果是就直接添加
        if (this.interfaces.contains(clazz)){
            this.interfaces.add(clazz);
        }
    }
}
