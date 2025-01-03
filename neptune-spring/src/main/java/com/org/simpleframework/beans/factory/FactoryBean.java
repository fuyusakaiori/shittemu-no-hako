package com.org.simpleframework.beans.factory;

public interface FactoryBean<T> {

    /**
     * <h3>工厂对象创建原始对象</h3>
     */
    T getObject() throws Exception;

    /**
     * <h3>工厂对象创建的原始对象的类型</h3>
     */
    Class<T> getObjectType();

    /**
     * <h3>被这个工厂管理的原始对象是否是单例</h3>
     */
    default boolean isSingleton(){return true;}
}
