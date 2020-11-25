package com.ponysdk.core.ui.form2.impl;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.form2.api.FormField;

public class StringTextBoxFormField extends FormField<String> {

    private PTextBox innerWidget;

    public StringTextBoxFormField(String caption) {
        super(caption);
    }

    @Override
    protected PTextBox createInnerWidget() {
        innerWidget = Element.newPTextBox();
        innerWidget.setTabindex(PWidget.TabindexMode.TABULABLE);
        innerWidget.addBlurHandler(e -> validate());
        innerWidget.addValueChangeHandler(e -> validate());
        return innerWidget;
    }

    @Override
    public void enable() {
        innerWidget.setEnabled(true);
    }

    @Override
    public void disable() {
        innerWidget.setEnabled(false);
    }

    @Override
    public boolean isEnabled() {
        return innerWidget.isEnabled();
    }

    @Override
    public String getStringToValidate() {
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
