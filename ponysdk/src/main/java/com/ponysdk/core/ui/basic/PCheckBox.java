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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;

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

    private PCheckBoxState state = PCheckBoxState.UNCHECKED;

    /**
     * Creates a check box with no label.
     */
    protected PCheckBox() {
    }

    /**
     * Creates a check box with the specified text label.
     *
     * @param label
     *            the check box's label
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
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.VALUE_CHECKBOX, state.getValue()));
    }

    public PCheckBoxState getState() {
        return state;
    }

    /**
     * Determines whether this check box is currently checked.
     *
     * @return <code>true</code> if the check box is checked, false otherwise.
     *         Will not return null
     * @deprecated Use {@link #getState()} instead
     */
    @Deprecated
    @Override
    public Boolean getValue() {
        return PCheckBoxState.CHECKED.equals(state);
    }

    /**
     * Checks or unchecks the check box.
     *
     * @param value
     *            true to check, false to uncheck; null value implies false
     * @deprecated Use {@link #setState(PCheckBoxState)} instead
     */
    @Deprecated
    @Override
    public void setValue(final Boolean value) {
        setState(Boolean.TRUE.equals(value) ? PCheckBoxState.CHECKED : PCheckBoxState.UNCHECKED);
    }

    /**
     * Determines whether this check box is currently checked.
     *
     * @return <code>true</code> if the check box is checked, false otherwise.
     */
    public boolean isChecked() {
        return PCheckBoxState.CHECKED.equals(state);
    }

    @Override
    public void onValueChange(final PValueChangeEvent<Boolean> event) {
        final PCheckBoxState state = Boolean.TRUE.equals(event.getData()) ? PCheckBoxState.CHECKED : PCheckBoxState.UNCHECKED;
        if (Objects.equals(this.state, state)) return;
        this.state = state;
        if (handlers != null) {
            for (final PValueChangeHandler<Boolean> handler : handlers) {
                handler.onValueChange(event);
            }
        }
    }

    @Override
    public void onClientData(final JsonObject jsonObject) {
        if (jsonObject.containsKey(ClientToServerModel.HANDLER_BOOLEAN_VALUE_CHANGE.toStringValue())) {
            onValueChange(new PValueChangeEvent<>(this,
                jsonObject.getBoolean(ClientToServerModel.HANDLER_BOOLEAN_VALUE_CHANGE.toStringValue())));
        } else {
            super.onClientData(jsonObject);
        }
    }

}
