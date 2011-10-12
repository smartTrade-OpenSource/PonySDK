/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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
package com.ponysdk.ui.terminal.addon.disclosurepanel;

import com.google.gwt.user.client.ui.Image;
import com.ponysdk.ui.terminal.Addon;
import com.ponysdk.ui.terminal.PonyAddOn;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.Remove;
import com.ponysdk.ui.terminal.instruction.Update;
import com.ponysdk.ui.terminal.ui.PTImage;
import com.ponysdk.ui.terminal.ui.PTWidget;

@PonyAddOn
public final class PCDisclosurePanelAddon extends PTWidget implements Addon {

    public static final String SIGNATURE = "com.ponysdk.ui.terminal.addon.disclosurepanel.PCDisclosurePanelAddon";

    private PCDisclosurePanel disclosurePanel;

    @Override
    public String getSignature() {
        return SIGNATURE;
    }

    @Override
    public void create(Create create, UIService uiService) {

        final Property mainProperty = create.getMainProperty();

        final PTImage openImg = (PTImage) uiService.getUIObject(mainProperty.getLongProperty(PropertyKey.DISCLOSURE_PANEL_OPEN_IMG));
        final PTImage closeImg = (PTImage) uiService.getUIObject(mainProperty.getLongProperty(PropertyKey.DISCLOSURE_PANEL_CLOSE_IMG));

        final String headerText = mainProperty.getValue();
        final Image openImage = openImg.cast();
        final Image closeImage = closeImg.cast();

        init(disclosurePanel = new PCDisclosurePanel(openImage, closeImage, headerText));
    }

    @Override
    public void add(Add add, UIService uiService) {
        final PTWidget w = (PTWidget) uiService.getUIObject(add.getObjectID());
        disclosurePanel.setContent(w.cast());
    }

    @Override
    public void update(Update update, UIService uiService) {
        // nothing
    }

    @Override
    public void remove(Remove remove, UIService uiService) {
        // nothing
    }

    @Override
    public void addHandler(AddHandler addHandler, UIService uiService) {
        // nothing
    }

    @Override
    public PTWidget asPTWidget() {
        return this;
    }
}
