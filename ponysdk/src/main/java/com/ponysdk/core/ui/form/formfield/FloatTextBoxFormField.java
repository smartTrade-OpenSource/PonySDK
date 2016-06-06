
package com.ponysdk.core.ui.form.formfield;

import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.form.dataconverter.FloatConverter;

public class FloatTextBoxFormField extends TextBoxFormField<Float> {

    public FloatTextBoxFormField() {
        this(new PTextBox());
    }

    public FloatTextBoxFormField(final PTextBox textBox) {
        super(textBox, new FloatConverter());
    }
}
