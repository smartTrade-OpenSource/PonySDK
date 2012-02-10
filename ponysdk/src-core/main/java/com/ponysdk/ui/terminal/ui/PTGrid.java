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

import com.google.gwt.user.client.ui.Grid;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTGrid extends PTHTMLTable {

    @Override
    public void create(final Create create, final UIService uiService) {
        if (create.getMainProperty().containsChildProperty(PropertyKey.ROW)) {
            int rows = create.getMainProperty().getIntPropertyValue(PropertyKey.ROW);
            int columns = create.getMainProperty().getIntPropertyValue(PropertyKey.COLUMN);
            init(create, uiService, new Grid(rows, columns));
        } else {
            init(create, uiService, new Grid());
        }
    }

    @Override
    public void update(final Update update, final UIService uiService) {

        final Property property = update.getMainProperty();
        final PropertyKey propertyKey = property.getKey();

        if (PropertyKey.CLEAR_ROW.equals(propertyKey)) {
            cast().removeRow(property.getIntValue());
        } else if (PropertyKey.INSERT_ROW.equals(propertyKey)) {
            cast().insertRow(property.getIntValue());
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public Grid cast() {
        return (Grid) uiObject;
    }
}
