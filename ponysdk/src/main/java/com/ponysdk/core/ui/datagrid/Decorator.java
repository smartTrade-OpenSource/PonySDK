package com.ponysdk.core.ui.datagrid;

import java.util.Objects;

class Decorator<DataType> {

    final DataType data;
    private final Object key;

    Decorator(final Object key, final DataType data) {
        this.key = key;
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Decorator<?> decorator = (Decorator<?>) o;
        return Objects.equals(key, decorator.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}