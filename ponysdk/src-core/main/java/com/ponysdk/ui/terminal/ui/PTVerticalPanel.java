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

import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTVerticalPanel extends PTCellPanel<VerticalPanel> {

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        this.uiObject = new VerticalPanel();
        this.objectID = objectId;
        uiService.registerUIObject(this.objectID, uiObject);
    }

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        uiObject.insert(asWidget(ptObject), buffer.getInt(Model.INDEX));
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (Model.BORDER_WIDTH.equals(binaryModel.getModel())) {
            uiObject.setBorderWidth(binaryModel.getIntValue());
            return true;
        }
        if (Model.SPACING.equals(binaryModel.getModel())) {
            uiObject.setSpacing(binaryModel.getIntValue());
            return true;
        }
        if (Model.HORIZONTAL_ALIGNMENT.equals(binaryModel.getModel())) {
            final PHorizontalAlignment horizontalAlignment = PHorizontalAlignment.values()[binaryModel.getIntValue()];
            switch (horizontalAlignment) {
                case ALIGN_LEFT:
                    uiObject.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
                    break;
                case ALIGN_CENTER:
                    uiObject.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
                    break;
                case ALIGN_RIGHT:
                    uiObject.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
                    break;
                default:
                    break;
            }
            return true;
        }
        if (Model.VERTICAL_ALIGNMENT.equals(binaryModel.getModel())) {
            final PVerticalAlignment verticalAlignment = PVerticalAlignment.values()[binaryModel.getIntValue()];
            switch (verticalAlignment) {
                case ALIGN_TOP:
                    uiObject.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
                    break;
                case ALIGN_MIDDLE:
                    uiObject.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
                    break;
                case ALIGN_BOTTOM:
                    uiObject.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
                    break;
                default:
                    break;
            }
            return true;
        }

        return super.update(buffer, binaryModel);
    }
}
