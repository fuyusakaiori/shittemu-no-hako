package com.org.simpleframework.beans.factory.support;

import com.org.simpleframework.beans.exception.BeanDefinitionException;
import com.org.simpleframework.core.io.Resource;
import com.org.simpleframework.core.io.ResourceLoader;

/**
 * <h2>读取配置文件生成对应的 BeanDefinition 实例</h2>
 */
public interface BeanDefinitionReader {


    /**
     * <h3>获取 BeanDefinition 注册中心: 用于注册 BeanDefinition 实例</h3>
     * @return BeanDefinitionRegistry 实例
     */
    BeanDefinitionRegistry getBeanDefinitionRegistry();

    /**
     * <h3>获取 ResourceLoader 资源加载器: 负责加载配置文件, 并生成 Resource 实例</h3>
     */
    ResourceLoader getResourceLoader();

    /**
     * <h3>通过资源实例创建 BeanDefinition 实例</h3>
     * @return 加载的数量
     */
    int loadBeanDefinitions(Resource resource) throws BeanDefinitionException;

    int loadBeanDefinitions(Resource... resources) throws BeanDefinitionException;

    /**
     * <h3>根据文件路径创建 BeanDefinition 实例</h3>
     * <h3>注: 实际在实现的时候就是调用的上面两个方法</h3>
     * @param location 文件路径
     * @return 加载的数量
     */
    int loadBeanDefinitions(String location) throws BeanDefinitionException;

    int loadBeanDefinitions(String... locations) throws BeanDefinitionException;

}
