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

import com.google.gwt.user.client.ui.DisclosurePanel;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.Create;

public class PTDisclosurePanel extends PTWidget {

    @Override
    public void create(final Create create, final UIService uiService) {
        final Property mainProperty = create.getMainProperty();

        final Long openImg = mainProperty.getLongProperty(PropertyKey.DISCLOSURE_PANEL_OPEN_IMG);
        final Long closeImg = mainProperty.getLongProperty(PropertyKey.DISCLOSURE_PANEL_CLOSE_IMG);
        final String headerText = mainProperty.getValue();

        PTImage open = (PTImage) uiService.getPTObject(openImg);
        PTImage close = (PTImage) uiService.getPTObject(closeImg);

        PImageResource openImageResource = new PImageResource(open.cast().getUrl(), 16, 16);
        PImageResource closeImageResource = new PImageResource(close.cast().getUrl(), 16, 16);

        init(create, uiService, new DisclosurePanel(openImageResource, closeImageResource, headerText));
    }

    @Override
    public void add(final Add add, final UIService uiService) {
        final com.google.gwt.user.client.ui.Widget w = asWidget(add.getObjectID(), uiService);
        cast().setContent(w);
    }

    @Override
    public DisclosurePanel cast() {
        return (DisclosurePanel) uiObject;
    }
}
