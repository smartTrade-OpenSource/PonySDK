
package com.ponysdk.core.ui.form.formfield;

import com.ponysdk.core.ui.basic.PPasswordTextBox;
import com.ponysdk.core.ui.form.dataconverter.IdentityConverter;

public class StringPasswordTextBoxFormField extends PasswordTextBoxFormField<String> {

    public StringPasswordTextBoxFormField() {
        this(new PPasswordTextBox());
    }

    public StringPasswordTextBoxFormField(final PPasswordTextBox textBox) {
        super(textBox, IdentityConverter.STRING);
    }

    @Override
    public String getValue() {
        final String v = super.getValue();
        if (v == null || v.isEmpty()) return null;
        return v;
    }
}
