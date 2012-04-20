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

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.ListBox;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.instruction.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTListBox extends PTFocusWidget {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        final boolean multiselect = create.getBoolean(PROPERTY.MULTISELECT);
        final ListBox listBox = new ListBox(multiselect);
        init(create, uiService, listBox);
    }

    @Override
    public void addHandler(final PTInstruction addHandler, final UIService uiService) {
        final ListBox listBox = cast();

        if (addHandler.getString(HANDLER.KEY).equals(HANDLER.CHANGE_HANDLER)) {
            listBox.addChangeHandler(new ChangeHandler() {

                @Override
                public void onChange(final ChangeEvent event) {
                    final int selectedIndex = listBox.getSelectedIndex();
                    if (selectedIndex == -1) {
                        final PTInstruction eventInstruction = new PTInstruction();
                        eventInstruction.setObjectID(addHandler.getObjectID());
                        eventInstruction.put(TYPE.KEY, TYPE.EVENT);
                        eventInstruction.put(HANDLER.KEY, HANDLER.CHANGE_HANDLER);
                        eventInstruction.put(PROPERTY.VALUE, "-1");
                        uiService.triggerEvent(eventInstruction);
                    } else {
                        String selectedIndexes = selectedIndex + "";
                        for (int i = 0; i < listBox.getItemCount(); i++) {
                            if (listBox.isItemSelected(i)) {
                                if (i != selectedIndex) {
                                    selectedIndexes += "," + i;
                                }
                            }
                        }
                        final PTInstruction eventInstruction = new PTInstruction();
                        eventInstruction.setObjectID(addHandler.getObjectID());
                        eventInstruction.put(TYPE.KEY, TYPE.EVENT);
                        eventInstruction.put(HANDLER.KEY, HANDLER.CHANGE_HANDLER);
                        eventInstruction.put(PROPERTY.VALUE, selectedIndexes);
                        uiService.triggerEvent(eventInstruction);
                    }
                }
            });
            return;
        }

        super.addHandler(addHandler, uiService);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.CLEAR)) {
            cast().clear();
        } else if (update.containsKey(PROPERTY.ITEM_INSERTED)) {
            final int index = update.getInt(PROPERTY.INDEX);
            final String item = update.getString(PROPERTY.ITEM_TEXT);
            final String value = update.getString(PROPERTY.VALUE);
            cast().insertItem(item, value, index);
        } else if (update.containsKey(PROPERTY.ITEM_REMOVED)) {
            cast().removeItem(update.getInt(PROPERTY.INDEX));
        } else if (update.containsKey(PROPERTY.SELECTED)) {
            final boolean selected = update.getBoolean(PROPERTY.SELECTED);
            final int index = update.getInt(PROPERTY.SELECTED_INDEX);
            if (index == -1) cast().setSelectedIndex(index);
            else cast().setItemSelected(index, selected);
        } else if (update.containsKey(PROPERTY.VISIBLE_ITEM_COUNT)) {
            cast().setVisibleItemCount(update.getInt(PROPERTY.VISIBLE_ITEM_COUNT));
        } else if (update.containsKey(PROPERTY.MULTISELECT)) {
            cast().setMultipleSelect(update.getBoolean(PROPERTY.MULTISELECT));
        } else {
            super.update(update, uiService);
        }

    }

    @Override
    public ListBox cast() {
        return (ListBox) uiObject;
    }
}
