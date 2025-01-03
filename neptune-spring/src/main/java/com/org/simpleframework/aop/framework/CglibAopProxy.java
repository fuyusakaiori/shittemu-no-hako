package com.org.simpleframework.aop.framework;

import com.org.simpleframework.aop.AdvisedSupport;
import com.org.simpleframework.aop.TargetSource;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * <h3>CGLIB 代理</h3>
 * <h3>1. 通过继承目标对象然后重写方法实现的代理</h3>
 * <h3>2. 目标对象和代理对象之间是父子关系, 所以可以强制转换</h3>
 * <h3>3. 不可变类是不可以被代理的: 不能够采用继承实现</h3>
 */
public class CglibAopProxy implements AopProxy {

    private final AdvisedSupport advisedSupport;

    public CglibAopProxy(AdvisedSupport advisedSupport) {
        this.advisedSupport = advisedSupport;
    }

    /**
     * <h3>作用: 获取代理对象</h3>
     * @return 代理对象
     */
    @Override
    public Object getProxy() {
        // 1. 获取目标对象
        TargetSource target = advisedSupport.getTargetSource();
        // 2. 获取目标对象实现的接口和该类的父类: 如果没有父类, 那么就是自己
        Class<?> clazz = target.getTargetClass();
        Class<?>[] interfaces = target.getInterfaces();
        // 3. 增强的逻辑: 需要实现 MethodInterceptor, 然后在实现的方法中调用 AOP 联盟提供的 MethodInterceptor 方法增强逻辑
        MethodInterceptor callback = new DynamicAdvisedInterceptor(advisedSupport);
        // 4. 创建代理对象
        return Enhancer.create(clazz, interfaces, callback);
    }

    /**
     * <h3>执行增强逻辑和原始逻辑</h3>
     */
    private static class DynamicAdvisedInterceptor implements MethodInterceptor{

        private final AdvisedSupport advisedSupport;

        public DynamicAdvisedInterceptor(AdvisedSupport advisedSupport) {
            this.advisedSupport = advisedSupport;
        }

        @Override
        public Object intercept(Object proxy, Method method, Object[] arguments, MethodProxy methodProxy) throws Throwable {
            CglibMethodInvocation methodInvocation = new CglibMethodInvocation(advisedSupport.getTargetSource().getTarget(), method, arguments, methodProxy);
            if (advisedSupport.getMethodMatcher().matches(method, advisedSupport.getTargetSource().getTargetClass())){
                return this.advisedSupport.getMethodInterceptor().invoke(methodInvocation);
            }
            return methodProxy.invoke(advisedSupport.getTargetSource().getTarget(), arguments);
        }
    }

    /**
     * <h3>执行原始逻辑</h3>
     */
    private static class CglibMethodInvocation extends ReflectiveMethodInvocation{

        /**
         * <h3>cglib 是通过 methodProxy 执行方法的</h3>
         */
        private final MethodProxy methodProxy;

        public CglibMethodInvocation(Object target, Method method, Object[] arguments, MethodProxy methodProxy) {
            super(target, method, arguments);
            this.methodProxy = methodProxy;
        }

        /**
         * <h3>作用: 执行原始方法的逻辑</h3>
         * <h3>注: MethodProxy 不是采用反射执行方法的, Method 是采用反射执行方法的</h3>
         */
        @Override
        public Object proceed() throws Throwable {
            return this.methodProxy.invoke(this.target, this.arguments);
        }
    }
}
