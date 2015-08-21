
package com.ponysdk.ui.server.form.dataconverter;

public class DoubleConverter implements DataConverter<String, Double> {

    @Override
    public String from(final Double t) {
        if (t == null) return null;
        return t.toString();
    }

    @Override
    public Double to(final String t) {
        if (t == null || t.isEmpty()) return null;
        return Double.parseDouble(t);
    }

}
