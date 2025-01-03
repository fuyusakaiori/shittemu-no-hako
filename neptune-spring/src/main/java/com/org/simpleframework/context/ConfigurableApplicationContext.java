package com.org.simpleframework.context;

import com.org.simpleframework.beans.exception.BeansException;

import java.io.Closeable;

/**
 * <h2>提供初始化容器的方法</h2>
 */
public interface ConfigurableApplicationContext extends ApplicationContext, Closeable {

    String CONVERSION_SERVICE_BEAN_NAME = "conversionService";

    /**
     * <h3>启动容器: 核心方法</h3>
     */
    void refresh() throws BeansException, IllegalStateException;

    /**
     * <h3>容器关闭前执行的方法: 钩子方法</h3>
     */
    void registerShutdownHook();

    /**
     * <h3>关闭容器</h3>
     */
    void close();

}
