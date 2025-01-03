package com.org.simpleframework.beans.factory.support;

import com.org.simpleframework.beans.exception.BeanCurrentlyInCreationException;
import com.org.simpleframework.beans.exception.BeansException;
import com.org.simpleframework.beans.factory.BeanFactory;
import com.org.simpleframework.beans.factory.FactoryBean;
import com.org.simpleframework.beans.factory.config.BeanDefinition;
import com.org.simpleframework.beans.factory.config.BeanPostProcessor;
import com.org.simpleframework.beans.factory.config.ConfigurableBeanFactory;
import com.org.simpleframework.core.covert.ConversionService;
import com.org.simpleframework.util.Assert;
import com.org.simpleframework.util.StringValueResolver;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h2>抽象类容器</h2>
 * <h3>1. 提供高级容器需要使用的所有功能以及很多额外功能</h3>
 * <h3>1.1 最基本的功能: 负责容器获取、添加、创建等功能</h3>
 * <h3>1.2 扩展的功能: 添加父容器、类型转换器、后置处理器等功能</h3>
 * <h3>2. ConfigurableBeanFactory 接口</h3>
 * <h3>2.1. 继承 SingletonRegistry、BeanFactory、HierarchicalBeanFactory 三个接口</h3>
 * <h3>2.2。所以这个接口中具有所有和 Bean 实例相关的方法, 并具有许多扩展方法</h3>
 * <h3>3. 因为继承 DefaultSingletonBeanRegistry, 所以基本实现了 Registry 中的方法</h3>
 * <h3>4. 所以这个抽象类主要实现 BeanFactory 中的方法</h3>
 */
@Slf4j
public abstract class AbstractBeanFactory extends DefaultSingletonBeanRegistry implements ConfigurableBeanFactory {

    /**
     * <h3>后置处理器集合</h3>
     */
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    /**
     * <h3>解析占位符</h3>
     */
    private final List<StringValueResolver> embeddedValueResolver = new ArrayList<>();

    /**
     * <h3>存放工厂产生的原始对象的缓存</h3>
     * <h3>注: 本身不在这个类中, 但是工厂注册中心的大多数逻辑太复杂不会实现, 所以只能暂时把缓存放在这里</h3>
     */
    private final Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<>();

    /**
     * <h3>记录当前正在创建的原型对象</h3>
     * <h3>1. 泛型使用 Object 的目的</h3>
     * <h3>1.1 因为 ThreadLocal 只能过存储单个变量, 如果想要存储多个, 那么就只能在里面放入集合</h3>
     * <h3>1.2 这里放入多个变量的目的就是为了记录当前线程已经创建了哪些原型对象, 用于检测循环依赖是否出现</h3>
     */
    private final ThreadLocal<Object> prototypesCurrentlyInCreation = new ThreadLocal<>();

    /**
     * <h3>容器的父容器</h3>
     */
    private  BeanFactory parentBeanFactory;

    /**
     * <h3>类型转换器</h3>
     */
    private ConversionService conversionService;


    //========================================= Bean 实例 =========================================

    /**
     * <h3>根据名字获取 Bean 实例</h3>
     * <h3>注: 没有传入需要查找的对象的类型, 所以不知道是什么类型, 可以使用 Object 或者泛型作为返回值</h3>
     */
    @Override
    public Object getBean(String beanName) throws BeansException {
        return doGetBean(beanName, null);
    }

    /**
     * <h3>根据名字和类型获取 Bean 实例</h3>
     */
    @Override
    public <T> T getBean(String beanName, Class<T> requiredType) throws BeansException {
        return doGetBean(beanName, requiredType);
    }

    /**
     * <h3>根据类型获取 Bean 实例</h3>
     */
    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        return doGetBean(null, requiredType);
    }

    /**
     * <h3>获取 Bean 实例的方法</h3>
     */
    @SuppressWarnings("unchecked")
    private <T> T doGetBean(String beanName, Class<T> requiredType){
        // 1. 从一级缓存中获取单例对象, 如果没有才会考虑去创建
        Object singletonObject = getSingleton(beanName);
        // 2. 如果返回不为空, 那么返回的可能是工厂对象, 也可能是单例对象本身, 所以直接调用实例化对象的方法
        if (singletonObject != null){
            singletonObject = getObjectForBeanInstance(beanName, singletonObject);
        }else{
            // 3. 这里是检测采用原型模式创建的对象是否存在循环依赖的问题, spring 本身也没有解决这个问题, 只是抛出异常了
            // 注: 暂时不知道为什么 spring 不支持原型对象的循环依赖
            if (isPrototypeCurrentlyInCreation(beanName))
                throw new BeanCurrentlyInCreationException(beanName);

            // 4. 首先检查父容器是否存在
            BeanFactory parentBeanFactory = getParentBeanFactory();
            // 5. 如果父容器存在并且当前容器中没有对应的 BeanDefinition 实例, 那么就考虑采用父容器创建原始对象
            if (parentBeanFactory != null && !containsBeanDefinition(beanName)){
                // 注: 有可能这个父容器没有继承抽象类实现, 可能直接实现的顶级容器接口, 所以可能不是其实例
                if (parentBeanFactory instanceof AbstractBeanFactory){
                    return ((AbstractBeanFactory) parentBeanFactory).doGetBean(beanName, requiredType);
                }else if (requiredType != null){
                    // 注: 源码中是根据参数是否存在选择不同的方法, 如果什么参数也没有, 那么就直接调用最原始的方法
                    return parentBeanFactory.getBean(beanName, requiredType);
                }else{
                    return (T) parentBeanFactory.getBean(beanName);
                }
            }

            // TODO 注: 类型检查不实现、显示的依赖暂时不实现

            // 6. 不实现属性继承, 所以直接获取 BeanDefinition 实例
            BeanDefinition beanDefinition = getBeanDefinition(beanName);

            // 7. 判断创建的实例是单例对象还是原型对象, 或者是其他几种类型的
            if (beanDefinition.isSingleton()){
                // 8. 调用带有工厂的方法来创建实例
                singletonObject = getSingleton(beanName, () -> createBean(beanName, beanDefinition));
            }else if (beanDefinition.isPrototype()){
                Object prototypeObject = null;
                // 前置处理
                beforeSingletonCreation(beanName);
                prototypeObject = createBean(beanName, beanDefinition);
                // 后置处理
                afterPrototypeCreation(beanName);
                // 工厂实例化
                prototypeObject = getObjectForBeanInstance(beanName, prototypeObject);
                return (T) prototypeObject;
            }else{
                log.warn("当前暂时不支持其他形式创建实例的方式");
                throw new IllegalArgumentException(beanName);
            }
        }


        return (T) singletonObject;
    }

    /**
     * <h3>1. 如果是原始对象就直接返回</h3>
     * <h3>2. 如果不是原始对象, 就需要利用工厂类创建原始对象</h3>
     * <h3>注: 不支持属性继承 (parent), 因为实在是太难写了, 这里中间会有合并继承的操作, 没有实现</h3>
     * <h3>注: 此外, 在利用工厂创建 Bean 实例的前后, 应该有相应的前后处理器, 也没有实现</h3>
     */
    private Object getObjectForBeanInstance(String beanName, Object beanInstance){
        // TODO 1. 检查传入的原始对象的名称是否符合命名规范, 大意是这个, 但是确实没有看明白里面为什么会有返回值

        // 2. 检查当前原始对象是否为工厂对象, 如果不是直接返回, 如果是, 再用工厂对象创建
        if (!(beanInstance instanceof FactoryBean))
            return beanInstance;
        Object bean = null;
        try {
            // 3. 从缓存中获取此前工厂创建的 Bean 实例, 如果没有再使用当前的工厂实例
            bean = getCachedObjectFromFactoryBean(beanName);
            // 4. 如果缓存中没有对应的 Bean 实例, 那么只能够由工厂创建了
            if (bean == null){
                FactoryBean<?> factoryBean = (FactoryBean<?>) beanInstance;
                if (factoryBean.isSingleton()){
                    // 5. 如果是单例对象, 那么创建之后放入缓存, 防止下次再创建
                    beforeSingletonCreation(beanName);
                    bean = factoryBean.getObject();
                    afterSingletonCreation(beanName);
                    this.factoryBeanObjectCache.put(beanName, bean);
                }else{
                    // 6. 如果是原型对象, 那么直接新创建一个即可
                    bean = factoryBean.getObject();
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException("FactoryBean 创建 [" + beanName + "] 对象异常");
        }
        return bean;
    }

    /**
     * <h3>判断是否有 Bean 实例的扩展实现</h3>
     * @param beanName 对象的名字
     * @return 是否存在
     */
    @Override
    public boolean containsBean(String beanName) {
        // 1. 检查注册中心是否有单例 Bean 或者是否有对应的 BeanDefinition 实例
        if (containsSingleton(beanName) || containsBeanDefinition(beanName)) return true;
        // 2. 如果没有找到, 可以考虑从父容器中获得
        BeanFactory parentBeanFactory = getParentBeanFactory();
        return (parentBeanFactory != null && parentBeanFactory.containsBean(beanName));
    }

    @Override
    public boolean isSingleton(String beanName) {
        Object singletonObject = getSingleton(beanName);
        // 1. 如果找到了, 就分为普通实例和工厂实例进行判断
        if (singletonObject != null) return true;
        // 2. 如果没有找到, 那么从父容器中找
        BeanFactory parentBeanFactory = getParentBeanFactory();
        if (parentBeanFactory != null)
            return parentBeanFactory.isSingleton(beanName);
        // 3. 还是没有找的的话, 才考虑从 BeanDefinition 实例中查
        return getBeanDefinition(beanName).isSingleton();
    }

    /**
     * <h3>注: 没有原型对象的注册中心</h3>
     */
    @Override
    public boolean isPrototype(String beanName) {
        BeanFactory parentBeanFactory = getParentBeanFactory();
        if (parentBeanFactory != null)
            return parentBeanFactory.isPrototype(beanName);
        return getBeanDefinition(beanName).isPrototype();
    }

    //========================================= 辅助方法 =========================================

    /**
     * <h3>判断是否存在对应的 BeanDefinition 实例</h3>
     * <h3>注: 等待由子类来实现</h3>
     * @param beanName Bean 名字
     */
    protected abstract boolean containsBeanDefinition(String beanName);

    /**
     * <h3>直接获取 BeanDefinition 实例</h3>
     * @param beanName 获取 BeanDefinition 实例
     * @return BeanDefinition 实例
     */
    protected abstract BeanDefinition getBeanDefinition(String beanName);

    /**
     * <h3>创建 Bean 实例</h3>
     * @param beanName Bean 名字
     * @param beanDefinition BeanDefinition 实例
     * @return Bean 实例
     */
    protected abstract Object createBean(String beanName, BeanDefinition beanDefinition);

    /**
     * <h3>从缓存中获取工厂产生的 Bean 实例</h3>
     * @param beanName Bean 实例的名字
     * @return Bean 实例
     */
    protected Object getCachedObjectFromFactoryBean(String beanName){
        return this.factoryBeanObjectCache.get(beanName);
    }

    /**
     * <h3>原型对象创建前将其放入线程的本地变量中</h3>
     * @param beanName Bean 实例名字
     */
    @SuppressWarnings("unchecked")
    protected void beforePrototypeCreation(String beanName){
        Object currentValue = this.prototypesCurrentlyInCreation.get();
        if (currentValue == null){
            // 1. 如果本地变量中没有保存名字, 那就是没有原型对象, 直接设置就好
            this.prototypesCurrentlyInCreation.set(beanName);
        }else if (currentValue instanceof String){
            // 2. 如果已经保存了名字, 那么直接就需要改为集合存放
            Set<String> beanNameSet = new HashSet<>();
            beanNameSet.add(String.valueOf(currentValue));
            beanNameSet.add(beanName);
            this.prototypesCurrentlyInCreation.set(beanNameSet);
        }else{
            // 3. 如果也不是字符串, 那么就代表里面有多个值, 直接获取集合后添加
            Set<String> beanNameSet = (Set<String>) currentValue;
            beanNameSet.add(beanName);
        }
    }

    /**
     * <h3>原型对象创建完毕之后将其从本地变量中移除</h3>
     * @param beanName Bean 实例名字
     */
    @SuppressWarnings("unchecked")
    protected void afterPrototypeCreation(String beanName){
        Object currentValue = this.prototypesCurrentlyInCreation.get();
        if (currentValue instanceof String){
            this.prototypesCurrentlyInCreation.remove();
        }else{
            Set<String> beanNameSet = (Set<String>) currentValue;
            beanNameSet.remove(beanName);
            if(beanNameSet.isEmpty())
                this.prototypesCurrentlyInCreation.remove();
        }
    }

    /**
     * <h3>spring 不支持原型对象的循环依赖</h3>
     * @param beanName Bean 实例的名字
     * @return 是否出现循环依赖
     */
    @SuppressWarnings("unchecked")
    protected boolean isPrototypeCurrentlyInCreation(String beanName){
        Assert.notNull(beanName);
        Object currentValue = this.prototypesCurrentlyInCreation.get();
        if (currentValue instanceof String){
            return beanName.equals(currentValue);
        }else if (currentValue instanceof Set){
            Set<String> beanNameSet = (Set<String>) currentValue;
            return beanNameSet.contains(beanName);
        }
        return false;
    }

    /**
     * <h3>解析 Bean 实例对应的 Class 对象</h3>
     * @param beanDefinition BeanDefinition 实例
     * @param beanName Bean 实例的名字
     * @return Class 对象
     */
    protected Class<?> resolveBeanClass(BeanDefinition beanDefinition, String beanName){
        return null;
    }


    //========================================= 设置属性 =========================================

    @Override
    public BeanFactory getParentBeanFactory() {
        return this.parentBeanFactory;
    }

    @Override
    public void setParentBeanFactory(BeanFactory parentBeanFactory) {
        // 如果已经有父容器就不允许更换, 设置的父容器不允许为自己
        if (this.parentBeanFactory != null && this.parentBeanFactory != parentBeanFactory)
            throw new RuntimeException("容器已经有相应的父容器了, 不要重复设置 \t" + this.parentBeanFactory);
        this.parentBeanFactory = parentBeanFactory;
    }


    @Override
    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        if (beanPostProcessor == null)
            throw new RuntimeException("BeanPostProcessor 对象为空");
        // 如果此前有相同的后置处理器, 那么就需要移除后再添加
        this.beanPostProcessors.remove(beanPostProcessor);
        this.beanPostProcessors.add(beanPostProcessor);
    }

    public List<BeanPostProcessor> getBeanPostProcessors(){
        return this.beanPostProcessors;
    }

    @Override
    public void addEmbeddedValueResolver(StringValueResolver resolver) {
        if (resolver == null)
            throw new RuntimeException("StringValueResolver 对象为空");
        // TODO 为什么这个不考虑先移除再添加呢?
        this.embeddedValueResolver.add(resolver);
    }

    public List<StringValueResolver> getEmbeddedValueResolver(){
        return this.embeddedValueResolver;
    }

    /**
     * <h3>是否有解析占位符的类</h3>
     */
    @Override
    public boolean hasEmbeddedValueResolver() {
        return !this.embeddedValueResolver.isEmpty();
    }

    @Override
    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public ConversionService getConversionService() {
        return this.conversionService;
    }

}
