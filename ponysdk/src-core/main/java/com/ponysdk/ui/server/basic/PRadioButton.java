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

package com.ponysdk.ui.server.basic;

import com.ponysdk.core.stm.TxnObject;
import com.ponysdk.core.stm.TxnString;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * A mutually-exclusive selection radio button widget. Fires {@link PClickEvent}s when the radio button is
 * clicked, and {@link PValueChangeEvent}s when the button becomes checked. Note, however, that browser
 * limitations prevent PValueChangeEvents from being sent when the radio button is cleared as a side effect of
 * another in the group being clicked.
 * <p>
 * <img class='gallery' src='doc-files/PRadioButton.png'/>
 * </p>
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-RadioButton</dt>
 * <dd>the outer element</dd>
 * </dl>
 */
public class PRadioButton extends PCheckBox {

    private final TxnString name = new TxnString();

    public PRadioButton(final String label) {
        super(label);
        this.name.setListener(this);
    }

    public PRadioButton(final String name, final String label) {
        this(label);
        setName(name);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.RADIO_BUTTON;
    }

    public void setName(final String name) {
        this.name.set(name);
    }

    public String getName() {
        return name.get();
    }

    @Override
    public void beforeFlush(final TxnObject<?> txnObject) {
        if (txnObject == name) {
            saveUpdate(PROPERTY.NAME, name.get());
        } else {
            super.beforeFlush(txnObject);
        }
    }

}
