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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.OptGroupElement;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.ListBox;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTListBox extends PTFocusWidget<ListBox> {

    @Override
    protected ListBox createUIObject() {
        return new ListBox();
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel, final UIService uiService) {
        if (HandlerModel.HANDLER_CHANGE_HANDLER.equals(handlerModel)) {
            uiObject.addChangeHandler(new ChangeHandler() {

                @Override
                public void onChange(final ChangeEvent event) {
                    final int selectedIndex = uiObject.getSelectedIndex();
                    if (selectedIndex == -1) {
                        final PTInstruction eventInstruction = new PTInstruction();
                        eventInstruction.setObjectID(getObjectID());
                        // eventInstruction.put(Model.TYPE_EVENT);
                        eventInstruction.put(HandlerModel.HANDLER_CHANGE_HANDLER);
                        eventInstruction.put(Model.VALUE, "-1");
                        uiService.sendDataToServer(uiObject, eventInstruction);
                    } else {
                        String selectedIndexes = selectedIndex + "";
                        for (int i = 0; i < uiObject.getItemCount(); i++) {
                            if (uiObject.isItemSelected(i)) {
                                if (i != selectedIndex) {
                                    selectedIndexes += "," + i;
                                }
                            }
                        }
                        final PTInstruction eventInstruction = new PTInstruction();
                        eventInstruction.setObjectID(getObjectID());
                        // eventInstruction.put(Model.TYPE_EVENT);
                        eventInstruction.put(HandlerModel.HANDLER_CHANGE_HANDLER);
                        eventInstruction.put(Model.VALUE, selectedIndexes);
                        uiService.sendDataToServer(uiObject, eventInstruction);
                    }
                }
            });
            return;
        } else {
            super.addHandler(buffer, handlerModel, uiService);
        }
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (Model.CLEAR.equals(binaryModel.getModel())) {
            uiObject.clear();
            return true;
        }
        if (Model.ITEM_INSERTED.equals(binaryModel.getModel())) {
            final String item = binaryModel.getStringValue();
            final BinaryModel indexModel = buffer.getBinaryModel();
            if (Model.INDEX.equals(indexModel.getModel())) {
                uiObject.insertItem(item, indexModel.getIntValue());
            } else {
                buffer.rewind(indexModel);
                uiObject.addItem(item);
            }
            return true;
        }
        if (Model.ITEM_ADD.equals(binaryModel.getModel())) {
            final String items = binaryModel.getStringValue();
            // Model.ITEM_GROUP
            final String groupName = buffer.getBinaryModel().getStringValue();
            final SelectElement select = uiObject.getElement().cast();

            final OptGroupElement groupElement = Document.get().createOptGroupElement();
            groupElement.setLabel(groupName);

            final String[] tokens = items.split(";");

            for (final String token : tokens) {
                final OptionElement optElement = Document.get().createOptionElement();
                optElement.setInnerText(token);
                groupElement.appendChild(optElement);
            }
            select.appendChild(groupElement);
            return true;
        }
        if (Model.ITEM_UPDATED.equals(binaryModel.getModel())) {
            final String item = binaryModel.getStringValue();
            // Model.INDEX
            final int index = buffer.getBinaryModel().getIntValue();
            uiObject.setItemText(index, item);
            return true;
        }
        if (Model.ITEM_REMOVED.equals(binaryModel.getModel())) {
            uiObject.removeItem(binaryModel.getIntValue());
            return true;
        }
        if (Model.SELECTED.equals(binaryModel.getModel())) {
            final boolean selected = binaryModel.getBooleanValue();
            // Model.INDEX
            final int index = buffer.getBinaryModel().getIntValue();
            if (index == -1)
                uiObject.setSelectedIndex(index);
            else
                uiObject.setItemSelected(index, selected);
            return true;
        }
        if (Model.VISIBLE_ITEM_COUNT.equals(binaryModel.getModel())) {
            uiObject.setVisibleItemCount(binaryModel.getIntValue());
            return true;
        }
        if (Model.MULTISELECT.equals(binaryModel.getModel())) {
            uiObject.setMultipleSelect(binaryModel.getBooleanValue());
            return true;
        }
        return super.update(buffer, binaryModel);

    }
}
