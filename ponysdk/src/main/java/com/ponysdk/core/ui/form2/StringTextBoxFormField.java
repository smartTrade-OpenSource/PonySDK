package com.ponysdk.core.ui.form2;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PTextBox;

public class StringTextBoxFormField extends FormField<String, PTextBox> {
    public StringTextBoxFormField(String caption) {
        super(caption);
    }

    public StringTextBoxFormField(String caption, boolean required) {
        super(caption, required);
    }

    @Override
    protected PTextBox createInnerWidget() {
        return Element.newPTextBox();
    }

    @Override
    public void setEnabled(boolean enabled) {
        innerWidget.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return innerWidget.isEnabled();
    }

    @Override
    public String getStringValue() {
        return innerWidget.getText();
    }

    @Override
    public String getValue() {
        return innerWidget.getText();
    }

    @Override
    public void setValue(String value) {
        innerWidget.setText(value);
    }
}
