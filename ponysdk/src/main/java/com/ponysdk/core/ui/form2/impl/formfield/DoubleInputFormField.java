package com.ponysdk.core.ui.form2.impl.formfield;

public class DoubleInputFormField extends NumberInputFormField<Double> {
    public DoubleInputFormField(String caption) {
        super(caption);
    }

    @Override
    public Double getValue() {
        return Double.valueOf(input.getText());
    }
}
