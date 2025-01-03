package com.org.simpleframework.core.covert.converter;

/**
 * <h2>类型转换注册中心</h2>
 * <h3>向哈希表中添加类型转换器, 方便之后根据原始类型和目标类型查找类型转换器</h3>
 */
public interface ConverterRegistry {

    /**
     * <h3>添加类型转换器</h3>
     * @param converter 类型转换器
     */
    void addConverter(Converter<?, ?> converter);

    /**
     * <h3>添加类型转换器工厂</h3>
     * @param factory 类型转换器工厂
     */
    void addConverterFactory(ConverterFactory<?, ?> factory);

    /**
     * <h3>添加泛用的类型转换器</h3>
     * <h3>注: 泛用类型转换器提供了封装能力</h3>
     */
    void addConverter(GenericConverter converter);
}
