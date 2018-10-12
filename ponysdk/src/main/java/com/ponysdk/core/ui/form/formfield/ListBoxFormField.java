/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.ui.form.formfield;

import java.util.Map;
import java.util.Map.Entry;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PWidget.TabindexMode;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.ui.form.dataconverter.DataConverter;

public class ListBoxFormField<T> extends AbstractFormField<T, PListBox> {

    public ListBoxFormField() {
        this(false);
    }

    public ListBoxFormField(final boolean dirtyMode) {
        this(Element.newPListBox(), null, dirtyMode);
    }

    public ListBoxFormField(final DataConverter<String, T> dataProvider) {
        this(dataProvider, false);
    }

    public ListBoxFormField(final DataConverter<String, T> dataProvider, final boolean dirtyMode) {
        this(Element.newPListBox(), dataProvider, dirtyMode);
    }

    public ListBoxFormField(final PListBox widget) {
        this(widget, false);
    }

    public ListBoxFormField(final PListBox widget, final boolean dirtyMode) {
        this(widget, null, dirtyMode);
    }

    public ListBoxFormField(final Map<String, T> datas) {
        this(Element.newPListBox());
        for (final Entry<String, T> entry : datas.entrySet()) {
            widget.addItem(entry.getKey(), entry.getValue());
        }
    }

    public ListBoxFormField(final PListBox widget, final DataConverter<String, T> dataProvider) {
        this(widget, dataProvider, false);
    }

    public ListBoxFormField(final PListBox widget, final DataConverter<String, T> dataProvider, final boolean dirtyMode) {
        super(widget, dataProvider, dirtyMode);
        widget.setTabindex(TabindexMode.TABULABLE);
    }

    @Override
    public void addValueChangeHandler(final PValueChangeHandler<T> handler) {
        if (handlers == null) widget.addChangeHandler(event -> fireValueChange(getValue()));
        super.addValueChangeHandler(handler);
    }

    @Override
    public void reset0() {
        widget.setSelectedIndex(-1);
    }

    @Override
    public T getValue() {
        if (dataProvider != null) return dataProvider.to(widget.getSelectedItem());
        return (T) widget.getSelectedValue();
    }

    @Override
    protected String getStringValue() {
        return widget.getSelectedItem();
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        widget.setEnabled(enabled);
    }

    @Override
    public void setValue(final T value) {
        if (dataProvider != null) widget.setSelectedItem(dataProvider.from(value));
        else widget.setSelectedValue(value);
    }

}
