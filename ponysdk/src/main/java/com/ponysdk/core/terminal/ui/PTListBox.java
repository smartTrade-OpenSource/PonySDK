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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.OptGroupElement;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.user.client.ui.ListBox;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTListBox extends PTFocusWidget<ListBox> {

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        super.create(buffer, objectId, uiService);
        addHandler(uiService);
    }

    @Override
    protected ListBox createUIObject() {
        return new ListBox() {

            @Override
            public int getTabIndex() {
                final int tabIndex = super.getTabIndex();
                return tabIndex == -1 ? -2 : tabIndex;
            }
        };
    }

    private void addHandler(final UIBuilder uiService) {
        uiObject.addChangeHandler(event -> {
            final int selectedIndex = uiObject.getSelectedIndex();
            if (selectedIndex == -1) {
                final PTInstruction eventInstruction = new PTInstruction(getObjectID());
                eventInstruction.put(ClientToServerModel.HANDLER_CHANGE, "-1");
                uiService.sendDataToServer(uiObject, eventInstruction);
            } else {
                final StringBuilder selectedIndexes = new StringBuilder(String.valueOf(selectedIndex));
                for (int i = 0; i < uiObject.getItemCount(); i++) {
                    if (uiObject.isItemSelected(i) && i != selectedIndex) selectedIndexes.append(",").append(i);
                }
                final PTInstruction eventInstruction = new PTInstruction(getObjectID());
                eventInstruction.put(ClientToServerModel.HANDLER_CHANGE, selectedIndexes.toString());
                uiService.sendDataToServer(uiObject, eventInstruction);
            }
        });
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.CLEAR == model) {
            uiObject.clear();
            return true;
        } else if (ServerToClientModel.ITEM_INSERTED == model) {
            final String item = binaryModel.getStringValue() != null ? binaryModel.getStringValue() : "";
            final BinaryModel indexModel = buffer.readBinaryModel();
            if (ServerToClientModel.INDEX == indexModel.getModel()) {
                uiObject.insertItem(item, indexModel.getIntValue());
            } else {
                buffer.rewind(indexModel);
                uiObject.addItem(item);
            }
            return true;
        } else if (ServerToClientModel.ITEM_ADD == model) {
            final JSONArray items = binaryModel.getArrayValue();
            // ServerToClientModel.ITEM_GROUP
            final String groupName = buffer.readBinaryModel().getStringValue();
            final SelectElement select = uiObject.getElement().cast();

            final OptGroupElement groupElement = Document.get().createOptGroupElement();
            groupElement.setLabel(groupName);

            for (int i = 0; i < items.size(); i++) {
                final OptionElement optElement = Document.get().createOptionElement();
                optElement.setInnerText(items.get(i).isString().stringValue());
                groupElement.appendChild(optElement);
            }
            select.appendChild(groupElement);
            return true;
        } else if (ServerToClientModel.ITEM_UPDATED == model) {
            final String item = binaryModel.getStringValue() != null ? binaryModel.getStringValue() : "";
            // ServerToClientModel.INDEX
            final int index = buffer.readBinaryModel().getIntValue();
            uiObject.setItemText(index, item);
            return true;
        } else if (ServerToClientModel.ITEM_REMOVED == model) {
            uiObject.removeItem(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.SELECTED == model) {
            final boolean selected = binaryModel.getBooleanValue();
            // ServerToClientModel.INDEX
            final int index = buffer.readBinaryModel().getIntValue();
            if (index == -1) uiObject.setSelectedIndex(index);
            else uiObject.setItemSelected(index, selected);
            return true;
        } else if (ServerToClientModel.VISIBLE_ITEM_COUNT == model) {
            uiObject.setVisibleItemCount(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.MULTISELECT == model) {
            uiObject.setMultipleSelect(binaryModel.getBooleanValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

}
