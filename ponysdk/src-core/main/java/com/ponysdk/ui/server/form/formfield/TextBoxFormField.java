
package com.ponysdk.ui.server.form.formfield;

import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.form.dataconverter.DataConverter;

public class TextBoxFormField<T> extends FormField<T> {

    protected final PTextBox textBox;

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
    public void reset0() {
        textBox.setText(null);
    }

    @Override
    public T getValue() {
        return dataProvider.to(textBox.getText());
    }

    @Override
    public void setValue(final T value) {
        textBox.setValue(dataProvider.from(value));
    }

    @Override
    protected String getStringValue() {
        return textBox.getText();
    }

    public PTextBox getTextBox() {
        return textBox;
    }

}
