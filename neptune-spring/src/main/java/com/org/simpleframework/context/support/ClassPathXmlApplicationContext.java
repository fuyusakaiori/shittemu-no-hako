package com.org.simpleframework.context.support;

/**
 * <h2>最底层的高级容器</h2>
 * <h3>仅负责获取配置文件的路径</h3>
 */
public class ClassPathXmlApplicationContext extends AbstractXmlApplicationContext {

    private final String[] locations;

    public ClassPathXmlApplicationContext(String location){
        this(new String[]{location});
    }

    /**
     * <h3>这也太怪了, 我都傻了, 这里调用抽象类中的 refresh 方法</h3>
     * <h3>refresh 方法最后又会调用这个子类里面的获取配置文件路径的方法</h3>
     * <h3>为什么要这样设计啊? 循环调用回来了</h3>
     * @param locations
     */
    public ClassPathXmlApplicationContext(String[] locations) {
        // 1. 传入配文件路径后, 自动启动容器
        this.locations = locations;
        // 2. 自动启动容器
        refresh();
    }

    @Override
    protected String[] getDefaultConfigLocations() {
        return this.locations;
    }
}
