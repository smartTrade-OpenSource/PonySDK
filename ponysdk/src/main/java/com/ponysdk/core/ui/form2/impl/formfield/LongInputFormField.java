package com.ponysdk.core.ui.form2.impl.formfield;

public class LongInputFormField extends NumberInputFormField<Long> {
    public LongInputFormField(String caption) {
        super(caption);
    }

    @Override
    public Long getValue() {
        if (input.getText().isEmpty()) return null;
        return Long.valueOf(input.getText());
    }
}
