package com.org.simpleframework.beans;

import java.util.Arrays;
import java.util.Iterator;

/**
 * <h2>负责遍历 Bean 实例中的所有属性</h2>
 */
public interface PropertyValues extends Iterable<PropertyValue> {

    /**
     * <h3>获取迭代器</h3>
     */
    default Iterator<PropertyValue> iterator(){
        return Arrays.asList(getPropertyValues()).iterator();
    }

    /**
     * <h3>获取所有的属性</h3>
     * @return 属性数组
     */
    PropertyValue[] getPropertyValues();

    /**
     * <h3>根据名字获取单个属性</h3>
     * @return 属性
     */
    PropertyValue getPropertyValue(String propertyName);

    void addPropertyValue(PropertyValue propertyValue);

    void addPropertyValue(String name, String value);

    /**
     * <h3>是否包含某个属性</h3>
     * @param propertyName 属性名字
     * @return 是否包含
     */
    boolean contains(String propertyName);

    /**
     * <h3>属性集合是否为空</h3>
     */
    boolean isEmpty();

}
