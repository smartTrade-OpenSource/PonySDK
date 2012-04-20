
package com.ponysdk.ui.server.form2.formfield;

import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.form2.dataconverter.IdentityConverter;

public class StringTextBoxFormField extends TextBoxFormField<String> {

    public StringTextBoxFormField() {
        this(new PTextBox());
    }

    public StringTextBoxFormField(final PTextBox textBox) {
        super(textBox, IdentityConverter.STRING);
    }
}
