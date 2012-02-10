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

import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTCellPanel extends PTComplexPanel {

    @Override
    public void update(final Update update, final UIService uiService) {

        final Property property = update.getMainProperty();
        final PropertyKey propertyKey = property.getKey();
        final com.google.gwt.user.client.ui.CellPanel cellPanel = cast();

        if (PropertyKey.CELL_HORIZONTAL_ALIGNMENT.equals(propertyKey)) {
            final PHorizontalAlignment horizontalAlignment = PHorizontalAlignment.values()[property.getIntValue()];
            final Widget w = asWidget(property.getLongPropertyValue(PropertyKey.CELL), uiService);
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
        } else if (PropertyKey.CELL_VERTICAL_ALIGNMENT.equals(propertyKey)) {
            final PVerticalAlignment verticalAlignment = PVerticalAlignment.values()[property.getIntValue()];
            final Widget w = asWidget(property.getLongPropertyValue(PropertyKey.CELL), uiService);
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
        } else if (PropertyKey.CELL_WIDTH.equals(propertyKey)) {
            cellPanel.setCellWidth(asWidget(property.getLongPropertyValue(PropertyKey.CELL), uiService), property.getValue());
        } else if (PropertyKey.CELL_HEIGHT.equals(propertyKey)) {
            cellPanel.setCellHeight(asWidget(property.getLongPropertyValue(PropertyKey.CELL), uiService), property.getValue());
        } else {
            super.update(update, uiService);
        }

    }

    @Override
    public com.google.gwt.user.client.ui.CellPanel cast() {
        return (com.google.gwt.user.client.ui.CellPanel) uiObject;
    }

}
