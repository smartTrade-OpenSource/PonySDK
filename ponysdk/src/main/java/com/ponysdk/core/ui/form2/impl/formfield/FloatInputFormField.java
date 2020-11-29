package com.ponysdk.core.ui.form2.impl.formfield;

public class FloatInputFormField extends NumberInputFormField<Float> {
    public FloatInputFormField(String caption) {
        super(caption);
    }

    @Override
    public Float getValue() {
        return Float.valueOf(input.getText());
    }
}
