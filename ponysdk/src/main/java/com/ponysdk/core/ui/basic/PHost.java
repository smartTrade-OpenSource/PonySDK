/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.core.ui.basic;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.websocket.Subprotocols;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * The element {@code <pony-app>} an embedded application runs in, as the application sees it
 * (docs/decisions/0008-embedded-application.md). The host page talks to the application through it:
 * <ul>
 * <li>attributes in: the element's attributes, but {@code src}, {@code isolation} and {@code history};</li>
 * <li>events in: the {@code CustomEvent}s the host dispatches on the element;</li>
 * <li>events out: {@link #dispatchEvent}, a {@code CustomEvent} on the element, bubbling and composed.</li>
 * </ul>
 * <pre>
 * final PHost host = PHost.get(); // null when the application runs as a page
 * if (host != null) {
 *     label.setText(host.getAttribute("instrument"));
 *     final HandlerRegistration following = host.addAttributeListener("instrument", label::setText);
 *     host.dispatchEvent("trade", Json.createObjectBuilder().add("side", "BUY").build());
 * }
 * </pre>
 * Each listener comes with its {@link HandlerRegistration}, which removes it (docs/decisions/0011-one-way-to-listen.md).
 */
public final class PHost extends PObject {

    private static final Logger log = LoggerFactory.getLogger(PHost.class);

    private static final String KEY = PHost.class.getName();
    private static final String ATTRIBUTES_KEY = ClientToServerModel.HOST_ATTRIBUTES.toStringValue();
    private static final String EVENT_NAME_KEY = ClientToServerModel.WC_EVENT_NAME.toStringValue();
    private static final String EVENT_DETAIL_KEY = ClientToServerModel.WC_EVENT_DETAIL.toStringValue();

    private final Map<String, String> attributes;
    private final String credential;
    private Map<String, List<Consumer<String>>> attributeListeners;
    private Map<String, List<Consumer<JsonValue>>> eventListeners;

    private PHost(final Map<String, String> attributes, final String credential) {
        this.attributes = attributes;
        this.credential = credential;
    }

    /**
     * The element of the current context's application, created on the first call; null when the application runs as
     * a page.
     */
    public static PHost get() {
        final UIContext uiContext = UIContext.get();
        if (uiContext == null) throw new IllegalStateException("PHost.get() needs a UIContext: call it on the UI thread");
        final PHost existing = uiContext.getAttribute(KEY);
        if (existing != null) return existing;
        if (!uiContext.isEmbedded()) return null; // a page's link can carry attributes too
        final List<String> handshake = uiContext.getParameterMap().get(ATTRIBUTES_KEY);
        if (handshake == null || handshake.isEmpty()) return null;
        final PHost host = new PHost(parse(handshake.get(0)), Subprotocols.credential(uiContext.getHeader(Subprotocols.HEADER)));
        uiContext.setAttribute(KEY, host);
        host.attach(PWindow.getMain(), null);
        return host;
    }

    private static Map<String, String> parse(final String json) {
        final Map<String, String> attributes = new LinkedHashMap<>();
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            reader.readObject().forEach((name, value) -> {
                if (value instanceof JsonString s) attributes.put(name, s.getString());
            });
        } catch (final JsonException | ClassCastException e) {
            log.warn("Ignoring malformed host attributes in the handshake");
        }
        return attributes;
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.HOST;
    }

    /**
     * The credential the host page gave the element (its property {@code credential}) for the handshake that created
     * this context: an opaque string, a JWT or an OAuth access token, that the application validates as it sees fit
     * (docs/decisions/0009-embedding-limits.md). Null when the host gave none. A credential renewed while the
     * application runs comes as an event: {@link #addEventListener(String, Consumer)}.
     */
    public String getCredential() {
        return credential;
    }

    /** The attribute's value, null when the element does not have it. */
    public String getAttribute(final String name) {
        return attributes.get(name);
    }

    /** The element's attributes, but {@code src}, {@code isolation} and {@code history}. */
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Called with the attribute's new value each time the host changes it, with null when it removes it.
     *
     * @return removes the listener
     */
    public HandlerRegistration addAttributeListener(final String name, final Consumer<String> listener) {
        Objects.requireNonNull(listener, "listener");
        if (attributeListeners == null) attributeListeners = new HashMap<>(4);
        attributeListeners.computeIfAbsent(name, n -> new ArrayList<>(1)).add(listener);
        return () -> removeAttributeListener(name, listener);
    }

    private void removeAttributeListener(final String name, final Consumer<String> listener) {
        // attributeListeners: created with the registration, never cleared
        final List<Consumer<String>> listeners = attributeListeners.get(name);
        if (listeners != null && listeners.remove(listener) && listeners.isEmpty()) attributeListeners.remove(name);
    }

    /**
     * Called with the detail of each {@code CustomEvent} of this type the host dispatches on the element:
     * {@link JsonValue#NULL} without detail, a JSON string when the detail is not JSON. The events this application
     * dispatches are not delivered to it.
     *
     * @return removes the listener; the last one of a type removed, the terminal stops listening
     */
    public HandlerRegistration addEventListener(final String type, final Consumer<JsonValue> listener) {
        Objects.requireNonNull(listener, "listener");
        if (eventListeners == null) eventListeners = new HashMap<>(4);
        final List<Consumer<JsonValue>> listeners = eventListeners.computeIfAbsent(type, t -> new ArrayList<>(1));
        if (listeners.isEmpty()) saveUpdate(writer -> writer.write(ServerToClientModel.WC_LISTEN_EVENT, type));
        listeners.add(listener);
        return () -> removeEventListener(type, listener);
    }

    private void removeEventListener(final String type, final Consumer<JsonValue> listener) {
        // eventListeners: created with the registration, never cleared
        final List<Consumer<JsonValue>> listeners = eventListeners.get(type);
        if (listeners == null || !listeners.remove(listener) || !listeners.isEmpty()) return;
        eventListeners.remove(type);
        saveUpdate(writer -> writer.write(ServerToClientModel.WC_UNLISTEN_EVENT, type));
    }

    /** Dispatches a {@code CustomEvent} of this type on the element, without detail. */
    public void dispatchEvent(final String type) {
        dispatchEvent(type, null);
    }

    /** Dispatches a {@code CustomEvent} of this type on the element, bubbling and composed, with this detail. */
    public void dispatchEvent(final String type, final JsonValue detail) {
        final Object[] event = { type, detail == null ? null : detail.toString() };
        saveUpdate(writer -> writer.write(ServerToClientModel.DISPATCH_EVENT, event));
    }

    @Override
    public void onClientData(final JsonObject message) {
        if (destroy) return;
        if (message.containsKey(ATTRIBUTES_KEY)) {
            message.getJsonObject(ATTRIBUTES_KEY).forEach(this::attributeChanged);
        } else if (message.containsKey(EVENT_NAME_KEY)) {
            eventReceived(message.getString(EVENT_NAME_KEY), message.getString(EVENT_DETAIL_KEY, null));
        } else {
            super.onClientData(message);
        }
    }

    private void attributeChanged(final String name, final JsonValue json) {
        final String value = json instanceof JsonString s ? s.getString() : null;
        final String previous = value == null ? attributes.remove(name) : attributes.put(name, value);
        if (Objects.equals(previous, value) || attributeListeners == null) return;
        final List<Consumer<String>> listeners = attributeListeners.get(name);
        if (listeners != null) for (final Consumer<String> listener : List.copyOf(listeners)) listener.accept(value);
    }

    private void eventReceived(final String type, final String detail) {
        final List<Consumer<JsonValue>> listeners = eventListeners == null ? null : eventListeners.get(type);
        if (listeners == null) return;
        JsonValue value = JsonValue.NULL;
        if (detail != null) {
            try (JsonReader reader = Json.createReader(new StringReader(detail))) {
                value = reader.readValue();
            } catch (final JsonException e) {
                value = Json.createValue(detail); // not JSON: the terminal's String(detail)
            }
        }
        for (final Consumer<JsonValue> listener : List.copyOf(listeners)) listener.accept(value);
    }

    @Override
    public String toString() {
        return "PHost #" + ID + " " + attributes;
    }
}
