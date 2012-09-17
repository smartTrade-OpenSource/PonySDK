
package com.ponysdk.ui.server.form2.dataconverter;

public class IntegerConverter implements DataConverter<String, Integer> {

    @Override
    public String from(final Integer t) {
        return t.toString();
    }

    @Override
    public Integer to(final String t) {
        return Integer.parseInt(t);
    }

}
