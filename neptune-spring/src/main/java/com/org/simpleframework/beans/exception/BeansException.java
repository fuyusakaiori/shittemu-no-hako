package com.org.simpleframework.beans.exception;

/**
 * <h2>无法找到 Bean 实例异常</h2>
 */
public class BeansException extends RuntimeException{

    public BeansException(String message) {
        super(message);
    }

    public BeansException(String message, Throwable cause) {
        super(message, cause);
    }
}
