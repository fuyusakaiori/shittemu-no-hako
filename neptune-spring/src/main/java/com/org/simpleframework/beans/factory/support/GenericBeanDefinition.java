package com.org.simpleframework.beans.factory.support;

import com.org.simpleframework.beans.PropertyValues;
import com.org.simpleframework.beans.factory.config.BeanDefinition;

public class GenericBeanDefinition implements BeanDefinition {

    /**
     * <h3>用于创建对象, 也就是控制反转</h3>
     */
    private Class<?> beanClass;

    /**
     * <h3>用于注入属性, 也就是依赖注入</h3>
     */
    private PropertyValues propertyValues;

    private String scope = BeanDefinition.SCOPE_SINGLETON;

    private String initMethodName;

    private String destroyMethodName;

    private boolean isSingleton = true;

    private boolean isPrototype = false;


    @Override
    public Class<?> getBeanClass() {
        return beanClass;
    }

    @Override
    public void setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    @Override
    public boolean hasBeanClass() {
        return beanClass != null;
    }

    public PropertyValues getPropertyValues() {
        return propertyValues;
    }

    public void setPropertyValues(PropertyValues propertyValues) {
        this.propertyValues = propertyValues;
    }

    public boolean hasPropertyValues(){
        return propertyValues != null;
    }

    @Override
    public String getScope()
    {
        return scope;
    }

    @Override
    public void setScope(String scope) {
        this.scope = scope;
        this.isSingleton = BeanDefinition.SCOPE_SINGLETON.equals(scope);
        this.isPrototype = BeanDefinition.SCOPE_PROTOTYPE.equals(scope);
    }

    @Override
    public String getInitMethodName() {
        return initMethodName;
    }

    @Override
    public void setInitMethodName(String initMethodName) {
        this.initMethodName = initMethodName;
    }

    @Override
    public String getDestroyMethodName()
    {
        return destroyMethodName;
    }

    @Override
    public void setDestroyMethodName(String destroyMethodName)
    {
        this.destroyMethodName = destroyMethodName;
    }

    @Override
    public boolean isSingleton() {
        return isSingleton;
    }

    @Override
    public boolean isPrototype()
    {
        return isPrototype;
    }

    // TODO 为什么要重写 hashCode 和 equals 方法 ?
}
