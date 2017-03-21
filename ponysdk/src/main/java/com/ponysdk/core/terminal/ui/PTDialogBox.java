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

import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.user.client.ui.DialogBox;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTDialogBox extends PTDecoratedPopupPanel {

    public PTDialogBox() {
        this.draggable = true;
    }

    @Override
    protected DialogBox createUIObject() {
        return new MyDialogBox(autoHide);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.POPUP_CAPTION.ordinal() == modelOrdinal) {
            final DialogBox dialogBox = cast();
            dialogBox.setHTML(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.POPUP_DRAGGABLE.ordinal() == modelOrdinal) {
            draggable = binaryModel.getBooleanValue();
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    public DialogBox cast() {
        return (DialogBox) uiObject;
    }

    public final class MyDialogBox extends DialogBox {

        public MyDialogBox(final boolean autoHide) {
            super(autoHide, false);
        }

        @Override
        protected void beginDragging(final MouseDownEvent event) {
            if (draggable) super.beginDragging(event);
            else event.preventDefault();
        }

    }

}
