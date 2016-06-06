
package com.ponysdk.core.ui.form.formfield;

import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.form.dataconverter.DoubleConverter;

public class DoubleTextBoxFormField extends TextBoxFormField<Double> {

    public DoubleTextBoxFormField() {
        this(new PTextBox());
    }

    public DoubleTextBoxFormField(final PTextBox textBox) {
        super(textBox, new DoubleConverter());
    }
}
