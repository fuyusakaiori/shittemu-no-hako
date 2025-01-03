package com.org.simpleframework.core.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ClassPathResource implements Resource{

    private final String classpath;

    public ClassPathResource(String classpath) {
        this.classpath = classpath;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        // 注: 这里的路径不能够写成绝对路径的形式, 因为采用的是类加载器加载的方式
        InputStream inputStream = this.getClass().getClassLoader()
                                          .getResourceAsStream(classpath);
        if (inputStream == null)
            throw new FileNotFoundException();
        return inputStream;
    }
}
