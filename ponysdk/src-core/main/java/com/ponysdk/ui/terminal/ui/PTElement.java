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

import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTElement extends PTComplexPanel<MyWidget> {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new MyWidget(create.getString(Model.TAG)));
    }

    @Override
    public void add(final PTInstruction add, final UIService uiService) {
        if (add.containsKey(Model.INDEX)) {
            final int beforeIndex = add.getInt(Model.INDEX);
            uiObject.insert(asWidget(add.getObjectID(), uiService), uiObject.getElement(), beforeIndex, true);
        } else {
            super.add(add, uiService);
        }
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(Model.INNER_HTML)) {
            uiObject.getElement().setInnerHTML(update.getString(Model.INNER_HTML));
        } else if (update.containsKey(Model.INNER_TEXT)) {
            uiObject.getElement().setInnerText(update.getString(Model.INNER_TEXT));
        } else if (update.containsKey(Model.CLEAR_INNER_TEXT)) {// ? if null setInnerText of null do
            uiObject.getElement().setInnerText(null);
        } else {
            super.update(update, uiService);
        }
    }

}
