package com.org.simpleframework.beans.factory.xml;

import com.org.simpleframework.beans.factory.support.BeanDefinitionRegistry;
import org.dom4j.Document;

/**
 * <h2>负责读取文档对象然后注册 BeanDefinition 实例</h2>
 */
public interface BeanDefinitionDocumentReader {

    void registerBeanDefinition(Document document, BeanDefinitionRegistry beanDefinitionRegistry);

}
