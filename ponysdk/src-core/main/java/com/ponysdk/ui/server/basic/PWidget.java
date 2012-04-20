
package com.ponysdk.ui.server.basic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.event.Event;
import com.ponysdk.core.event.EventHandler;
import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.core.event.SimpleEventBus;
import com.ponysdk.core.instruction.AddHandler;
import com.ponysdk.core.instruction.RemoveHandler;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.server.basic.event.HasPWidgets;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PDomEvent;
import com.ponysdk.ui.server.basic.event.PKeyPressEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PMouseOutEvent;
import com.ponysdk.ui.server.basic.event.PMouseOverEvent;
import com.ponysdk.ui.terminal.DomHandlerType;
import com.ponysdk.ui.terminal.instruction.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;

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

public abstract class PWidget extends PObject implements IsPWidget {

    private static Logger log = LoggerFactory.getLogger(PWidget.class);

    protected PWidget parent;

    private boolean visible = true;

    private String title;

    protected Object data;

    private final Set<String> styleNames = new HashSet<String>();

    private final Map<String, String> styleProperties = new HashMap<String, String>();

    private final SimpleEventBus domHandler = new SimpleEventBus();

    private String width;

    private String height;

    private String styleName;

    private String debugID;

    public static PWidget asWidgetOrNull(final IsPWidget w) {
        return w == null ? null : w.asWidget();
    }

    protected void stackUpdate(final String key, final int value) {
        final Update update = new Update(getID());
        update.put(key, value);
        getPonySession().stackInstruction(update);
    }

    protected void stackUpdate(final String key, final String value) {
        final Update update = new Update(getID());
        update.put(key, value);
        getPonySession().stackInstruction(update);
    }

    public void setWidth(final String width) {
        this.width = width;
        stackUpdate(PROPERTY.WIDGET_WIDTH, width);
    }

    public void setHeight(final String height) {
        this.height = height;
        stackUpdate(PROPERTY.WIDGET_HEIGHT, height);
    }

    public PWidget getParent() {
        return parent;
    }

    public void setParent(final PWidget parent) {
        this.parent = parent;
    }

    public void setStyleProperty(final String name, final String value) {
        styleProperties.put(name, value);
        final Update update = new Update(ID);
        update.put(PROPERTY.STYLE_KEY, name);
        update.put(PROPERTY.STYLE_VALUE, value);
        getPonySession().stackInstruction(update);
    }

    public void setStyleName(final String styleName) {
        this.styleName = styleName;
        final Update update = new Update(ID);
        update.put(PROPERTY.STYLE_NAME, styleName);
        getPonySession().stackInstruction(update);
    }

    public void addStyleName(final String styleName) {
        if (styleNames.add(styleName)) {
            final Update update = new Update(ID);
            update.put(PROPERTY.ADD_STYLE_NAME, styleName);
            getPonySession().stackInstruction(update);
        }
    }

    public void removeStyleName(final String styleName) {
        if (styleNames.remove(styleName)) {
            final Update update = new Update(ID);
            update.put(PROPERTY.REMOVE_STYLE_NAME, styleName);
            getPonySession().stackInstruction(update);
        }
    }

    public void ensureDebugId(final String debugID) {
        this.debugID = debugID;
        final Update update = new Update(ID);
        update.put(PROPERTY.ENSURE_DEBUG_ID, debugID);
        getPonySession().stackInstruction(update);
    }

    @Override
    public PWidget asWidget() {
        return this;
    }

    public void setSizeFull() {
        setWidth("100%");
        setHeight("100%");
    }

    public void setVisible(final boolean visible) {
        this.visible = visible;
        final Update update = new Update(ID);
        update.put(PROPERTY.WIDGET_VISIBLE, visible);
        getPonySession().stackInstruction(update);
    }

    public void setTitle(final String title) {
        this.title = title;
        final Update update = new Update(ID);
        update.put(PROPERTY.WIDGET_TITLE, title);
        getPonySession().stackInstruction(update);
    }

    public String getTitle() {
        return title;
    }

    public Object getData() {
        return data;
    }

    public void setData(final Object data) {
        this.data = data;
    }

    public <H extends EventHandler> HandlerRegistration removeDomHandler(final H handler, final PDomEvent.Type<H> type) {
        final HandlerRegistration handlerRegistration = domHandler.addHandler(type, handler);
        final RemoveHandler removeHandler = new RemoveHandler(getID(), HANDLER.DOM_HANDLER);
        if (handler instanceof JSONObject) {
            removeHandler.put(PROPERTY.DOM_HANDLER_CODE, handler);
        }
        getPonySession().stackInstruction(removeHandler);
        return handlerRegistration;
    }

    public <H extends EventHandler> HandlerRegistration addDomHandler(final H handler, final PDomEvent.Type<H> type) {
        final HandlerRegistration handlerRegistration = domHandler.addHandler(type, handler);
        final AddHandler addHandler = new AddHandler(getID(), HANDLER.DOM_HANDLER);
        addHandler.put(PROPERTY.DOM_HANDLER_CODE, type.getDomHandlerType().ordinal());
        // TODO nciaravola temp

        // if (handler instanceof JSONObject) {
        // addHandler.put(PROPERTY.DOM_HANDLER, handler);
        // }
        getPonySession().stackInstruction(addHandler);
        return handlerRegistration;
    }

    @Override
    public void onEventInstruction(final JSONObject instruction) throws JSONException {
        if (instruction.getString(HANDLER.KEY).equals(HANDLER.DOM_HANDLER)) {
            final DomHandlerType domHandler = DomHandlerType.values()[instruction.getInt(PROPERTY.DOM_HANDLER_TYPE)];
            switch (domHandler) {
                case KEY_PRESS:
                    fireEvent(new PKeyPressEvent(this, instruction.getInt(PROPERTY.VALUE)));
                    break;
                case KEY_UP:
                    fireEvent(new PKeyUpEvent(this, instruction.getInt(PROPERTY.VALUE)));
                    break;
                case CLICK:
                    final PClickEvent event = new PClickEvent(this);
                    event.setClientX(instruction.getInt(PROPERTY.CLIENT_X));
                    event.setClientY(instruction.getInt(PROPERTY.CLIENT_Y));
                    event.setSourceAbsoluteLeft((int) instruction.getDouble(PROPERTY.SOURCE_ABSOLUTE_LEFT));
                    event.setSourceAbsoluteTop((int) instruction.getDouble(PROPERTY.SOURCE_ABSOLUTE_TOP));
                    event.setSourceOffsetHeight((int) instruction.getDouble(PROPERTY.SOURCE_OFFSET_HEIGHT));
                    event.setSourceOffsetWidth((int) instruction.getDouble(PROPERTY.SOURCE_OFFSET_WIDTH));
                    fireEvent(event);
                    break;
                case MOUSE_OVER:
                    fireEvent(new PMouseOverEvent(this));
                    break;
                case MOUSE_OUT:
                    fireEvent(new PMouseOutEvent(this));
                    break;
                default:
                    log.error("Dom Handler not implemented: " + domHandler);
                    break;
            }
        }
    }

    protected <H extends EventHandler> Set<H> getHandlerSet(final PDomEvent.Type<H> type, final Object source) {
        return domHandler.getHandlerSet(type, null);
    }

    public void fireEvent(final Event<?> event) {
        domHandler.fireEvent(event);
    }

    public void removeFromParent() {
        if (parent instanceof HasPWidgets) {
            ((HasPWidgets) parent).remove(this);
        } else if (parent != null) { throw new IllegalStateException("This widget's parent does not implement HasPWidgets"); }
    }

    public boolean isVisible() {
        return visible;
    }

    public String getWidth() {
        return width;
    }

    public String getHeight() {
        return height;
    }

    public String getStyleName() {
        return styleName;
    }

    public String getDebugID() {
        return debugID;
    }

}
