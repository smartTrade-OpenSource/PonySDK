package com.ponysdk.core.ui.datagrid.dynamic;

import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Predicate;

public class Configuration<DataType> {

    public static final Function<String, String> DEFAULT_TRANSFORM = s -> s.replaceAll("^get|^set|^is", "").toUpperCase();
    public static final Predicate<Method> DEFAULT_FILTER = m -> m.getReturnType() != Void.TYPE && 0 == m.getParameterCount();

    private final Class<DataType> type;

    private final Function<String, String> captionTransform;

    private final Predicate<Method> filter;

    public Configuration(Class<DataType> type) {
        this.type = type;
        this.captionTransform = DEFAULT_TRANSFORM;
        this.filter = DEFAULT_FILTER;
    }

    public Configuration(Class<DataType> type, Function<String, String> captionTransform, Predicate<Method> filter) {
        this.type = type;
        this.captionTransform = captionTransform;
        this.filter = DEFAULT_FILTER.and(filter);
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