package com.org.simpleframework.beans.factory.support;


import com.org.simpleframework.beans.exception.BeanDefinitionException;
import com.org.simpleframework.core.io.Resource;
import com.org.simpleframework.core.io.ResourceLoader;
import com.org.simpleframework.util.Assert;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractBeanDefinitionReader
        implements BeanDefinitionReader
{
    /**
     * <h3>注册中心是唯一的, 不可以变</h3>
     */
    private final BeanDefinitionRegistry registry;

    /**
     * <h3>资源加载器不是唯一的, 不可以变</h3>
     */
    private ResourceLoader resourceLoader;

    public AbstractBeanDefinitionReader(BeanDefinitionRegistry registry, ResourceLoader resourceLoader) {
        this.registry = registry;
        this.resourceLoader = resourceLoader;
    }

    public BeanDefinitionRegistry getBeanDefinitionRegistry() {
        return this.registry;
    }

    @Override
    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    //=============================================== 加载资源 ===============================================

    @Override
    public int loadBeanDefinitions(Resource... resources) throws BeanDefinitionException {
        Assert.notNull(resources);
        int count = 0;
        for (Resource resource : resources) {
            count += loadBeanDefinitions(resource);
        }
        return count;
    }

    @Override
    public int loadBeanDefinitions(String location) throws BeanDefinitionException {
        ResourceLoader resourceLoader = getResourceLoader();
        Resource resource = resourceLoader.getResource(location);
        return loadBeanDefinitions(resource);
    }

    @Override
    public int loadBeanDefinitions(String... locations) throws BeanDefinitionException {
        Assert.notNull(locations);
        int count = 0;
        for (String location : locations) {
            count += loadBeanDefinitions(location);
        }
        return count;
    }

    // 注: 每次加载单个资源实例的方法不由抽象类实现, 由每个子类单独实现, 因为只有这个方法才是真正的实现加载逻辑的方法
}
