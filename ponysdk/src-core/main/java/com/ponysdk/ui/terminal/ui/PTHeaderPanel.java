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

import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.Dictionnary;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTHeaderPanel extends PTPanel<HeaderPanel> {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new HeaderPanel());
    }

    @Override
    public void add(final PTInstruction add, final UIService uiService) {
        final Widget w = asWidget(add.getObjectID(), uiService);
        if (add.containsKey(Dictionnary.PROPERTY.INDEX)) {
            final int index = add.getInt(Dictionnary.PROPERTY.INDEX);
            if (index == 0) cast().setHeaderWidget(w);
            else if (index == 2) cast().setFooterWidget(w);
            else cast().setContentWidget(w);
        } else {
            uiObject.add(asWidget(add.getObjectID(), uiService));
        }
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.RESIZE)) {
            uiObject.onResize();
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public void remove(final PTInstruction remove, final UIService uiService) {
        uiObject.remove(asWidget(remove.getObjectID(), uiService));
    }

}
