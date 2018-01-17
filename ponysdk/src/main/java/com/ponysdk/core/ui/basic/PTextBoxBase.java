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

package com.ponysdk.core.ui.basic;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PHasText;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.writer.ModelWriter;

import javax.json.JsonObject;
import java.util.*;

public abstract class PTextBoxBase extends PValueBoxBase implements PHasText, HasPValue<String> {

    private static final String EMPTY = "";

    private List<PValueChangeHandler<String>> handlers;

    private String text = EMPTY;
    private String placeholder = EMPTY;

    protected PTextBoxBase() {
        this(EMPTY);
    }

    protected PTextBoxBase(final String text) {
        super();
        this.text = text != null ? text : EMPTY;
    }

    @Override
    protected void enrichForUpdate(final ModelWriter writer) {
        super.enrichForUpdate(writer);
        if (!EMPTY.equals(text)) writer.write(ServerToClientModel.TEXT, this.text);
        if (!EMPTY.equals(placeholder)) writer.write(ServerToClientModel.PLACEHOLDER, this.placeholder);
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
        if (initialized) saveUpdate(ServerToClientModel.TEXT, this.text);
    }

    @Override
    public String getValue() {
        return getText();
    }

    @Override
    public void setValue(final String value) {
        setText(value);
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        if (placeholder == null) placeholder = EMPTY; // null not send over json
        if (Objects.equals(this.placeholder, placeholder)) return;
        this.placeholder = placeholder;
        if (initialized) saveUpdate(ServerToClientModel.PLACEHOLDER, this.placeholder);
    }

    @Override
    public void addValueChangeHandler(final PValueChangeHandler<String> handler) {
        if (handlers == null) handlers = new ArrayList<>();
        handlers.add(handler);
    }

    @Override
    public boolean removeValueChangeHandler(final PValueChangeHandler<String> handler) {
        return handlers != null && handlers.remove(handler);
    }

    @Override
    public Collection<PValueChangeHandler<String>> getValueChangeHandlers() {
        return handlers != null ? Collections.unmodifiableCollection(handlers) : Collections.emptyList();
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (!isVisible()) return;
        if (instruction.containsKey(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue())) {
            final String value = instruction.getString(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue());
            fireOnValueChange(new PValueChangeEvent<>(this, value));
        } else {
            super.onClientData(instruction);
        }
    }

    protected void fireOnValueChange(final PValueChangeEvent<String> event) {
        this.text = event.getData();
        if (handlers != null) handlers.forEach(handler -> handler.onValueChange(event));
    }

    @Override
    public String toString() {
        return super.toString() + ", text=" + text + ", placeholder=" + placeholder;
    }
}
