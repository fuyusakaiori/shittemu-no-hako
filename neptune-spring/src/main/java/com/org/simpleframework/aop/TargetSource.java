package com.org.simpleframework.aop;

/**
 * <h2>被代理的对象</h2>
 */
public class TargetSource {

    private final Object target;

    public TargetSource(Object target) {
        this.target = target;
    }

    public Class<?> getTargetClass(){
        return this.target.getClass();
    }

    public Class<?>[] getInterfaces(){
        return this.getTargetClass().getInterfaces();
    }

    public Object getTarget() {
        return target;
    }
}
