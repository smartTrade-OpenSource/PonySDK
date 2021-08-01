package com.ponysdk.core.ui.form2.impl.formfield;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.form2.api.FormField;
import com.ponysdk.core.ui.form2.api.FormFieldValidator;
import com.ponysdk.core.ui.form2.api.ValidationResult;

public abstract class AbstractInputFormField<V> extends FormField<V> {
    protected PTextBox input;

    public AbstractInputFormField(String caption) {
        super(caption);
    }

    @Override
    protected PTextBox createInnerWidget() {
        input = Element.newPTextBox();
        input.setTabindex(PWidget.TabindexMode.TABULABLE);
        configureBehaviour();
        return input;
    }

    @Override
    protected ValidationResult doValidation(FormFieldValidator validator, V value) {
        if (validator == null) return ValidationResult.OK();
        return validator.isValid(input.getText());
    }

    protected void configureBehaviour() {
        input.addValueChangeHandler(e -> validate());
    }

    @Override
    public void enable() {
        input.removeAttribute("disabled");
    }

    @Override
    public void disable() {
        input.setAttribute("disabled");
    }

    @Override
    public boolean isEnabled() {
        return !input.hasAttribute("disabled");
    }

    @Override
    public void focus() {
        input.focus();
    }

    @Override
    public void doSetValue(V value) {
        input.setText(value.toString());
    }
}
