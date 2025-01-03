package com.org.simpleframework.util;

public abstract class Assert {

    /**
     * <h3>断言传入的参数不为空, 如果为空, 直接抛出异常</h3>
     */
    public static void notNull(Object object, String beanName){
        if (object == null || beanName == null)
            throw new IllegalArgumentException();
    }

    public static void notNull(Object beanName){
        if (beanName == null)
            throw new IllegalArgumentException();
    }

    public static void isTrue(boolean flag){
        if (!flag)
            throw new IllegalStateException();
    }
}
