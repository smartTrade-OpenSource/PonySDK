
package com.ponysdk.core.ui.form.formfield;

import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.form.dataconverter.IdentityConverter;

public class StringTextBoxFormField extends TextBoxFormField<String> {

    public StringTextBoxFormField() {
        this(new PTextBox());
    }

    public StringTextBoxFormField(final PTextBox textBox) {
        super(textBox, IdentityConverter.STRING);
    }

    @Override
    public String getValue() {
        final String v = super.getValue();
        if (v == null || v.isEmpty()) return null;
        return v;
    }
}
