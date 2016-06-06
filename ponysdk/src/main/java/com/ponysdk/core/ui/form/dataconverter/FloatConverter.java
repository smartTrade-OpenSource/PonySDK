
package com.ponysdk.core.ui.form.dataconverter;

public class FloatConverter implements DataConverter<String, Float> {

    @Override
    public String from(final Float t) {
        if (t == null) return null;
        return t.toString();
    }

    @Override
    public Float to(final String t) {
        if (t == null || t.isEmpty()) return null;
        return Float.parseFloat(t);
    }

}
