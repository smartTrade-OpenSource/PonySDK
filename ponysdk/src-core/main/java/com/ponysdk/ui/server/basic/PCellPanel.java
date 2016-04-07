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

import java.util.Objects;

import com.ponysdk.core.Parser;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;
import com.ponysdk.ui.terminal.model.Model;

/**
 * A panel whose child widgets are contained within the cells of a table. Each
 * cell's size may be set independently. Each child widget can take up a subset
 * of its cell and can be aligned within it.
 */
public abstract class PCellPanel extends PComplexPanel {

    private Integer borderWidth;
    private Integer spacing;

    public void setBorderWidth(final Integer borderWidth) {
        if (Objects.equals(this.borderWidth, borderWidth))
            return;
        this.borderWidth = borderWidth;
        saveUpdate(Model.BORDER_WIDTH, this.borderWidth);
    }

    public void setSpacing(final Integer spacing) {
        if (Objects.equals(this.spacing, spacing))
            return;
        this.spacing = spacing;
        saveUpdate(Model.SPACING, this.spacing);
    }

    public void setCellHorizontalAlignment(final PWidget widget, final PHorizontalAlignment horizontalAlignment) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE, ID);
        if (window != null) {
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.parse(Model.HORIZONTAL_ALIGNMENT, horizontalAlignment.getValue());
        parser.parse(Model.WIDGET_ID, widget.getID());
        parser.endObject();
    }

    public void setCellVerticalAlignment(final PWidget widget, final PVerticalAlignment verticalAlignment) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE, ID);
        if (window != null) {
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.parse(Model.VERTICAL_ALIGNMENT, verticalAlignment.getValue());
        parser.parse(Model.WIDGET_ID, widget.getID());
        parser.endObject();
    }

    public void setCellHeight(final PWidget widget, final String height) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE, ID);
        if (window != null) {
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.parse(Model.CELL_HEIGHT, height);
        parser.parse(Model.WIDGET_ID, widget.getID());
        parser.endObject();
    }

    public void setCellWidth(final PWidget widget, final String width) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE, ID);
        if (window != null) {
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.parse(Model.CELL_WIDTH, width);
        parser.parse(Model.WIDGET_ID, widget.getID());
        parser.endObject();
    }

    public Integer getBorderWidth() {
        return borderWidth;
    }

    public Integer getSpacing() {
        return spacing;
    }

}
