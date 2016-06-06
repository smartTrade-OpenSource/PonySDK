/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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
import com.ponysdk.core.server.application.Parser;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;

import javax.json.JsonObject;
import java.util.*;

/**
 * A standard check box widget. This class also serves as a base class for
 * {@link PRadioButton}.
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-CheckBox</dt>
 * <dd>the outer element</dd>
 * <dt>.gwt-CheckBox-disabled</dt>
 * <dd>applied when PCheckbox is disabled</dd>
 * </dl>
 */
public class PCheckBox extends PButtonBase implements HasPValue<Boolean>, PValueChangeHandler<Boolean> {

    private List<PValueChangeHandler<Boolean>> handlers;

    private Boolean value = Boolean.FALSE;

    /**
     * Creates a check box with no label.
     */
    public PCheckBox() {
    }

    /**
     * Creates a check box with the specified text label.
     *
     * @param label
     *            the check box's label
     */
    public PCheckBox(final String label) {
        super(label);
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        super.enrichOnInit(parser);
        if (this.value != Boolean.FALSE) parser.parse(ServerToClientModel.VALUE_CHECKBOX, this.value);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.CHECKBOX;
    }

    @Override
    public void addValueChangeHandler(final PValueChangeHandler<Boolean> handler) {
        if (handlers == null) handlers = new ArrayList<>();
        handlers.add(handler);
    }

    @Override
    public boolean removeValueChangeHandler(final PValueChangeHandler<Boolean> handler) {
        return handlers != null && handlers.remove(handler);
    }

    @Override
    public Collection<PValueChangeHandler<Boolean>> getValueChangeHandlers() {
        return handlers != null ? Collections.unmodifiableCollection(handlers) : Collections.emptyList();
    }

    /**
     * Determines whether this check box is currently checked.
     *
     * @return <code>true</code> if the check box is checked, false otherwise.
     *         Will not return null
     */
    @Override
    public Boolean getValue() {
        return value;
    }

    /**
     * Checks or unchecks the check box.
     *
     * @param value
     *            true to check, false to uncheck; null value implies false
     */
    @Override
    public void setValue(final Boolean value) {
        if (Objects.equals(this.value, value)) return;
        this.value = value;
        saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.VALUE_CHECKBOX, this.value);
        });
    }

    @Override
    public void onValueChange(final PValueChangeEvent<Boolean> event) {
        this.value = event.getValue();
        if (handlers == null) return;
        for (final PValueChangeHandler<Boolean> handler : handlers) {
            handler.onValueChange(event);
        }
    }

    @Override
    public void onClientData(final JsonObject jsonObject) {
        if (jsonObject.containsKey(ClientToServerModel.HANDLER_BOOLEAN_VALUE_CHANGE.toStringValue())) {
            onValueChange(new PValueChangeEvent<Boolean>(this,
                    jsonObject.getBoolean(ClientToServerModel.HANDLER_BOOLEAN_VALUE_CHANGE.toStringValue())));
        } else {
            super.onClientData(jsonObject);
        }
    }

}
