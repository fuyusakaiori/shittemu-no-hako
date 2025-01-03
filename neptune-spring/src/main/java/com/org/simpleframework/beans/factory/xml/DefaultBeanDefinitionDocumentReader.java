package com.org.simpleframework.beans.factory.xml;

import cn.hutool.core.util.StrUtil;
import com.org.simpleframework.beans.MutablePropertyValues;
import com.org.simpleframework.beans.PropertyValue;
import com.org.simpleframework.beans.exception.BeansException;
import com.org.simpleframework.beans.factory.config.BeanDefinition;
import com.org.simpleframework.beans.factory.config.BeanReference;
import com.org.simpleframework.beans.factory.config.RuntimeBeanNameReference;
import com.org.simpleframework.beans.factory.support.BeanDefinitionRegistry;
import com.org.simpleframework.beans.factory.support.GenericBeanDefinition;
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.List;

public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {

    /**
     * <h3>需要解析的属性</h3>
     */
    public static final String BEAN_ELEMENT = "bean";
    public static final String PROPERTY_ELEMENT = "property";
    public static final String ID_ATTRIBUTE = "id";
    public static final String NAME_ATTRIBUTE = "name";
    public static final String CLASS_ATTRIBUTE = "class";
    public static final String VALUE_ATTRIBUTE = "value";
    public static final String REF_ATTRIBUTE = "ref";
    public static final String INIT_METHOD_ATTRIBUTE = "init-method";
    public static final String DESTROY_METHOD_ATTRIBUTE = "destroy-method";
    public static final String SCOPE_ATTRIBUTE = "scope";
    public static final String BASE_PACKAGE_ATTRIBUTE = "base-package";
    public static final String COMPONENT_SCAN_ELEMENT = "component-scan";

    /**
     * <h3>获取对象的根结点, 然后开始扫描</h3>
     * @param document 文档对象
     * @param beanDefinitionRegistry 注册中心
     */
    @Override
    public void registerBeanDefinition(Document document, BeanDefinitionRegistry beanDefinitionRegistry) {
        doRegisterBeanDefinitions(document.getRootElement(), beanDefinitionRegistry);
    }

    /**
     * <h3>真正解析</h3>
     * @param root 文档对象根结点
     * @param beanDefinitionRegistry 注册中心
     */
    private void doRegisterBeanDefinitions(Element root, BeanDefinitionRegistry beanDefinitionRegistry) {

        Element componentScan = root.element(COMPONENT_SCAN_ELEMENT);
        // TODO 注解扫描暂时不支持

        List<Element> beanList = root.elements(BEAN_ELEMENT);
        for (Element bean : beanList) {
            // 1. 获取各个属性的对应的值
            String beanId = bean.attributeValue(ID_ATTRIBUTE);
            String beanName = bean.attributeValue(NAME_ATTRIBUTE);
            String className = bean.attributeValue(CLASS_ATTRIBUTE);
            String initMethodName = bean.attributeValue(INIT_METHOD_ATTRIBUTE);
            String destroyMethodName = bean.attributeValue(DESTROY_METHOD_ATTRIBUTE);
            String beanScope = bean.attributeValue(SCOPE_ATTRIBUTE);
            // 2. 通过类的全限定名动态地将类加载进入内存
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new BeansException("Cannot find class [" + className + "]");
            }
            // 3. 优先将 id 属性作为对象的名字; 如果没有 id 属性那么就采用 name 属性作为对象的名字
            beanName = StrUtil.isNotEmpty(beanId) ? beanId : beanName;
            if (StrUtil.isEmpty(beanName)) {
                // 如果 id 属性和 name 属性都为空的话, 就将类的名字的第一个字母小写作为对象的名字
                beanName = StrUtil.lowerFirst(clazz.getSimpleName());
            }
            // 4. 直接创建 BeanDefinition 实例
            BeanDefinition beanDefinition = new GenericBeanDefinition();
            // 5. 给 BeanDefinition 实例设置好相应的属性
            beanDefinition.setBeanClass(clazz);
            beanDefinition.setInitMethodName(initMethodName);
            beanDefinition.setDestroyMethodName(destroyMethodName);
            beanDefinition.setPropertyValues(new MutablePropertyValues());
            if (StrUtil.isNotEmpty(beanScope)) {
                beanDefinition.setScope(beanScope);
            }
            // 6. 给 BeanDefinition 设置拥有的成员变量
            List<Element> propertyList = bean.elements(PROPERTY_ELEMENT);
            for (Element property : propertyList) {
                // 7. 获取成员变量的名字, 值, 以及是否是引用对象
                String propertyNameAttribute = property.attributeValue(NAME_ATTRIBUTE);
                String propertyValueAttribute = property.attributeValue(VALUE_ATTRIBUTE);
                String propertyRefAttribute = property.attributeValue(REF_ATTRIBUTE);

                if (StrUtil.isEmpty(propertyNameAttribute)) {
                    throw new BeansException("The name attribute cannot be null or empty");
                }
                Object value = propertyValueAttribute;
                if (StrUtil.isNotEmpty(propertyRefAttribute)) {
                    value = new RuntimeBeanNameReference(propertyRefAttribute);
                }
                // 8. 将成员变量的名字和值封装在 PropertyValue 对象中
                PropertyValue propertyValue = new PropertyValue(propertyNameAttribute, value);
                // 9. 将 PropertyValue 对象放入集合中
                beanDefinition.getPropertyValues().addPropertyValue(propertyValue);
            }
            // 10. BeanDefinition 不可以重名
            if (beanDefinitionRegistry.containsBeanDefinition(beanName)) {
                throw new BeansException("Duplicate beanName[" + beanName + "] is not allowed");
            }
            // 11. 向注册中心注册 BeanDefinition 实例对象
            beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinition);
        }

    }
}
