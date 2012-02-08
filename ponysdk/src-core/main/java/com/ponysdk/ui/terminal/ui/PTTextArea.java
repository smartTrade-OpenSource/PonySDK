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

import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTTextArea extends PTTextBoxBase {

    @Override
    public void create(final Create create, final UIService uiService) {
        init(create, uiService, new com.google.gwt.user.client.ui.TextArea());
    }

    @Override
    public void update(final Update update, final UIService uiService) {

        final Property mainProperty = update.getMainProperty();
        final com.google.gwt.user.client.ui.TextArea textArea = cast();

        for (final Property property : mainProperty.getChildProperties().values()) {
            final PropertyKey propertyKey = property.getKey();
            if (PropertyKey.VISIBLE_LINES.equals(propertyKey)) {
                textArea.setVisibleLines(property.getIntValue());
            } else if (PropertyKey.CHARACTER_WIDTH.equals(propertyKey)) {
                textArea.setCharacterWidth(property.getIntValue());
            }
        }

        super.update(update, uiService);
    }

    @Override
    public com.google.gwt.user.client.ui.TextArea cast() {
        return (com.google.gwt.user.client.ui.TextArea) uiObject;
    }
}
