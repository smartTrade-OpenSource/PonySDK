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

package com.ponysdk.core.ui.dropdown;

import java.util.HashSet;
import java.util.Set;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PPanel;
import com.ponysdk.core.ui.basic.event.PDomEvent.Type;
import com.ponysdk.core.ui.eventbus.EventHandler;
import com.ponysdk.core.ui.model.PEventType;

public class DropDownContainerAddon extends PAddOnComposite<PPanel> {

    private static final String PROPERTY_VISIBILITY = "visibility";
    private static final String PROPERTY_HIDDEN = "hidden";
    private static final String PROPERTY_POSITION = "position";
    private static final String PROPERTY_ABSOLUTE = "absolute";
    private static final String PROPERTY_TOP = "top";
    private static final String PROPERTY_LEFT = "left";
    private static final String PROPERTY_RIGHT = "right";
    private static final String PROPERTY_0 = "0";

    private static final String PARENT_ID = "parentId";
    private static final String STICK_LEFT = "stickLeft";
    private static final String UPDATE_POSITION = "updatePosition";
    private static final String SET_VISIBLE = "setVisible";
    private static final String DISABLE_SPACE_WHEN_OPENED = "disableSpaceWhenOpened";
    private static final String WINDOW_EVENT = "windowEvent";
    private static final String RESIZE = "resize";
    private static final String SCROLL = "scroll";
    private static final String CLICK = "click";

    private boolean visible;

    private final Set<DropDownContainerAddonListener> listeners;

    public DropDownContainerAddon(final String parentId) {
        this(parentId, true);
    }

    public DropDownContainerAddon(final String parentId, final boolean stickLeft) {
        super(Element.newPFlowPanel(), createJsonObject(parentId, stickLeft));
        widget.setStyleProperty(PROPERTY_POSITION, PROPERTY_ABSOLUTE);
        widget.setStyleProperty(PROPERTY_TOP, PROPERTY_0);
        if (stickLeft) {
            widget.setStyleProperty(PROPERTY_LEFT, PROPERTY_0);
        } else {
            widget.setStyleProperty(PROPERTY_RIGHT, PROPERTY_0);
        }
        this.listeners = new HashSet<>();
        setTerminalHandler(event -> {
            final JsonObject jsonObject = event.getData();
            if (jsonObject.containsKey(WINDOW_EVENT)) {
                final String windowEvent = jsonObject.getString(WINDOW_EVENT);
                if (RESIZE.equals(windowEvent) || SCROLL.equals(windowEvent) || CLICK.equals(windowEvent)) {
                    visible = false;
                    widget.setStyleProperty(PROPERTY_VISIBILITY, PROPERTY_HIDDEN);
                    listeners.forEach(DropDownContainerAddonListener::onHide);
                }
            }
        });
        hide();
    }

    public void add(final IsPWidget content) {
        widget.add(content);
    }

    public void addStyleName(final String styleName) {
        widget.addStyleName(styleName);
    }

    public void removeStyleName(final String styleName) {
        widget.removeStyleName(styleName);
    }

    public void setStyleProperty(final String name, final String value) {
        widget.setStyleProperty(name, value);
    }

    public void show() {
        visible = true;
        updatePosition();
        widget.setStyleProperty(PROPERTY_VISIBILITY, null);
        callTerminalMethod(SET_VISIBLE, true);

    }

    public void hide() {
        visible = false;
        widget.setStyleProperty(PROPERTY_VISIBILITY, PROPERTY_HIDDEN);
        callTerminalMethod(SET_VISIBLE, false);
    }

    public void updatePosition() {
        callTerminalMethod(UPDATE_POSITION);
    }

    public void disableSpaceWhenOpened() {
        callTerminalMethod(DISABLE_SPACE_WHEN_OPENED);
    }

    public boolean isVisible() {
        return visible;
    }

    public void stopEvent(final PEventType eventType) {
        widget.stopEvent(eventType);
    }

    public void addDomHandler(final EventHandler eventHandler, final Type type) {
        widget.addDomHandler(eventHandler, type);
    }

    public void addListener(final DropDownContainerAddonListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(final DropDownContainerAddonListener listener) {
        this.listeners.remove(listener);
    }

    private static JsonObject createJsonObject(final String parentId, final boolean stickLeft) {
        final JsonObjectBuilder builder = UIContext.get().getJsonProvider().createObjectBuilder();
        builder.add(PARENT_ID, parentId);
        builder.add(STICK_LEFT, stickLeft);
        return builder.build();
    }

    public interface DropDownContainerAddonListener {

        void onHide();

    }
}
