package com.org.simpleframework.beans.factory.config;


import com.org.simpleframework.beans.factory.ListableBeanFactory;

/**
 * <h3>扩展容器能力</h3>
 * <h3>1. 可以根据类型获取所有同类型的实例对象</h3>
 * <h3>2. 可以调用前置处理和后置处理两个处理方法</h3>
 * <h3>3. 可以为容器进行初始化操作</h3>
 * <h3>4. 扩展: 为容器添加容器级别的后置处理器</h3>
 */
public interface ConfigurableListableBeanFactory extends ListableBeanFactory, AutowireCapableBeanFactory, ConfigurableBeanFactory {

    /**
     * <h3>实例化所有单例 Bean</h3>
     */
    void preInstanceSingleBean();

}
