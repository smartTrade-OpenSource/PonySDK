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
import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;

import javax.json.JsonObject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A standard check box widget.
 * This class also serves as a base class for {@link PRadioButton}.
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-CheckBox</dt>
 * <dd>the outer element</dd>
 * <dt>.gwt-CheckBox-disabled</dt>
 * <dd>applied when PCheckbox is disabled</dd>
 * </dl>
 *
 * @see com.google.gwt.user.client.ui.CheckBox
 */
public class PCheckBox extends PButtonBase implements HasPValue<Boolean>, PValueChangeHandler<Boolean> {

    protected List<PValueChangeHandler<Boolean>> handlers;

    protected PCheckBoxState state = PCheckBoxState.UNCHECKED;

    private Map<String, String> inputAttributes;
    
    /**
     * Creates a check box with no label.
     */
    protected PCheckBox() {
    }

    /**
     * Creates a check box with the specified text label.
     *
     * @param label the check box's label
     */
    protected PCheckBox(final String label) {
        super(label);
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

    public void setState(final PCheckBoxState state) {
        if (Objects.equals(this.state, state)) return;
        this.state = state;
        saveUpdate(ServerToClientModel.VALUE_CHECKBOX, state.getValue());
    }

    public PCheckBoxState getState() {
        return state;
    }

    public void setInputAttribute(final String name, final String value) {
        if (name == null) return;

        if(Objects.equals(safeInputAttributes().put(name, value), value)) return;

        saveUpdate(writer -> {
            writer.write(ServerToClientModel.PUT_INPUT_ATTRIBUTE_KEY, name);
            writer.write(ServerToClientModel.INPUT_ATTRIBUTE_VALUE, value);
        });
    }
   
    public void removeInputAttribute(final String name) {
        if (safeInputAttributes().remove(name) != null) {
            saveUpdate(writer -> writer.write(ServerToClientModel.REMOVE_INPUT_ATTRIBUTE_KEY, name));
        }
    }
    /**
     * Determines whether this check box is currently checked.
     *
     * @return <code>true</code> if the check box is checked, false otherwise.
     * Will not return null
     */
    @Override
    public Boolean getValue() {
        return PCheckBoxState.CHECKED == state;
    }

    /**
     * Checks or unchecks the check box.
     *
     * @param value true to check, false to uncheck; null value implies false
     */
    @Override
    public void setValue(final Boolean value) {
        setState(Boolean.TRUE.equals(value) ? PCheckBoxState.CHECKED : PCheckBoxState.UNCHECKED);
    }

    @Override
    public void onValueChange(final PValueChangeEvent<Boolean> event) {
        final PCheckBoxState state = Boolean.TRUE.equals(event.getData()) ? PCheckBoxState.CHECKED : PCheckBoxState.UNCHECKED;
        if (Objects.equals(this.state, state)) return;
        this.state = state;
        if (handlers != null) handlers.forEach(handler -> handler.onValueChange(event));
    }

    @Override
    public void onClientData(final JsonObject jsonObject) {
        if (!isVisible() || !isEnabled()) return;
        if (jsonObject.containsKey(ClientToServerModel.HANDLER_BOOLEAN_VALUE_CHANGE.toStringValue())) {
            onValueChange(new PValueChangeEvent<>(this,
                    jsonObject.getBoolean(ClientToServerModel.HANDLER_BOOLEAN_VALUE_CHANGE.toStringValue())));
        } else {
            super.onClientData(jsonObject);
        }
    }

    @Override
    public String toString() {
        return super.toString() + ", state=" + state;
    }

    @Override
    public String dumpDOM() {
        StringBuilder DOM = new StringBuilder();
        DOM.append("<input type=\"checkbox\"");
        DOM.append(" pid=\"" + ID + "\"");
        if (state == PCheckBoxState.CHECKED) DOM.append(" checked");
        DOM.append(" class=\"" + getStyleNames().collect(Collectors.joining(" ")) + "\"");
        DOM.append(">");
        if (getText() != null) DOM.append(getText());
        if (getHTML() != null) DOM.append(getHTML());
        DOM.append("</input>");
        return DOM.toString();
    }
    
    private Map<String, String> safeInputAttributes() {
        if (inputAttributes == null) inputAttributes = new HashMap<>(8);
        return inputAttributes;
    }
}
