package com.ponysdk.core.ui.datagrid.dynamic;

import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Predicate;

public class Configuration<DataType> {

    private static final Function<String, String> DEFAULT_TRANSFORM = s -> s.replaceAll("^get|^set|^is", "").toUpperCase();
    private static final Predicate<Method> DEFAULT_FILTER = m -> m.getReturnType() != Void.TYPE && 0 == m.getParameterCount();

    private final Class<DataType> type;

    private Function<String, String> captionTransform;

    private Predicate<Method> filter;

    public Configuration(Class<DataType> type) {
        this.type = type;
        this.captionTransform = DEFAULT_TRANSFORM;
        this.filter = DEFAULT_FILTER;
    }

    public void setCaptionTransform(Function<String, String> captionTransform){
        this.captionTransform = captionTransform;
    }

    public void setFilter(Predicate<Method> filter){
        this.filter = filter;
    }

    public Class<DataType> getType() {
        return type;
    }

    public Function<String, String> getCaptionTransform() {
        return captionTransform;
    }

    public Predicate<Method> getFilter() {
        return filter;
    }

}