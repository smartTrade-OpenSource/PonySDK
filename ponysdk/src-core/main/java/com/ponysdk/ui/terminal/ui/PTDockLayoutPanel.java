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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel.Direction;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTDockLayoutPanel extends PTComplexPanel<DockLayoutPanel> {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new DockLayoutPanel(Unit.values()[create.getInt(Model.UNIT)]));
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(Model.WIDGET_SIZE)) {
            final double newSize = update.getDouble(Model.WIDGET_SIZE);
            final Widget w = asWidget(update.getInt(Model.WIDGET_ID), uiService);
            uiObject.setWidgetSize(w, newSize);
        } else if (update.containsKey(Model.WIDGET_HIDDEN)) {
            final boolean hidden = update.getBoolean(Model.WIDGET_HIDDEN);
            final Widget w = asWidget(update.getInt(Model.WIDGET_ID), uiService);
            uiObject.setWidgetHidden(w, hidden);
        } else if (update.containsKey(Model.ANIMATE)) {
            uiObject.animate(update.getInt(Model.ANIMATE));
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public void add(final PTInstruction add, final UIService uiService) {

        final Widget w = asWidget(add.getObjectID(), uiService);
        final Direction direction = Direction.values()[add.getInt(Model.DIRECTION)];
        final double size = add.getDouble(Model.SIZE);

        switch (direction) {
            case CENTER: {
                uiObject.add(w);
                break;
            }
            case NORTH: {
                uiObject.addNorth(w, size);
                break;
            }
            case SOUTH: {
                uiObject.addSouth(w, size);
                break;
            }
            case EAST: {
                uiObject.addEast(w, size);
                break;
            }
            case WEST: {
                uiObject.addWest(w, size);
                break;
            }
            case LINE_START: {
                uiObject.addLineStart(w, size);
                break;
            }
            case LINE_END: {
                uiObject.addLineEnd(w, size);
                break;
            }
        }
    }

}
