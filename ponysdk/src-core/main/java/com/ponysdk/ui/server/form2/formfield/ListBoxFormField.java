
package com.ponysdk.ui.server.form2.formfield;

import java.util.Map;
import java.util.Map.Entry;

import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.form2.dataconverter.DataConverter;

public class ListBoxFormField<T> extends FormField<T> {

    private PListBox listBox;

    public ListBoxFormField() {
        this(new PListBox(), null);
    }

    public ListBoxFormField(final DataConverter<String, T> dataProvider) {
        this(new PListBox(), dataProvider);
    }

    public ListBoxFormField(final PListBox listBox) {
        this(listBox, null);
    }

    public ListBoxFormField(final Map<String, T> datas) {
        this(new PListBox(), null);
        for (final Entry<String, T> entry : datas.entrySet()) {
            listBox.addItem(entry.getKey(), entry.getValue());
        }
    }

    public ListBoxFormField(final PListBox listBox, final DataConverter<String, T> dataProvider) {
        super(dataProvider);
        this.listBox = listBox;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getValue() {
        if (dataProvider != null) return dataProvider.to(listBox.getSelectedItem());
        return (T) listBox.getSelectedValue();
    }

    @Override
    public void setValue(final T value) {
        if (dataProvider != null) listBox.setSelectedItem(dataProvider.from(value));
        else listBox.setSelectedValue(value);
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
    public void reset0() {
        listBox.setSelectedIndex(-1);
    }

    public PListBox getListBox() {
        return listBox;
    }

}
