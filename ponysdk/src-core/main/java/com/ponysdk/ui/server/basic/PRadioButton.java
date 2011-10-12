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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Update;

public class PRadioButton extends PCheckBox {

    private static final Map<String, List<PRadioButton>> radioButtonGroupsByName = new HashMap<String, List<PRadioButton>>();
    private String name;

    public PRadioButton(String text) {
        super(text);
    }

    public PRadioButton(String name, String text) {
        super(text);
        setName(name);
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.RADIO_BUTTON;
    }

    public void setName(String name) {
        removeFromGroup(name);
        addToGroup(name);
        this.name = name;
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.NAME, name);
        getPonySession().stackInstruction(update);
    }

    public String getName() {
        return name;
    }

    @Override
    public void onValueChange(Boolean value) {
        if (radioButtonGroupsByName.get(name) != null) {
            for (final PRadioButton radioButton : radioButtonGroupsByName.get(name)) {
                if (radioButton != this) {
                    radioButton.setValue(false);
                }
            }
        }
        super.onValueChange(value);
    }

    private void addToGroup(String name) {
        if (radioButtonGroupsByName.get(name) == null) {
            radioButtonGroupsByName.put(name, new ArrayList<PRadioButton>());
        }
        radioButtonGroupsByName.get(name).add(this);
    }

    private void removeFromGroup(String name) {
        if (name != null && radioButtonGroupsByName.get(name) != null) {
            radioButtonGroupsByName.get(name).remove(this);
        }
    }
}
