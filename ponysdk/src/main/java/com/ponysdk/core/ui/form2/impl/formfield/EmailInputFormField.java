package com.ponysdk.core.ui.form2.impl.formfield;

import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.form2.impl.validator.EmailValidator;

public class EmailInputFormField extends AbstractInputFormField<String> {

    public EmailInputFormField(String caption) {
        super(caption);
        setValidator(new EmailValidator());
    }

    @Override
    protected PTextBox createInnerWidget() {
        PTextBox input = super.createInnerWidget();
        input.setAttribute("type", "email");
        return input;
    }

    @Override
    public void doSetValue(String email) {
        input.setText(email);
    }

}
