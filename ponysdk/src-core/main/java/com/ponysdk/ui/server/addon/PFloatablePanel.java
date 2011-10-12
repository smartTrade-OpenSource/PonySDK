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
package com.ponysdk.ui.server.addon;

import com.ponysdk.ui.server.basic.PAddOn;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.addon.floatablepanel.PCFloatablePanelAddon;
import com.ponysdk.ui.terminal.instruction.Update;

public class PFloatablePanel extends PSimplePanel implements PAddOn {

    private PScrollPanel linkedScrollPanel;

    public PFloatablePanel() {
    }

    public PFloatablePanel(PScrollPanel linkedScrollPanel) {
        setLinkedScrollPanel(linkedScrollPanel);
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.ADDON;
    }

    public void setLinkedScrollPanel(PScrollPanel linkedScrollPanel) {
        this.linkedScrollPanel = linkedScrollPanel;
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.REFERENCE_SCROLL_PANEL, linkedScrollPanel.getID());
        getPonySession().stackInstruction(update);
    }

    public void correct() {
        if (linkedScrollPanel == null)
            return;
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.CORRECT_DIMENSION, linkedScrollPanel.getID());
        getPonySession().stackInstruction(update);
    }

    @Override
    public String getSignature() {
        return PCFloatablePanelAddon.SIGNATURE;
    }

}
