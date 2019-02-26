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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.model.PDirection;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTDockLayoutPanel<T extends DockLayoutPanel> extends PTComplexPanel<T> {

    private Unit unit;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiBuilder) {
        // ServerToClientModel.UNIT
        unit = Unit.values()[buffer.readBinaryModel().getIntValue()];
        super.create(buffer, objectId, uiBuilder);
    }

    @Override
    protected T createUIObject() {
        return (T) new DockLayoutPanel(unit);
    }

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        final Widget w = asWidget(ptObject);
        // ServerToClientModel.DIRECTION
        final PDirection direction = PDirection.fromRawValue(buffer.readBinaryModel().getIntValue());

        // ServerToClientModel.SIZE
        final double size = buffer.readBinaryModel().getDoubleValue();

        if (PDirection.CENTER == direction) uiObject.add(w);
        else if (PDirection.NORTH == direction) uiObject.addNorth(w, size);
        else if (PDirection.SOUTH == direction) uiObject.addSouth(w, size);
        else if (PDirection.EAST == direction) uiObject.addEast(w, size);
        else if (PDirection.WEST == direction) uiObject.addWest(w, size);
        else if (PDirection.LINE_START == direction) uiObject.addLineStart(w, size);
        else if (PDirection.LINE_END == direction) uiObject.addLineEnd(w, size);
        else throw new IllegalArgumentException("Unkown direction : " + direction);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.WIDGET_SIZE == model) {
            final double newSize = binaryModel.getDoubleValue();
            // ServerToClientModel.WIDGET_ID
            final Widget w = asWidget(buffer.readBinaryModel().getIntValue(), uiBuilder);
            uiObject.setWidgetSize(w, newSize);
            return true;
        } else if (ServerToClientModel.WIDGET_HIDDEN == model) {
            final boolean hidden = binaryModel.getBooleanValue();
            // ServerToClientModel.WIDGET_ID
            final Widget w = asWidget(buffer.readBinaryModel().getIntValue(), uiBuilder);
            uiObject.setWidgetHidden(w, hidden);
            return true;
        } else if (ServerToClientModel.ANIMATE == model) {
            uiObject.animate(binaryModel.getIntValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

}
