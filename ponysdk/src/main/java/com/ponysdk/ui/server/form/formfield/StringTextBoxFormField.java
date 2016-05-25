
package com.ponysdk.ui.server.form.formfield;

import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.form.dataconverter.IdentityConverter;

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
