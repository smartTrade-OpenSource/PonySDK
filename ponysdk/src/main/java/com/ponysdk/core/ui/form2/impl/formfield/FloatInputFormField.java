package com.ponysdk.core.ui.form2.impl.formfield;

public class FloatInputFormField extends NumberInputFormField<Float> {
    public FloatInputFormField(String caption) {
        super(caption);
    }

    @Override
    public Float getValue() {
        if (input.getText().isEmpty()) return null;
        return Float.valueOf(input.getText());
    }
}
