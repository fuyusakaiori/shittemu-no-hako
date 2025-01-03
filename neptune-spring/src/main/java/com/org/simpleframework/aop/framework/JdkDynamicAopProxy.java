package com.org.simpleframework.aop.framework;

import com.org.simpleframework.aop.AdvisedSupport;
import com.org.simpleframework.aop.TargetSource;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * <h2>JDK 代理</h2>
 * <h3>1. 只能够针对接口代理, 重新实现目标对象接口中的方法</h3>
 * <h3>2. 目标对象和代理对象实现相同的接口, 肯定不能够相互强制转换</h3>
 * <h3>3. 允许目标对象为不可变对象</h3>
 */
@Slf4j
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {

    /**
     * <h3>作用: 辅助类</h3>
     * <h3>1. 保存目标对象</h3>
     * <h3>2. 保存通知, 也就是增强逻辑</h3>
     */
    private final AdvisedSupport advised;

    public JdkDynamicAopProxy(AdvisedSupport advised) {
        this.advised = advised;
    }

    /**
     * <h3>作用: 生成代理对象</h3>
     * @return 代理对象
     */
    @Override
    public Object getProxy() {
        // 1. 获取类加载器: 代理类会在运行过程中动态地生成字节码, 然后这个字节码肯定需要通过类加载器加载进入, 所以需要获取类加载器
        ClassLoader classLoader = this.getClass().getClassLoader();
        // 2. 获取目标对象实现的接口: 代理对象也需要实现相应的接口, 然后在原有实现的基础上添加增强逻辑, 所以是肯定需要目标对象实现的所有接口的
        Class<?>[] interfaces = advised.getTargetSource().getInterfaces();
        // 3. 编写增强的逻辑: 增强逻辑通过 MethodInterceptor 实现, 需要调用这个接口中的方法提供增强逻辑, 然后再调用原始的方法逻辑, 最终实现增强
        Object proxy = Proxy.newProxyInstance(classLoader, interfaces, this);
        // 4. 最终动态生成代理对象
        log.debug("proxy dynamic...");
        // 5. 返回代理对象供使用者调用方法
        return proxy;
    }

    /**
     * <h3>作用: 执行增强逻辑和原始逻辑</h3>
     * <h3>注: 先实现 InvocationHandler 接口, 然后在实现的方法中调用 MethodInterceptor 接口提供的方法</h3>
     * @param proxy 代理对象
     * @param method 增强的方法
     * @param args 方法的参数
     * @return 方法的返回结果
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 0. 目标对象
        TargetSource target = this.advised.getTargetSource();
        // 1. 如果这个方法是需要增强的方法, 那么就需要执行增强逻辑
        if (advised.getMethodMatcher().matches(method, target.getTargetClass())){
            // 2. 获取通知或者说增强逻辑
            MethodInterceptor interceptor = advised.getMethodInterceptor();
            // 3. 执行增强逻辑和原始逻辑: (1) MethodInterceptor.invoke => (2) Method.invoke
            return interceptor.invoke(new ReflectiveMethodInvocation(target.getTarget(), method, args));
            // TODO 暂时不了解为什么要传入 MethodInvocation
        }
        // 4. 如果不是要增强逻辑的方法, 那么直接执行就可以
        return method.invoke(target.getTarget(), args);
    }
}
