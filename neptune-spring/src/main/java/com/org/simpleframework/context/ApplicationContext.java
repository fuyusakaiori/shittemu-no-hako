package com.org.simpleframework.context;

import com.org.simpleframework.beans.factory.HierarchicalBeanFactory;
import com.org.simpleframework.beans.factory.ListableBeanFactory;
import com.org.simpleframework.core.io.ResourceLoader;

/**
 * <h2>所有对外提供的高级容器都必须实现的接口</h2>
 * <h3>1. 负责对 Bean 实例的查询操作</h3>
 * <h3>2. 负责实现 XML 文件的加载: 这里不支持 Ant 风格的路径</h3>
 * <h3>注: 容器里提供的其余方法基本都是和容器自身相关的, 这里仅列出来不会做实现</h3>
 */
public interface ApplicationContext extends ListableBeanFactory, ResourceLoader, HierarchicalBeanFactory {

    /**
     * <h3>获取容器的名字</h3>
     * @return 容器名字
     */
    String getApplicationContextName();

    /**
     * <h3>获取容器的父容器</h3>
     * @return 父容器实例
     */
    ApplicationContext getParent();

}
