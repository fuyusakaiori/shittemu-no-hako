package com.org.simpleframework.core.covert.converter;

/**
 * <h2>类型转换器工厂</h2>
 * <h3>工厂可以根据提供的原始类型和目标类型生成对应的类型转换器</h3>
 * <h3>工厂类型就是我们要具体实现的</h3>
 */
public interface ConverterFactory<S, R> {

    /**
     * <h3>注: R 就是用来限制能够产生哪些类型转换器 </h3>
     */
    <T extends R> Converter<S, T> getConverter(Class<T> targetType);

}
