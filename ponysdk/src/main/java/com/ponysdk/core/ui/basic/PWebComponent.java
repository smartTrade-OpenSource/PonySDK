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
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.util.CompactStringMap;
import com.ponysdk.core.util.OffHeapJsonStore;
import com.ponysdk.core.writer.ModelWriter;
import jakarta.json.*;
import jakarta.json.spi.JsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.*;
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
 * // Then use uniformly: values are JSON text, a string in quotes
 * title.set("\"Revenue Q4\"");
 * theme.set("\"dark\"");
 * dataset.set(hugeJson);       // off-heap + patch automatically
 * stream.set(tempData);        // no cache, no delta
 *
 * // Read back
 * String t = title.get();      // works for on-heap and off-heap
 *
 * // Events: the custom event's detail; the registration removes the listener
 * HandlerRegistration clicks = chart.addEventListener("point-click", detail -> { ... });
 *
 * // Attributes, as on any widget
 * chart.setAttribute("aria-label", "Chart");
 *
 * flowPanel.add(chart);
 * }</pre>
 * A library's Custom Elements Manifest gives typed classes of its elements: {@link com.ponysdk.core.tools.WebComponentGenerator}.
 */
public class PWebComponent extends PComplexPanel {

    private static final Logger log = LoggerFactory.getLogger(PWebComponent.class);

    private static final String WC_EVENT_NAME_KEY = ClientToServerModel.WC_EVENT_NAME.toStringValue();
    private static final String WC_EVENT_DETAIL_KEY = ClientToServerModel.WC_EVENT_DETAIL.toStringValue();

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

    // Custom event listeners, by event name — lazy
    private Map<String, List<Consumer<JsonValue>>> wcEventListeners;

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

        /** Store this property off-heap (direct ByteBuffer). Ideal for large JSON. Before the first {@link #set}. */
        public PropertyHandle offHeap() {
            requireNoValue("offHeap()");
            this.mode = StorageMode.OFF_HEAP;
            return this;
        }

        /** Don't cache this property server-side. No delta, no replay. Before the first {@link #set}. */
        public PropertyHandle stateless() {
            requireNoValue("stateless()");
            this.mode = StorageMode.STATELESS;
            this.patch = false;
            return this;
        }

        /** Enable JSON merge-patch for incremental updates (default for ON_HEAP/OFF_HEAP). Before the first {@link #set}. */
        public PropertyHandle withPatch() {
            requireNoValue("withPatch()");
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

        /**
         * Sets the property to a JSON text: {@code "\"dark\""} for the string dark, {@code "42"}, {@code "{\"a\":1}"}.
         * The text is not checked: a string without its quotes reaches the element as that string only if it is not
         * valid JSON. Behavior depends on the storage mode.
         */
        public void set(final String value) {
            if (mode == StorageMode.STATELESS) sendFull(name, value);
            else if (mode == StorageMode.ON_HEAP) setOnHeap(name, value, patch);
            else setOffHeap(name, value, patch);
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
            final boolean removed = switch (mode) {
                case ON_HEAP -> onHeapValues != null && onHeapValues.remove(name) != null;
                case OFF_HEAP -> offHeapValues != null && offHeapValues.remove(name);
                case STATELESS -> false;
            };
            if (removed && !notCreatedYet()) {
                saveUpdate(writer -> writer.write(ServerToClientModel.WC_REMOVE_PROPERTY, name));
            }
        }

        /** The storage is chosen before the first value: a value held would stay in the storage left. */
        private void requireNoValue(final String method) {
            final boolean held = onHeapValues != null && onHeapValues.get(name) != null
                    || offHeapValues != null && offHeapValues.readValue(name) != null;
            if (held) throw new IllegalStateException(method + " on property '" + name + "', which holds a value: choose the storage before the first set");
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

    /**
     * True until the CREATE has been written. Cached state (on/off-heap properties, attributes, event
     * subscriptions) is then only recorded: {@link #enrichForUpdate} sends its final value with the
     * CREATE. Stacking the update as well would send it twice (and every intermediate value).
     * Stateless properties and method calls are not cached, so they still go through saveUpdate.
     */
    private boolean notCreatedYet() {
        return !isInitialized();
    }

    private void sendFull(final String name, final String value) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.WC_SET_PROPERTY, name);
            writer.write(ServerToClientModel.WC_PROPERTY_VALUE, value);
        });
    }

    private void setOnHeap(final String name, final String value, final boolean patchEnabled) {
        if (onHeapValues == null) onHeapValues = new CompactStringMap();
        final String previous = onHeapValues.put(name, value);
        if (Objects.equals(previous, value) || notCreatedYet()) return;

        if (patchEnabled && previous != null && value != null) {
            sendPatchOrFull(name, value, previous);
        } else {
            sendFull(name, value);
        }
    }

    private void setOffHeap(final String name, final String value, final boolean patchEnabled) {
        if (offHeapValues == null) offHeapValues = new OffHeapJsonStore();
        if (value == null) {
            // The store holds no null: the value is dropped and, as on-heap, a full null is sent if one was held
            if (offHeapValues.remove(name) && !notCreatedYet()) sendFull(name, null);
            return;
        }

        if (offHeapValues.hasSameValue(name, value)) return;
        if (notCreatedYet()) {
            offHeapValues.put(name, value);
            return;
        }

        String previous = null;
        if (patchEnabled) previous = offHeapValues.readValue(name);

        offHeapValues.put(name, value);

        if (patchEnabled && previous != null) {
            sendPatchOrFull(name, value, previous);
        } else {
            sendFull(name, value);
        }
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
                // A SlotPanel has no CREATE of its own, so it is never "initialized": its window is set
                // when the slot is attached (which attaches the children present at that time). Testing
                // isInitialized() here dropped every child added once the web component was live.
                if (window != null) child.attach(window, getFrame());
                // Inject WC_SLOT_NAME alongside the standard TYPE_ADD
                child.saveAdd(child.getID(), PWebComponent.this.getID(),
                    new com.ponysdk.core.ui.model.ServerBinaryModel(ServerToClientModel.WC_SLOT_NAME, slotName));
            } else {
                throw new IllegalAccessError("Widget already attached to another window");
            }
        }

        @Override
        protected boolean attach(final PWindow window, final PFrame frame) {
            setFrame(frame);
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
            if (window != null) sp.attach(window, getFrame());
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
        super.init0(); // the children added at no slot
        // Propagate attach to all declared slots so their children get initialized
        if (slots != null) {
            for (final SlotPanel sp : slots.values()) {
                sp.attach(window, getFrame());
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
                writer.write(ServerToClientModel.WC_SET_PROPERTY, key);
                writer.write(ServerToClientModel.WC_PROPERTY_VALUE, offHeapValues.readValue(key)); // a key holds a value
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

    /**
     * Cached provider: the static {@code Json.createXxx()} helpers resolve the provider through a
     * ServiceLoader lookup on every call, which dominated the cost of small patches.
     */
    private static final JsonProvider JSON = JsonProvider.provider();

    /**
     * Computes a shallow RFC 7396 merge-patch turning {@code oldJson} into {@code newJson}.
     *
     * @return the patch, or {@code null} when a patch cannot represent the change and the caller must
     *         send the full value: either side is not a JSON object, nothing changed, or the new value
     *         holds an explicit {@code null} (in a merge-patch, null means "delete the key", so the
     *         client would drop it instead of setting it to null)
     */
    static String computeJsonPatch(final String oldJson, final String newJson) {
        // Cheap guard first: scalar / array properties are common and not mergeable — don't pay a
        // parse (and an exception) to find that out.
        if (!looksLikeJsonObject(oldJson) || !looksLikeJsonObject(newJson)) return null;
        try {
            final JsonObject oldObj = parseJson(oldJson);
            final JsonObject newObj = parseJson(newJson);
            if (oldObj == null || newObj == null) return null;

            final JsonObjectBuilder patch = JSON.createObjectBuilder();
            boolean hasDiff = false;

            // Keys in new but changed or added
            for (final Map.Entry<String, JsonValue> e : newObj.entrySet()) {
                final JsonValue newVal = e.getValue();
                if (!newVal.equals(oldObj.get(e.getKey()))) {
                    if (newVal.getValueType() == JsonValue.ValueType.NULL) return null; // see javadoc
                    patch.add(e.getKey(), newVal);
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

    /** True if the first non-whitespace char is '{' (a necessary condition for a JSON object). */
    private static boolean looksLikeJsonObject(final String json) {
        if (json == null) return false;
        for (int i = 0, n = json.length(); i < n; i++) {
            final char c = json.charAt(i);
            if (c == '{') return true;
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') return false;
        }
        return false;
    }

    private static JsonObject parseJson(final String json) {
        try (final JsonReader reader = JSON.createReader(new StringReader(json))) {
            return reader.readObject();
        } catch (final Exception e) {
            return null;
        }
    }

    // ======== JSON values of the classes generated from a Custom Elements Manifest (ADR-0010) ========

    /** A string as JSON text, quoted and escaped. */
    protected static String jsonOf(final String value) {
        return JSON.createValue(value).toString();
    }

    /** A number as JSON text, an integral value without a fraction: {@code 42}, not {@code 42.0}. */
    protected static String jsonOf(final double value) {
        return value == Math.rint(value) && Math.abs(value) < 1e15 ? Long.toString((long) value) : Double.toString(value);
    }

    /** The string a JSON text holds, null when it holds no string. */
    protected static String stringOf(final String json) {
        return readJson(json) instanceof JsonString string ? string.getString() : null;
    }

    /** The number a JSON text holds, null when it holds no number. */
    protected static Double numberOf(final String json) {
        return readJson(json) instanceof JsonNumber number ? number.doubleValue() : null;
    }

    /** The boolean a JSON text holds, null when it holds no boolean. */
    protected static Boolean booleanOf(final String json) {
        final JsonValue value = readJson(json);
        if (value == null) return null;
        return switch (value.getValueType()) {
            case TRUE -> Boolean.TRUE;
            case FALSE -> Boolean.FALSE;
            default -> null;
        };
    }

    /** The detail of a custom event the terminal sent, {@link JsonValue#NULL} when it has none. */
    private static JsonValue detailOf(final JsonObject event) {
        if (!(event.get(WC_EVENT_DETAIL_KEY) instanceof JsonString detail)) return JsonValue.NULL;
        final JsonValue value = readJson(detail.getString());
        return value != null ? value : JSON.createValue(detail.getString()); // not JSON: the terminal's String(detail)
    }

    private static JsonValue readJson(final String json) {
        if (json == null) return null;
        try (final JsonReader reader = JSON.createReader(new StringReader(json))) {
            return reader.readValue();
        } catch (final JsonException e) {
            return null;
        }
    }

    /** Sends the patch when it is smaller than the full value, the full value otherwise. */
    private void sendPatchOrFull(final String name, final String value, final String previous) {
        final String patch = computeJsonPatch(previous, value);
        if (patch != null && patch.length() < value.length()) {
            saveUpdate(writer -> {
                writer.write(ServerToClientModel.WC_PATCH_PROPERTY, name);
                writer.write(ServerToClientModel.WC_PROPERTY_VALUE, patch);
            });
        } else {
            sendFull(name, value);
        }
    }

    // ======== Event API ========

    /**
     * Listens to a custom event the element dispatches: the listener receives its {@code detail}, parsed;
     * {@link JsonValue#NULL} without one, a JSON string when the detail is not JSON
     * (docs/decisions/0011-one-way-to-listen.md). A DOM event read by named fields is
     * {@link #addEventListener(String, PEventOptions, Consumer)}.
     *
     * @param eventName the custom event's name, {@code count-changed}
     * @return removes the listener; the last one of a name removed, the terminal stops listening
     */
    public HandlerRegistration addEventListener(final String eventName, final Consumer<JsonValue> listener) {
        Objects.requireNonNull(eventName, "eventName");
        Objects.requireNonNull(listener, "listener");
        if (wcEventListeners == null) wcEventListeners = new HashMap<>(4);
        final List<Consumer<JsonValue>> listeners = wcEventListeners.computeIfAbsent(eventName, k -> new ArrayList<>(1));
        final boolean firstListener = listeners.isEmpty();
        listeners.add(listener);
        if (firstListener && !notCreatedYet()) {
            saveUpdate(writer -> writer.write(ServerToClientModel.WC_LISTEN_EVENT, eventName));
        }
        return () -> removeCustomEventListener(eventName, listener);
    }

    private void removeCustomEventListener(final String eventName, final Consumer<JsonValue> listener) {
        if (wcEventListeners == null) return;
        final List<Consumer<JsonValue>> listeners = wcEventListeners.get(eventName);
        if (listeners == null || !listeners.remove(listener) || !listeners.isEmpty()) return;
        wcEventListeners.remove(eventName);
        if (!notCreatedYet()) saveUpdate(writer -> writer.write(ServerToClientModel.WC_UNLISTEN_EVENT, eventName));
    }

    // ======== Method call API ========

    /**
     * Calls a method on the web component element (no arguments).
     */
    public void call(final String methodName) {
        saveUpdate(writer -> writer.write(ServerToClientModel.WC_CALL_METHOD, methodName));
    }

    /**
     * Calls a method on the web component element with these arguments: numbers, strings, booleans and null as they
     * are; a {@link JsonValue} object or array reaches the element parsed (as does a string that is a JSON object or array).
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
            final List<Consumer<JsonValue>> listeners = wcEventListeners != null ? wcEventListeners.get(event.getString(WC_EVENT_NAME_KEY)) : null;
            if (listeners == null) return;
            final JsonValue detail = detailOf(event);
            // Over a copy: a listener may remove itself or another
            for (final Consumer<JsonValue> listener : List.copyOf(listeners)) listener.accept(detail);
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
