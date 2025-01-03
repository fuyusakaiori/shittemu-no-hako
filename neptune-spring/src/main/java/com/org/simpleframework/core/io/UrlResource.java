package com.org.simpleframework.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * <h2>URL 资源实例</h2>
 * <h3>注: 根据 url 读取配置文件</h3>
 */
public class UrlResource implements Resource{

    /**
     * <h3>保存资源的 URL 地址</h3>
     */
    private final URL url;

    public UrlResource(URL url) {
        this.url = url;
    }

    /**
     * <h3>根据 url 路径将资源读取进来封装成输入流</h3>
     * @return 输入流
     */
    @Override
    public InputStream getInputStream() throws IOException {
        // 1. 创建远程连接对象: 注意这里没有真的建立连接, 只是建立了连接的对象
        URLConnection connection = this.url.openConnection();
        // 2. 开始建立连接, 然后可以可以从远程连接获取相应的数据
        try {
            return connection.getInputStream();
        }
        catch (IOException e) {
            throw new IOException(e);
        }
    }
}
