package com.org.simpleframework.core.covert.support;

import com.org.simpleframework.core.covert.converter.Converter;
import com.org.simpleframework.core.covert.converter.ConverterFactory;

public class StringToNumberConverterFactory implements ConverterFactory<String, Number> {

    @Override
    public <T extends Number> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToNumber<T>(targetType);
    }

    /**
     * <h3>可以将字符串转换为任何数字类型</h3>
     * @param <T>
     */
    private static final class StringToNumber <T extends Number> implements Converter<String, T>{

        private final Class<T> targetType;

        public StringToNumber(Class<T> targetType) {
            this.targetType = targetType;
        }

        /**
         * @param sourceType 源类型
         */
        @Override
        @SuppressWarnings("unchecked")
        public T convert(String sourceType) {
            if (sourceType.length() == 0)
                return null;

            if (targetType.equals(Integer.class)){
                // 注: parseInt 返回的是基本数据类型, valueOf 返回的是包装类型
                return (T) Integer.valueOf(sourceType);
            }else if (targetType.equals(Long.class)){
                return (T) Long.valueOf(sourceType);
            }else if (targetType.equals(Float.class)){
                return (T) Float.valueOf(sourceType);
            }else if (targetType.equals(Double.class)){
                return (T) Double.valueOf(sourceType);
            }else{
                throw new IllegalArgumentException();
            }
        }
    }
}
