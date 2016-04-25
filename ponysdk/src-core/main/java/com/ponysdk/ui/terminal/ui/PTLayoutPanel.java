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
import com.google.gwt.layout.client.Layout.Alignment;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.PUnit;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.basic.PAlignment;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

public class PTLayoutPanel extends PTComplexPanel<LayoutPanel> {

    private UIService uiService;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        super.create(buffer, objectId, uiService);
        this.uiService = uiService;
    }

    @Override
    protected LayoutPanel createUIObject() {
        return new LayoutPanel();
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.HORIZONTAL_ALIGNMENT.equals(binaryModel.getModel())) {
            final Alignment alignment = PAlignment.getAlignement(PAlignment.values()[binaryModel.getByteValue()]);
            // ServerToClientModel.WIDGET_ID
            final Widget w = asWidget(buffer.getBinaryModel().getIntValue(), uiService);
            uiObject.setWidgetHorizontalPosition(w, alignment);
            return true;
        }
        if (ServerToClientModel.VERTICAL_ALIGNMENT.equals(binaryModel.getModel())) {
            final Alignment alignment = PAlignment.getAlignement(PAlignment.values()[binaryModel.getByteValue()]);
            // ServerToClientModel.WIDGET_ID
            final Widget w = asWidget(buffer.getBinaryModel().getIntValue(), uiService);
            uiObject.setWidgetVerticalPosition(w, alignment);
            return true;
        }
        if (ServerToClientModel.UNIT.equals(binaryModel.getModel())) {
            // ServerToClientModel.WIDGET_ID
            final Widget w = asWidget(buffer.getBinaryModel().getIntValue(), uiService);
            final Unit unit = PUnit.getUnit(PUnit.values()[binaryModel.getByteValue()]);

            final BinaryModel key1 = buffer.getBinaryModel();
            final BinaryModel key2 = buffer.getBinaryModel();
            if (ServerToClientModel.LEFT.equals(key1.getModel())) {
                final double left = key1.getDoubleValue();
                if (ServerToClientModel.RIGHT.equals(key2.getModel())) {
                    final double right = key2.getDoubleValue();
                    uiObject.setWidgetLeftRight(w, left, unit, right, unit);
                    return true;
                } else if (ServerToClientModel.WIDTH.equals(key2.getModel())) {
                    final double width = key2.getDoubleValue();
                    uiObject.setWidgetLeftWidth(w, left, unit, width, unit);
                    return true;
                }
            } else if (ServerToClientModel.RIGHT.equals(key1.getModel())) {
                final double right = key1.getDoubleValue();
                // ServerToClientModel.WIDTH
                final double width = key2.getDoubleValue();
                uiObject.setWidgetRightWidth(w, right, unit, width, unit);
                return true;
            } else if (ServerToClientModel.TOP.equals(key1.getModel())) {
                final double top = key1.getDoubleValue();
                if (ServerToClientModel.BOTTOM.equals(key2.getModel())) {
                    final double bottom = key2.getDoubleValue();
                    uiObject.setWidgetTopBottom(w, top, unit, bottom, unit);
                    return true;
                } else if (ServerToClientModel.HEIGHT.equals(key2.getModel())) {
                    final double height = key2.getDoubleValue();
                    uiObject.setWidgetTopHeight(w, top, unit, height, unit);
                    return true;
                }
            } else if (ServerToClientModel.BOTTOM.equals(key1.getModel())) {
                final double bottom = key1.getDoubleValue();
                // ServerToClientModel.HEIGHT
                final double height = key2.getDoubleValue();
                uiObject.setWidgetBottomHeight(w, bottom, unit, height, unit);
                return true;
            }
        }
        if (ServerToClientModel.ANIMATE.equals(binaryModel.getModel())) {
            uiObject.animate(binaryModel.getIntValue());
            return true;
        }
        return super.update(buffer, binaryModel);
    }

}
