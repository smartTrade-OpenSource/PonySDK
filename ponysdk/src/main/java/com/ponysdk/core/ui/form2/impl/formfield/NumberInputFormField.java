package com.ponysdk.core.ui.form2.impl.formfield;

import com.ponysdk.core.ui.basic.PTextBox;

import java.util.Objects;

public abstract class NumberInputFormField<T extends Number> extends AbstractInputFormField<T> {
    public NumberInputFormField(String caption) {
        super(caption);
    }

    @Override
    protected PTextBox createInnerWidget() {
        PTextBox input = super.createInnerWidget();
        input.setAttribute("type", "number");
        return input;
    }

    @Override
    public void setValue(T value) {
        input.setText(Objects.requireNonNullElse(value, "").toString());
    }
}
