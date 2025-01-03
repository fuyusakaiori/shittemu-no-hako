package com.org.simpleframework.beans.exception;

public class NoSuchBeanDefinitionException extends RuntimeException
{
    public NoSuchBeanDefinitionException(String message)
    {
        super(message);
    }

    public NoSuchBeanDefinitionException(Throwable cause)
    {
        super(cause);
    }
}
