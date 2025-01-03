package com.org.simpleframework.beans.exception;

public class ApplicationContextException extends RuntimeException
{
    public ApplicationContextException(String message)
    {
        super(message);
    }

    public ApplicationContextException(Throwable cause)
    {
        super(cause);
    }
}
