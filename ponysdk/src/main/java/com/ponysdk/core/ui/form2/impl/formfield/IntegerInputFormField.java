package com.ponysdk.core.ui.form2.impl.formfield;

public class IntegerInputFormField extends NumberInputFormField<Integer> {
    public IntegerInputFormField(String caption) {
        super(caption);
    }

    @Override
    public Integer getValue() {
        return Integer.valueOf(input.getText());
    }
}
