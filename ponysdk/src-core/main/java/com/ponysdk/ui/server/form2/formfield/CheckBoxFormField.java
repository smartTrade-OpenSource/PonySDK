
package com.ponysdk.ui.server.form2.formfield;

import com.ponysdk.ui.server.basic.PCheckBox;
import com.ponysdk.ui.server.basic.PWidget;

public class CheckBoxFormField extends FormField<Boolean> {

    private final PCheckBox checkBox;

    public CheckBoxFormField() {
        this(new PCheckBox());
    }

    public CheckBoxFormField(final PCheckBox textBox) {
        super(null);
        this.checkBox = textBox;
    }

    @Override
    public PWidget asWidget() {
        return checkBox;
    }

    @Override
    public void reset() {
        checkBox.setValue(false);
    }

    @Override
    public Boolean getValue() {
        return checkBox.getValue();
    }

    @Override
    public void setValue(final Boolean value) {
        checkBox.setValue(value);
    }

    @Override
    protected String getStringValue() {
        return checkBox.getValue().toString();
    }

}
