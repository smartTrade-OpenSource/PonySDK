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
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Add;

public class PTHTMLTable extends PTPanel {

    @Override
    public com.google.gwt.user.client.ui.HTMLTable cast() {
        return (com.google.gwt.user.client.ui.HTMLTable) uiObject;
    }

    @Override
    public void add(Add add, UIService uiService) {

        final Widget w = asWidget(add.getObjectID(), uiService);
        final com.google.gwt.user.client.ui.HTMLTable table = cast();

        final int row = add.getMainProperty().getIntProperty(PropertyKey.ROW);
        final int cell = add.getMainProperty().getIntProperty(PropertyKey.CELL);
        table.getCellFormatter().addStyleName(row, cell, "pony-PFlextable-Cell");// temp nciaravola we
        // don't
        // // have this mirro server
        // // side
        table.setWidget(row, cell, w);
    }

    // @Override
    // public void update(Update update, UIService uiService) {
    // final Property property = update.getMainProperty();
    // final PropertyKey propertyKey = property.getKey();
    //
    // if (PropertyKey.COLUMN_FORMATTER_COLUMN_WIDTH.equals(propertyKey)) {
    // final int column = property.getChildProperty(PropertyKey.COLUMN).getIntValue();
    // final String width = property.getChildProperty(PropertyKey.WIDTH).getValue();
    // cast().getColumnFormatter().setWidth(column, width);
    // } else if (PropertyKey.COLUMN_FORMATTER_ADD_STYLE_NAME.equals(propertyKey)) {
    // final int column = property.getChildProperty(PropertyKey.COLUMN).getIntValue();
    // final String styleName = property.getChildProperty(PropertyKey.STYLE_NAME).getValue();
    // cast().getColumnFormatter().addStyleName(column, styleName);
    // } else if (PropertyKey.COLUMN_FORMATTER_REMOVE_STYLE_NAME.equals(propertyKey)) {
    // final int column = property.getChildProperty(PropertyKey.COLUMN).getIntValue();
    // final String styleName = property.getChildProperty(PropertyKey.STYLE_NAME).getValue();
    // cast().getColumnFormatter().removeStyleName(column, styleName);
    // } else {
    // super.update(update, uiService);
    // }
    // }

}
