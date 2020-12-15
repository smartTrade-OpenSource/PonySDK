package com.ponysdk.core.ui.form2.impl.formfield;

public class IntegerInputFormField extends NumberInputFormField<Integer> {
    public IntegerInputFormField(String caption) {
        super(caption);
    }

    @Override
    public Integer getValue() {
        if (input.getText().isEmpty()) return null;
        return Integer.valueOf(input.getText());
    }
}
