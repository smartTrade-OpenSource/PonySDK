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
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTListBox extends PTFocusWidget {

    @Override
    public void create(final Create create, final UIService uiService) {
        final boolean multiselect = create.getMainProperty().getBooleanValue();
        final com.google.gwt.user.client.ui.ListBox listBox = new com.google.gwt.user.client.ui.ListBox(multiselect);
        init(create, uiService, listBox);
    }

    @Override
    public void addHandler(final AddHandler addHandler, final UIService uiService) {

        if (HandlerType.CHANGE_HANDLER.equals(addHandler.getHandlerType())) {
            final com.google.gwt.user.client.ui.ListBox listBox = cast();
            listBox.addChangeHandler(new ChangeHandler() {

                @Override
                public void onChange(final ChangeEvent event) {
                    final int selectedIndex = listBox.getSelectedIndex();
                    if (selectedIndex == -1) {
                        final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(), HandlerType.CHANGE_HANDLER);
                        eventInstruction.setMainPropertyValue(PropertyKey.VALUE, "-1");
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
                        final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(), HandlerType.CHANGE_HANDLER);
                        eventInstruction.setMainPropertyValue(PropertyKey.VALUE, selectedIndexes);
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
    public void update(final Update update, final UIService uiService) {

        final Property property = update.getMainProperty();
        final PropertyKey propertyKey = property.getPropertyKey();
        final com.google.gwt.user.client.ui.ListBox listBox = cast();

        switch (propertyKey) {
            case CLEAR:
                listBox.clear();
                break;
            case ITEM_INSERTED: {
                final int index = property.getIntPropertyValue(PropertyKey.INDEX);
                final String item = property.getStringPropertyValue(PropertyKey.ITEM_TEXT);
                final String value = property.getStringPropertyValue(PropertyKey.VALUE);
                listBox.insertItem(item, value, index);
                break;
            }
            case ITEM_REMOVED: {
                final int index = property.getIntPropertyValue(PropertyKey.INDEX);
                listBox.removeItem(index);
                break;
            }
            case SELECTED:
                final boolean selected = property.getBooleanValue();
                final int index = property.getIntPropertyValue(PropertyKey.SELECTED_INDEX);
                if (index == -1) listBox.setSelectedIndex(index);
                else listBox.setItemSelected(index, selected);
                break;
            case VISIBLE_ITEM_COUNT:
                listBox.setVisibleItemCount(property.getIntValue());
                break;
            case MULTISELECT:
                listBox.setMultipleSelect(property.getBooleanValue());
                break;

            default:
                break;
        }

        super.update(update, uiService);

    }

    @Override
    public com.google.gwt.user.client.ui.ListBox cast() {
        return (com.google.gwt.user.client.ui.ListBox) uiObject;
    }
}
