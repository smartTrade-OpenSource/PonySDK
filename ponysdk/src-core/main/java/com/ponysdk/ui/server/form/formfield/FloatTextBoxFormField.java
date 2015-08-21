
package com.ponysdk.ui.server.form.formfield;

import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.form.dataconverter.FloatConverter;

public class FloatTextBoxFormField extends TextBoxFormField<Float> {

    public FloatTextBoxFormField() {
        this(new PTextBox());
    }

    public FloatTextBoxFormField(final PTextBox textBox) {
        super(textBox, new FloatConverter());
    }
}
