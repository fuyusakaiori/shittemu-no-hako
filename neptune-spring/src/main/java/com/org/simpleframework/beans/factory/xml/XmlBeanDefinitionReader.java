package com.org.simpleframework.beans.factory.xml;

import com.org.simpleframework.beans.exception.BeanDefinitionException;
import com.org.simpleframework.beans.factory.support.AbstractBeanDefinitionReader;
import com.org.simpleframework.beans.factory.support.BeanDefinitionRegistry;
import com.org.simpleframework.core.io.Resource;
import com.org.simpleframework.core.io.ResourceLoader;
import com.org.simpleframework.util.Assert;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

    public XmlBeanDefinitionReader(BeanDefinitionRegistry registry, ResourceLoader resourceLoader) {
        super(registry, resourceLoader);
    }

    @Override
    public int loadBeanDefinitions(Resource resource) throws BeanDefinitionException {
        Assert.notNull(resource);
        int count = 0;
        InputStream inputStream = null;
        try {
            inputStream = resource.getInputStream();
            count = doLoadBeanDefinitions(inputStream);
        }
        catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return count;
    }

    /**
     * <h3>真正读取配置文件的方法</h3>
     * <h3>注: 由于这里需要将配置文件转换成对应的文档对象, 自己实现是不可能的, 所以直接开抄, 看懂就行</h3>
     * @param inputStream 输入流
     * @return 加载的数量
     */
    protected int doLoadBeanDefinitions(InputStream inputStream){
        // 注: 按照 spring 源码的方式构建的
        return registerBeanDefinition(doLoadDocument(inputStream));
    }

    protected int registerBeanDefinition(Document document){
        BeanDefinitionDocumentReader documentReader = new DefaultBeanDefinitionDocumentReader();
        int beforeCount = getBeanDefinitionRegistry().getBeanDefinitionCount();
        documentReader.registerBeanDefinition(document, getBeanDefinitionRegistry());
        return getBeanDefinitionRegistry().getBeanDefinitionCount() - beforeCount;
    }

    //============================================= 辅助方法 =============================================

    /**
     * <h3>注: 直接使用 SAXReader 解析 XML 文件</h3>
     * @param inputStream 输入流
     * @return 文档对象
     */
    protected Document doLoadDocument(InputStream inputStream){
        SAXReader reader = new SAXReader();
        Document document = null;
        try {
            document = reader.read(inputStream);
        }
        catch (DocumentException e) {
            log.error(e.getMessage());
        }
        return document;
    }
}
