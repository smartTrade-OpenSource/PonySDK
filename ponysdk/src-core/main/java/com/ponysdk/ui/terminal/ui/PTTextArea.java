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

import com.google.gwt.user.client.ui.TextArea;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ServerToClientModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTTextArea extends PTTextBoxBase<TextArea> {

    @Override
    protected TextArea createUIObject() {
        return new TextArea();
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.VISIBLE_LINES.equals(binaryModel.getModel())) {
            uiObject.setVisibleLines(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.CHARACTER_WIDTH.equals(binaryModel.getModel())) {
            uiObject.setCharacterWidth(binaryModel.getIntValue());
            return true;
        }
        return super.update(buffer, binaryModel);
    }

}
