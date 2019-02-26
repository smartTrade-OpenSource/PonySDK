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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.writer.ModelWriter;

public abstract class PTextBoxBase extends PValueBoxBase implements HasPValue<String> {

    private List<PValueChangeHandler<String>> handlers;

    private String placeholder = EMPTY;

    protected PTextBoxBase() {
        super();
    }

    protected PTextBoxBase(final String text) {
        super(text);
    }

    @Override
    protected void enrichForUpdate(final ModelWriter writer) {
        super.enrichForUpdate(writer);
        if (!EMPTY.equals(placeholder)) writer.write(ServerToClientModel.PLACEHOLDER, this.placeholder);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.TEXTBOX;
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
        if (!isVisible() || !isEnabled()) return;
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
        return super.toString() + ", placeholder=" + placeholder;
    }
}
