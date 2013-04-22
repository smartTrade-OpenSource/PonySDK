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

package com.ponysdk.ui.server.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.ponysdk.core.instruction.AddHandler;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.core.stm.TxnObject;
import com.ponysdk.core.stm.TxnString;
import com.ponysdk.ui.server.basic.event.PHasText;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

public class PTextBoxBase extends PFocusWidget implements PHasText, HasPValue<String> {

    private final List<PValueChangeHandler<String>> handlers = new ArrayList<PValueChangeHandler<String>>();

    private final TxnString text = new TxnString("");
    private final TxnString placeholder = new TxnString("");

    public PTextBoxBase() {
        this(null);
    }

    public PTextBoxBase(final String text) {
        super();
        this.text.setListener(this);
        this.placeholder.setListener(this);

        setText(text);
        final AddHandler addHandler = new AddHandler(getID(), HANDLER.KEY_.STRING_VALUE_CHANGE_HANDLER);
        Txn.get().getTxnContext().save(addHandler);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.TEXTBOX;
    }

    @Override
    public String getText() {
        return text.get();
    }

    @Override
    public void setText(final String text) {
        this.text.set(text);
    }

    @Override
    public String getValue() {
        return getText();
    }

    @Override
    public void setValue(final String value) {
        setText(value);
    }

    public void setPlaceholder(final String placeholder) {
        this.placeholder.set(placeholder);
    }

    public String getPlaceholder() {
        return placeholder.get();
    }

    @Override
    public void addValueChangeHandler(final PValueChangeHandler<String> handler) {
        handlers.add(handler);
    }

    @Override
    public void removeValueChangeHandler(final PValueChangeHandler<String> handler) {
        handlers.remove(handler);
    }

    @Override
    public Collection<PValueChangeHandler<String>> getValueChangeHandlers() {
        return Collections.unmodifiableCollection(handlers);
    }

    @Override
    public void onClientData(final JSONObject e) throws JSONException {
        if (e.has(HANDLER.KEY) && e.getString(HANDLER.KEY).equals(HANDLER.KEY_.STRING_VALUE_CHANGE_HANDLER)) {
            final PValueChangeEvent<String> event = new PValueChangeEvent<String>(this, e.getString(PROPERTY.VALUE));
            fireOnValueChange(event);
        } else {
            super.onClientData(e);
        }
    }

    protected void fireOnValueChange(final PValueChangeEvent<String> event) {
        this.text.reset(event.getValue());
        for (final PValueChangeHandler<String> handler : handlers) {
            handler.onValueChange(event);
        }
    }

    @Override
    public void beforeFlush(final TxnObject<?> txnObject) {
        if (txnObject == text) {
            saveUpdate(PROPERTY.TEXT, text.get());
        } else if (txnObject == placeholder) {
            saveUpdate(PROPERTY.PLACEHOLDER, placeholder.get());
        } else {
            super.beforeFlush(txnObject);
        }
    }

}
