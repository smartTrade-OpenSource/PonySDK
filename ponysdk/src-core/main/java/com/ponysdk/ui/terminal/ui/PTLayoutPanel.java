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
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTLayoutPanel extends PTComplexPanel<LayoutPanel> {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new LayoutPanel());
    }

    private Alignment getAlignement(final Model property, final PTInstruction update) {
        final PAlignment alignment = PAlignment.values()[update.getInt(property)];
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

    private Unit getUnit(final PTInstruction update) {
        final PUnit u = PUnit.values()[update.getInt(Model.UNIT)];
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
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(Model.HORIZONTAL_ALIGNMENT)) {
            final Widget w = asWidget(update.getLong(Model.WIDGET), uiService);
            final Alignment alignment = getAlignement(Model.HORIZONTAL_ALIGNMENT, update);
            uiObject.setWidgetHorizontalPosition(w, alignment);
        } else if (update.containsKey(Model.VERTICAL_ALIGNMENT)) {
            final Widget w = asWidget(update.getLong(Model.WIDGET), uiService);
            final Alignment alignment = getAlignement(Model.VERTICAL_ALIGNMENT, update);
            uiObject.setWidgetVerticalPosition(w, alignment);
        } else if (update.containsKey(Model.UNIT)) {
            final Widget w = asWidget(update.getLong(Model.WIDGET), uiService);
            final Unit unit = getUnit(update);

            if (update.containsKey(Model.LEFT)) {
                final double left = update.getDouble(Model.LEFT);
                if (update.containsKey(Model.RIGHT)) {
                    final double right = update.getDouble(Model.RIGHT);
                    uiObject.setWidgetLeftRight(w, left, unit, right, unit);
                } else if (update.containsKey(Model.WIDTH)) {
                    final double width = update.getDouble(Model.WIDTH);
                    uiObject.setWidgetLeftWidth(w, left, unit, width, unit);
                }
            } else if (update.containsKey(Model.RIGHT)) {
                final double right = update.getDouble(Model.RIGHT);
                final double width = update.getDouble(Model.WIDTH);
                uiObject.setWidgetRightWidth(w, right, unit, width, unit);
            } else if (update.containsKey(Model.TOP)) {
                final double top = update.getDouble(Model.TOP);
                if (update.containsKey(Model.BOTTOM)) {
                    final double bottom = update.getDouble(Model.BOTTOM);
                    uiObject.setWidgetTopBottom(w, top, unit, bottom, unit);
                } else if (update.containsKey(Model.HEIGHT)) {
                    final double height = update.getDouble(Model.HEIGHT);
                    uiObject.setWidgetTopHeight(w, top, unit, height, unit);
                }
            } else if (update.containsKey(Model.BOTTOM)) {
                final double bottom = update.getDouble(Model.BOTTOM);
                final double height = update.getDouble(Model.HEIGHT);
                uiObject.setWidgetBottomHeight(w, bottom, unit, height, unit);
            }
        } else if (update.containsKey(Model.ANIMATE)) {
            uiObject.animate(update.getInt(Model.ANIMATE));
        } else {
            super.update(update, uiService);
        }
    }
}
