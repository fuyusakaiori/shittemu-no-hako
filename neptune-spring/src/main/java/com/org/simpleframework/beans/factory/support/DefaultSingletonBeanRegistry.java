package com.org.simpleframework.beans.factory.support;

import com.org.simpleframework.beans.exception.BeanCurrentlyInCreationException;
import com.org.simpleframework.beans.factory.ObjectFactory;
import com.org.simpleframework.beans.factory.config.SingletonBeanRegistry;
import com.org.simpleframework.util.Assert;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;


/**
 * <h2>负责创建 Bean 实例</h2>
 * <h3>1. Registry 主要负责 Bean 实例的创建, 虽然可以获取单例对象, 但是不是主要的功能</h3>
 * <h3>2. BeanFactory 提供的查询方式更加多样, 可以根据类型、以及类型 + 名字的方式查询</h3>
 * <h3>3. 前者侧重创建 Bean 实例、后者侧重查询 Bean 实例</h3>
 * <h3>注: Registry 负责的是添加对象而不是他妈的创建对象</h3>
 */
public class DefaultSingletonBeanRegistry implements SingletonBeanRegistry {


    /**
     * <h3>二级缓存: 存放所有属性还没有被注入的完整对象</h3>
     * <h3>注: 解决循环依赖问题</h3>
     */
    private final Map<String, Object> earlySingletonObjects = new HashMap<>();

    /**
     * <h3>一级缓存: 存放所有属性都已经注入完成的完整的对象</h3>
     */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();

    /**
     * <h3>三级缓存: 存放创建 Bean 实例的工厂 Bean 实例</h3>
     * <h3>注: 这里不使用并发集合类, 因为二级缓存和三级缓存都是在上锁的代码块中修改, 所以不会有并发安全问题</h3>
     */
    private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>();

    /**
     * <h3>缓存当前正在创建的对象的</h3>
     * <h3>注: JUC 中没有 ConcurrentHashSet, 只有将 ConcurrentHashMap 转换成对应的 ConcurrentHashMap</h3>
     * <h3>注: 还在考虑是否实现</h3>
     */
    private final Set<String> singletonsCurrentlyInCreation = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * <h3>添加 Bean 实例: 调用子方法实现的</h3>
     * @param beanName 对象名称
     * @param beanObject 原始对象
     */
    @Override
    public void registerSingleton(String beanName, Object beanObject) {
        Assert.notNull(beanObject, beanName);
        // 注: 这里因为在获取 Bean 实例之后还需要做判断, 如果不加锁可能出现获取的时候没有, 但是在添加前, 这个对象被其他线程创建了
        synchronized (this.singletonObjects){
            Object singletonObject = this.singletonObjects.get(beanName);
            if (singletonObject != null)
                throw new IllegalStateException("对象\t" + beanName + "\t已经被创建了");
            // 如果一级缓存中没有对象, 那么就直接添加到缓存中
            addSingleton(beanName, beanObject);
        }
    }

    /**
     * <h3>缓存中获取 Bean 实例</h3>
     * @param beanName Bean 的名字
     * @return Bean 实例
     */
    @Override
    public Object getSingleton(String beanName) {
        // 1. 从一级缓存中获取 Bean 实例
        Object singletonObject = this.singletonObjects.get(beanName);
        // 2. 如果获取到的单例对象为空, 并且对象正处于创建的过程, 那么就需要从二级缓存中获取
        if(singletonObject == null && isSingletonCurrentlyInCreation(beanName)){
            // 3. 尝试获取二级缓存中的对象
            singletonObject = this.earlySingletonObjects.get(beanName);
            // 4. 如果二级缓存中也没有就会尝试获取工厂来创建
            if (singletonObject == null){
                ObjectFactory<?> objectFactory = this.singletonFactories.get(beanName);
                // 5. 获取工厂实例
                if (objectFactory != null){
                    singletonObject = objectFactory.getObject();
                    // 6.移除工厂类, 因为是单例
                    this.singletonFactories.remove(beanName);
                    // 7. 添加进入二级缓存, 因为没有设置任何属性
                    this.earlySingletonObjects.put(beanName, singletonObject);
                }
            }
        }

        return singletonObject;
    }

    /**
     * <h3>创建 Bean 实例并放入缓存中</h3>
     * @param beanName Bean 的名字
     * @param objectFactory Bean 实例对应的工厂
     * @return Bean 实例
     */
    public Object getSingleton(String beanName, ObjectFactory<?> objectFactory){
        Assert.notNull(objectFactory, beanName);
        synchronized (this.singletonObjects){
            // 再次检查一级缓存中是否存在相应的 Bean 实例
            Object singletonObject = this.singletonObjects.get(beanName);
            if (singletonObject == null){
                // 将当前正在创建的对象放到集合中去
                beforeSingletonCreation(beanName);
                singletonObject = objectFactory.getObject();
                // 然后将当前正在创建的对象从集合中移除
                afterSingletonCreation(beanName);
                // 添加当前的对象到集合中
                addSingleton(beanName, singletonObject);
            }
            // 注: 同步块里面返回也可以
            return singletonObject;
        }
    }

    /**
     * <h3>判断完整的 Bean 实例是否存在</h3>
     * @param beanName Bean 名字
     * @return 是否存在
     */
    @Override
    public boolean containsSingleton(String beanName) {
        return this.singletonObjects.containsKey(beanName);
    }

    /**
     * <h3>获取所有完整的 Bean 实例的名字</h3>
     * @return Bean 实例名字集合
     */
    @Override
    public String[] getSingletonNames() {
        LinkedList<String> beanNames = new LinkedList<>();
        this.singletonObjects.forEach((beanName, beanObject) -> {
            beanNames.add(beanName);
        });
        return beanNames.toArray(new String[0]);
    }

    //========================================= 辅助方法 =========================================

    /**
     * <h3>真正添加 Bean 实例的方法</h3>
     * @param beanName Bean 实例的名字
     * @param singletonObject Bean 实例
     */
    protected void addSingleton(String beanName, Object singletonObject){
        synchronized (this.singletonObjects){
            this.singletonObjects.put(beanName, singletonObject);
            this.earlySingletonObjects.remove(beanName);
            this.singletonFactories.remove(beanName);
        }
    }

    /**
     * <h3>添加 Ban 实例对应的工厂实例</h3>
     * <h3>注: 可以避免没有工厂的情况</h3>
     * @param beanName Bean 实例的名字
     * @param objectFactory Bean 实例的生产工厂
     */
    protected void addSingletonFactory(String beanName, ObjectFactory<?> objectFactory){
        Assert.notNull(objectFactory, beanName);
        // TODO 为什么要向三级缓存中添加工厂对象的同时, 移除掉二级缓存中的半成品啊
        synchronized (this.singletonObjects){
            // 注: 如果一级缓存中已经有完成品了, 那么也就没有必要创建了
            if(!this.singletonObjects.containsKey(beanName)){
                // 注: 大概是因为三级缓存中的工厂使用后会直接向二级缓存中添加
                this.singletonFactories.put(beanName, objectFactory);
                this.earlySingletonObjects.remove(beanName);
            }
        }
    }

    /**
     * <h3>判断当前对象是否正在创建</h3>
     */
    protected boolean isSingletonCurrentlyInCreation(String beanName){
        return this.singletonsCurrentlyInCreation.contains(beanName);
    }

    protected void beforeSingletonCreation(String beanName){
        if (!this.singletonsCurrentlyInCreation.add(beanName))
            throw new BeanCurrentlyInCreationException(beanName);
    }

    protected void afterSingletonCreation(String beanName){
        if (!this.singletonsCurrentlyInCreation.remove(beanName))
            throw new IllegalStateException(beanName);
    }
}