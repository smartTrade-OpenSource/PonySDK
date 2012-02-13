
package com.ponysdk.ui.server.form2.formfield;

import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.form2.dataconverter.DataConverter;

public class TextBoxFormField<T> extends FormField<T> {

    private PTextBox textBox;

    public TextBoxFormField(final DataConverter<String, T> dataProvider) {
        this(new PTextBox(), dataProvider);
    }

    public TextBoxFormField(final PTextBox textBox, final DataConverter<String, T> dataProvider) {
        super(dataProvider);
        this.textBox = textBox;
    }

    @Override
    public PWidget asWidget() {
        return textBox;
    }

    @Override
    public void reset() {
        textBox.setText(null);
    }

    @Override
    public T getValue() {
        return dataProvider.to(textBox.getValue());
    }

    @Override
    public void setValue(final T value) {
        textBox.setValue(dataProvider.from(value));
    }

    @Override
    protected String getStringValue() {
        return textBox.getValue();
    }

}
