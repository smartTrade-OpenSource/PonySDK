package com.ponysdk.core.ui.form2.impl.formfield;

import com.ponysdk.core.ui.basic.PTextBox;

public class DoubleInputFormField extends NumberInputFormField<Double> {

    public DoubleInputFormField(final String caption) {
        super(caption);
    }

    @Override
    public Double getValue() {
        if (input.getText().isEmpty()) return null;
        return Double.valueOf(input.getText());
    }

    @Override
    protected PTextBox createInnerWidget() {
        final PTextBox input = super.createInnerWidget();
        input.setAttribute("step", "any");
        return input;
    }
}
