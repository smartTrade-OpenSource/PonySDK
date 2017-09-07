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

package com.ponysdk.core.terminal.ui;

import com.google.gwt.user.client.ui.RadioButton;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTRadioButton extends PTCheckBox<RadioButton> {

    @Override
    protected RadioButton createUIObject() {
        return new RadioButton(null);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.NAME.ordinal() == modelOrdinal) {
            uiObject.setName(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.VALUE_CHECKBOX.ordinal() == modelOrdinal) {
            uiObject.setValue(PCheckBoxState.CHECKED.equals(PCheckBoxState.fromRawValue(binaryModel.getByteValue())), true);
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    protected void addValueChangeHandler(final UIBuilder uiService) {
        uiObject.addValueChangeHandler(event -> {
            final PTInstruction instruction = new PTInstruction(objectID);
            instruction.put(ClientToServerModel.HANDLER_BOOLEAN_VALUE_CHANGE, event.getValue());
            uiService.sendDataToServer(uiObject, instruction);
        });
    }

}
