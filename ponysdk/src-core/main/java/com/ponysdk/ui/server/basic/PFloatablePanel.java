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

package com.ponysdk.ui.server.basic;

import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.addon.floatablepanel.PCFloatablePanelAddon;

public class PFloatablePanel extends PSimplePanel implements PAddOn {

    private PScrollPanel linkedScrollPanel;

    public PFloatablePanel() {}

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ADDON;
    }

    public void setLinkedScrollPanel(final PScrollPanel linkedScrollPanel) {
        this.linkedScrollPanel = linkedScrollPanel;
        final Update update = new Update(getID());
        update.put(PROPERTY.REFERENCE_SCROLL_PANEL, linkedScrollPanel.getID());
        getPonySession().stackInstruction(update);
    }

    public void correct() {
        if (linkedScrollPanel == null) return;
        final Update update = new Update(getID());
        update.put(PROPERTY.CORRECT_DIMENSION, linkedScrollPanel.getID());
        getPonySession().stackInstruction(update);
    }

    @Override
    public String getSignature() {
        return PCFloatablePanelAddon.SIGNATURE;
    }

}
