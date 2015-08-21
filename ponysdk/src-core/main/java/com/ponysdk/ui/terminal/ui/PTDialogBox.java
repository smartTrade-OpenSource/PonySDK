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
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTDialogBox extends PTDecoratedPopupPanel {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        boolean autoHide = false;
        boolean modal = false;

        if (create.containsKey(Model.POPUP_AUTO_HIDE.getKey())) {
            autoHide = create.getBoolean(PROPERTY.POPUP_AUTO_HIDE);
        }
        if (create.containsKey(PROPERTY.POPUP_MODAL)) {
            modal = create.getBoolean(PROPERTY.POPUP_MODAL);
        }

        init(create, uiService, new DialogBox(autoHide, modal));
        addCloseHandler(create, uiService);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        final DialogBox dialogBox = cast();

        if (update.containsKey(PROPERTY.POPUP_CAPTION)) {
            dialogBox.setHTML(update.getString(PROPERTY.POPUP_CAPTION));
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public DialogBox cast() {
        return (DialogBox) uiObject;
    }
}
