
package com.ponysdk.ui.server.form2.formfield;

import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.form2.dataconverter.IntegerConverter;

public class IntegerTextBoxFormField extends TextBoxFormField<Integer> {

    public IntegerTextBoxFormField() {
        this(new PTextBox());
    }

    public IntegerTextBoxFormField(final PTextBox textBox) {
        super(textBox, new IntegerConverter());
    }
}
