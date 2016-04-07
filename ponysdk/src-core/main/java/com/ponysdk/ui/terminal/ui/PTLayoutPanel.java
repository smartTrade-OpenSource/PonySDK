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
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTLayoutPanel extends PTComplexPanel<LayoutPanel> {

    private UIService uiService;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        this.uiObject = new LayoutPanel();
        this.objectID = objectId;
        uiService.registerUIObject(this.objectID, uiObject);
        this.uiService = uiService;
    }

    private Alignment getAlignement(final byte alignementId) {
        final PAlignment alignment = PAlignment.values()[alignementId];
        switch (alignment) {
            case BEGIN:
                return Alignment.BEGIN;
            case END:
                return Alignment.END;
            case STRETCH:
                return Alignment.STRETCH;
        }
        return null;
    }

    private static final Unit getUnit(final byte unit) {
        final PUnit u = PUnit.values()[unit];
        switch (u) {
            case PX:
                return Unit.PX;
            case EM:
                return Unit.EM;
            case PCT:
                return Unit.PCT;
            case CM:
                return Unit.CM;
            case EX:
                return Unit.EX;
            case IN:
                return Unit.IN;
            case MM:
                return Unit.MM;
            case PC:
                return Unit.PC;
            case PT:
                return Unit.PT;
        }
        return null;
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (Model.HORIZONTAL_ALIGNMENT.equals(binaryModel.getModel())) {
            final Alignment alignment = getAlignement(binaryModel.getByteValue());
            // Model.WIDGET_ID
            final Widget w = asWidget(buffer.getBinaryModel().getIntValue(), uiService);
            uiObject.setWidgetHorizontalPosition(w, alignment);
            return true;
        }
        if (Model.VERTICAL_ALIGNMENT.equals(binaryModel.getModel())) {
            final Alignment alignment = getAlignement(binaryModel.getByteValue());
            // Model.WIDGET_ID
            final Widget w = asWidget(buffer.getBinaryModel().getIntValue(), uiService);
            uiObject.setWidgetVerticalPosition(w, alignment);
            return true;
        }
        if (Model.UNIT.equals(binaryModel.getModel())) {
            // Model.WIDGET_ID
            final Widget w = asWidget(buffer.getBinaryModel().getIntValue(), uiService);
            final Unit unit = getUnit(binaryModel.getByteValue());

            final BinaryModel key1 = buffer.getBinaryModel();
            final BinaryModel key2 = buffer.getBinaryModel();
            if (Model.LEFT.equals(key1.getModel())) {
                final double left = key1.getDoubleValue();
                if (Model.RIGHT.equals(key2.getModel())) {
                    final double right = key2.getDoubleValue();
                    uiObject.setWidgetLeftRight(w, left, unit, right, unit);
                    return true;
                } else if (Model.WIDTH.equals(key2.getModel())) {
                    final double width = key2.getDoubleValue();
                    uiObject.setWidgetLeftWidth(w, left, unit, width, unit);
                    return true;
                }
            } else if (Model.RIGHT.equals(key1.getModel())) {
                final double right = key1.getDoubleValue();
                // Model.WIDTH
                final double width = key2.getDoubleValue();
                uiObject.setWidgetRightWidth(w, right, unit, width, unit);
                return true;
            } else if (Model.TOP.equals(key1.getModel())) {
                final double top = key1.getDoubleValue();
                if (Model.BOTTOM.equals(key2.getModel())) {
                    final double bottom = key2.getDoubleValue();
                    uiObject.setWidgetTopBottom(w, top, unit, bottom, unit);
                    return true;
                } else if (Model.HEIGHT.equals(key2.getModel())) {
                    final double height = key2.getDoubleValue();
                    uiObject.setWidgetTopHeight(w, top, unit, height, unit);
                    return true;
                }
            } else if (Model.BOTTOM.equals(key1.getModel())) {
                final double bottom = key1.getDoubleValue();
                // Model.HEIGHT
                final double height = key2.getDoubleValue();
                uiObject.setWidgetBottomHeight(w, bottom, unit, height, unit);
                return true;
            }
        }
        if (Model.ANIMATE.equals(binaryModel.getModel())) {
            uiObject.animate(binaryModel.getIntValue());
            return true;
        }
        return super.update(buffer, binaryModel);
    }
}
