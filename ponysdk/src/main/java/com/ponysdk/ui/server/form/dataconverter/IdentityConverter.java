
package com.ponysdk.ui.server.form.dataconverter;

public class IdentityConverter<T> implements DataConverter<T, T> {

    public static IdentityConverter<String> STRING = new IdentityConverter<>();

    @Override
    public T from(final T t) {
        return t;
    }

    @Override
    public T to(final T t) {
        return t;
    }

}
