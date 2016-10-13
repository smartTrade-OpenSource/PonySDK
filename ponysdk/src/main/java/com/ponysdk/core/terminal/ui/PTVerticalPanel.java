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

import com.google.gwt.user.client.ui.VerticalPanel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import com.ponysdk.core.terminal.ui.alignment.AlignmentConverter;

public class PTVerticalPanel extends PTCellPanel<VerticalPanel> {

    @Override
    protected VerticalPanel createUIObject() {
        return new VerticalPanel();
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
        if (ServerToClientModel.BORDER_WIDTH.equals(binaryModel.getModel())) {
            uiObject.setBorderWidth(binaryModel.getIntValue());
            return true;
        }
        if (ServerToClientModel.SPACING.equals(binaryModel.getModel())) {
            uiObject.setSpacing(binaryModel.getIntValue());
            return true;
        }
        if (ServerToClientModel.HORIZONTAL_ALIGNMENT.equals(binaryModel.getModel())) {
            uiObject.setHorizontalAlignment(AlignmentConverter.asHorizontalAlignmentConstant(binaryModel));
            return true;
        }
        if (ServerToClientModel.VERTICAL_ALIGNMENT.equals(binaryModel.getModel())) {
            uiObject.setVerticalAlignment(AlignmentConverter.asVerticalAlignmentConstant(binaryModel));
            return true;
        }

        return super.update(buffer, binaryModel);
    }
}
