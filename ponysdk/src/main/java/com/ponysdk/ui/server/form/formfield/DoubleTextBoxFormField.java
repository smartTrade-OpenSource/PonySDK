
package com.ponysdk.ui.server.form.formfield;

import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.form.dataconverter.DoubleConverter;

public class DoubleTextBoxFormField extends TextBoxFormField<Double> {

    public DoubleTextBoxFormField() {
        this(new PTextBox());
    }

    public DoubleTextBoxFormField(final PTextBox textBox) {
        super(textBox, new DoubleConverter());
    }
}
