package com.org.simpleframework.core.covert.converter;

/**
 * <h2>类型转换</h2>
 * <h3>仅提供类型转换功能</h3>
 * <h3>这个通常作为工厂类中的内部类实现</h3>
 */
public interface Converter<S, T> {

    /**
     * <h3>执行类型转换</h3>
     * @param sourceType 源类型
     * @return 目标类型
     */
    T convert(S sourceType);
}
