package com.org.simpleframework.beans.factory;

import com.org.simpleframework.beans.exception.BeansException;

public interface ObjectFactory<T> {

    T getObject() throws BeansException;

}
