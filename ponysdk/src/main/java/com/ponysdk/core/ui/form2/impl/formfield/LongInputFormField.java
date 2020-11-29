package com.ponysdk.core.ui.form2.impl.formfield;

public class LongInputFormField extends NumberInputFormField<Long> {
    public LongInputFormField(String caption) {
        super(caption);
    }

    @Override
    public Long getValue() {
        return Long.valueOf(input.getText());
    }
}
