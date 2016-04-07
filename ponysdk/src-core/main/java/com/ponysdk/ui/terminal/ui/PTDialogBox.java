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

import com.google.gwt.user.client.ui.DialogBox;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTDialogBox extends PTDecoratedPopupPanel {

    @Override
    protected DialogBox createUIObject() {
        return new DialogBox(autoHide, false);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (Model.POPUP_CAPTION.equals(binaryModel.getModel())) {
            final DialogBox dialogBox = cast();
            dialogBox.setHTML(binaryModel.getStringValue());
        }
        return super.update(buffer, binaryModel);
    }

    @Override
    public DialogBox cast() {
        return (DialogBox) uiObject;
    }
}
