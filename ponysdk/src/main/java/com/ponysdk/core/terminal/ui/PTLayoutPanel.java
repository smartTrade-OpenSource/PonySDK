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
import com.google.gwt.layout.client.Layout.Alignment;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import com.ponysdk.core.terminal.ui.converter.GWTConverter;

public class PTLayoutPanel extends PTComplexPanel<LayoutPanel> {

    @Override
    protected LayoutPanel createUIObject() {
        return new LayoutPanel();
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.WIDGET_HORIZONTAL_ALIGNMENT == model) {
            final Alignment alignment = GWTConverter.asAlignment(binaryModel.getIntValue());
            // ServerToClientModel.WIDGET_ID
            final Widget w = asWidget(buffer.readBinaryModel().getIntValue(), uiBuilder);
            uiObject.setWidgetHorizontalPosition(w, alignment);
            return true;
        } else if (ServerToClientModel.WIDGET_VERTICAL_ALIGNMENT == model) {
            final Alignment alignment = GWTConverter.asAlignment(binaryModel.getIntValue());
            // ServerToClientModel.WIDGET_ID
            final Widget w = asWidget(buffer.readBinaryModel().getIntValue(), uiBuilder);
            uiObject.setWidgetVerticalPosition(w, alignment);
            return true;
        } else if (ServerToClientModel.UNIT == model) {
            final Unit unit = GWTConverter.asUnit(binaryModel.getIntValue());
            // ServerToClientModel.WIDGET_ID
            final Widget w = asWidget(buffer.readBinaryModel().getIntValue(), uiBuilder);

            final BinaryModel key1 = buffer.readBinaryModel();
            if (ServerToClientModel.LEFT == key1.getModel()) {
                final double left = key1.getDoubleValue();
                final BinaryModel key2 = buffer.readBinaryModel();
                if (ServerToClientModel.RIGHT == key2.getModel()) {
                    final double right = key2.getDoubleValue();
                    uiObject.setWidgetLeftRight(w, left, unit, right, unit);
                    return true;
                } else if (ServerToClientModel.WIDTH == key2.getModel()) {
                    final double width = key2.getDoubleValue();
                    uiObject.setWidgetLeftWidth(w, left, unit, width, unit);
                    return true;
                }
            } else if (ServerToClientModel.RIGHT == key1.getModel()) {
                final double right = key1.getDoubleValue();
                // ServerToClientModel.WIDTH
                final BinaryModel key2 = buffer.readBinaryModel();
                final double width = key2.getDoubleValue();
                uiObject.setWidgetRightWidth(w, right, unit, width, unit);
                return true;
            } else if (ServerToClientModel.TOP == key1.getModel()) {
                final double top = key1.getDoubleValue();
                final BinaryModel key2 = buffer.readBinaryModel();
                if (ServerToClientModel.BOTTOM == key2.getModel()) {
                    final double bottom = key2.getDoubleValue();
                    uiObject.setWidgetTopBottom(w, top, unit, bottom, unit);
                    return true;
                } else if (ServerToClientModel.HEIGHT == key2.getModel()) {
                    final double height = key2.getDoubleValue();
                    uiObject.setWidgetTopHeight(w, top, unit, height, unit);
                    return true;
                }
            } else if (ServerToClientModel.BOTTOM == key1.getModel()) {
                final double bottom = key1.getDoubleValue();
                // ServerToClientModel.HEIGHT
                final BinaryModel key2 = buffer.readBinaryModel();
                final double height = key2.getDoubleValue();
                uiObject.setWidgetBottomHeight(w, bottom, unit, height, unit);
                return true;
            }
        }

        // FIXME
        if (ServerToClientModel.ANIMATE == model) {
            uiObject.animate(binaryModel.getIntValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

}
