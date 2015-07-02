
package com.ponysdk.ui.server.form2.formfield;

import com.ponysdk.ui.server.basic.PPasswordTextBox;
import com.ponysdk.ui.server.form2.dataconverter.IdentityConverter;

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
