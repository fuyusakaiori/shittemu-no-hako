package com.org.simpleframework.beans;

/**
 * <h2>键值对的方式存储属性</h2>
 * <h3>注: 保存的最有效的信息就是属性的名字和属性的值</h3>
 */
public class PropertyValue {
    /**
     * <h3>属性的名字</h3>
     */
    private final String name;

    /**
     * <h3>属性的值</h3>
     */
    private final Object value;

    public PropertyValue(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }
}
