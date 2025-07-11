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

import java.util.Objects;

import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;

/**
 * A mutually-exclusive selection radio button widget. Fires
 * {@link com.ponysdk.core.ui.basic.event.PClickEvent}s when the radio
 * button is clicked, and {@link com.ponysdk.core.ui.basic.event.PValueChangeEvent}s when the button
 * becomes checked. Note, however,
 * that browser limitations prevent PValueChangeEvents from being sent when the radio button is
 * cleared as a side effect of another in the group being clicked.
 * <h2>CSS Style Rules</h2>
 * <dl>
 * <dt>.gwt-RadioButton</dt>
 * <dd>the outer element</dd>
 * </dl>
 */
public class PRadioButton extends PCheckBox {

    private String name;

    protected PRadioButton() {
        super();
    }

    protected PRadioButton(final String label) {
        super(label);
    }

    @Override
    public void setState(final PCheckBoxState state) {
        this.setState(state, true);
    }

    protected void setState(final PCheckBoxState state, final boolean propagate) {
        if (PCheckBoxState.INDETERMINATE != state) {
            if (Objects.equals(this.state, state)) return;
            if (propagate) {
                super.setState(state);
                if (handlers != null) {
                    final PValueChangeEvent<Boolean> event = new PValueChangeEvent<>(this, getValue());
                    handlers.forEach(handler -> handler.onValueChange(event));
                }
            } else {
                this.state = state;
            }
        } else {
            throw new IllegalArgumentException("State of a RadioButton can't be indeterminate");
        }
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.RADIO_BUTTON;
    }

    void setName(final String name) {
        if (Objects.equals(this.name, name)) return;
        this.name = name;
        saveUpdate(ServerToClientModel.NAME, name);
    }

}
