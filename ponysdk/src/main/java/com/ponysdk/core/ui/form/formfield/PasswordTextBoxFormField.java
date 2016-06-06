
package com.ponysdk.core.ui.form.formfield;

import com.ponysdk.core.ui.basic.PPasswordTextBox;
import com.ponysdk.core.ui.form.dataconverter.DataConverter;

public class PasswordTextBoxFormField<T> extends TextBoxFormField<T> {

    public PasswordTextBoxFormField() {
        this(new PPasswordTextBox(), null);
    }

    public PasswordTextBoxFormField(final DataConverter<String, T> dataProvider) {
        this(new PPasswordTextBox(), dataProvider);
    }

    public PasswordTextBoxFormField(final PPasswordTextBox textBox, final DataConverter<String, T> dataProvider) {
        super(textBox, dataProvider);
    }

}
