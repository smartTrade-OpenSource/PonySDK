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

package com.ponysdk.core.ui.basic;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.util.CompactStringMap;
import com.ponysdk.core.util.OffHeapJsonStore;
import com.ponysdk.core.writer.ModelWriter;
import jakarta.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Server-side representation of a custom Web Component.
 * <p>
 * Allows creating, configuring, and interacting with any HTML custom element
 * (Web Component) from the server side, using PonySdk's binary protocol for
 * efficient property synchronization and event handling.
 * </p>
 * <h2>Usage example:</h2>
 * <pre>{@code
 * PWebComponent chart = new PWebComponent("my-chart");
 *
 * // Declare properties with their storage strategy (once)
 * var title   = chart.property("title");                          // on-heap (default)
 * var theme   = chart.property("theme");                          // on-heap
 * var dataset = chart.property("dataset").offHeap().withPatch();  // off-heap + delta
 * var stream  = chart.property("stream").stateless();             // fire & forget
 *
 * // Then use uniformly
 * title.set("\"Revenue Q4\"");
 * theme.set("\"dark\"");
 * dataset.set(hugeJson);       // off-heap + patch automatically
 * stream.set(tempData);        // no cache, no delta
 *
 * // Read back
 * String t = title.get();      // works for on-heap and off-heap
 *
 * // Events
 * chart.onEvent("point-click", event -> { ... });
 *
 * // Attributes
 * chart.attr("aria-label", "Chart");
 *
 * flowPanel.add(chart);
 * }</pre>
 */
public class PWebComponent extends PComplexPanel {

    private static final Logger log = LoggerFactory.getLogger(PWebComponent.class);

    private static final String WC_EVENT_NAME_KEY = ClientToServerModel.WC_EVENT_NAME.toStringValue();

    // ---- Storage strategy for properties ----

    public enum StorageMode {
        /** On-heap CompactStringMap. Delta check + JSON patch + reconnect replay. Default. */
        ON_HEAP,
        /** Off-heap ByteBuffer. Delta check via hash + JSON patch + reconnect replay. For large JSON. */
        OFF_HEAP,
        /** No cache. Always sends full value. No delta, no replay. For ephemeral/streaming data. */
        STATELESS
    }

    // ---- Fields ----

    private final String tagName;

    // Property handles by name — lazy
    private Map<String, PropertyHandle> properties;

    // On-heap property values — lazy
    private CompactStringMap onHeapValues;
    // Off-heap property values — lazy
    private OffHeapJsonStore offHeapValues;
    // Tracks which properties have patch enabled (subset of ON_HEAP and OFF_HEAP)
    private Set<String> patchEnabled;

    // Attributes — lazy
    private CompactStringMap wcAttributes;
    // Event listeners — lazy
    private Map<String, List<Consumer<JsonObject>>> wcEventListeners;

    // ======== PropertyHandle — the unified property API ========

    /**
     * A typed handle to a single web component property.
     * Declare once with a storage strategy, then use {@link #set}/{@link #get}/{@link #remove}.
     */
    public final class PropertyHandle {

        private final String name;
        private StorageMode mode = StorageMode.ON_HEAP;
        private boolean patch = true; // patch enabled by default for ON_HEAP and OFF_HEAP

        PropertyHandle(final String name) {
            this.name = name;
        }

        /** Store this property off-heap (direct ByteBuffer). Ideal for large JSON. */
        public PropertyHandle offHeap() {
            this.mode = StorageMode.OFF_HEAP;
            return this;
        }

        /** Don't cache this property server-side. No delta, no replay. */
        public PropertyHandle stateless() {
            this.mode = StorageMode.STATELESS;
            this.patch = false;
            return this;
        }

        /** Enable JSON merge-patch for incremental updates (default for ON_HEAP/OFF_HEAP). */
        public PropertyHandle withPatch() {
            if (mode == StorageMode.STATELESS) throw new IllegalStateException("Cannot enable patch on stateless property");
            this.patch = true;
            if (patchEnabled == null) patchEnabled = new HashSet<>();
            patchEnabled.add(name);
            return this;
        }

        /** Disable JSON merge-patch — always send full replacement. */
        public PropertyHandle withoutPatch() {
            this.patch = false;
            if (patchEnabled != null) patchEnabled.remove(name);
            return this;
        }

        /** Sets the property value. Behavior depends on the storage mode. */
        public void set(final String value) {
            switch (mode) {
                case STATELESS -> sendFull(name, value);
                case ON_HEAP -> setOnHeap(name, value, patch);
                case OFF_HEAP -> setOffHeap(name, value, patch);
            }
        }

        /** Convenience: set an int value. */
        public void set(final int value) { set(String.valueOf(value)); }

        /** Convenience: set a double value. */
        public void set(final double value) { set(String.valueOf(value)); }

        /** Convenience: set a boolean value. */
        public void set(final boolean value) { set(String.valueOf(value)); }

        /** Reads the current cached value. Returns null for stateless properties. */
        public String get() {
            return switch (mode) {
                case STATELESS -> null;
                case ON_HEAP -> onHeapValues != null ? onHeapValues.get(name) : null;
                case OFF_HEAP -> offHeapValues != null ? offHeapValues.readValue(name) : null;
            };
        }

        /** Removes the property from the web component. */
        public void remove() {
            boolean removed = false;
            switch (mode) {
                case ON_HEAP -> removed = onHeapValues != null && onHeapValues.remove(name) != null;
                case OFF_HEAP -> removed = offHeapValues != null && offHeapValues.remove(name);
                case STATELESS -> removed = false;
            }
            if (removed) {
                saveUpdate(writer -> writer.write(ServerToClientModel.WC_REMOVE_PROPERTY, name));
            }
        }

        public String getName() { return name; }
        public StorageMode getMode() { return mode; }
        public boolean isPatchEnabled() { return patch; }
    }

    // ======== Constructor ========

    public PWebComponent(final String tagName) {
        if (tagName == null || !tagName.contains("-")) {
            throw new IllegalArgumentException("Web Component tag name must contain a hyphen: " + tagName);
        }
        this.tagName = tagName;
    }

    // ======== Property API ========

    /**
     * Declares (or retrieves) a property handle. Call configuration methods on the
     * returned handle to set the storage strategy, then use {@code .set()} / {@code .get()}.
     * <p>
     * The handle is created once and reused. Calling {@code property("x")} twice
     * returns the same handle.
     * </p>
     */
    public PropertyHandle property(final String name) {
        if (properties == null) properties = new LinkedHashMap<>();
        return properties.computeIfAbsent(name, PropertyHandle::new);
    }

    // ---- Internal property write methods ----

    private void sendFull(final String name, final String value) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.WC_SET_PROPERTY, name);
            writer.write(ServerToClientModel.WC_PROPERTY_VALUE, value);
        });
    }

    private void setOnHeap(final String name, final String value, final boolean patchEnabled) {
        if (onHeapValues == null) onHeapValues = new CompactStringMap();
        final String previous = onHeapValues.put(name, value);
        if (Objects.equals(previous, value)) return;

        if (patchEnabled && previous != null) {
            final String patch = computeJsonPatch(previous, value);
            if (patch != null) {
                saveUpdate(writer -> {
                    writer.write(ServerToClientModel.WC_PATCH_PROPERTY, name);
                    writer.write(ServerToClientModel.WC_PROPERTY_VALUE, patch);
                });
                return;
            }
        }
        sendFull(name, value);
    }

    private void setOffHeap(final String name, final String value, final boolean patchEnabled) {
        if (offHeapValues == null) offHeapValues = new OffHeapJsonStore();

        if (offHeapValues.hasSameValue(name, value)) return;

        String previous = null;
        if (patchEnabled) previous = offHeapValues.readValue(name);

        offHeapValues.put(name, value);

        if (patchEnabled && previous != null) {
            final String patch = computeJsonPatch(previous, value);
            if (patch != null) {
                saveUpdate(writer -> {
                    writer.write(ServerToClientModel.WC_PATCH_PROPERTY, name);
                    writer.write(ServerToClientModel.WC_PROPERTY_VALUE, patch);
                });
                return;
            }
        }
        sendFull(name, value);
    }

    // ======== Slot API — targeted child insertion ========

    /**
     * A virtual panel that targets a named slot inside the Web Component's DOM.
     * <p>
     * Any PWidget added to a SlotPanel will have {@code slot="slotName"} set on its element
     * and be appended to the custom element. The web component handles projection via
     * {@code <slot name="slotName">} in its shadow DOM.
     * </p>
     * <pre>{@code
     * wc.slot("toolbar").add(myButton);
     * wc.slot("content").add(myLabel, myGrid);
     * }</pre>
     */
    public final class SlotPanel extends PComplexPanel {

        private final String slotName;

        SlotPanel(final String slotName) {
            this.slotName = slotName;
        }

        @Override
        protected WidgetType getWidgetType() {
            // SlotPanel is never created independently — it delegates to the parent WC
            throw new UnsupportedOperationException("SlotPanel is not a standalone widget");
        }

        @Override
        public void add(final PWidget child) {
            assertNotMe(child);
            if (child.getWindow() == null || child.getWindow() == window) {
                child.removeFromParent();
                if (children == null) children = new PWidgetCollection(this);
                children.add(child);
                adopt(child);
                if (isInitialized()) child.attach(window, frame);
                // Inject WC_SLOT_NAME alongside the standard TYPE_ADD
                child.saveAdd(child.getID(), PWebComponent.this.getID(),
                    new com.ponysdk.core.ui.model.ServerBinaryModel(ServerToClientModel.WC_SLOT_NAME, slotName));
            } else {
                throw new IllegalAccessError("Widget already attached to another window");
            }
        }

        @Override
        protected boolean attach(final PWindow window, final PFrame frame) {
            this.frame = frame;
            if (this.window == null && window != null) {
                this.window = window;
                // Don't call init() — SlotPanel has no own CREATE message
                if (children != null) {
                    for (final PWidget child : children) {
                        child.attach(window, frame);
                    }
                }
                return true;
            }
            return false;
        }

        public String getSlotName() { return slotName; }
    }

    // Slot registry — lazy
    private Map<String, SlotPanel> slots;

    /**
     * Returns (or creates) a SlotPanel targeting the named slot inside this Web Component.
     * <p>
     * The Web Component must expose a {@code <slot name="slotName">} in its shadow DOM,
     * or handle the {@code slot} attribute in light DOM as needed.
     * </p>
     */
    public SlotPanel slot(final String slotName) {
        if (slots == null) slots = new LinkedHashMap<>();
        return slots.computeIfAbsent(slotName, name -> {
            final SlotPanel sp = new SlotPanel(name);
            // Propagate window/frame if already attached
            if (window != null) sp.attach(window, frame);
            return sp;
        });
    }

    // ======== Widget type ========

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.WEB_COMPONENT;
    }

    // ======== Protocol: creation & reconnect ========

    @Override
    void init0() {
        // Propagate attach to all declared slots so their children get initialized
        if (slots != null) {
            for (final SlotPanel sp : slots.values()) {
                sp.attach(window, frame);
            }
        }
    }

    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        super.enrichForCreation(writer);
        writer.write(ServerToClientModel.WC_TAG_NAME, tagName);
    }

    @Override
    protected void enrichForUpdate(final ModelWriter writer) {
        super.enrichForUpdate(writer);
        // Replay on-heap properties
        if (onHeapValues != null) {
            for (final var entry : onHeapValues.entrySet()) {
                writer.write(ServerToClientModel.WC_SET_PROPERTY, entry.getKey());
                writer.write(ServerToClientModel.WC_PROPERTY_VALUE, entry.getValue());
            }
        }
        // Replay off-heap properties
        if (offHeapValues != null && !offHeapValues.isEmpty()) {
            for (final String key : offHeapValues.keys()) {
                final String value = offHeapValues.readValue(key);
                if (value != null) {
                    writer.write(ServerToClientModel.WC_SET_PROPERTY, key);
                    writer.write(ServerToClientModel.WC_PROPERTY_VALUE, value);
                }
            }
        }
        // Replay attributes
        if (wcAttributes != null) {
            for (final var entry : wcAttributes.entrySet()) {
                writer.write(ServerToClientModel.WC_SET_ATTRIBUTE, entry.getKey());
                writer.write(ServerToClientModel.WC_ATTRIBUTE_VALUE, entry.getValue());
            }
        }
        // Replay event listeners
        if (wcEventListeners != null) {
            for (final String eventName : wcEventListeners.keySet()) {
                writer.write(ServerToClientModel.WC_LISTEN_EVENT, eventName);
            }
        }
    }

    // ======== JSON merge-patch (RFC 7396) ========

    static String computeJsonPatch(final String oldJson, final String newJson) {
        try {
            final JsonObject oldObj = parseJson(oldJson);
            final JsonObject newObj = parseJson(newJson);
            if (oldObj == null || newObj == null) return null;

            final JsonObjectBuilder patch = Json.createObjectBuilder();
            boolean hasDiff = false;

            // Keys in new but changed or added
            for (final String key : newObj.keySet()) {
                final JsonValue newVal = newObj.get(key);
                final JsonValue oldVal = oldObj.get(key);
                if (!newVal.equals(oldVal)) {
                    patch.add(key, newVal);
                    hasDiff = true;
                }
            }
            // Keys removed (present in old, absent in new)
            for (final String key : oldObj.keySet()) {
                if (!newObj.containsKey(key)) {
                    patch.addNull(key);
                    hasDiff = true;
                }
            }

            return hasDiff ? patch.build().toString() : null;
        } catch (final Exception e) {
            log.debug("Cannot compute JSON patch, falling back to full send", e);
            return null;
        }
    }

    private static JsonObject parseJson(final String json) {
        try (final JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        } catch (final Exception e) {
            return null;
        }
    }

    // ======== Attribute API ========

    /**
     * Sets an HTML attribute on the web component element.
     */
    public void attr(final String name, final String value) {
        if (wcAttributes == null) wcAttributes = new CompactStringMap();
        wcAttributes.put(name, value);
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.WC_SET_ATTRIBUTE, name);
            writer.write(ServerToClientModel.WC_ATTRIBUTE_VALUE, value);
        });
    }

    /**
     * Removes an HTML attribute from the web component element.
     */
    public void removeAttr(final String name) {
        if (wcAttributes != null) wcAttributes.remove(name);
        saveUpdate(writer -> writer.write(ServerToClientModel.WC_REMOVE_ATTRIBUTE, name));
    }

    /**
     * Returns the cached value of an HTML attribute, or null.
     */
    public String getAttr(final String name) {
        return wcAttributes != null ? wcAttributes.get(name) : null;
    }

    // ======== Event API ========

    /**
     * Registers a listener for a custom event dispatched by the web component.
     *
     * @param eventName the custom event name (e.g. "count-changed")
     * @param listener  receives a JsonObject with event detail
     */
    public void onEvent(final String eventName, final Consumer<JsonObject> listener) {
        if (wcEventListeners == null) wcEventListeners = new ConcurrentHashMap<>();
        final List<Consumer<JsonObject>> listeners = wcEventListeners.computeIfAbsent(eventName, k -> new ArrayList<>());
        final boolean firstListener = listeners.isEmpty();
        listeners.add(listener);
        if (firstListener) {
            saveUpdate(writer -> writer.write(ServerToClientModel.WC_LISTEN_EVENT, eventName));
        }
    }

    /**
     * Removes a specific event listener.
     */
    public void removeEventListener(final String eventName, final Consumer<JsonObject> listener) {
        if (wcEventListeners == null) return;
        final List<Consumer<JsonObject>> listeners = wcEventListeners.get(eventName);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                wcEventListeners.remove(eventName);
                saveUpdate(writer -> writer.write(ServerToClientModel.WC_UNLISTEN_EVENT, eventName));
            }
        }
    }

    /**
     * Removes all listeners for a given event.
     */
    public void removeAllEventListeners(final String eventName) {
        if (wcEventListeners == null) return;
        if (wcEventListeners.remove(eventName) != null) {
            saveUpdate(writer -> writer.write(ServerToClientModel.WC_UNLISTEN_EVENT, eventName));
        }
    }

    // ======== Method call API ========

    /**
     * Calls a method on the web component element (no arguments).
     */
    public void call(final String methodName) {
        saveUpdate(writer -> writer.write(ServerToClientModel.WC_CALL_METHOD, methodName));
    }

    /**
     * Calls a method on the web component element with JSON-encoded arguments.
     */
    public void call(final String methodName, final Object... args) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.WC_CALL_METHOD, methodName);
            writer.write(ServerToClientModel.WC_METHOD_ARGS, args);
        });
    }

    // ======== Configuration getters/setters ========

    public String getTagName() {
        return tagName;
    }

    // ======== Client data handling ========

    @Override
    public void onClientData(final JsonObject event) {
        if (event.containsKey(WC_EVENT_NAME_KEY)) {
            final String eventName = event.getString(WC_EVENT_NAME_KEY);
            if (wcEventListeners != null) {
                final List<Consumer<JsonObject>> listeners = wcEventListeners.get(eventName);
                if (listeners != null) {
                    for (final Consumer<JsonObject> listener : listeners) {
                        listener.accept(event);
                    }
                }
            }
        } else {
            super.onClientData(event);
        }
    }

    // ======== Lifecycle ========

    @Override
    public void onDestroy() {
        if (offHeapValues != null) {
            offHeapValues.release();
            offHeapValues = null;
        }
        onHeapValues = null;
        properties = null;
        wcAttributes = null;
        wcEventListeners = null;
        patchEnabled = null;
        slots = null;
        super.onDestroy();
    }

    // ======== Diagnostics ========

    /**
     * Returns the approximate off-heap memory usage in bytes, or 0 if none.
     */
    public int getOffHeapBytes() {
        return offHeapValues != null ? offHeapValues.offHeapBytes() : 0;
    }

    @Override
    public String toString() {
        return "PWebComponent{tag='" + tagName + "', id=" + getID() + '}';
    }

}
