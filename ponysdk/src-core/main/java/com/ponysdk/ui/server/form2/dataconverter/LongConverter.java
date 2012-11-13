
package com.ponysdk.ui.server.form2.dataconverter;

public class LongConverter implements DataConverter<String, Long> {

    @Override
    public String from(final Long t) {
        return t.toString();
    }

    @Override
    public Long to(final String t) {
        if (t == null || t.isEmpty()) return null;
        return Long.parseLong(t);
    }

}
