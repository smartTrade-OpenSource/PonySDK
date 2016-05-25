
package com.ponysdk.ui.server.form.dataconverter;

public class IntegerConverter implements DataConverter<String, Integer> {

    @Override
    public String from(final Integer t) {
        if (t == null) return null;
        return t.toString();
    }

    @Override
    public Integer to(final String t) {
        if (t == null || t.isEmpty()) return null;
        return Integer.parseInt(t);
    }

}
