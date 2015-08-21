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

import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTScrollPanel extends PTSimplePanel {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new ScrollPanel());
    }

    @Override
    public void add(final PTInstruction add, final UIService uiService) {
        final Widget w = asWidget(add.getObjectID(), uiService);
        cast().setWidget(w);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(Model.HORIZONTAL_SCROLL_POSITION)) {
            cast().setHorizontalScrollPosition(update.getInt(Model.HORIZONTAL_SCROLL_POSITION));
        } else if (update.containsKey(Model.SCROLL_TO)) {
            final long scrollTo = update.getLong(Model.SCROLL_TO);
            if (scrollTo == 0) cast().scrollToBottom();
            else if (scrollTo == 1) cast().scrollToLeft();
            else if (scrollTo == 2) cast().scrollToRight();
            else if (scrollTo == 3) cast().scrollToTop();
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public ScrollPanel cast() {
        return (ScrollPanel) uiObject;
    }
}
