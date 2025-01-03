package com.org.simpleframework.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * <h3>负责提取解析字节码文件加载实体类</h3>
 */
@Slf4j
@SuppressWarnings("unchecked")
public class ClassUtil {

    private static final String FILE_PROTOCOL = "file";

    /**
     * <h3>作用: 传递包名 => 扫描包下的所有实体类然后加载进内存</h3>
     * @param packageName 包名
     */
    public static Set<Class<?>> extractPackage(String packageName){
        Set<Class<?>> set = new HashSet<>();
        // 1. 获取相应的类加载器
        ClassLoader classLoader = getClassLoader();
        // 2. 调用类加载器获取包所在的路径
        // 注: 这里不能使用原始包名去查找资源
        URL url = classLoader.getResource(packageName.replace(".", "/"));
        // 注: 判断路径是否为空, 如果路径为空, 那么就不要继续向下处理
        if (url == null){
            log.error("传入的包名不存在！");
            throw new RuntimeException("传入的包名不存在!");
        }
        // 3. 根据路径的协议判断提取哪种类型的文件: 这里暂时只需要提取字节码文件
        if (url.getProtocol().equalsIgnoreCase(FILE_PROTOCOL)){
            // 3.1 获取包所在的路径
            File packageDirectory = new File(url.getPath());
            // 3.2 扫描包所在的路径, 然后提取相应的字节码文件, 并加载
            extractClassFile(set, packageDirectory, packageName);

        }else{
            // TODO 如果以后路径的协议是其他类型的, 那么也可以在这里进行添加处理
        }
        return set;
    }

    /**
     * <h3>获取类加载器: 会利用双亲委派机制去加载类</h3>
     * @return 类加载器
     */
    public static ClassLoader getClassLoader(){
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * <h3>作用: 负责扫描包下的所有字节码文件, 然后将其加载到内存中</h3>
     * <h3>注: 传入的包下可能还有其他包, 所以需要递归调用</h3>
     * @param clazzSet Class 对象集合
     * @param source 包文件夹
     * @param packageName 原始包名
     */
    public static void extractClassFile(Set<Class<?>> clazzSet, File source, String packageName){
        // 1. 如果不是文件夹, 那么就直接返回
        if (!source.isDirectory()) return;

        // 2. 如果是文件夹, 那么就列出所有的文件夹, 然后执行递归调用, 非文件夹的就添加进入集合中
        File[] files = source.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                // 如果不是目录, 那么就判断该文件是否为字节码文件, 如果是字节码文件, 就添加进入集合
                if (!file.isDirectory()){
                    String path = file.getAbsolutePath();
                    log.debug("文件的绝对路径: {}", path);
                    // 如果是字节码文件才进行处理
                    if (path.endsWith(".class")){
                        clazzSet.add(analyzeClassName(path));
                    }
                }
                // 如果是目录, 那么就直接递归调用 => 深度遍历
                return true;
            }

            /**
             * <h3>根据字符串解析类的名字</h3>
             * @param path 类所在的路径
             * @return Class 对象
             */
            private Class<?> analyzeClassName(String path){
                // 0. 处理路径: 现在的路径是带有 "/" 或者 "\" 的, 需要转换为相应的 "."
                // 注: 不同的操作系统文件路径不同, 需要统一处理
                path = path.replace(File.separator, ".");
                // 1. 从绝对路径中提取包含包名的路径
                String classFilePath = path.substring(path.indexOf(packageName));
                // 2. 从路径总提取出类的全限定名
                String className = classFilePath.substring(0, classFilePath.lastIndexOf("."));
                // 3. 根据全限定名生成相应的 Class 对象
                return loadClass(className);
            }
        });

        // 3. 每个目录继续递归调用, 扫描子目录下的所有包
        if (files != null){
            for (File file : files) {
                extractClassFile(clazzSet, file, packageName);
            }
        }

    }

    /**
     * <h3>根据类名生成相应的 Class 对象</h3>
     * @param className 类名
     * @return Class 对象
     */
    public static Class<?> loadClass(String className){
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
        }
        catch (ClassNotFoundException e) {
            log.error(e.getMessage());
        }
        return clazz;
    }

    /**
     * <h3>根据 Class 对象创建实例对象</h3>
     * @param clazz Class 对象
     * @param flag 是否要创建构造方法私有的实例对象
     * @param <T> 返回的对象类型
     * @return 实例对象
     */
    public static <T> T getInstance(Class<?> clazz, boolean flag){
        T t = null;
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(flag);
            // 注: 可能强制转换失败
            t = (T) constructor.newInstance();
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        return t;
    }

    /**
     * <h3>将字段对应的实例对象设置到需要注入实例的对象中</h3>
     * @param field 字段
     * @param targetBean 依赖注入的对象
     * @param fieldBean 需要依赖注入的对象
     * @param flag 是否允许注入访问权限私有的对象
     */
    public static void setFiledBean(Field field, Object targetBean, Object fieldBean, boolean flag){
        // 设置允许访问权限私有的成员变量
        field.setAccessible(flag);
        // 向 targetBean 中的这个 field 成员变量设置 fieldBean 实例
        try {
            field.set(targetBean, fieldBean);
        }
        catch (IllegalAccessException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }


}
