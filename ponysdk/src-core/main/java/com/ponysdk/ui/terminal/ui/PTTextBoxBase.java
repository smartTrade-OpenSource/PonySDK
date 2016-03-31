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
import com.google.gwt.user.client.ui.TextBoxBase;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTTextBoxBase<W extends TextBoxBase> extends PTValueBoxBase<W, String> {

    @Override
    protected void init(final PTInstruction create, final UIService uiService, final W uiObject) {
        uiObject.addValueChangeHandler(new ValueChangeHandler<String>() {

            @Override
            public void onValueChange(final ValueChangeEvent<String> event) {
                final PTInstruction eventInstruction = new PTInstruction();
                eventInstruction.setObjectID(create.getObjectID());
                eventInstruction.put(Model.HANDLER_STRING_VALUE_CHANGE_HANDLER);
                eventInstruction.put(Model.VALUE, event.getValue());
                uiService.sendDataToServer(uiObject, eventInstruction);
            }
        });
        super.init(create, uiService, uiObject);
        update(create, uiService);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        super.update(update, uiService);
        if (update.containsKey(Model.TEXT)) {
            uiObject.setText(update.getString(Model.TEXT));
        }
        if (update.containsKey(Model.PLACEHOLDER)) {
            uiObject.getElement().setAttribute("placeholder", update.getString(Model.PLACEHOLDER));
        }
    }

}
