package com.ponysdk.core.ui.form2.impl.formfield;

import com.ponysdk.core.ui.basic.PTextBox;

public class FloatInputFormField extends NumberInputFormField<Float> {

    public FloatInputFormField(final String caption) {
        super(caption);
    }

    @Override
    public Float getValue() {
        if (input.getText().isEmpty()) return null;
        return Float.valueOf(input.getText());
    }

    @Override
    protected PTextBox createInnerWidget() {
        final PTextBox input = super.createInnerWidget();
        input.setAttribute("step", "any");
        return input;
    }
}
