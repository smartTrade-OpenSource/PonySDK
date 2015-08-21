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
import java.util.Objects;

import javax.json.JsonObject;

import com.ponysdk.ui.server.basic.event.PHasText;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.Model;

public class PTextBoxBase extends PValueBoxBase implements PHasText, HasPValue<String> {

    private static final String EMPTY = "";

    private final List<PValueChangeHandler<String>> handlers = new ArrayList<>();

    private String text = EMPTY;
    private String placeholder = EMPTY;

    public PTextBoxBase() {
        this(EMPTY);
    }

    public PTextBoxBase(final String text) {
        super();
        setText(text);
        saveAddHandler(Model.HANDLER_STRING_VALUE_CHANGE_HANDLER);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.TEXTBOX;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        if (text == null) text = EMPTY; // null not send over json
        if (Objects.equals(this.text, text)) return;
        this.text = text;
        saveUpdate(Model.TEXT, this.text);
    }

    @Override
    public String getValue() {
        return getText();
    }

    @Override
    public void setValue(final String value) {
        setText(value);
    }

    public void setPlaceholder(String placeholder) {
        if (placeholder == null) placeholder = EMPTY; // null not send over json
        if (Objects.equals(this.placeholder, placeholder)) return;
        this.placeholder = placeholder;
        saveUpdate(Model.PLACEHOLDER, this.placeholder);
    }

    public String getPlaceholder() {
        return placeholder;
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
    public void onClientData(final JsonObject jsonObject) {
        if (jsonObject.has(HANDLER.KEY) && jsonObject.getString(HANDLER.KEY).equals(HANDLER.KEY_.STRING_VALUE_CHANGE_HANDLER)) {
            final PValueChangeEvent<String> event = new PValueChangeEvent<>(this, e.getString(PROPERTY.VALUE));
            fireOnValueChange(event);
        } else {
            super.onClientData(jsonObject);
        }
    }

    protected void fireOnValueChange(final PValueChangeEvent<String> event) {
        this.text = event.getValue();
        for (final PValueChangeHandler<String> handler : handlers) {
            handler.onValueChange(event);
        }
    }

    @Override
    public String toString() {
        return toString("text=" + text);
    }

}
