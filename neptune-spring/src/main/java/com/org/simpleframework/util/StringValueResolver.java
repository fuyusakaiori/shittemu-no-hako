package com.org.simpleframework.util;

/**
 * <h3>负责解析出占位符对应的值</h3>
 */
public interface StringValueResolver {

    /**
     * <h3>${...} 内容 => 实际值</h3>
     */
    String resolveStringValue(String strVal);

}
