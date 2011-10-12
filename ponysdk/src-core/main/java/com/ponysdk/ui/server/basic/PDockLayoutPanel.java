/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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
package com.ponysdk.ui.server.basic;

import com.google.gwt.user.client.ui.DockLayoutPanel.Direction;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Add;

public class PDockLayoutPanel extends PComplexPanel {

    @Override
    protected WidgetType getType() {
        return WidgetType.DOCK_LAYOUT_PANEL;
    }

    @Override
    public void add(PWidget child) {
        add(child, Direction.CENTER, 0);
    }

    public void addNorth(PWidget widget, double size) {
        add(widget, Direction.NORTH, size);
    }

    public void addSouth(PWidget widget, double size) {
        add(widget, Direction.SOUTH, size);
    }

    public void addEast(PWidget widget, double size) {
        add(widget, Direction.EAST, size);
    }

    public void addWest(PWidget widget, double size) {
        add(widget, Direction.WEST, size);
    }

    public void addLineEnd(PWidget widget, double size) {
        add(widget, Direction.LINE_END, size);
    }

    public void addLineStart(PWidget widget, double size) {
        add(widget, Direction.LINE_START, size);
    }

    public void add(PWidget child, Direction direction, double size) {
        // Detach new child.
        child.removeFromParent();

        // Logical attach.
        getChildren().add(child);
        // Adopt.
        adopt(child);
        final Add add = new Add(child.getID(), getID());

        add.getMainProperty().setProperty(PropertyKey.DIRECTION, direction.ordinal());
        add.getMainProperty().setProperty(PropertyKey.SIZE, size);

        getPonySession().stackInstruction(add);
    }

}
