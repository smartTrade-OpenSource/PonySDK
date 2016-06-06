
package com.ponysdk.core.ui.form.formfield;

import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.form.dataconverter.LongConverter;

public class LongTextBoxFormField extends TextBoxFormField<Long> {

    public LongTextBoxFormField() {
        this(new PTextBox());
    }

    public LongTextBoxFormField(final PTextBox textBox) {
        super(textBox, new LongConverter());
    }
}
