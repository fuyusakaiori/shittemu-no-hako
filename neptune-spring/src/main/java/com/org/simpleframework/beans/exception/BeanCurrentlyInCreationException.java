package com.org.simpleframework.beans.exception;

public class BeanCurrentlyInCreationException extends RuntimeException
{
    public BeanCurrentlyInCreationException(String message)
    {
        super(message);
    }

    public BeanCurrentlyInCreationException(Throwable cause)
    {
        super(cause);
    }
}
