
package com.ponysdk.ui.server.form2.dataconverter;

public class FloatConverter implements DataConverter<String, Float> {

    @Override
    public String from(final Float t) {
        return t.toString();
    }

    @Override
    public Float to(final String t) {
        if (t == null || t.isEmpty()) return null;
        return Float.parseFloat(t);
    }

}
