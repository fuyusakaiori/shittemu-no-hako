package com.org.simpleframework.context.support;

import com.org.simpleframework.beans.factory.config.BeanPostProcessor;
import com.org.simpleframework.context.ApplicationContextAware;
import com.org.simpleframework.context.ConfigurableApplicationContext;

public class ApplicationContextAwareProcessor implements BeanPostProcessor {

    private final ConfigurableApplicationContext applicationContext;

    public ApplicationContextAwareProcessor(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * <h3>如果 Bean 实现了任何的 Aware 接口</h3>
     * <h3>这个后置处理器就会将相应的内容设置到对象中</h3>
     * @param beanName Bean 名字
     * @param bean Bean 实例
     * @return Bean 实例
     */
    @Override
    public Object postProcessBeforeInitialization(String beanName, Object bean) {
        if (bean instanceof ApplicationContextAware){
            ((ApplicationContextAware) bean).setApplicationContext(applicationContext);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(String beanName, Object bean) {
        return bean;
    }
}
