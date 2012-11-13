
package com.ponysdk.ui.server.form2.formfield;

import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.form2.dataconverter.LongConverter;

public class LongTextBoxFormField extends TextBoxFormField<Long> {

    public LongTextBoxFormField() {
        this(new PTextBox());
    }

    public LongTextBoxFormField(final PTextBox textBox) {
        super(textBox, new LongConverter());
    }
}
