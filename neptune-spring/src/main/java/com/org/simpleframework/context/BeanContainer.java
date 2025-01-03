package com.org.simpleframework.context;

import com.org.simpleframework.context.annotation.Component;
import com.org.simpleframework.context.annotation.Controller;
import com.org.simpleframework.context.annotation.Repository;
import com.org.simpleframework.context.annotation.Service;
import com.org.simpleframework.util.ClassUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h3> IOC 容器</h3>
 * <h3>1. 创建保存所有 Class 对象的数据结构</h3>
 * <h3>2. 实现容器的加载, 将所有 Class 对象加载到内存中</h3>
 * <h3>3. 提供容器对外的方法, 方便用户从容器中取出实例对象</h3>
 * <h3>注: 容器是唯一的, 所以需要采用单例模式创建实例</h3>
 */
@Slf4j
// 确保容器类的构造方法是私有的: 这个可以使用 Lombok 注解添加
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BeanContainer {

    /**
     * <h3>判断容器是否已经被加载过</h3>
     */
    private boolean isLoaded = false;

    /**
     * <h3>记录 Class 对象和实例对象的关系</h3>
     */
    private final Map<Class<?>, Object> map = new ConcurrentHashMap<>();

    /**
     * <h3>获取枚举创建的实例</h3>
     * @return 容器实例
     */
    public static BeanContainer getInstance(){
        return BeanContainerHolder.BEAN_CONTAINER_HOLDER.container;
    }

    /**
     * <h3>单例模式: 枚举创建容器实例</h3>
     */
    private enum BeanContainerHolder{
        BEAN_CONTAINER_HOLDER;
        private final BeanContainer container;
        BeanContainerHolder() {
            this.container = new BeanContainer();
        }
    }

    /**
     * <h3>保存需要生效的注解类型</h3>
     */
    private static final List<Class<? extends Annotation>> ANNOTATIONS =
            Arrays.asList(Component.class, Controller.class, Service.class, Repository.class);

    /**
     * <h3>1. 将包下所有的类都加载到容器中</h3>
     * <h3>2. 根据此前标记的注解类型, 选择将哪些类型实例化</h3>
     * @param packageName 包名
     */
    public void loadBeans(String packageName, boolean flag){
        if (isLoaded){
            log.warn(" IOC 容器已经被实例化完成, 不需要重复创建...");
            return;
        }
        // 1. 调用此前定义的获取包下的 Class 对象方法, 获取到包下所有的 Class 对象
        Set<Class<?>> classSet = ClassUtil.extractPackage("com.demo");
        // 2. 判断 Class 对象集合是否存在元素, 不存在元素就直接返回
        if (classSet.isEmpty()){
            log.warn("包 {} 不存在任何的 Class 对象...", packageName);
            return;
        }
        // 3. 遍历所有的 Class 对象, 检查是否存在相应的注解
        for (Class<?> clazz : classSet) {
            for (Class<? extends Annotation> annotation : ANNOTATIONS) {
                // 3.1 如果该 Class 对象已经被相应的注解标注, 那么就生成相应的实体类对象
                if (clazz.isAnnotationPresent(annotation)){
                    map.put(clazz, ClassUtil.getInstance(clazz, flag));
                }
            }
        }

        isLoaded = true;
    }

    /**
     * <h3>判断当前容器是否已经被加载过</h3>
     * @return 是否已经被加载
     */
    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * <h3>哈希表的大小: 加载的类的数量</h3>
     * @return 加载的类型的数量
     */
    public int size(){
        return map.size();
    }

    public Object addBean(Class<?> clazz, Object bean){
        if (clazz == null || bean == null){
            log.error("class 实例和对象实例为空...");
            return null;
        }
        return map.put(clazz, bean);
    }

    public Object removeBean(Class<?> clazz){
        if (clazz == null) return null;
        return map.remove(clazz);
    }

    /**
     * <h3>问题: 相同类型的实例对象如何处理? 不会覆盖吗?</h3>
     * @param clazz Class 对象
     * @return Class 对象对应的实例对象
     */
    public Object getBean(Class<?> clazz) {
        if (clazz == null) return null;
        return map.get(clazz);
    }

    public Set<Class<?>> getClasses(){
        return map.keySet();
    }

    public Set<Object> getBeans(){
        return new HashSet<>(map.values());
    }

    public Set<Class<?>> getClassesByAnnotation(Class<? extends Annotation> annotation){
        if (annotation == null){
            log.error("传入的注解为空...");
            return null;
        }
        // 1. 获取所有的类类型对象
        Set<Class<?>> classSet = getClasses();
        // 2. 检查类类型对象集合是否为空
        if(classSet == null || classSet.isEmpty()){
            log.error("Class 对象集合为空...");
            return null;
        }
        // 3. 如果不为空就直接遍历
        Set<Class<?>> set = new HashSet<>();
        for (Class<?> clazz : classSet) {
            if (clazz.isAnnotationPresent(annotation)){
                set.add(clazz);
            }
        }

        return set.isEmpty() ? null: set;
    }

    /**
     * <h3>通过类的实现接口或者超类来获取相应的 Class 对线</h3>
     * @param interfaceOrSuper 接口或者父类
     * @return 对应的实现类或者子类的 Class 对象
     */
    public Set<Class<?>> getClassesBySuper(Class<?> interfaceOrSuper){
        if (interfaceOrSuper == null) return null;

        Set<Class<?>> classSet = getClasses();
        if (classSet == null || classSet.isEmpty()) return null;

        Set<Class<?>> set = new HashSet<>();
        for (Class<?> clazz : classSet) {
            if (interfaceOrSuper.isAssignableFrom(clazz) && !clazz.equals(interfaceOrSuper)){
                set.add(clazz);
            }
        }

        return set.isEmpty() ? null: set;
    }
}
