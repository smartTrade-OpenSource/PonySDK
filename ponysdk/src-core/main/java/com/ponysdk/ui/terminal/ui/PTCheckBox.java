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
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTCheckBox extends PTButtonBase<CheckBox> {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new CheckBox());
        update(create, uiService);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        super.update(update, uiService);
        if (update.containsKey(Model.VALUE_CHECKBOX)) {
            uiObject.setValue(update.getBoolean(Model.VALUE_CHECKBOX));
        }
    }

    @Override
    public void addHandler(final PTInstruction addHandler, final UIService uiService) {
        if (addHandler.containsKey(Model.HANDLER_BOOLEAN_VALUE_CHANGE_HANDLER)) {
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
                instruction.put(Model.HANDLER_BOOLEAN_VALUE_CHANGE_HANDLER);
                instruction.put(Model.VALUE_CHECKBOX, event.getValue());
                uiService.sendDataToServer(uiObject, instruction);
            }
        });
    }

}
