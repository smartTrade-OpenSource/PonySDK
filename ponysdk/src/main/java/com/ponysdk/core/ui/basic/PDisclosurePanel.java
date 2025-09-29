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

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.*;
import com.ponysdk.core.writer.ModelWriter;

import jakarta.json.JsonObject;
import java.util.*;

/**
 * A widget that consists of a header and a content panel that discloses the content when a user clicks on the header.
 * <h2>CSS Style Rules</h2>
 * <dl class="css">
 * <dt>.gwt-DisclosurePanel
 * <dd>the panel's primary style
 * <dt>.gwt-DisclosurePanel-open
 * <dd>dependent style set when panel is open
 * <dt>.gwt-DisclosurePanel-closed
 * <dd>dependent style set when panel is closed
 * </dl>
 * <p>
 * The header and content sections can be easily selected using css with a child selector:
 * .gwt-DisclosurePanel-open .header { ... }
 * </p>
 */
public class PDisclosurePanel extends PWidget implements HasPWidgets, PAcceptsOneWidget {

    private final List<PCloseHandler> closeHandlers = new ArrayList<>();
    private final List<POpenHandler> openHandlers = new ArrayList<>();
    private final String headerText;
    private boolean animationEnabled = false;
    private PWidget content;
    private boolean isOpen;

    protected PDisclosurePanel(final String headerText) {
        this.headerText = headerText;
    }

    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        super.enrichForCreation(writer);
        writer.write(ServerToClientModel.TEXT, headerText);

        // TODO add ImageResources parametters ..
        // writer.writeModel(ServerToClientModel.DISCLOSURE_PANEL_OPEN_IMG, openImage.getID());
        // writer.writeModel(ServerToClientModel.DISCLOSURE_PANEL_CLOSE_IMG, closeImage.getID());
    }

    @Override
    public void onClientData(final JsonObject jsonObject) {
        if (!isVisible()) return;
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

    @Override
    public void setWidget(final IsPWidget w) {
        setContent(w.asWidget());
    }

    public void setContent(final PWidget w) {
        // Validate
        if (w == content) return;

        // Detach new child.
        w.removeFromParent();

        // Remove old child.
        if (content != null) content.removeFromParent();

        // Logical attach.
        adopt(w);
        content = w;

        // Physical attach.
        w.attach(window, frame);
        w.saveAdd(w.getID(), getID());
    }

    @Override
    public Iterator<PWidget> iterator() {
        return Collections.singletonList(content).iterator();
    }

    @Override
    public void add(final PWidget w) {
        if (content == null) setContent(w);
        else throw new IllegalStateException("A DisclosurePanel can only contain two Widgets.");
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
        if (w == content) {
            content.removeFromParent();
            content = null;
            return true;
        } else {
            return false;
        }
    }

    private void adopt(final PWidget child) {
        if (child.getParent() == null) child.setParent(this);
        else throw new IllegalStateException("Can't adopt an already widget attached to a parent");
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
        if (Objects.equals(this.isOpen, isOpen)) return;
        this.isOpen = isOpen;
        if (isOpen) saveUpdate(ServerToClientModel.OPEN, true);
        else saveUpdate(ServerToClientModel.CLOSE, false);
    }

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    public void setAnimationEnabled(final boolean animationEnabled) {
        if (Objects.equals(this.animationEnabled, animationEnabled)) return;
        this.animationEnabled = animationEnabled;
        saveUpdate(ServerToClientModel.ANIMATION, animationEnabled);
    }

    @Override
    public String dumpDOM() {
        return "<div pid=\"" + ID + "\"><div>" + headerText + "</div>" + content.dumpDOM() + "</div>";
    }
}
