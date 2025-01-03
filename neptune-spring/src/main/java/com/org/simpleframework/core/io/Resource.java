package com.org.simpleframework.core.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * <h2>资源实例</h2>
 * <h3>1. FileSystemResource 负责根据文件路径读取配置文件</h3>
 * <h3>2. ClassPathResource 负责根据从用户路径读取配置文件</h3>
 * <h3>3. UrlResource 负责根据 URL 地址读取配置文件</h3>
 * <h3>4. ServletContextResource 负责根据相对路径读取配置文件, 这个是 Web 项目</h3>
 * <h3>注: 本质上资源实例就是对输入流进行的封装, 没有什么特殊的</h3>
 */
@FunctionalInterface
public interface Resource {

    /**
     * <h3>可以认为是函数式接口, 因为很多其他的方法没有必要去实现</h3>
     * @return 输入流
     */
    InputStream getInputStream() throws IOException;

}
