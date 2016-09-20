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

package com.ponysdk.core.ui.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.Parser;
import com.ponysdk.core.ui.basic.event.HasPAnimation;
import com.ponysdk.core.ui.basic.event.HasPWidgets;
import com.ponysdk.core.ui.basic.event.PCloseEvent;
import com.ponysdk.core.ui.basic.event.PCloseHandler;
import com.ponysdk.core.ui.basic.event.POpenEvent;
import com.ponysdk.core.ui.basic.event.POpenHandler;

/**
 * A widget that consists of a header and a content panel that discloses the
 * content when a user clicks on the header.
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
 * The header and content sections can be easily selected using css with a child
 * selector:<br/>
 * .gwt-DisclosurePanel-open .header { ... }
 * </p>
 */
public class PDisclosurePanel extends PWidget implements HasPWidgets, HasPAnimation {

    private final List<PCloseHandler> closeHandlers = new ArrayList<>();
    private final List<POpenHandler> openHandlers = new ArrayList<>();
    private final String headerText;
    private boolean animationEnabled = false;
    private PWidget content;
    private boolean isOpen;

    public PDisclosurePanel(final String headerText) {
        this.headerText = headerText;
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        super.enrichOnInit(parser);
        parser.parse(ServerToClientModel.TEXT, headerText);

        // TODO add ImageResources parametters ..
        // parser.parse(ServerToClientModel.DISCLOSURE_PANEL_OPEN_IMG,
        // openImage.getID());
        // parser.parse(ServerToClientModel.DISCLOSURE_PANEL_CLOSE_IMG,
        // closeImage.getID());
    }

    @Override
    public void onClientData(final JsonObject jsonObject) {
        if (jsonObject.containsKey(ClientToServerModel.HANDLER_CLOSE.toStringValue())) {
            isOpen = false;
            for (final PCloseHandler closeHandler : closeHandlers) {
                closeHandler.onClose(new PCloseEvent(this));
            }
        } else if (jsonObject.containsKey(ClientToServerModel.HANDLER_OPEN.toStringValue())) {
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

    public PWidget getContent() {
        return content;
    }

    public void setContent(final PWidget w) {
        // Validate
        if (w == content)
            return;

        // Detach new child.
        if (w != null)
            w.removeFromParent();

        // Remove old child.
        if (content != null)
            remove(content);

        // Logical attach.
        content = w;

        if (w != null) {
            // Physical attach.
            w.saveAdd(w.getID(), getID());
            w.attach(windowID);
            adopt(w);
        }
    }

    @Override
    public Iterator<PWidget> iterator() {
        return Collections.singletonList(content).iterator();
    }

    @Override
    public void add(final PWidget w) {
        if (this.getContent() == null)
            setContent(w);
        else
            throw new IllegalStateException("A DisclosurePanel can only contain two Widgets.");
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
        } else {
            return false;
        }
    }

    private void adopt(final PWidget child) {
        assert child.getParent() == null;
        child.setParent(this);
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

    public void setOpen(final boolean isOpen) {
        if (this.isOpen != isOpen) {
            this.isOpen = isOpen;
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.OPEN_CLOSE, isOpen));
        }
    }

    @Override
    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    @Override
    public void setAnimationEnabled(final boolean animationEnabled) {
        this.animationEnabled = animationEnabled;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.ANIMATION, animationEnabled));
    }
}
