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

import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.ponysdk.core.model.PHorizontalAlignment;
import com.ponysdk.core.model.PVerticalAlignment;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTHorizontalPanel extends PTCellPanel<HorizontalPanel> {

    @Override
    protected HorizontalPanel createUIObject() {
        return new HorizontalPanel();
    }

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        final BinaryModel model = buffer.readBinaryModel();
        if (ServerToClientModel.INDEX.equals(model.getModel())) {
            uiObject.insert(asWidget(ptObject), model.getIntValue());
        } else {
            buffer.rewind(model);
            super.add(buffer, ptObject);
        }
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.BORDER_WIDTH.ordinal() == modelOrdinal) {
            uiObject.setBorderWidth(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.SPACING.ordinal() == modelOrdinal) {
            uiObject.setSpacing(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.HORIZONTAL_ALIGNMENT.ordinal() == modelOrdinal) {
            final PHorizontalAlignment horizontalAlignment = PHorizontalAlignment.fromRawValue(binaryModel.getByteValue());
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
        } else if (ServerToClientModel.VERTICAL_ALIGNMENT.ordinal() == modelOrdinal) {
            final PVerticalAlignment verticalAlignment = PVerticalAlignment.fromRawValue(binaryModel.getByteValue());
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
        } else {
            return super.update(buffer, binaryModel);
        }
    }

}
