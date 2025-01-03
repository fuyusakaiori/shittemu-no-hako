package com.org.simpleframework.beans.factory.support;

import cn.hutool.core.bean.BeanException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.TypeUtil;
import com.org.simpleframework.beans.PropertyValue;
import com.org.simpleframework.beans.PropertyValues;
import com.org.simpleframework.beans.exception.BeansException;
import com.org.simpleframework.beans.factory.Aware;
import com.org.simpleframework.beans.factory.BeanFactoryAware;
import com.org.simpleframework.beans.factory.BeanNameAware;
import com.org.simpleframework.beans.factory.ObjectFactory;
import com.org.simpleframework.beans.factory.config.*;
import com.org.simpleframework.core.covert.ConversionService;

import java.lang.reflect.Type;

/**
 * <h2>实现类的创建</h2>
 * <h3>1. 负责空属性的 Bean 实例创建</h3>
 * <h3>2. 负责单例 Bean 的属性注入</h3>
 * <h3>注: 基本可以认为 Bean 的实例化和初始化都是在这个阶段完成的</h3>
 */
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements AutowireCapableBeanFactory {

    /**
     * <h3>{@code spring} 本身默认采用的是 cglib 实例策略</h3>
     */
    private InstantiationStrategy instantiationStrategy = new SimpleInstantiationStrategy();

    //================================================ 创建实例 ================================================
    @Override
    protected Object createBean(String beanName, BeanDefinition beanDefinition) {
        // 1. 获取 Bean 实例对应的 Class 对象

        // 2. 创建副本设置 Class 对象

        // 注: 方法重写不去实现 (look-up replace-method)

        // 3. 如果对象存在代理类, 那么直接返回代理类, 也就是给 AOP 机会
        Object beanProxy = resolveBeforeInstantiation(beanName, beanDefinition);
        // 4. 如果代理不为空, 那么就直接返回代理类, 不再去创建实例
        if (beanProxy != null)
            return beanProxy;

        return doCreateBean(beanName, beanDefinition);
    }

    /**
     * <h3>负责在对象实例化前执行代理</h3>
     * <h3>1. 本质上就是调用实例化之前需要执行的方法, 进行代理</h3>
     * <h3>2. 如果决定要去代理对象, 那么就会返回相应的对象, 如果不想代理对象, 那么就会返回为空</h3>
     * <h3>3. 问题: 为什么在代理完成后仅调用后置处理的处理器呢? 前置处理的为什么不调用?</h3>
     * @param beanName Bean 名字
     * @param beanDefinition BeanDefinition 实例
     */
    protected Object resolveBeforeInstantiation(String beanName, BeanDefinition beanDefinition) {
        Object proxyBean = applyBeanPostProcessorBeforeInstantiation(beanName, beanDefinition.getBeanClass());
        if (proxyBean != null){
            // 注: 为什么只调用单个后置处理器呢? 为什么不调用前置处理的后置处理器?
            proxyBean = applyBeanPostProcessorAfterInitialization(proxyBean, beanName);
        }
        return proxyBean;
    }

    protected Object applyBeanPostProcessorBeforeInstantiation(String beanName, Class<?> clazz){
        for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
            if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor){
                Object proxyBean = ((InstantiationAwareBeanPostProcessor) beanPostProcessor).postProcessBeforeInstantiation(clazz, beanName);
                if (proxyBean != null)
                    return proxyBean;
            }
        }
        return null;
    }

    /**
     * <h3>真正创建 Bean 实例的方法</h3>
     * <h3>注: 源码中这里采用包装类对 Bean 实例进行了包装, 这里就不这么实现, 不重要</h3>
     * <h3>注: 源码中设计工厂实例缓存, 这个不太明白到底起什么作用, 之后再说</h3>
     * @param beanName Bean 实例
     * @param beanDefinition BeanDefinition 实例
     * @return Bean 实例
     */
    private Object doCreateBean(String beanName, BeanDefinition beanDefinition) {
        Object bean = null;

        // 1. 创建 Bean 实例
        bean = createBeanInstance(beanName, beanDefinition);
        // ================================ 对象实例化结束 ================================

        // 2. 解决循环依赖: 提前将没有设置属性的对象放入二级缓存中, 实际放入一级缓存中就能解决
        // ================================ 处理循环依赖 ================================
        if (beanDefinition.isSingleton()){
            final Object finalBean = bean;
            addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, beanDefinition, finalBean));
        }

        boolean continueWithPropertyPopulation = applyBeanPostProcessorAfterInstantiation(beanName, bean);
        if (!continueWithPropertyPopulation)
            return bean;
        // 3. 给 Bean 实例注入属性: 源码中是在注入属性的方法中去进行后置处理的, 而提出来也是可以的
        try {
            // 4. 注入属性之前, 修改属性, 或者叫包装属性, 源码中也被包装到注入属性的方法中了
            populateBean(beanName, beanDefinition, bean);
            bean = initializationBean(bean, beanName, beanDefinition);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        // TODO 后面看不懂在干嘛

        return bean;
    }

    /**
     * <h3>负责创建 Bean 实例</h3>
     * @param beanName Bean 名字
     * @param beanDefinition BeanDefinition 实例
     * @return Bean 实例
     */
    protected Object createBeanInstance(String beanName, BeanDefinition beanDefinition) {
        // TODO 1. 这里首先会再次尝试获取 Class 对象, 然后进行权限校验

        // TODO 2. 检测是否有函数式接口（Supplier）, 如果有, 那么直接调用这个接口创建

        // TODO 3. 检测是否有工厂实例, 如果有直接调用工厂创建实例

        // 4. 直接调用无参构造函数去创建
        return getInstantiationStrategy().instantiate(beanDefinition, beanName, getParentBeanFactory());
    }

    /**
     * <h3>对提前暴露的引用进行动态代理</h3>
     * <h3>1. 如果循环依赖和动态代理同时存在的话, 直接将目标对象放入缓存是有问题的</h3>
     * <h3>2. 循环依赖的对象从缓存中获取到的对象是没有代理的对象, 确实可以解决循环依赖, 但是拿到的对象可能存在问题</h3>
     * <h3>3. 因为原来的对象是会在执行初始化方法之后进行动态代理的, 也就造成缓存中的对象和实际的对象不一致, 所以放入缓存前需要动态代理</h3>
     * @param beanName Bean 名字
     * @param beanDefinition BeanDefinition 实例
     * @param finalBean 半成品
     * @return 半成品
     */
    protected Object getEarlyBeanReference(String beanName, BeanDefinition beanDefinition, Object finalBean) {
        Object earlyBean = finalBean;
        // 注: 假装调用后置处理器进行处理, 实际只会直接返回此前传入的半成品对象
        for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
            // 注: 因为半完成品对象已经被创建, 所以执行的是初始化相关的后置处理器
            if (beanPostProcessor instanceof SmartInstantiationBeanPostProcessor){
                // 注: 不是仅调用一个后置处理器处理, 而是所有存在的后置处理器都要调用
                earlyBean = ((SmartInstantiationBeanPostProcessor) beanPostProcessor).getEarlyBeanReference(beanName, finalBean);
            }
        }
        return earlyBean;
    }

    /**
     * <h3>Bean 实例化之后执行的方法</h3>
     * @param beanName Bean 名字
     * @param bean Bean 实例
     * @return 是否继续处理
     */
    protected boolean applyBeanPostProcessorAfterInstantiation(String beanName, Object bean){
        boolean continueWithPropertyPopulation = true;
        for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
            if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor){
                // 注: 如果在包装之后, 不打算继续处理, 那么就退出
                if (!((InstantiationAwareBeanPostProcessor) beanPostProcessor).postProcessAfterInstantiation(bean, beanName)){
                    continueWithPropertyPopulation = false;
                    break;
                }
            }
        }
        return continueWithPropertyPopulation;
    }

    /**
     * <h3>Bean 注入属性之前允许提前修改属性</h3>
     * @param beanName Bean 名字
     * @param bean Bean 实例
     * @param beanDefinition BeanDefinition 实例
     */
    protected void applyBeanPostProcessorBeforeApplyingPropertyValues(String beanName, Object bean, BeanDefinition beanDefinition) {
        for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
            if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor){
                PropertyValues propertyValues = ((InstantiationAwareBeanPostProcessor) beanPostProcessor)
                                                        .postProcessPropertyValues(beanDefinition.getPropertyValues(), bean, beanName);
                // TODO 注: 需要保留原来的属性值?
                if (propertyValues != null){
                    propertyValues.forEach(propertyValue -> beanDefinition.getPropertyValues().addPropertyValue(propertyValue));
                }
            }
        }
    }


    /**
     * <h3>负责属性注入</h3>
     * <h3>注: 这里仅实现注入属性的最核心的方法, 其余估计都是些前置处理</h3>
     * @param beanName Bean 名字
     * @param beanDefinition BeanDefinition 实例
     */
    protected void populateBean(String beanName, BeanDefinition beanDefinition, Object bean) {
        // 1. 判断是否需要进行后置处理, 如果需要进入循环
        applyBeanPostProcessorBeforeApplyingPropertyValues(beanName, bean, beanDefinition);
        // 2. 如果不需要或者后置处理完成, 那么就直接开始属性注入
        if(beanDefinition.hasPropertyValues()){
            applyPropertyValues(beanName, bean, beanDefinition, beanDefinition.getPropertyValues());
        }
    }

    /**
     * <h3>执行属性注入</h3>
     * <h3>注: 这段源码是在过于复杂了...之后再看吧, 大致流程先明确了</h3>
     * @param beanName Bean 名字
     * @param beanDefinition BeanDefinition 实例
     */
    protected void applyPropertyValues(String beanName, Object bean, BeanDefinition beanDefinition, PropertyValues propertyValues) {
        try {
            // 1. 如果属性集合汇总为空, 那么直接返回
            if (propertyValues.isEmpty())
                return;
            // 2. 开始遍历属性集合开始注入
            for (PropertyValue propertyValue : propertyValues.getPropertyValues()) {
                String name = propertyValue.getName();
                Object value = propertyValue.getValue();
                // 3. 如果是引用类型, 那么就需要优先创建, 这些属性应该会在容器启动的时候加载
                if (value instanceof BeanReference){
                    BeanReference beanReference = (BeanReference) value;
                    value = getBean(beanReference.getBeanName());
                }else{
                    // 4. 如果不是引用类型, 那么就考虑是否执行类型转换
                    Class<?> sourceType = value.getClass();
                    // 注: 配置文件中的 value 属性被读取进来之后是字符串类型, 而不是我们想象中的整型, 所以是需要类型转换的
                    Class<?> targetType = (Class<?>) TypeUtil.getFieldType(bean.getClass(), name);
                    // 注: 如果原有的属性类型和实例中的实际类型不同, 那么就要进行类型转换
                    ConversionService conversionService = getConversionService();
                    if (conversionService != null){
                        // 注: 判断是否能够发生类型转换
                        if (conversionService.canConvert(sourceType, targetType)){
                            // 注: 这里返回的是值
                            value = conversionService.convert(value, targetType);
                        }
                    }
                }
                // 注: 这里注入的原理之后会详细解释
                BeanUtil.setFieldValue(bean, name, value);
            }
        }
        catch (BeansException e) {
            throw new BeansException(e.getMessage());
        }

    }

    /**
     * <h3>负责执行初始化方法 {@code init-method} 以及执行前置初始化和后置初始化方法</h3>
     * @param bean Bean 实例
     * @param beanName Bean 名字
     * @param beanDefinition BeanDefinition 实例
     * @return 初始化后的实例
     */
    public Object initializationBean(Object bean, String beanName, BeanDefinition beanDefinition){
        // 1. 执行感知方法
        invokeAwareMethods(beanName ,bean);
        // 2. 执行前置初始化方法
        bean = applyBeanPostProcessorBeforeInitialization(bean, beanName);
        try{
            // 3. 调用初始化方法
            invokeInitMethods(bean, beanName, beanDefinition);
        }catch (Exception e){
            throw new BeansException(e.getMessage());
        }
        // 4. 执行后置初始化方法
        return applyBeanPostProcessorAfterInitialization(bean, beanName);
    }

    /**
     * <h3>负责给初始化感知接口</h3>
     * <h3>1. 如果某个类想要感知到某些信息, 比如说加载自己的类加载器, 自己的名字, 自己所在的容器</h3>
     * <h3>2. 那么该类就可以实现 Aware 接口, 为自己的这些成员变量赋值, 最终通过成员变量成员变量就可以获取了</h3>
     * @param beanName Bean 名字
     * @param bean Bean 实例
     */
    protected void invokeAwareMethods(String beanName, Object bean) {
        if (bean instanceof Aware){
            if (bean instanceof BeanFactoryAware){
                // 注: 自身就是容器
                ((BeanFactoryAware) bean).setBeanFactory(this);
            }else if (bean instanceof BeanNameAware){
                ((BeanNameAware) bean).setBeanName(beanName);
            }
        }
    }

    /**
     * <h3>负责调用初始化方法</h3>
     * @param bean Bean 实例
     * @param beanName Bean 名字
     * @param beanDefinition BeanDefinition 实例
     */
    private void invokeInitMethods(Object bean, String beanName, BeanDefinition beanDefinition) {
        // TODO 暂时不支持使用 init-method 属性
    }


    /**
     * <h3>初始化前的处理: 可以执行动态代理</h3>
     * @param bean Bean 实例
     * @param beanName Bean 实例名字
     * @return 代理对象
     */
    @Override
    public Object applyBeanPostProcessorBeforeInitialization(Object bean, String beanName) throws BeanException {
        Object existingBean = bean;
        for (BeanPostProcessor processor : getBeanPostProcessors()) {
            Object proxy = processor.postProcessBeforeInitialization(beanName, bean);
            // 注: 方法的默认返回就是原对象, 所以如果返回对象为空, 那么肯定有问题
            if (proxy == null)
                return existingBean;
            existingBean = proxy;
        }
        return existingBean;
    }

    @Override
    public Object applyBeanPostProcessorAfterInitialization(Object bean, String beanName) throws BeanException {
        Object existingBean = bean;
        for (BeanPostProcessor processor : getBeanPostProcessors()) {
            Object proxy = processor.postProcessAfterInitialization(beanName, bean);
            if (proxy == null)
                return existingBean;
            existingBean = proxy;
        }
        return existingBean;
    }


    //================================================ 设置属性 ================================================
    public InstantiationStrategy getInstantiationStrategy() {
        return this.instantiationStrategy;
    }

    public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
    }
}
