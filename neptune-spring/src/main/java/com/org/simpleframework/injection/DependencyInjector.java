package com.org.simpleframework.injection;

import com.org.simpleframework.context.BeanContainer;
import com.org.simpleframework.injection.annotation.Autowired;
import com.org.simpleframework.util.ClassUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * <h3>负责实现依赖注入</h3>
 */
@Slf4j
public class DependencyInjector {

    private final BeanContainer beanContainer;

    public DependencyInjector() {
        this.beanContainer = BeanContainer.getInstance();
    }

    /**
     * <h3>为所有容器中的实例对象执行依赖注入</h3>
     */
    public void injected(){
        // 1. 获取容器中所有 Class 实例对象
        Set<Class<?>> classSet = beanContainer.getClasses();
        // 注: 判断容器是否为空, 如果不是空才会继续执行, 如果为空, 就没有必要执行了
        if (classSet == null || classSet.isEmpty()){
            log.error("容器中没有任何实例对象...");
            throw new RuntimeException("容器中没有任何实例对象...");
        }
        // 2. 遍历容器中的所有 Class 对象
        for (Class<?> clazz : classSet) {
            // 3. 获取需要依赖注入的实例对象
            Object targetBean = beanContainer.getBean(clazz);
            // 4. 获取 Class 对象中的所有成员变量
            Field[] fields = clazz.getFields();
            if (fields.length == 0){
                log.warn("该实例对象没有任何成员变量, 不需要依赖注入");
                continue;
            }
            // 4. 对于有成员变量的实例对象执行依赖注入
            for (Field field : fields) {
                // 5. 检查该成员变量是否被 Autowired 注解标注
                if (field.isAnnotationPresent(Autowired.class)) {
                    // 注: 额外添加根据注解中的值查找
                    Autowired annotation = field.getAnnotation(Autowired.class);
                    String value = annotation.value();
                    // 5.1 获取成员变量的类型
                    Class<?> type = field.getType();
                    // 5.2 在容器中查找相应的类型
                    Object fieldBean = getFiledInstance(type, value);
                    if (fieldBean == null){
                        log.error("该成员变量没有被初始化到容器中...");
                        throw new RuntimeException("该成员变量没有被初始化到容器中...");
                    }else{
                        // 5.3 通过反射将成员变量的实例对象设置到需要依赖注入的实例对象中
                        ClassUtil.setFiledBean(field, targetBean, type, true);
                    }
                }
            }
        }
    }

    /**
     * <h3>容器中查找相应根据字段的类型查找相应的实例</h3>
     * <h3>注: 接口类是没有相应的实例的, 只有接口的实现类才有相应的实例</h3>
     * @param clazz 字段类型的 Class 实例
     * @param value Autowired 注解中定义的字段的名字
     * @return 字段对应的 Class 实例
     */
    private Object getFiledInstance(Class<?> clazz, String value) {
        // TODO 存在问题
        Object bean = beanContainer.getBean(clazz);
        // 1. 如果查询到的实例不为空, 那么证明查询的实例就是类实例
        if (bean != null){
            return bean;
        }else{
            // 2. 如果查询不到, 那么证明查询的是接口的实例
            Class<?> implementedClass = getImplementedClass(clazz, value);
            if (implementedClass != null){
                return beanContainer.getBean(implementedClass);
            }else{
                log.error("容器中没有对应的实例...");
                throw new RuntimeException("容器中没有对应的实例...");
            }
        }
    }

    private Class<?> getImplementedClass(Class<?> interfaceClass, String value) {
        Set<Class<?>> classSet = beanContainer.getClassesBySuper(interfaceClass);
        if (classSet == null || classSet.isEmpty()){
            log.error("...");
            throw new RuntimeException("...");
        }
        if ("".equals(value)){
            if (classSet.size() == 1)
                return classSet.iterator().next();
            else
                // 该接口有多个实现类, 用户没有指定的话, 就暂时先抛出异常
                throw new RuntimeException("接口存在多个实现类, 请指定具体的实现类...");
        }else{
            for (Class<?> clazz : classSet) {
                // TODO 如果是随便取的名字, 怎么办?
                if (value.equals(clazz.getSimpleName())){
                    // 这里就相当于指定了具体的实现类
                    return clazz;
                }
            }
        }
        return null;
    }
}
