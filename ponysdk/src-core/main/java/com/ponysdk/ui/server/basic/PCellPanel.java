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

import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;
import com.ponysdk.ui.terminal.instruction.Update;

public abstract class PCellPanel extends PComplexPanel {

    private int borderWidth;

    private int spacing;

    public void setBorderWidth(int width) {
        this.borderWidth = width;
        final Update update = new Update(getID());
        final Property property = new Property(PropertyKey.BORDER_WIDTH);
        property.setValue(String.valueOf(width));
        update.setMainProperty(property);
        getPonySession().stackInstruction(update);
    }

    public void setSpacing(int spacing) {
        this.spacing = spacing;
        final Update update = new Update(getID());
        final Property property = new Property(PropertyKey.SPACING);
        property.setValue(String.valueOf(spacing));
        update.setMainProperty(property);
        getPonySession().stackInstruction(update);
    }

    public void setCellHorizontalAlignment(PWidget widget, PHorizontalAlignment horizontalAlignment) {
        final Update update = new Update(getID());
        update.setMainPropertyValue(PropertyKey.CELL_HORIZONTAL_ALIGNMENT, horizontalAlignment.ordinal());
        update.getMainProperty().setProperty(PropertyKey.CELL, widget.getID());
        getPonySession().stackInstruction(update);
    }

    public void setCellVerticalAlignment(PWidget widget, PVerticalAlignment verticalAlignment) {
        final Update update = new Update(getID());
        update.setMainPropertyValue(PropertyKey.CELL_VERTICAL_ALIGNMENT, verticalAlignment.ordinal());
        update.getMainProperty().setProperty(PropertyKey.CELL, widget.getID());
        getPonySession().stackInstruction(update);
    }

    public void setCellHeight(PWidget widget, String height) {
        final Update update = new Update(getID());
        update.setMainPropertyValue(PropertyKey.CELL_HEIGHT, height);
        update.getMainProperty().setProperty(PropertyKey.CELL, widget.getID());
        getPonySession().stackInstruction(update);
    }

    public void setCellWidth(PWidget widget, String width) {
        final Update update = new Update(getID());
        update.setMainPropertyValue(PropertyKey.CELL_WIDTH, width);
        update.getMainProperty().setProperty(PropertyKey.CELL, widget.getID());
        getPonySession().stackInstruction(update);
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public int getSpacing() {
        return spacing;
    }
}
