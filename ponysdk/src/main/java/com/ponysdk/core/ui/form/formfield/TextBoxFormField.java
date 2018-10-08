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

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget.TabindexMode;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.ui.form.dataconverter.DataConverter;

public class TextBoxFormField<T> extends AbstractFormField<T, PTextBox> {

    public TextBoxFormField(final DataConverter<String, T> dataProvider) {
        this(dataProvider, false);
    }

    public TextBoxFormField(final DataConverter<String, T> dataProvider, final boolean dirtyMode) {
        this(Element.newPTextBox(), dataProvider, dirtyMode);
    }

    public TextBoxFormField(final PTextBox widget, final DataConverter<String, T> dataProvider) {
        this(widget, dataProvider, false);
    }

    public TextBoxFormField(final PTextBox widget, final DataConverter<String, T> dataProvider, final boolean dirtyMode) {
        super(widget, dataProvider, dirtyMode);
        widget.setTabindex(TabindexMode.TABULABLE);
    }

    @Override
    public void addValueChangeHandler(final PValueChangeHandler<T> handler) {
        if (handlers == null) widget.addValueChangeHandler(event -> fireValueChange(getValue()));
        super.addValueChangeHandler(handler);
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
        super.setEnabled(enabled);
        widget.setEnabled(enabled);
    }

}
