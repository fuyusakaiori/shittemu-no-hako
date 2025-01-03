package com.org.simpleframework.context.support;

import com.org.simpleframework.beans.factory.support.BeanDefinitionReader;
import com.org.simpleframework.beans.factory.support.DefaultListableBeanFactory;
import com.org.simpleframework.beans.factory.xml.XmlBeanDefinitionReader;

public abstract class AbstractXmlApplicationContext extends AbstractRefreshableApplicationContext {

    /**
     * <h3>创建 Reader 读取 XML 文件创建 BeanDefinition 实例</h3>
     * @param beanFactory 内部容器
     */
    @Override
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
        // 1. 创建 BeanDefinitionReader 读取 XML 配置文件
        BeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory, this);
        // 2. 获取所有配置文件的路径
        String[] configLocations = getDefaultConfigLocations();
        // 3. 根据配置文件路径加载资源
        if (configLocations != null){
            beanDefinitionReader.loadBeanDefinitions(configLocations);
        }
    }

    /**
     * <h3>每个容器子类负责实现</h3>
     * @return 配置文件路径数组
     */
    protected abstract String[] getDefaultConfigLocations();


}
