package com.org.simpleframework.beans.factory.config;

public class RuntimeBeanNameReference implements BeanReference {

    private final String name;

    public RuntimeBeanNameReference(String name) {
        this.name = name;
    }

    @Override
    public String getBeanName() {
        return this.name;
    }
}
