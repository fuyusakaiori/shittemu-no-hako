package com.org.simpleframework.beans.factory;

import java.util.Map;

/**
 * <h2>扩展顶级容器接口的能力</h2>
 * <h3>注: 源码中应该考虑到扩展性, 还提供了很多其他方法, 但是有些方法已经在其他接口提供了, 感觉重复</h3>
 */
public interface ListableBeanFactory extends BeanFactory {

    /**
     * <h3>根据同类型的所有实例对象, 并将名字一起返回</h3>
     * @param type 类型
     * @return 对象名字和实例组合的集合
     */
    <T> Map<String, T> getBeansOfType(Class<T> type);


}
