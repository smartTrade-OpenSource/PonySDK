
package com.ponysdk.ui.server.form2.formfield;

import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.form2.dataconverter.DataConverter;

public class ListBoxFormField<T> extends FormField<T> {

    private PListBox listBox;

    public ListBoxFormField(final DataConverter<String, T> dataProvider) {
        this(new PListBox(), dataProvider);
    }

    public ListBoxFormField(final PListBox listBox, final DataConverter<String, T> dataProvider) {
        super(dataProvider);
        this.listBox = listBox;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getValue() {
        return (T) listBox.getSelectedValue();
    }

    @Override
    public void setValue(final T value) {
        listBox.setSelectedItem(value);
    }

    @Override
    public PWidget asWidget() {
        return listBox;
    }

    @Override
    protected String getStringValue() {
        return listBox.getSelectedItem();
    }

    @Override
    public void reset() {
        listBox.setSelectedIndex(-1);
    }

    public PListBox getListBox() {
        return listBox;
    }

}
