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
import com.google.gwt.user.client.ui.DockLayoutPanel.Direction;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.Create;

public class PTDockLayoutPanel extends PTComplexPanel {

    @Override
    public void create(Create create, UIService uiService) {
        init(new com.google.gwt.user.client.ui.DockLayoutPanel(Unit.PX));// must be a parametter
    }

    @Override
    public com.google.gwt.user.client.ui.DockLayoutPanel cast() {
        return (com.google.gwt.user.client.ui.DockLayoutPanel) uiObject;
    }

    @Override
    public void add(Add add, UIService uiService) {

        final Widget w = asWidget(add.getObjectID(), uiService);
        final com.google.gwt.user.client.ui.DockLayoutPanel dockLayoutPanel = cast();

        final Direction direction = Direction.values()[add.getMainProperty().getIntProperty(PropertyKey.DIRECTION)];
        final double size = add.getMainProperty().getDoubleProperty(PropertyKey.SIZE);
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
}
