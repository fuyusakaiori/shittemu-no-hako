package com.org.simpleframework.injection.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <h3>标记需要注入实例的字段</h3>
 * TODO <h3>暂时仅支持注入字段, 构造方法和 Setter 方法注入没有实现</h3>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {

    /**
     * <h3>需要注入实例的字段的名字</h3>
     * @return 字段的类型
     */
    public String value() default "";

}
