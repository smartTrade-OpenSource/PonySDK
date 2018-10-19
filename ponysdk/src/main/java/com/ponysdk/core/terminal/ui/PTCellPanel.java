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

package com.ponysdk.core.terminal.ui;

import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import com.ponysdk.core.terminal.ui.converter.GWTConverter;

public abstract class PTCellPanel<W extends CellPanel> extends PTComplexPanel<W> {

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.WIDGET_HORIZONTAL_ALIGNMENT == model) {
            final HorizontalAlignmentConstant horizontalAlignment = GWTConverter
                .asHorizontalAlignmentConstant(binaryModel.getIntValue());
            // ServerToClientModel.WIDGET_ID
            final Widget w = asWidget(buffer.readBinaryModel().getIntValue(), uiBuilder);
            uiObject.setCellHorizontalAlignment(w, horizontalAlignment);
            return true;
        } else if (ServerToClientModel.WIDGET_VERTICAL_ALIGNMENT == model) {
            final VerticalAlignmentConstant verticalAlignment = GWTConverter.asVerticalAlignmentConstant(binaryModel.getIntValue());
            // ServerToClientModel.WIDGET_ID
            final Widget w = asWidget(buffer.readBinaryModel().getIntValue(), uiBuilder);
            uiObject.setCellVerticalAlignment(w, verticalAlignment);
            return true;
        } else if (ServerToClientModel.CELL_WIDTH == model) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.WIDGET_ID
            uiObject.setCellWidth(asWidget(buffer.readBinaryModel().getIntValue(), uiBuilder), value);
            return true;
        } else if (ServerToClientModel.CELL_HEIGHT == model) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.WIDGET_ID
            uiObject.setCellHeight(asWidget(buffer.readBinaryModel().getIntValue(), uiBuilder), value);
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

}
