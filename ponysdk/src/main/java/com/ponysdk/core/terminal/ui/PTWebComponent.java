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

package com.ponysdk.core.terminal.ui;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Terminal-side handler for {@link com.ponysdk.core.ui.basic.PWebComponent}.
 * <p>
 * Creates a custom HTML element (Web Component) and manages its lifecycle:
 * property/attribute updates, named slot insertion, custom event forwarding,
 * and JS method calls.
 * </p>
 * <p>
 * Shadow DOM is entirely managed by the web component itself — PonySDK never
 * touches it. Slot insertion uses the native {@code slot="name"} attribute on
 * child elements, which works whether the component uses a shadow DOM or not.
 * </p>
 * <p>
 * Property values are JSON-encoded strings. On set, the terminal parses them
 * via {@code JSON.parse()} and assigns the result as a JS property. On patch,
 * a JSON merge-patch (RFC 7396) is applied to the existing property value.
 * </p>
 */
public class PTWebComponent extends PTComplexPanel<PTWebComponent.WCHTMLPanel> {

    private static final Logger log = Logger.getLogger(PTWebComponent.class.getName());

    private String tag;
    private final Map<String, JavaScriptObject> eventHandlers = new HashMap<>();

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        tag = buffer.readBinaryModel().getStringValue();
        super.create(buffer, objectId, uiService);
    }

    @Override
    protected WCHTMLPanel createUIObject() {
        return new WCHTMLPanel(tag, "");
    }

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        final BinaryModel binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.WC_SLOT_NAME == binaryModel.getModel()) {
            // Native slot projection: set slot="name" on the child, append to the custom element.
            // The web component decides whether to use shadow DOM — PonySDK doesn't care.
            final String slotName = binaryModel.getStringValue();
            final Widget child = asWidget(ptObject);
            child.getElement().setAttribute("slot", slotName);
            uiObject.insert(child, uiObject.getElement(), uiObject.getElement().getChildCount(), true);
        } else if (ServerToClientModel.INDEX == binaryModel.getModel()) {
            uiObject.insert(asWidget(ptObject), uiObject.getElement(), binaryModel.getIntValue(), true);
        } else {
            buffer.rewind(binaryModel);
            super.add(buffer, ptObject);
        }
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();

        if (ServerToClientModel.WC_SET_PROPERTY == model) {
            final String propName = binaryModel.getStringValue();
            final String propValue = buffer.readBinaryModel().getStringValue(); // WC_PROPERTY_VALUE
            setJsProperty(uiObject.getElement(), propName, propValue);
            return true;
        } else if (ServerToClientModel.WC_PATCH_PROPERTY == model) {
            final String propName = binaryModel.getStringValue();
            final String patchJson = buffer.readBinaryModel().getStringValue(); // WC_PROPERTY_VALUE (patch)
            patchJsProperty(uiObject.getElement(), propName, patchJson);
            return true;
        } else if (ServerToClientModel.WC_REMOVE_PROPERTY == model) {
            removeJsProperty(uiObject.getElement(), binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.WC_SET_ATTRIBUTE == model) {
            final String attrName = binaryModel.getStringValue();
            final String attrValue = buffer.readBinaryModel().getStringValue(); // WC_ATTRIBUTE_VALUE
            uiObject.getElement().setAttribute(attrName, attrValue);
            return true;
        } else if (ServerToClientModel.WC_REMOVE_ATTRIBUTE == model) {
            uiObject.getElement().removeAttribute(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.WC_LISTEN_EVENT == model) {
            final String eventName = binaryModel.getStringValue();
            listenCustomEvent(eventName);
            return true;
        } else if (ServerToClientModel.WC_UNLISTEN_EVENT == model) {
            final String eventName = binaryModel.getStringValue();
            unlistenCustomEvent(eventName);
            return true;
        } else if (ServerToClientModel.WC_CALL_METHOD == model) {
            final String methodName = binaryModel.getStringValue();
            final BinaryModel argsBm = buffer.readBinaryModel();
            if (ServerToClientModel.WC_METHOD_ARGS == argsBm.getModel()) {
                callMethod(uiObject.getElement(), methodName, argsBm.getArrayValue().getJavaScriptObject());
            } else {
                buffer.rewind(argsBm);
                callMethod(uiObject.getElement(), methodName, null);
            }
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    private void listenCustomEvent(final String eventName) {
        if (eventHandlers.containsKey(eventName)) return;
        final JavaScriptObject handler = addCustomEventListener(uiObject.getElement(), eventName, getObjectID());
        eventHandlers.put(eventName, handler);
    }

    private void unlistenCustomEvent(final String eventName) {
        final JavaScriptObject handler = eventHandlers.remove(eventName);
        if (handler != null) {
            removeCustomEventListener(uiObject.getElement(), eventName, handler);
        }
    }

    /**
     * Called from native JS when a custom event fires on the web component.
     */
    private void onCustomEvent(final String eventName, final String detailJson) {
        final PTInstruction instruction = new PTInstruction(getObjectID());
        instruction.put(ClientToServerModel.WC_EVENT_NAME, eventName);
        if (detailJson != null) {
            instruction.put(ClientToServerModel.WC_EVENT_DETAIL, detailJson);
        }
        uiBuilder.sendDataToServer(uiObject, instruction);
    }

    @Override
    public void destroy() {
        // Clean up all event listeners
        for (final var entry : eventHandlers.entrySet()) {
            removeCustomEventListener(uiObject.getElement(), entry.getKey(), entry.getValue());
        }
        eventHandlers.clear();
        super.destroy();
    }

    // ---- Native JS methods ----

    private static native void setJsProperty(Element el, String name, String jsonValue) /*-{
        try {
            el[name] = JSON.parse(jsonValue);
        } catch (e) {
            // Not valid JSON — set as plain string (e.g. simple string values without quotes)
            el[name] = jsonValue;
        }
    }-*/;

    /**
     * Applies a JSON merge-patch (RFC 7396) to an existing JS property.
     * For each key in the patch: null means delete, otherwise set the new value.
     * This avoids replacing the entire object — only changed fields are touched.
     */
    private static native void patchJsProperty(Element el, String name, String patchJson) /*-{
        var patch = JSON.parse(patchJson);
        var current = el[name];
        if (typeof current !== 'object' || current === null) {
            current = {};
        }
        for (var key in patch) {
            if (patch.hasOwnProperty(key)) {
                if (patch[key] === null) {
                    delete current[key];
                } else {
                    current[key] = patch[key];
                }
            }
        }
        el[name] = current;
    }-*/;

    private static native void removeJsProperty(Element el, String name) /*-{
        delete el[name];
    }-*/;

    private native JavaScriptObject addCustomEventListener(Element el, String eventName, int objectId) /*-{
        var self = this;
        var handler = function(e) {
            var detail = null;
            if (e.detail !== undefined && e.detail !== null) {
                try {
                    detail = JSON.stringify(e.detail);
                } catch (ex) {
                    detail = String(e.detail);
                }
            }
            self.@com.ponysdk.core.terminal.ui.PTWebComponent::onCustomEvent(Ljava/lang/String;Ljava/lang/String;)(eventName, detail);
        };
        el.addEventListener(eventName, handler);
        return handler;
    }-*/;

    private static native void removeCustomEventListener(Element el, String eventName, JavaScriptObject handler) /*-{
        el.removeEventListener(eventName, handler);
    }-*/;

    private static native void callMethod(Element el, String methodName, JavaScriptObject args) /*-{
        if (typeof el[methodName] === 'function') {
            if (args) {
                el[methodName].apply(el, args);
            } else {
                el[methodName]();
            }
        }
    }-*/;

    /**
     * Custom HTMLPanel subclass that exposes the protected insert method.
     */
    static final class WCHTMLPanel extends HTMLPanel {

        WCHTMLPanel(final String tag, final String html) {
            super(tag, html);
        }

        @Override
        public void insert(final Widget child, final Element container, final int beforeIndex, final boolean domInsert) {
            super.insert(child, container, beforeIndex, domInsert);
        }
    }
}
