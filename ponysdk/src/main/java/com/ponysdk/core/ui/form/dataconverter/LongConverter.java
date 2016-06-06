
package com.ponysdk.core.ui.form.dataconverter;

public class LongConverter implements DataConverter<String, Long> {

    @Override
    public String from(final Long t) {
        if (t == null) return null;
        return t.toString();
    }

    @Override
    public Long to(final String t) {
        if (t == null || t.isEmpty()) return null;
        return Long.parseLong(t);
    }

}
