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

package com.ponysdk.ui.terminal.ui;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.instruction.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTCheckBox extends PTButtonBase<CheckBox> {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new CheckBox());
    }

    @Override
    public void addHandler(final PTInstruction addHandler, final UIService uiService) {
        if (HANDLER.BOOLEAN_VALUE_CHANGE_HANDLER.equals(addHandler.getString(HANDLER.KEY))) {
            addValueChangeHandler(addHandler, uiService);
        } else {
            super.addHandler(addHandler, uiService);
        }

    }

    protected void addValueChangeHandler(final PTInstruction addHandler, final UIService uiService) {
        uiObject.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

            @Override
            public void onValueChange(final ValueChangeEvent<Boolean> event) {
                final PTInstruction instruction = new PTInstruction();
                instruction.setObjectID(addHandler.getObjectID());
                instruction.put(TYPE.KEY, TYPE.EVENT);
                instruction.put(HANDLER.KEY, HANDLER.BOOLEAN_VALUE_CHANGE_HANDLER);
                instruction.put(PROPERTY.VALUE, event.getValue());
                uiService.triggerEvent(instruction);
            }
        });
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.VALUE)) {
            uiObject.setValue(update.getBoolean(PROPERTY.VALUE));
        } else if (update.containsKey(PROPERTY.TEXT)) {
            uiObject.setText(update.getString(PROPERTY.TEXT));
        } else if (update.containsKey(PROPERTY.HTML)) {
            uiObject.setHTML(update.getString(PROPERTY.HTML));
        } else {
            super.update(update, uiService);
        }
    }

}
