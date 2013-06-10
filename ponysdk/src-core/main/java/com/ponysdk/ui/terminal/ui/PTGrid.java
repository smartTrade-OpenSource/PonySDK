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
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTGrid extends PTHTMLTable {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        Grid grid;
        if (create.containsKey(PROPERTY.ROW)) {
            final int rows = create.getInt(PROPERTY.ROW);
            final int columns = create.getInt(PROPERTY.COLUMN);
            grid = new Grid(rows, columns);
            init(create, uiService, grid);
        } else {
            grid = new Grid();
            init(create, uiService, grid);
        }

        grid.addStyleName("pony-PGrid");
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.CLEAR_ROW)) {
            cast().removeRow(update.getInt(PROPERTY.CLEAR_ROW));
        } else if (update.containsKey(PROPERTY.INSERT_ROW)) {
            cast().insertRow(update.getInt(PROPERTY.INSERT_ROW));
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public Grid cast() {
        return (Grid) uiObject;
    }
}
