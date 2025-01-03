package com.org.simpleframework.beans.exception;

public class BeanDefinitionException extends RuntimeException {
    public BeanDefinitionException(String message)
    {
        super(message);
    }

    public BeanDefinitionException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
