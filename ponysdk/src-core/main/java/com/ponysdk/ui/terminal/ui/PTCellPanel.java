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
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTCellPanel<W extends CellPanel> extends PTComplexPanel<W> {

    @Override
    public void update(final PTInstruction update, final UIService uiService) {

        if (update.containsKey(Model.CELL_HORIZONTAL_ALIGNMENT)) {
            final PHorizontalAlignment horizontalAlignment = PHorizontalAlignment.values()[update.getInt(Model.CELL_HORIZONTAL_ALIGNMENT)];
            final Widget w = asWidget(update.getInt(Model.CELL), uiService);
            switch (horizontalAlignment) {
                case ALIGN_LEFT:
                    uiObject.setCellHorizontalAlignment(w, HasHorizontalAlignment.ALIGN_LEFT);
                    break;
                case ALIGN_CENTER:
                    uiObject.setCellHorizontalAlignment(w, HasHorizontalAlignment.ALIGN_CENTER);
                    break;
                case ALIGN_RIGHT:
                    uiObject.setCellHorizontalAlignment(w, HasHorizontalAlignment.ALIGN_RIGHT);
                    break;
                default:
                    break;
            }
        } else if (update.containsKey(Model.CELL_VERTICAL_ALIGNMENT)) {
            final PVerticalAlignment verticalAlignment = PVerticalAlignment.values()[update.getInt(Model.CELL_VERTICAL_ALIGNMENT)];
            final Widget w = asWidget(update.getInt(Model.CELL), uiService);
            switch (verticalAlignment) {
                case ALIGN_TOP:
                    uiObject.setCellVerticalAlignment(w, HasVerticalAlignment.ALIGN_TOP);
                    break;
                case ALIGN_MIDDLE:
                    uiObject.setCellVerticalAlignment(w, HasVerticalAlignment.ALIGN_MIDDLE);
                    break;
                case ALIGN_BOTTOM:
                    uiObject.setCellVerticalAlignment(w, HasVerticalAlignment.ALIGN_BOTTOM);
                    break;
                default:
                    break;
            }
        } else if (update.containsKey(Model.CELL_WIDTH)) {
            uiObject.setCellWidth(asWidget(update.getInt(Model.CELL), uiService), update.get(Model.CELL_WIDTH).isString().stringValue());
        } else if (update.containsKey(Model.CELL_HEIGHT)) {
            uiObject.setCellHeight(asWidget(update.getInt(Model.CELL), uiService), update.get(Model.CELL_HEIGHT).isString().stringValue());
        } else {
            super.update(update, uiService);
        }
    }

}
