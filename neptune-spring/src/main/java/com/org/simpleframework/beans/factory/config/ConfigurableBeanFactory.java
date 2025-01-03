package com.org.simpleframework.beans.factory.config;

import com.org.simpleframework.beans.factory.BeanFactory;
import com.org.simpleframework.beans.factory.HierarchicalBeanFactory;
import com.org.simpleframework.core.covert.ConversionService;
import com.org.simpleframework.util.StringValueResolver;

/**
 * <h3>提供容器的初始化的相关方法</h3>
 * <h3>1. 容器会在 refresh 方法中调用 prepareBeanFactory 方法</h3>
 * <h3>2. prepareBeanFactory 方法中会让容器类调用这个接口中的方法进行初始化</h3>
 * <h3>注: 这个接口继承两个接口后主要负责容器的初始化配置, 以及提供单例对象的管理两大功能</h3>
 */
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory , SingletonBeanRegistry{

    /**
     * <h3>为当前容器设置父类容器</h3>
     * @param parentBeanFactory 父类容器
     */
    void setParentBeanFactory(BeanFactory parentBeanFactory);

    /**
     * <h3>设置后置处理器</h3>
     */
    void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);

    /**
     * <h3>设置 ${...} 键值对解析器</h3>
     */
    void addEmbeddedValueResolver(StringValueResolver resolver);

    boolean hasEmbeddedValueResolver();

    /**
     * <h3>设置类型转换器</h3>
     */
    void setConversionService(ConversionService conversionService);

    /**
     * <h3>获取类型转换器</h3>
     */
    ConversionService getConversionService();

}
