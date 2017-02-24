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
import java.util.List;
import java.util.Objects;

import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;

/**
 * A mutually-exclusive selection radio button widget. Fires
 * {@link com.ponysdk.core.ui.basic.event.PClickEvent}s when the radio button is clicked, and
 * {@link com.ponysdk.core.ui.basic.event.PValueChangeEvent}s when the button becomes checked. Note,
 * however, that browser limitations prevent PValueChangeEvents from being sent when the radio
 * button is cleared as a side effect of another in the group being clicked.
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-RadioButton</dt>
 * <dd>the outer element</dd>
 * </dl>
 */
public class PRadioButtonGroup {

    private List<PRadioButton> buttons;
    private List<PValueChangeHandler<Boolean>> handlers;

    private String name;

    protected PRadioButtonGroup(final String name) {
        this.name = name;
    }

    public void addValueChangeHandler(final PValueChangeHandler<Boolean> handler) {
        if (handlers == null) handlers = new ArrayList<>();
        handlers.add(handler);
    }

    public boolean removeValueChangeHandler(final PValueChangeHandler<Boolean> handler) {
        return handlers != null && handlers.remove(handler);
    }

    public void addRadioButton(final PRadioButton radioButton) {
        if (this.buttons == null) this.buttons = new ArrayList<>();
        radioButton.setName(name);
        radioButton.addValueChangeHandler(new PValueChangeHandler<Boolean>() {

            @Override
            public void onValueChange(final PValueChangeEvent<Boolean> event) {
                for (final PRadioButton button : buttons) {
                    if (button != radioButton) {
                        button.setState(event.getData() ? PCheckBoxState.UNCHECKED : PCheckBoxState.CHECKED, false);
                    }
                }
                if (handlers != null) {
                    for (final PValueChangeHandler<Boolean> handler : handlers) {
                        handler.onValueChange(event);
                    }
                }
            }
        });
        this.buttons.add(radioButton);
    }

    public void removeRadioButton(final PRadioButton radioButton) {
        if (buttons != null) buttons.remove(radioButton);
    }

    public void setName(final String name) {
        if (Objects.equals(this.name, name)) return;
        this.name = name;
        for (final PRadioButton button : buttons) {
            button.setName(name);
        }
    }

    public String getName() {
        return name;
    }

}
