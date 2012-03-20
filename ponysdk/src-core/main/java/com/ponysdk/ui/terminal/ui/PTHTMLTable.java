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

import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTHTMLTable extends PTPanel {

    @Override
    public com.google.gwt.user.client.ui.HTMLTable cast() {
        return (com.google.gwt.user.client.ui.HTMLTable) uiObject;
    }

    @Override
    public void add(final Add add, final UIService uiService) {

        final Widget w = asWidget(add.getObjectID(), uiService);
        final com.google.gwt.user.client.ui.HTMLTable table = cast();

        final int row = add.getMainProperty().getIntPropertyValue(PropertyKey.ROW);
        final int cell = add.getMainProperty().getIntPropertyValue(PropertyKey.CELL);
        table.getCellFormatter().addStyleName(row, cell, "pony-PFlextable-Cell");// temp nciaravola we
        // don't
        // // have this mirro server
        // // side
        table.setWidget(row, cell, w);
    }

    @Override
    public void update(final Update update, final UIService uiService) {
        final Property property = update.getMainProperty();
        final PropertyKey propertyKey = property.getPropertyKey();

        if (PropertyKey.CLEAR.equals(propertyKey)) {
            cast().clear();
        } else if (PropertyKey.CELL_SPACING.equals(propertyKey)) {
            cast().setCellSpacing(property.getIntValue());
        } else if (PropertyKey.CELL_PADDING.equals(propertyKey)) {
            cast().setCellPadding(property.getIntValue());
        } else if (PropertyKey.HTMLTABLE_ROW_STYLE.equals(propertyKey)) {
            final int row = property.getChildProperty(PropertyKey.ROW).getIntValue();
            final Property addProperty = property.getChildProperty(PropertyKey.ROW_FORMATTER_ADD_STYLE_NAME);
            final Property removeProperty = property.getChildProperty(PropertyKey.ROW_FORMATTER_REMOVE_STYLE_NAME);
            if (addProperty != null) {
                cast().getRowFormatter().addStyleName(row, addProperty.getValue());
            } else {
                cast().getRowFormatter().removeStyleName(row, removeProperty.getValue());
            }
        } else if (PropertyKey.HTMLTABLE_CELL_STYLE.equals(propertyKey)) {
            final int cellRow = property.getChildProperty(PropertyKey.ROW).getIntValue();
            final int cellColumn = property.getChildProperty(PropertyKey.COLUMN).getIntValue();
            final Property addCellProperty = property.getChildProperty(PropertyKey.CELL_FORMATTER_ADD_STYLE_NAME);
            if (addCellProperty != null) {
                cast().getCellFormatter().addStyleName(cellRow, cellColumn, addCellProperty.getValue());
            }
            final Property removeCellProperty = property.getChildProperty(PropertyKey.CELL_FORMATTER_REMOVE_STYLE_NAME);
            if (removeCellProperty != null) {
                cast().getCellFormatter().removeStyleName(cellRow, cellColumn, removeCellProperty.getValue());
            }
            final Property verticalAlignmentProperty = property.getChildProperty(PropertyKey.CELL_VERTICAL_ALIGNMENT);
            if (verticalAlignmentProperty != null) {
                cast().getCellFormatter().setVerticalAlignment(cellRow, cellColumn, PVerticalAlignment.values()[verticalAlignmentProperty.getIntValue()].asVerticalAlignmentConstant());
            }
            final Property horizontalAlignmentProperty = property.getChildProperty(PropertyKey.CELL_HORIZONTAL_ALIGNMENT);
            if (horizontalAlignmentProperty != null) {
                cast().getCellFormatter().setHorizontalAlignment(cellRow, cellColumn, PHorizontalAlignment.values()[horizontalAlignmentProperty.getIntValue()].asHorizontalAlignmentConstant());
            }

        } else {
            super.update(update, uiService);
        }
    }

}
