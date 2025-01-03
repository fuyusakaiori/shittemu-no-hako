package com.org.simpleframework.core.covert.support;

import com.org.simpleframework.core.covert.converter.ConverterRegistry;

public class DefaultConversionService extends GenericConversionService {

    public DefaultConversionService() {
        addDefaultConverters(this);
    }

    public static void addDefaultConverters(ConverterRegistry registry){
        registry.addConverterFactory(new StringToNumberConverterFactory());
    }
}
