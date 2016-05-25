
package com.ponysdk.ui.server.form.formfield;

import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.form.dataconverter.DataConverter;

public class TextBoxFormField<T> extends FormField<T, PTextBox> {

    public TextBoxFormField(final DataConverter<String, T> dataProvider) {
        this(new PTextBox(), dataProvider);
    }

    public TextBoxFormField(final PTextBox widget, final DataConverter<String, T> dataProvider) {
        super(widget, dataProvider);
    }

    @Override
    public void reset0() {
        widget.setText(null);
    }

    @Override
    public T getValue() {
        return dataProvider.to(widget.getText());
    }

    @Override
    public void setValue(final T value) {
        widget.setValue(dataProvider.from(value));
    }

    @Override
    protected String getStringValue() {
        return widget.getText();
    }

    @Override
    public void setEnabled(final boolean enabled) {
        widget.setEnabled(enabled);
    }

}
