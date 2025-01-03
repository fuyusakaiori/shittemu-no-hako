package com.org.simpleframework.core.covert.support;

import cn.hutool.core.convert.BasicType;
import com.org.simpleframework.core.covert.ConversionService;
import com.org.simpleframework.core.covert.converter.Converter;
import com.org.simpleframework.core.covert.converter.ConverterFactory;
import com.org.simpleframework.core.covert.converter.ConverterRegistry;
import com.org.simpleframework.core.covert.converter.GenericConverter;
import com.org.simpleframework.core.covert.converter.GenericConverter.ConvertiblePair;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class GenericConversionService implements ConversionService, ConverterRegistry {

    private final Map<ConvertiblePair, GenericConverter> converters = new ConcurrentHashMap<>();

    //================================ 负责获取类型转换器 ================================

    @Override
    public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
        // 1. 获取类型转换器
        GenericConverter converter = getConverter(sourceType, targetType);
        // 2. 只要类型转换器不是空的, 那么就可以转换
        return converter != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convert(Object source, Class<T> targetType) {
        // 1. 获取源对象的实际类型
        Class<?> sourceType = source.getClass();
        // 2. 目标类型可能是基本数据类型, 将其转换为包装类型
        targetType = (Class<T>) BasicType.wrap(targetType);
        // 3. 获取类型转换器开始转换
        GenericConverter converter = getConverter(sourceType, targetType);
        // 4. 使用转换器得到对应的结果, 最后强制转换即可
        return (T) converter.convert(source, sourceType, targetType);
    }


    //================================ 负责添加类型转换器 ================================
    @Override
    public void addConverter(Converter<?, ?> converter) {
        // 1. 获取转换器需要转换的原始类型和目标类型
        ConvertiblePair typeInfo = getRequiredTypeInfo(converter);
        ConverterAdapter adapter = new ConverterAdapter(typeInfo, converter);
        // 2. 放入类型转换器集合中
        synchronized (this.converters){
            this.converters.put(typeInfo, adapter);
        }
    }

    @Override
    public void addConverterFactory(ConverterFactory<?, ?> factory) {
        ConvertiblePair typeInfo = getRequiredTypeInfo(factory);
        ConverterAdapterFactory adapter = new ConverterAdapterFactory(typeInfo, factory);
        synchronized (this.converters){
            this.converters.put(typeInfo, adapter);
        }
    }

    @Override
    public void addConverter(GenericConverter converter) {
        synchronized (this.converters) {
            for (ConvertiblePair convertiblePair : converter.getConvertiblePairs()) {
                this.converters.put(convertiblePair, converter);
            }
        }
    }

    //================================= 辅助方法 =================================

    /**
     * <h3>根据源类型和目标类型获取类型转换器</h3>
     * @param sourceType 源类型
     * @param targetType 目标类型
     * @return 类型转换器
     */
    protected GenericConverter getConverter(Class<?> sourceType, Class<?> targetType){
        // 1. 分别获取源类型和目标类型的父类, 防止没有对应的子类转换器, 就可以使用父类的转换器
        List<Class<?>> sourceSuperClassType = getSuperClassType(sourceType);
        List<Class<?>> targetSuperClassType = getSuperClassType(targetType);
        // 2. 然后集合中的类型两两组合, 挑选对应的类型转换器
        for (Class<?> source : sourceSuperClassType) {
            for (Class<?> target : targetSuperClassType) {
                ConvertiblePair convertiblePair = new ConvertiblePair(source, target);
                // 有可能得到的是工厂, 也有可能得到的是转换器, 不过通过适配器模式都封装成实现了 GenericConverter 接口的适配器类
                GenericConverter converter = this.converters.get(convertiblePair);
                if (converter != null)
                    return converter;
            }
        }
        return null;
    }

    protected List<Class<?>> getSuperClassType(Class<?> clazz){
        List<Class<?>> candidate = new LinkedList<>();
        // 1. 基本数据类型转换为包装类型
        clazz = BasicType.wrap(clazz);
        while (clazz != null){
            candidate.add(clazz);
            clazz = clazz.getSuperclass();
        }
        return candidate;
    }

    /**
     * <h3>获取泛型中的原始类型和目标类型</h3>
     * @param object 类型转换器
     */
    private ConvertiblePair getRequiredTypeInfo(Object object) {
        Type[] types = object.getClass().getGenericInterfaces();
        ParameterizedType parameterized = (ParameterizedType) types[0];
        Type[] actualTypeArguments = parameterized.getActualTypeArguments();
        Class<?> sourceType = (Class<?>) actualTypeArguments[0];
        // TODO 这里存在问题, 不知道为什么第二个泛型获取不到, 如果是 Converter 接口的话
        Class<?> targetType = (Class<?>) actualTypeArguments[1];
        return new ConvertiblePair(sourceType, targetType);
    }

    /**
     * <h3>适配器模式</h3>
     * <h3>注: 将 Converter 类型封装成 GenericConverter </h3>
     */
    private static final class ConverterAdapter implements GenericConverter{
        /**
         * <h3>每个类型转换器仅有一个目标类型和原始类</h3>
         */
        private final ConvertiblePair convertiblePair;

        private final Converter<Object, Object> converter;

        @SuppressWarnings("unchecked")
        public ConverterAdapter(ConvertiblePair convertiblePair, Converter<?, ?> converter) {
            this.convertiblePair = convertiblePair;
            this.converter = (Converter<Object, Object>) converter;
        }

        @Override
        public Set<ConvertiblePair> getConvertiblePairs() {
            return Collections.singleton(convertiblePair);
        }

        @Override
        public Object convert(Object source, Class<?> sourceType, Class<?> targetType) {
            log.debug("原始类型: {}", sourceType);
            log.debug("目标类型: {}", targetType);
            return this.converter.convert(source);
        }
    }

    /**
     * <h3>适配器工厂</h3>
     */
    private static final class ConverterAdapterFactory implements GenericConverter{

        private final ConvertiblePair convertiblePair;

        private final ConverterFactory<Object, Object> converterFactory;

        @SuppressWarnings("unchecked")
        public ConverterAdapterFactory(ConvertiblePair convertiblePair, ConverterFactory<?, ?> converterFactory)
        {
            this.convertiblePair = convertiblePair;
            this.converterFactory = (ConverterFactory<Object, Object>) converterFactory;
        }

        @Override
        public Set<ConvertiblePair> getConvertiblePairs() {
            return Collections.singleton(convertiblePair);
        }

        @Override
        public Object convert(Object source, Class<?> sourceType, Class<?> targetType) {
            return this.converterFactory.getConverter(targetType).convert(source);
        }
    }
}
