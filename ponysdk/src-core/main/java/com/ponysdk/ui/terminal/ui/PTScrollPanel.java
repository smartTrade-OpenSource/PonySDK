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

import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTScrollPanel extends PTSimplePanel {

    @Override
    public void create(final Create create, final UIService uiService) {
        init(create, uiService, new PCScrollPanel());
    }

    @Override
    public void add(final Add add, final UIService uiService) {
        final Widget w = asWidget(add.getObjectID(), uiService);
        cast().setWidget(w);
    }

    @Override
    public void update(final Update update, final UIService uiService) {

        final Property mainProperty = update.getMainProperty();
        final com.google.gwt.user.client.ui.ScrollPanel scrollPanel = cast();

        for (final Property property : mainProperty.getChildProperties().values()) {
            final PropertyKey propertyKey = property.getKey();
            if (PropertyKey.HORIZONTAL_SCROLL_POSITION.equals(propertyKey)) {
                scrollPanel.setHorizontalScrollPosition(property.getIntValue());
            }
        }

        super.update(update, uiService);
    }

    @Override
    public PCScrollPanel cast() {
        return (PCScrollPanel) uiObject;
    }
}
