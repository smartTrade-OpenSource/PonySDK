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

package com.ponysdk.core.ui.basic;

import java.util.Objects;

import com.ponysdk.core.model.PHorizontalAlignment;
import com.ponysdk.core.model.PVerticalAlignment;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.writer.ModelWriter;

/**
 * A panel whose child widgets are contained within the cells of a table. Each cell's size may be set independently.
 * Each child widget can take up a subset of its cell and can be aligned within it.
 *
 * @see com.google.gwt.user.client.ui.CellPanel
 */
public abstract class PCellPanel extends PComplexPanel {

    private Integer borderWidth;
    private Integer spacing;

    /**
     * Instantiates a new PCellPanel
     */
    protected PCellPanel() {
    }

    @Override
    protected void enrichOnInit(final ModelWriter writer) {
        super.enrichOnInit(writer);
        if (this.borderWidth != null) writer.write(ServerToClientModel.BORDER_WIDTH, this.borderWidth);
        if (this.spacing != null) writer.write(ServerToClientModel.SPACING, this.spacing);
    }

    public void setCellHorizontalAlignment(final PWidget widget, final PHorizontalAlignment align) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.WIDGET_HORIZONTAL_ALIGNMENT, align.getValue());
            writer.write(ServerToClientModel.WIDGET_ID, widget.getID());
        });
    }

    public void setCellVerticalAlignment(final PWidget widget, final PVerticalAlignment align) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.WIDGET_VERTICAL_ALIGNMENT, align.getValue());
            writer.write(ServerToClientModel.WIDGET_ID, widget.getID());
        });
    }

    public void setCellHeight(final PWidget widget, final String height) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.CELL_HEIGHT, height);
            writer.write(ServerToClientModel.WIDGET_ID, widget.getID());
        });
    }

    public void setCellWidth(final PWidget widget, final String width) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.CELL_WIDTH, width);
            writer.write(ServerToClientModel.WIDGET_ID, widget.getID());
        });
    }

    public Integer getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(final Integer borderWidth) {
        if (Objects.equals(this.borderWidth, borderWidth)) return;
        this.borderWidth = borderWidth;
        if (initialized) saveUpdate(ServerToClientModel.BORDER_WIDTH, borderWidth);
    }

    public Integer getSpacing() {
        return spacing;
    }

    public void setSpacing(final Integer spacing) {
        if (Objects.equals(this.spacing, spacing)) return;
        this.spacing = spacing;
        if (initialized) saveUpdate(ServerToClientModel.SPACING, spacing);
    }

}
