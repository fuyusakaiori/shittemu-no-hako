package com.org.simpleframework.context.support;

import com.org.simpleframework.beans.exception.ApplicationContextException;
import com.org.simpleframework.beans.factory.config.ConfigurableBeanFactory;
import com.org.simpleframework.beans.factory.config.ConfigurableListableBeanFactory;
import com.org.simpleframework.beans.factory.support.DefaultListableBeanFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {

    /**
     * <h3>对内部容器内进行上锁</h3>
     */
    private final Object beanFactoryMonitor = new Object();

    /**
     * <h3>内部容器对象</h3>
     */
    private DefaultListableBeanFactory beanFactory;


    /**
     * <h3>1. 创建内部容器</h3>
     * <h3>2. 初始化 BeanDefinition 实例</h3>
     */
    @Override
    protected void refreshBeanFactory() {
        // 1. 如果容器已经关闭, 那么需要销毁所有 Bean 实例
        if(hasBeanFactory()){
            // TODO 执行结束前的操作
            log.error("容器启动失败");
        }
        // 2. 初始化内部容器
        try {
            // 2.1 获取内部容器
            DefaultListableBeanFactory beanFactory = createBeanFactory();
            // 2.2 将 BeanDefinition 实例加载到内部容器中
            loadBeanDefinitions(beanFactory);
            // 2.3 初始化成员变量
            synchronized (this.beanFactoryMonitor){
                this.beanFactory = beanFactory;
            }
        }
        catch (Exception e) {
            throw new ApplicationContextException(e);
        }
    }


    @Override
    protected final ConfigurableListableBeanFactory getBeanFactory() {
        return this.beanFactory;
    }

    /**
     * <h3>获取内部容器</h3>
     * <h3>注: 为什么不是采用单例模式创建</h3>
     * @return 内部容器
     */
    protected final DefaultListableBeanFactory createBeanFactory(){
        return new DefaultListableBeanFactory();
    }

    /**
     * <h3>容器是否正在运行, 如果没有运行就会关闭</h3>
     */
    protected final boolean hasBeanFactory(){
        synchronized (this.beanFactoryMonitor){
            return this.beanFactory != null;
        }
    }

    /**
     * <h3>抽象子类中实现: 负责调用相应的 Reader 读取 XML 文件创建 BeanDefinition 实例</h3>
     * @param beanFactory 内部容器
     */
    protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory);
}
