/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.ui.server.basic;

import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

/**
 * A panel whose child widgets are contained within the cells of a table. Each cell's size may be set
 * independently. Each child widget can take up a subset of its cell and can be aligned within it.
 * <p>
 * Note: This class is not related to the {@link PCell} based data presentation widgets such as
 * {@link PCellList} and {@link PCellTable}.
 * </p>
 */
public abstract class PCellPanel extends PComplexPanel {

    private int borderWidth;

    private int spacing;

    public void setBorderWidth(final int width) {
        this.borderWidth = width;
        final Update update = new Update(getID());
        update.put(PROPERTY.BORDER_WIDTH, width);
        getUIContext().stackInstruction(update);
    }

    public void setSpacing(final int spacing) {
        this.spacing = spacing;
        final Update update = new Update(getID());
        update.put(PROPERTY.SPACING, spacing);
        getUIContext().stackInstruction(update);
    }

    public void setCellHorizontalAlignment(final PWidget widget, final PHorizontalAlignment horizontalAlignment) {
        final Update update = new Update(getID());
        update.put(PROPERTY.CELL_HORIZONTAL_ALIGNMENT, horizontalAlignment.ordinal());
        update.put(PROPERTY.CELL, widget.getID());
        getUIContext().stackInstruction(update);
    }

    public void setCellVerticalAlignment(final PWidget widget, final PVerticalAlignment verticalAlignment) {
        final Update update = new Update(getID());
        update.put(PROPERTY.CELL_VERTICAL_ALIGNMENT, verticalAlignment.ordinal());
        update.put(PROPERTY.CELL, widget.getID());
        getUIContext().stackInstruction(update);
    }

    public void setCellHeight(final PWidget widget, final String height) {
        final Update update = new Update(getID());
        update.put(PROPERTY.CELL_HEIGHT, height);
        update.put(PROPERTY.CELL, widget.getID());
        getUIContext().stackInstruction(update);
    }

    public void setCellWidth(final PWidget widget, final String width) {
        final Update update = new Update(getID());
        update.put(PROPERTY.CELL_WIDTH, width);
        update.put(PROPERTY.CELL, widget.getID());
        getUIContext().stackInstruction(update);
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public int getSpacing() {
        return spacing;
    }
}
