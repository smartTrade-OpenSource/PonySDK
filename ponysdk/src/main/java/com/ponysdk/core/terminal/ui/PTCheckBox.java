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

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.CheckBox;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTCheckBox<T extends CheckBox> extends PTButtonBase<T> {

    private Element inputElement;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        super.create(buffer, objectId, uiService);
        addValueChangeHandler(uiService);

        inputElement = uiObject.getElement();
        final Element child = inputElement.getFirstChildElement();
        if (child != null) inputElement = child;
    }

    @Override
    protected T createUIObject() {
        return (T) new CheckBox() {

            @Override
            public int getTabIndex() {
                final int tabIndex = super.getTabIndex();
                return tabIndex == -1 ? -2 : tabIndex;
            }
        };
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.VALUE_CHECKBOX == model) {
            final PCheckBoxState state = PCheckBoxState.fromRawValue(binaryModel.getIntValue());
            final boolean indeterminate = PCheckBoxState.INDETERMINATE == state;
            if (!indeterminate) uiObject.setValue(PCheckBoxState.CHECKED == state);
            setIndeterminate(inputElement, indeterminate);
            return true;
        } else if (ServerToClientModel.TABINDEX == model) {
            inputElement.setTabIndex(binaryModel.getIntValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    protected void addValueChangeHandler(final UIBuilder uiService) {
        uiObject.addValueChangeHandler(event -> {
            final PTInstruction instruction = new PTInstruction(getObjectID());
            instruction.put(ClientToServerModel.HANDLER_BOOLEAN_VALUE_CHANGE, event.getValue());
            uiService.sendDataToServer(uiObject, instruction);
        });
    }

    /**
     * JavaScript to give an ID'd element in a form the focus
     *
     * @param checkbox
     *            Element to manipulate
     * @param indeterminate
     *            True if indeterminate, false if not
     */
    public static native void setIndeterminate(Element checkbox, boolean indeterminate) /*-{
                                                                                        checkbox.indeterminate = indeterminate;
                                                                                        }-*/;

}
