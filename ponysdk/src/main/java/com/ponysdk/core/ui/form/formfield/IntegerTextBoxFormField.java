
package com.ponysdk.core.ui.form.formfield;

import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.form.dataconverter.IntegerConverter;

public class IntegerTextBoxFormField extends TextBoxFormField<Integer> {

    public IntegerTextBoxFormField() {
        this(new PTextBox());
    }

    public IntegerTextBoxFormField(final PTextBox textBox) {
        super(textBox, new IntegerConverter());
    }
}
