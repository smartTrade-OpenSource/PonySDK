package com.ponysdk.core.ui.form2.impl.formfield;

import java.math.BigDecimal;

public class BigDecimalInputFormField extends NumberInputFormField<BigDecimal> {
    public BigDecimalInputFormField(String caption) {
        super(caption);
    }

    @Override
    public BigDecimal getValue() {
        if (input.getText().isEmpty()) return null;
        return BigDecimal.valueOf(Long.parseLong(input.getText()));
    }
}
