package com.org.simpleframework.core.covert;

/**
 * <h3>负责类型转换</h3>
 * <h3>注: 负责的是从对象到类型的转换, 前者是从任意类型到任意类型的转换, 侧重点不一样</h3>
 * <h3>注: 在为原始对象中的成员变量赋值的时候, 需要将值转换成变量对应的类型</h3>
 * <h3>注: spring 能够完成绝大多数的类型转换, 大多数时候不需要提供这种类型转换器</h3>
 */
public interface ConversionService {

    /**
     * <h3>主要负责的是 Class 类型之间的转换</h3>
     * @param sourceType 源类型
     * @param targetType 目标类型
     * @return 能否哦转换
     */
    boolean canConvert(Class<?> sourceType, Class<?> targetType);

    <T> T convert(Object source, Class<T> targetType);

}
