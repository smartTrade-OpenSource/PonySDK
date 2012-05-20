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

package com.ponysdk.ui.terminal.addon.floatablepanel;

import com.ponysdk.ui.terminal.PTAddon;
import com.ponysdk.ui.terminal.PonyAddOn;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.ui.PTComposite;
import com.ponysdk.ui.terminal.ui.PTScrollPanel;

@PonyAddOn
public class PCFloatablePanelAddon extends PTComposite implements PTAddon {

    public static final String SIGNATURE = "com.ponysdk.ui.terminal.addon.floatablepanel.PCFloatablePanelAddon";

    private PCFloatablePanel floatablePanel;

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        super.create(create, uiService);

        this.floatablePanel = new PCFloatablePanel();

        initWidget(floatablePanel);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.REFERENCE_SCROLL_PANEL)) {
            final PTScrollPanel scrollPanel = (PTScrollPanel) uiService.getPTObject(update.getLong(PROPERTY.REFERENCE_SCROLL_PANEL));
            floatablePanel.setScrollPanel(scrollPanel.cast());
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public String getSignature() {
        return SIGNATURE;
    }

}
