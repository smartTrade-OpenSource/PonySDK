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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.json.JsonObject;

import com.ponysdk.core.Parser;
import com.ponysdk.ui.server.basic.event.HasPAnimation;
import com.ponysdk.ui.server.basic.event.HasPWidgets;
import com.ponysdk.ui.server.basic.event.PCloseEvent;
import com.ponysdk.ui.server.basic.event.PCloseHandler;
import com.ponysdk.ui.server.basic.event.POpenEvent;
import com.ponysdk.ui.server.basic.event.POpenHandler;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.Model;

/**
 * A widget that consists of a header and a content panel that discloses the content when a user clicks on the
 * header.
 * <h3>CSS Style Rules</h3>
 * <dl class="css">
 * <dt>.gwt-DisclosurePanel
 * <dd>the panel's primary style
 * <dt>.gwt-DisclosurePanel-open
 * <dd>dependent style set when panel is open
 * <dt>.gwt-DisclosurePanel-closed
 * <dd>dependent style set when panel is closed
 * </dl>
 * <p>
 * The header and content sections can be easily selected using css with a child selector:<br/>
 * .gwt-DisclosurePanel-open .header { ... }
 * </p>
 */
public class PDisclosurePanel extends PWidget implements HasPWidgets, HasPAnimation {

    // TODO nciaravola must be moved in PTDisclosurePanel
    private static final String CLOSED = "images/disclosure_closed.png";
    private static final String OPENNED = "images/disclosure_openned.png";

    private boolean animationEnabled = false;
    private PWidget content;
    private boolean isOpen;

    private final List<PCloseHandler> closeHandlers = new ArrayList<>();
    private final List<POpenHandler> openHandlers = new ArrayList<>();

    private final String headerText;
    private final PImage openImage;
    private final PImage closeImage;

    public PDisclosurePanel(final String headerText) {
        this(headerText, new PImage(OPENNED, 0, 0, 14, 14), new PImage(CLOSED, 0, 0, 14, 14));
    }

    public PDisclosurePanel(final String headerText, final PImage openImage, final PImage closeImage) {
        this.headerText = headerText;
        this.openImage = openImage;
        this.closeImage = closeImage;

        init();
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        parser.parse(Model.TEXT, headerText);
        parser.parse(Model.DISCLOSURE_PANEL_OPEN_IMG, openImage.getID());
        parser.parse(Model.DISCLOSURE_PANEL_CLOSE_IMG, closeImage.getID());
    }

    @Override
    public void onClientData(final JsonObject jsonObject) {
        if (jsonObject.containsKey(Model.HANDLER_CLOSE_HANDLER)) {
            isOpen = false;
            for (final PCloseHandler closeHandler : closeHandlers) {
                closeHandler.onClose(new PCloseEvent(this));
            }
        } else if (jsonObject.containsKey(Model.HANDLER_OPEN_HANDLER)) {
            isOpen = true;
            for (final POpenHandler openHandler : openHandlers) {
                openHandler.onOpen(new POpenEvent(this));
            }
        } else {
            super.onClientData(jsonObject);
        }
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.DISCLOSURE_PANEL;
    }

    public void setContent(final PWidget w) {
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
            saveAdd(w.getID(), getID());
            adopt(w);
        }
    }

    public PWidget getContent() {
        return content;
    }

    @Override
    public Iterator<PWidget> iterator() {
        return Arrays.asList(content).iterator();
    }

    @Override
    public void add(final PWidget w) {
        if (this.getContent() == null) {
            setContent(w);
        } else {
            throw new IllegalStateException("A DisclosurePanel can only contain two Widgets.");
        }
    }

    @Override
    public void add(final IsPWidget w) {
        add(w.asWidget());
    }

    @Override
    public void clear() {
        setContent(null);
    }

    @Override
    public boolean remove(final PWidget w) {
        if (w == getContent()) {
            setContent(null);
            return true;
        }
        return false;
    }

    private final void adopt(final PWidget child) {
        assert(child.getParent() == null);
        child.setParent(this);
    }

    public void setOpen(final boolean isOpen) {
        if (this.isOpen != isOpen) {
            this.isOpen = isOpen;
            saveUpdate(Model.OPEN, isOpen);
        }
    }

    public void addCloseHandler(final PCloseHandler handler) {
        closeHandlers.add(handler);
    }

    public void addOpenHandler(final POpenHandler handler) {
        openHandlers.add(handler);
    }

    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    @Override
    public void setAnimationEnabled(final boolean animationEnabled) {
        this.animationEnabled = animationEnabled;
        saveUpdate(Model.ANIMATION, animationEnabled);
    }
}
