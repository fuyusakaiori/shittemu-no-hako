package com.org.simpleframework.beans.factory.config;

import com.org.simpleframework.beans.PropertyValues;

/**
 * <h2>描述对象的实例</h2>
 * <h3>1. 主要包含 Class 对象信息</h3>
 * <h3>2. 还包含四个额外信息: 作用域, 懒加载, 优先级, 工厂方法以及工厂对象</h3>
 */
public interface BeanDefinition {

    String SCOPE_SINGLETON = "singleton";

    String SCOPE_PROTOTYPE = "prototype";

    /**
     * <h3>获取对象的 Class 对象实例</h3>
     */
    Class<?> getBeanClass();

    /**
     * <h3>设置对象的 Class 对象</h3>
     */
    void setBeanClass(Class<?> beanClass);

    boolean hasBeanClass();

    PropertyValues getPropertyValues();

    void setPropertyValues(PropertyValues propertyValues);

    boolean hasPropertyValues();

    /**
     * <h3>获取对象的作用域</h3>
     */
    String getScope();

    /**
     * <h3>设置对象的作用域</h3>
     */
    void setScope(String scope);

    /**
     * <h3>获取对象的初始化时调用方法</h3>
     */
    String getInitMethodName();

    /**
     * <h3>设置对象初始化时调用的方法</h3>
     */
    void setInitMethodName(String initMethodName);

    /**
     * <h3>获取对象销毁时的方法</h3>
     */
    String getDestroyMethodName();

    /**
     * <h3>设置对象销毁时的方法</h3>
     */
    void setDestroyMethodName(String destroyMethodName);

    boolean isSingleton();

    boolean isPrototype();

}
