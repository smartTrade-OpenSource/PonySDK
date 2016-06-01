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

import com.google.gwt.user.client.ui.TextBox;
import com.ponysdk.ui.model.ServerToClientModel;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;
import com.ponysdk.ui.terminal.ui.widget.mask.TextBoxMaskedDecorator;

public class PTTextBox extends PTTextBoxBase<TextBox> {

    private TextBoxMaskedDecorator maskDecorator;

    @Override
    protected TextBox createUIObject() {
        return new TextBox();
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.TEXT.equals(binaryModel.getModel())) {
            uiObject.setText(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.VALUE.equals(binaryModel.getModel())) {
            uiObject.setValue(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.VISIBLE_LENGTH.equals(binaryModel.getModel())) {
            uiObject.setVisibleLength(binaryModel.getIntValue());
            return true;
        }
        if (ServerToClientModel.MAX_LENGTH.equals(binaryModel.getModel())) {
            uiObject.setMaxLength(binaryModel.getIntValue());
            return true;
        }
        if (ServerToClientModel.MASK.equals(binaryModel.getModel())) {
            final String mask = binaryModel.getStringValue();
            // ServerToClientModel.VISIBILITY
            final boolean showMask = binaryModel.getBooleanValue();
            // ServerToClientModel.REPLACEMENT_STRING
            final String replace = binaryModel.getStringValue();
            if (maskDecorator == null)
                maskDecorator = new TextBoxMaskedDecorator(cast());
            maskDecorator.setMask(mask, showMask, replace.charAt(0));
            return true;
        }
        return super.update(buffer, binaryModel);
    }

}
