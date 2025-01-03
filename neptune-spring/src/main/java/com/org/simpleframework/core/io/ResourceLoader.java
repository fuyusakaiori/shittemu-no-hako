package com.org.simpleframework.core.io;

/**
 * <h2>负责读取资源实例</h2>
 * <h3>注: 根据路径格式的不同创建对应的 Resource 实例</h3>
 */
public interface ResourceLoader {

    String CLASSPATH_URL_PREFIX = "classpath:";

    /**
     * <h3>传入文件路径后解析成资源实例</h3>
     * @param location XML 文件路径
     * @return 资源实例
     */
    Resource getResource(String location);

}
