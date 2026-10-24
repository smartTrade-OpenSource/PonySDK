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

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PPanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PDomEvent.Type;
import com.ponysdk.core.ui.eventbus.EventHandler;
import com.ponysdk.core.ui.model.PEventType;

public class DropDownContainerAddon extends PAddOnComposite<PPanel> {

    private static final String PROPERTY_POSITION = "position";
    private static final String PROPERTY_ABSOLUTE = "absolute";
    private static final String PROPERTY_TOP = "top";
    private static final String PROPERTY_LEFT = "left";
    private static final String PROPERTY_RIGHT = "right";
    private static final String PROPERTY_0 = "0";

    private static final String PARENT_ID = "parentId";
    private static final String STICK_LEFT = "stickLeft";
    private static final String MULTILEVEL = "multiLevel";
    private static final String UPDATE_POSITION = "updatePosition";
    private static final String ADJUST_POSITION = "adjustPosition";
    private static final String SET_VISIBLE = "setVisible";
    private static final String DISABLE_SPACE_WHEN_OPENED = "disableSpaceWhenOpened";
    private static final String POSITION_RIGHT = "setDropRight";
    private static final String WINDOW_EVENT = "windowEvent";
    private static final String RESIZE = "resize";
    private static final String SCROLL = "scroll";
    private static final String CLICK = "click";

    private boolean visible;

    private final PWidget parent;
    private final Set<DropDownContainerAddonListener> listeners;

    /**
     * The multilevel dropdown is meant to be stuck to the left of the container.
     * Use this static builder to ensure you create a multilevel instance with the right settings
     * 
     * @param parent
     * @return
     */
    public static DropDownContainerAddon newMultilevelDopdown(final PWidget parent) {
        return new DropDownContainerAddon(parent, true,  true);
    }

    public DropDownContainerAddon(final PWidget parent) {
        this(parent, true,  false);
    }

    private DropDownContainerAddon(final PWidget parent, final boolean stickLeft, final boolean multilevel) {
        super(Element.newPFlowPanel(), createJsonObject(parent.getID(), stickLeft, multilevel));
        this.parent = parent;
        widget.setStyleProperty(PROPERTY_POSITION, PROPERTY_ABSOLUTE);
        widget.setStyleProperty(PROPERTY_TOP, PROPERTY_0);
        if (stickLeft) {
            if (!multilevel) {
                // The multilevel dropdown is designed to open each new level next to the previous one, so it won't stick to the left of the container.
                widget.setStyleProperty(PROPERTY_LEFT, PROPERTY_0);
            }
        } else {
            widget.setStyleProperty(PROPERTY_RIGHT, PROPERTY_0);
        }
        this.listeners = new HashSet<>();
        setTerminalHandler(event -> {
            final JsonObject jsonObject = event.getData();
            if (jsonObject.containsKey(WINDOW_EVENT)) {
                final String windowEvent = jsonObject.getString(WINDOW_EVENT);
                if (RESIZE.equals(windowEvent) || SCROLL.equals(windowEvent) || CLICK.equals(windowEvent)) {
                    hide();
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
        parent.getWindow().getPRootPanel().add(widget);
        updatePosition();
        callTerminalMethod(SET_VISIBLE, true);
    }

    public void hide() {
        visible = false;
        callTerminalMethod(SET_VISIBLE, false);
        widget.removeFromParent();
    }

    public void updatePosition() {
        callTerminalMethod(UPDATE_POSITION);
    }
    
    public void adjustPosition() {
    	callTerminalMethod(ADJUST_POSITION);
    }

    public void disableSpaceWhenOpened() {
        callTerminalMethod(DISABLE_SPACE_WHEN_OPENED);
    }

    public void setDropRight() {
        callTerminalMethod(POSITION_RIGHT);
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

    /**
     * Set the attribute {@code multilvl-parent} on the given DropDownContainerAddon element.<br>
     * Server side we will be able to determine if the element is a parent without any DOM boundaries.<br>
     * 
     * @param dropdown the element which we attach the attribute
     */
    public void defineAsMultiLevelParent(DropDownContainerAddon dropdown) {
        dropdown.asWidget().setAttribute("multilvl-parent", String.valueOf(widget.getID()));
    }

    private static JsonObject createJsonObject(final int parentId, final boolean stickLeft, final boolean multiLevel) {
        final JsonObjectBuilder builder = UIContext.get().getJsonProvider().createObjectBuilder();
        builder.add(PARENT_ID, parentId);
        builder.add(STICK_LEFT, stickLeft);
        builder.add(MULTILEVEL, multiLevel);
        return builder.build();
    }

    public interface DropDownContainerAddonListener {

        void onHide();

    }
}
