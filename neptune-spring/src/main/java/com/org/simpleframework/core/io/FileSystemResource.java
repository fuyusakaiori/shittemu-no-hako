package com.org.simpleframework.core.io;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * <h3>通过文件的绝对路径定位配置文件所在的位置</h3>
 */
public class FileSystemResource implements Resource{

    /**
     * <h3>配置文件路径</h3>
     */
    private final String path;

    /**
     * <h3>配置文件对象</h3>
     */
    private final File file;

    /**
     * <h3>注: JDK 7 之后提供的工具类</h3>
     */
    private final Path filePath;

    /**
     * <h3>初始化方法</h3>
     */
    public FileSystemResource(String path) {
        this.path = path;
        this.file = new File(path);
        this.filePath = this.file.toPath();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            return Files.newInputStream(this.filePath);
        }
        catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    public String getPath()
    {
        return path;
    }

    public File getFile()
    {
        return file;
    }

    public Path getFilePath()
    {
        return filePath;
    }
}
