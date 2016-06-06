
package com.ponysdk.core.ui.form.formfield;

import com.ponysdk.core.ui.basic.PCheckBox;

public class CheckBoxFormField extends FormField<Boolean, PCheckBox> {

    public CheckBoxFormField() {
        this(new PCheckBox());
    }

    public CheckBoxFormField(final PCheckBox widget) {
        super(widget, null);
    }

    @Override
    public void reset0() {
        widget.setValue(false);
    }

    @Override
    public Boolean getValue() {
        return widget.getValue();
    }

    @Override
    public void setValue(final Boolean value) {
        widget.setValue(value);
    }

    @Override
    protected String getStringValue() {
        return widget.getValue().toString();
    }

    @Override
    public void setEnabled(final boolean enabled) {
        widget.setEnabled(enabled);
    }

}
