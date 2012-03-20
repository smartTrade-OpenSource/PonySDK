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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTElement extends PTComplexPanel {

    @Override
    public void create(final Create create, final UIService uiService) {
        init(create, uiService, new MyWidget(create.getMainProperty().getValue()));
    }

    @Override
    public void update(final Update update, final UIService uiService) {

        final Property property = update.getMainProperty();
        final PropertyKey propertyKey = property.getKey();

        final MyWidget cast = cast();

        switch (propertyKey) {
            case INNER_HTML:
                cast.getElement().setInnerHTML(property.getValue());
                break;
            case INNER_TEXT:
                cast.getElement().setInnerText(property.getValue());
                break;
            default:
                super.update(update, uiService);
        }
    }

    @Override
    public MyWidget cast() {
        return (MyWidget) uiObject;
    }

    private class MyWidget extends ComplexPanel {

        public MyWidget(final String tagName) {
            setElement(DOM.createElement(tagName));
        }

        @Override
        public void add(final Widget w) {
            super.add(w, getElement());
        }

    }

}
