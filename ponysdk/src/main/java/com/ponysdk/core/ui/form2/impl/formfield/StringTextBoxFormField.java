package com.ponysdk.core.ui.form2.impl.formfield;

public class StringTextBoxFormField extends AbstractInputFormField<String> {

    public StringTextBoxFormField(String caption) {
        super(caption);
    }

    @Override
    public String getValue() {
        return input.getText();
    }

    @Override
    public void setValue(String value) {
        input.setText(value);
    }
}
