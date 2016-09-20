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

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTRadioButton extends PTCheckBox {

    private static final Map<String, PTRadioButton> lastSelectedRadioButtonByGroup = new HashMap<>();

    @Override
    protected CheckBox createUIObject() {
        return new RadioButton(null);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.NAME.equals(binaryModel.getModel())) {
            cast().setName(binaryModel.getStringValue());
            return true;
        }

        // FIXME
        if (ServerToClientModel.VALUE_CHECKBOX.equals(binaryModel.getModel())) {
            if (binaryModel.getBooleanValue() && cast().getName() != null) {
                cast().setValue(true);
                lastSelectedRadioButtonByGroup.put(cast().getName(), this);
            }
        }

        return super.update(buffer, binaryModel);
    }

    @Override
    protected void addValueChangeHandler(final UIBuilder uiService) {
        final RadioButton radioButton = cast();

        radioButton.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

            @Override
            public void onValueChange(final ValueChangeEvent<Boolean> event) {
                fireInstruction(getObjectID(), uiService, event.getValue());

                if (cast().getName() != null) {
                    final PTRadioButton previouslySelected = lastSelectedRadioButtonByGroup.get(radioButton.getName());
                    if (previouslySelected != null && !previouslySelected.equals(PTRadioButton.this)) {
                        fireInstruction(previouslySelected.getObjectID(), uiService, previouslySelected.cast().getValue());
                    }
                    lastSelectedRadioButtonByGroup.put(radioButton.getName(), PTRadioButton.this);
                }
            }
        });
    }

    private void fireInstruction(final int objectID, final UIBuilder uiService, final boolean value) {
        final PTInstruction instruction = new PTInstruction(objectID);
        instruction.put(ClientToServerModel.HANDLER_BOOLEAN_VALUE_CHANGE, value);
        uiService.sendDataToServer(cast(), instruction);
    }

    @Override
    public RadioButton cast() {
        return (RadioButton) uiObject;
    }

}
