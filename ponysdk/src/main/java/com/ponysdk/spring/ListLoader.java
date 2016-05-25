
package com.ponysdk.spring;

import java.util.ArrayList;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ListLoader extends ArrayList<Object> implements ApplicationContextAware {

    private static final long serialVersionUID = 1L;

    private Class<?> type;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, ?> map = applicationContext.getBeansOfType(type);
        addAll(map.values());
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

}
