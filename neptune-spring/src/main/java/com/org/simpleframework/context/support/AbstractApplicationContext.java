package com.org.simpleframework.context.support;

import com.org.simpleframework.beans.exception.BeansException;
import com.org.simpleframework.beans.factory.BeanFactory;
import com.org.simpleframework.beans.factory.config.BeanFactoryPostProcessor;
import com.org.simpleframework.beans.factory.config.BeanPostProcessor;
import com.org.simpleframework.beans.factory.config.ConfigurableBeanFactory;
import com.org.simpleframework.beans.factory.config.ConfigurableListableBeanFactory;
import com.org.simpleframework.context.ApplicationContext;
import com.org.simpleframework.context.ConfigurableApplicationContext;
import com.org.simpleframework.core.covert.ConversionService;
import com.org.simpleframework.core.io.DefaultResourceLoader;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <h2>抽象容器类</h2>
 * <h3>负责实现高级容器的基本方法</h3>
 */
@Slf4j
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {

    /**
     * <h3>容器启动时间</h3>
     */
    private long startupDate;

    /**
     * <h3>容器是否启动</h3>
     */
    private final AtomicBoolean active = new AtomicBoolean();

    /**
     * <h3>容器是否关闭</h3>
     */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * <h3>在初始化容器和关闭容器时上锁</h3>
     */
    private final Object startupShutdownMonitor = new Object();

    /**
     * <h3>初始化容器的核心方法</h3>
     */
    @Override
    public void refresh() throws BeansException, IllegalStateException {
        synchronized (this.startupShutdownMonitor){
            // 1. 容器启动前准备工作
            prepareRefresh();
            // 2. 获取内部容器；DefaultListableBeanFactory
            ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
            // 3. 除此添加需要使用的后置处理器
            prepareBeanFactory(beanFactory);
            // 4.为容器注册可以修改 BeanDefinition 实例的后置处理器
            postProcessBeanFactory();
            // 5. 执行容器级别的后置处理器
            invokeBeanFactoryPostProcessors(beanFactory);
            // 6. 再次添加需要使用的后置处理器
            registerBeanPostProcessor(beanFactory);
            // TODO 7. 初始化事件发布者
            initApplicationEventMulticaster();
            // TODO 8. 注册事件监听器
            registerListeners();
            // 9. 实例化所有单例且非延迟加载的 Bean 实例
            finishBeanFactoryInitialization(beanFactory);
            // 10. 发布容器刷新完成事件
            finishRefresh();
        }
    }


    /**
     * <h3>初始化容器的前置工作</h3>
     * <h3>1. 负责将容器的标志设置为激活</h3>
     * <h3>2. 设置容器的启动时间</h3>
     * <h3>3. 处理和监听器相关的事情, 没看懂是什么</h3>
     */
    protected void prepareRefresh() {
        this.startupDate = System.currentTimeMillis();
        this.active.set(true);
        this.closed.set(false);
    }

    /**
     * <h3>负责获取内部容器</h3>
     * @return 内部使用的容器
     */
    protected ConfigurableListableBeanFactory obtainFreshBeanFactory(){
        // 1. 抽象子类负责实现: 主要用于加载 BeanDefinition 实例
        refreshBeanFactory();
        // 2. 获取加载之后的内部容器
        return getBeanFactory();
    }

    /**
     * <h3>添加必要的后置处理器</h3>
     * @param beanFactory 内部容器
     */
    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // 注: 容器必要的初始类有很多, 但是这里仅实现添加 Bean 级别的后置处理器
        beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
    }

    /**
     * <h3>{@code spring 没有默认实现}</h3>
     */
    protected void postProcessBeanFactory() {

    }

    /**
     * <h3>将后置处理器实例化</h3>
     * @param beanFactory 内部容器实例
     */
    protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        // 1. 后置处理器对象会在此前被添加到注册中心, 现在需要自己配置
        Collection<BeanFactoryPostProcessor> processors = beanFactory.getBeansOfType(BeanFactoryPostProcessor.class).values();
        // 2. 遍历并执行后置处理器
        for (BeanFactoryPostProcessor processor : processors) {
            processor.postProcessBeanFactory(beanFactory);
        }
    }

    /**
     * <h3>再次添加后置额处理器</h3>
     * @param beanFactory 内部容器
     */
    protected void registerBeanPostProcessor(ConfigurableListableBeanFactory beanFactory) {
        Map<String, BeanPostProcessor> beanPostProcessorMap = beanFactory.getBeansOfType(BeanPostProcessor.class);
        for (Map.Entry<String, BeanPostProcessor> entry : beanPostProcessorMap.entrySet()) {
            log.debug(entry.getKey() + "被添加到容器中");
            BeanPostProcessor processor = entry.getValue();
            beanFactory.addBeanPostProcessor(processor);
        }
    }

    protected void initApplicationEventMulticaster() {

    }

    protected void registerListeners()
    {

    }

    protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
        // 1. 设置类型转换器
        if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME)){
            Object conversionService = beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME);
            if (conversionService instanceof ConversionService){
                beanFactory.setConversionService((ConversionService) conversionService);
            }
        }
        // 2. 实例化单例 Bean
        beanFactory.preInstanceSingleBean();
    }

    protected void finishRefresh() {

    }

    //============================================ 抽象子类负责实现 ============================================
    protected abstract void refreshBeanFactory();


    protected abstract ConfigurableListableBeanFactory getBeanFactory();

    //============================================ 直接调用内部容器的方法 ============================================

    @Override
    public void registerShutdownHook() {

    }

    @Override
    public void close()
    {

    }

    @Override
    public String getApplicationContextName()
    {
        return null;
    }

    @Override
    public ApplicationContext getParent()
    {
        return null;
    }

    @Override
    public BeanFactory getParentBeanFactory()
    {
        return null;
    }


    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) {
        return getBeanFactory().getBeansOfType(type);
    }

    @Override
    public Object getBean(String beanName) throws BeansException {
        return getBeanFactory().getBean(beanName);
    }

    @Override
    public <T> T getBean(String beanName, Class<T> requiredType) throws BeansException {
        return getBeanFactory().getBean(beanName, requiredType);
    }

    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        return getBeanFactory().getBean(requiredType);
    }

    @Override
    public boolean containsBean(String beanName) {
        return getBeanFactory().containsBean(beanName);
    }

    @Override
    public boolean isSingleton(String beanName) {
        return getBeanFactory().isSingleton(beanName);
    }

    @Override
    public boolean isPrototype(String beanName) {
        return getBeanFactory().isPrototype(beanName);
    }
}
