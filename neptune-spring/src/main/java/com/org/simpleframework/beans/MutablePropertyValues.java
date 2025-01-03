package com.org.simpleframework.beans;

import java.util.ArrayList;
import java.util.List;

public class MutablePropertyValues implements PropertyValues {

    /**
     * <h3>存放属性的集合</h3>
     */
    private final List<PropertyValue> propertyValues;

    public MutablePropertyValues() {
        this.propertyValues = new ArrayList<>();
    }

    public MutablePropertyValues(List<PropertyValue> propertyValues) {
        this.propertyValues = propertyValues;
    }

    @Override
    public PropertyValue[] getPropertyValues() {
        return this.propertyValues.toArray(new PropertyValue[0]);
    }

    @Override
    public PropertyValue getPropertyValue(String propertyName) {
        for (PropertyValue propertyValue : this.propertyValues) {
            if (propertyValue.getName().equals(propertyName))
                return propertyValue;
        }
        return null;
    }

    public void addPropertyValue(PropertyValue propertyValue){
        // 1. 检查原先的集合中是否已经有这个属性了
        for (int index = 0; index < this.propertyValues.size(); index++) {
            PropertyValue currentPropertyValue = this.propertyValues.get(index);
            if (currentPropertyValue.getName().equals(propertyValue.getName())){
                // 2. 如果重复那么直接覆盖原有的属性
                this.propertyValues.set(index, propertyValue);
                return;
            }
        }
        // 3. 如果没有重复则直接添加
        this.propertyValues.add(propertyValue);
    }

    public void addPropertyValue(String name, String value){
        addPropertyValue(new PropertyValue(name, value));
    }

    @Override
    public boolean contains(String propertyName) {
        return getPropertyValue(propertyName) != null;
    }

    @Override
    public boolean isEmpty() {
        return this.propertyValues.isEmpty();
    }
}
