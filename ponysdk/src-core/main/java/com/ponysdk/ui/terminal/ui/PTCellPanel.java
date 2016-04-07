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

import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTCellPanel<W extends CellPanel> extends PTComplexPanel<W> {

    private UIService uiService;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        super.create(buffer, objectId, uiService);
        this.uiService = uiService;
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (Model.CELL_HORIZONTAL_ALIGNMENT.equals(binaryModel.getModel())) {
            final PHorizontalAlignment horizontalAlignment = PHorizontalAlignment.values()[binaryModel.getByteValue()];
            final Widget w = asWidget(buffer.getBinaryModel().getIntValue(), uiService);
            switch (horizontalAlignment) {
                case ALIGN_LEFT:
                    uiObject.setCellHorizontalAlignment(w, HasHorizontalAlignment.ALIGN_LEFT);
                    break;
                case ALIGN_CENTER:
                    uiObject.setCellHorizontalAlignment(w, HasHorizontalAlignment.ALIGN_CENTER);
                    break;
                case ALIGN_RIGHT:
                    uiObject.setCellHorizontalAlignment(w, HasHorizontalAlignment.ALIGN_RIGHT);
                    break;
                default:
                    break;
            }
            return true;
        }
        if (Model.CELL_VERTICAL_ALIGNMENT.equals(binaryModel.getModel())) {
            final PVerticalAlignment verticalAlignment = PVerticalAlignment.values()[binaryModel.getByteValue()];
            final Widget w = asWidget(buffer.getBinaryModel().getIntValue(), uiService);
            switch (verticalAlignment) {
                case ALIGN_TOP:
                    uiObject.setCellVerticalAlignment(w, HasVerticalAlignment.ALIGN_TOP);
                    break;
                case ALIGN_MIDDLE:
                    uiObject.setCellVerticalAlignment(w, HasVerticalAlignment.ALIGN_MIDDLE);
                    break;
                case ALIGN_BOTTOM:
                    uiObject.setCellVerticalAlignment(w, HasVerticalAlignment.ALIGN_BOTTOM);
                    break;
                default:
                    break;
            }
            return true;
        }
        if (Model.CELL_WIDTH.equals(binaryModel.getModel())) {
            uiObject.setCellWidth(asWidget(buffer.getBinaryModel().getIntValue(), uiService),
                    binaryModel.getStringValue());
            return true;
        }
        if (Model.CELL_HEIGHT.equals(binaryModel.getModel())) {
            uiObject.setCellHeight(asWidget(buffer.getBinaryModel().getIntValue(), uiService),
                    binaryModel.getStringValue());
            return true;
        }

        return super.update(buffer, binaryModel);
    }

}
