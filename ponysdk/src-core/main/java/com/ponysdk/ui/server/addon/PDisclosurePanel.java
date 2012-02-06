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

import java.util.Iterator;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PAddOn;
import com.ponysdk.ui.server.basic.PComposite;
import com.ponysdk.ui.server.basic.PImage;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.HasPWidgets;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.addon.disclosurepanel.PCDisclosurePanelAddon;
import com.ponysdk.ui.terminal.instruction.Add;

public class PDisclosurePanel extends PComposite implements HasPWidgets, PAddOn {

    private PWidget content;

    public PDisclosurePanel(String headerText, PImage openImage, PImage closeImage) {
        super();
        initWidget(new PVerticalPanel());
        final Property mainProperty = new Property(PropertyKey.TEXT, headerText);
        mainProperty.setProperty(PropertyKey.DISCLOSURE_PANEL_OPEN_IMG, openImage.getID());
        mainProperty.setProperty(PropertyKey.DISCLOSURE_PANEL_CLOSE_IMG, closeImage.getID());
        setMainProperty(mainProperty);
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.ADDON;
    }

    public void setContent(PWidget content) {

        final PWidget currentContent = getContent();

        // Remove existing content widget.
        if (currentContent != null) {
            setWidget(null);
        }

        // Add new content widget if != null.
        if (content != null) {
            setWidget(content);
        }
    }

    public PWidget getContent() {
        return content;
    }

    @Override
    public Iterator<PWidget> iterator() {
        return null;
    }

    @Override
    public void add(PWidget w) {
        if (this.getContent() == null) {
            setContent(w);
        } else {
            throw new IllegalStateException("A DisclosurePanel can only contain two Widgets.");
        }
    }

    @Override
    public void add(IsPWidget w) {
        add(w.asWidget());
    }

    @Override
    public void clear() {
        setContent(null);
    }

    @Override
    public boolean remove(PWidget w) {
        if (w == getContent()) {
            setContent(null);
            return true;
        }
        return false;
    }

    public void setWidget(PWidget w) {
        // Validate
        if (w == content) { return; }

        // Detach new child.
        if (w != null) {
            w.removeFromParent();
        }

        // Remove old child.
        if (content != null) {
            remove(content);
        }

        // Logical attach.
        content = w;

        if (w != null) {
            // Physical attach.
            final Add add = new Add(w.getID(), getID());
            getPonySession().stackInstruction(add);

            adopt(w);
        }
    }

    private final void adopt(PWidget child) {
        assert (child.getParent() == null);
        child.setParent(this);
    }

    @Override
    public String getSignature() {
        return PCDisclosurePanelAddon.SIGNATURE;
    }

}
