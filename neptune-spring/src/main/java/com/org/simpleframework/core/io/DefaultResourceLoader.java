package com.org.simpleframework.core.io;

import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * <h2>默认的资源实例加载器</h2>
 */
@Slf4j
public class DefaultResourceLoader implements ResourceLoader {

    /**
     * <h3>貌似里面没有使用到采用文件路径加载的方式</h3>
     * @param location XML 文件路径
     */
    @Override
    public Resource getResource(String location) {
        if (location.startsWith("/")){
            return getResourceByPath(location);
        }else if (location.startsWith(CLASSPATH_URL_PREFIX)){
            // 注: 记得把前缀删了
            return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()));
        }else{
            try {
                return new UrlResource(new URL(location));
            } catch (MalformedURLException e) {
                return getResourceByPath(location);
            }
        }
    }

    /**
     * <h3>1. 如果传入的路径不满足现有的格式, 可以调用这个方法</h3>
     * <h3>2. 子类可以根据自己需要重写这个方法, 从而按照其他格式的路径加载配置文件</h3>
     * @param location 文件路径
     * @return Resource 实例
     */
    protected Resource getResourceByPath(String location) {
        return new FileSystemResource(location);
    }
}
