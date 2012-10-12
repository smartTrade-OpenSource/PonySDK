
package com.ponysdk.ui.server.form2.formfield;

import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.form2.dataconverter.FloatConverter;

public class FloatTextBoxFormField extends TextBoxFormField<Float> {

    public FloatTextBoxFormField() {
        this(new PTextBox());
    }

    public FloatTextBoxFormField(final PTextBox textBox) {
        super(textBox, new FloatConverter());
    }
}
