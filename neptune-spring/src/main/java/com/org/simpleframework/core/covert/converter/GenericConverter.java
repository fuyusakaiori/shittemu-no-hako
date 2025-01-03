package com.org.simpleframework.core.covert.converter;

import java.util.Objects;
import java.util.Set;

/**
 * <h2>泛用类型转换器</h2>
 * <h3>注: 用于统一生产类型转换器的工厂和类型转换器</h3>
 */
public interface GenericConverter {

    Set<ConvertiblePair> getConvertiblePairs();

    Object convert(Object source, Class<?> sourceType, Class<?> targetType);

    /**
     * <h3>记录这个类型转换器对应的源类型和目的类型</h3>
     */
    final class ConvertiblePair{

        private final Class<?> sourceType;
        private final Class<?> targetType;

        public ConvertiblePair(Class<?> sourceType, Class<?> targetType) {
            this.sourceType = sourceType;
            this.targetType = targetType;
        }

        public Class<?> getSourceType() {
            return this.sourceType;
        }

        public Class<?> getTargetType() {
            return this.targetType;
        }

        /**
         * <h3>因为需要将转换器存放在集合中, 所以需要重写这两个方法</h3>
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;

            if (obj == null || getClass() != obj.getClass()) return false;

            ConvertiblePair that = (ConvertiblePair) obj;
            return Objects.equals(sourceType, that.sourceType) &&
                           Objects.equals(targetType, that.targetType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sourceType, targetType);
        }
    }

}
