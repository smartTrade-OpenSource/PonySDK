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

import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTVerticalPanel extends PTCellPanel {

    @Override
    public void create(final Create create, final UIService uiService) {
        init(create, uiService, new com.google.gwt.user.client.ui.VerticalPanel());
    }

    @Override
    public void add(final Add add, final UIService uiService) {
        final Widget w = asWidget(add.getObjectID(), uiService);
        final int beforeIndex = add.getMainProperty().getIntValue();
        cast().insert(w, beforeIndex);
    }

    @Override
    public void update(final Update update, final UIService uiService) {

        final com.google.gwt.user.client.ui.VerticalPanel verticalPanel = cast();
        final Property property = update.getMainProperty();
        final PropertyKey propertyKey = property.getKey();
        if (PropertyKey.BORDER_WIDTH.equals(propertyKey)) {
            verticalPanel.setBorderWidth(property.getIntValue());
        } else if (PropertyKey.SPACING.equals(propertyKey)) {
            verticalPanel.setSpacing(property.getIntValue());
        } else if (PropertyKey.HORIZONTAL_ALIGNMENT.equals(propertyKey)) {
            final PHorizontalAlignment horizontalAlignment = PHorizontalAlignment.values()[property.getIntValue()];
            switch (horizontalAlignment) {
                case ALIGN_LEFT:
                    verticalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
                    break;
                case ALIGN_CENTER:
                    verticalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
                    break;
                case ALIGN_RIGHT:
                    verticalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
                    break;
                default:
                    break;
            }
        } else if (PropertyKey.VERTICAL_ALIGNMENT.equals(propertyKey)) {
            final PVerticalAlignment verticalAlignment = PVerticalAlignment.values()[property.getIntValue()];
            switch (verticalAlignment) {
                case ALIGN_TOP:
                    verticalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
                    break;
                case ALIGN_MIDDLE:
                    verticalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
                    break;
                case ALIGN_BOTTOM:
                    verticalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
                    break;
                default:
                    break;
            }
        } else {
            super.update(update, uiService);
        }

    }

    @Override
    public com.google.gwt.user.client.ui.VerticalPanel cast() {
        return (com.google.gwt.user.client.ui.VerticalPanel) uiObject;
    }
}
