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
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;

public class PTDockLayoutPanel extends PTComplexPanel {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        final Unit unit = Unit.values()[create.getInt(PROPERTY.UNIT)];
        init(create, uiService, new DockLayoutPanel(unit));
    }

    @Override
    public void add(final PTInstruction add, final UIService uiService) {

        final Widget w = asWidget(add.getObjectID(), uiService);
        final DockLayoutPanel dockLayoutPanel = cast();

        final Direction direction = Direction.values()[add.getInt(PROPERTY.DIRECTION)];
        final double size = add.getDouble(PROPERTY.SIZE);
        switch (direction) {
            case CENTER: {
                dockLayoutPanel.add(w);
                break;
            }
            case NORTH: {
                dockLayoutPanel.addNorth(w, size);
                break;
            }
            case SOUTH: {
                dockLayoutPanel.addSouth(w, size);
                break;
            }
            case EAST: {
                dockLayoutPanel.addEast(w, size);
                break;
            }
            case WEST: {
                dockLayoutPanel.addWest(w, size);
                break;
            }
            case LINE_START: {
                dockLayoutPanel.addLineStart(w, size);
                break;
            }
            case LINE_END: {
                dockLayoutPanel.addLineEnd(w, size);
                break;
            }
        }
    }

    @Override
    public DockLayoutPanel cast() {
        return (DockLayoutPanel) uiObject;
    }

}
