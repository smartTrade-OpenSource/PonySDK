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

import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTCellPanel extends PTComplexPanel {

    @Override
    public void update(final PTInstruction update, final UIService uiService) {

        final CellPanel cellPanel = cast();

        if (update.containsKey(PROPERTY.CELL_HORIZONTAL_ALIGNMENT)) {
            final PHorizontalAlignment horizontalAlignment = PHorizontalAlignment.values()[update.getInt(PROPERTY.CELL_HORIZONTAL_ALIGNMENT)];
            final Widget w = asWidget(update.getLong(PROPERTY.CELL), uiService);
            switch (horizontalAlignment) {
                case ALIGN_LEFT:
                    cellPanel.setCellHorizontalAlignment(w, HasHorizontalAlignment.ALIGN_LEFT);
                    break;
                case ALIGN_CENTER:
                    cellPanel.setCellHorizontalAlignment(w, HasHorizontalAlignment.ALIGN_CENTER);
                    break;
                case ALIGN_RIGHT:
                    cellPanel.setCellHorizontalAlignment(w, HasHorizontalAlignment.ALIGN_RIGHT);
                    break;
                default:
                    break;
            }
        } else if (update.containsKey(PROPERTY.CELL_VERTICAL_ALIGNMENT)) {
            final PVerticalAlignment verticalAlignment = PVerticalAlignment.values()[update.getInt(PROPERTY.CELL_VERTICAL_ALIGNMENT)];
            final Widget w = asWidget(update.getLong(PROPERTY.CELL), uiService);
            switch (verticalAlignment) {
                case ALIGN_TOP:
                    cellPanel.setCellVerticalAlignment(w, HasVerticalAlignment.ALIGN_TOP);
                    break;
                case ALIGN_MIDDLE:
                    cellPanel.setCellVerticalAlignment(w, HasVerticalAlignment.ALIGN_MIDDLE);
                    break;
                case ALIGN_BOTTOM:
                    cellPanel.setCellVerticalAlignment(w, HasVerticalAlignment.ALIGN_BOTTOM);
                    break;
                default:
                    break;
            }
        } else if (update.containsKey(PROPERTY.CELL_WIDTH)) {
            cellPanel.setCellWidth(asWidget(update.getLong(PROPERTY.CELL), uiService), update.get(PROPERTY.CELL_WIDTH).isString().stringValue());
        } else if (update.containsKey(PROPERTY.CELL_HEIGHT)) {
            cellPanel.setCellHeight(asWidget(update.getLong(PROPERTY.CELL), uiService), update.get(PROPERTY.CELL_HEIGHT).isString().stringValue());
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public CellPanel cast() {
        return (CellPanel) uiObject;
    }

}
