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

package com.ponysdk.core.ui.fdc3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.PAddOn;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * FDC3 from Java: the desktop agent of the page, {@code window.fdc3} (docs/decisions/0014-fdc3.md). The application
 * lists {@code script/fdc3-addon.js} with its scripts.
 * <pre>
 * final Fdc3 fdc3 = Fdc3.get();
 * fdc3.addContextListener("fdc3.instrument", instrument -&gt; chart.show(instrument.getJsonObject("id").getString("ticker")));
 * row.addClickHandler(e -&gt; fdc3.broadcast(Fdc3.instrument("AAPL", "Apple")));
 * fdc3.addIntentListener("ViewChart", context -&gt; { chart.show(context); return JsonValue.NULL; });
 * </pre>
 * Listeners are called on the context's UI thread; each comes with its {@link HandlerRegistration}. A call made before
 * the agent is ready waits for it in the browser.
 */
public final class Fdc3 extends PAddOn {

    private static final Logger log = LoggerFactory.getLogger(Fdc3.class);

    private static final String KEY = Fdc3.class.getName();
    private static final String NATIVE = ClientToServerModel.NATIVE.toStringValue();
    /** The key of the listeners of every context type, as the JavaScript names it. */
    private static final String ANY_TYPE = "*";

    /** Where the desktop agent is. */
    public enum State {
        /** Looked for: {@code window.fdc3} or its {@code fdc3Ready} event. */
        WAITING,
        READY,
        /** No agent in the page within 5 seconds. */
        UNAVAILABLE
    }

    /**
     * What came back of a raised intent: the application that handled it, and its result ({@link JsonValue#NULL} for
     * none), or the error the agent gave (FDC3's {@code ResolveError}, {@code NoAppsFound} among them).
     */
    public record IntentResult(String app, JsonValue result, String error) {

        public boolean failed() {
            return error != null;
        }
    }

    private State state = State.WAITING;
    private JsonObject info;
    private List<JsonObject> userChannels = List.of();
    private String currentChannel;
    private final List<Consumer<State>> stateListeners = new ArrayList<>(1);
    private final List<Consumer<String>> channelListeners = new ArrayList<>(1);
    private final Map<String, List<Consumer<JsonObject>>> contextListeners = new HashMap<>(4);
    private final Map<String, Function<JsonObject, JsonValue>> intentHandlers = new HashMap<>(4);
    private final Map<Integer, Consumer<IntentResult>> raised = new HashMap<>(4);
    private int nextRaise;

    private Fdc3() {
    }

    /** The current context's, attached to its main window on the first call. */
    public static Fdc3 get() {
        final UIContext uiContext = UIContext.get();
        if (uiContext == null) throw new IllegalStateException("Fdc3.get() needs a UIContext: call it on the UI thread");
        final Fdc3 existing = uiContext.getAttribute(KEY);
        if (existing != null) return existing;
        final Fdc3 fdc3 = new Fdc3();
        uiContext.setAttribute(KEY, fdc3);
        fdc3.attach(PWindow.getMain());
        return fdc3;
    }

    /** An {@code fdc3.instrument} context. */
    public static JsonObject instrument(final String ticker, final String name) {
        final var context = Json.createObjectBuilder().add("type", "fdc3.instrument");
        if (name != null) context.add("name", name);
        return context.add("id", Json.createObjectBuilder().add("ticker", ticker)).build();
    }

    public State getState() {
        return state;
    }

    /** Called with each state the agent reaches; at once with the current one. */
    public HandlerRegistration addStateListener(final Consumer<State> listener) {
        Objects.requireNonNull(listener, "listener");
        stateListeners.add(listener);
        listener.accept(state);
        return () -> stateListeners.remove(listener);
    }

    /** The agent's {@code ImplementationMetadata} ({@code fdc3Version}, {@code provider}…), null until ready. */
    public JsonObject getInfo() {
        return info;
    }

    /** The agent's user channels, each with its {@code id} and {@code displayMetadata}; empty until ready. */
    public List<JsonObject> getUserChannels() {
        return userChannels;
    }

    /** The id of the user channel this application is on, null on none. */
    public String getCurrentChannel() {
        return currentChannel;
    }

    /** Called with the id of each channel this application joins, null when it leaves. */
    public HandlerRegistration addChannelListener(final Consumer<String> listener) {
        Objects.requireNonNull(listener, "listener");
        channelListeners.add(listener);
        return () -> channelListeners.remove(listener);
    }

    public void joinUserChannel(final String channelId) {
        callTerminalMethod("joinUserChannel", Objects.requireNonNull(channelId, "channelId"));
    }

    public void leaveCurrentChannel() {
        callTerminalMethod("leaveCurrentChannel");
    }

    /** Broadcasts a context on the current channel: the other applications of the channel receive it. */
    public void broadcast(final JsonObject context) {
        callTerminalMethod("broadcast", requireContext(context).toString());
    }

    /**
     * Called with each context of this type the channel brings (the channel's current one when this application joins
     * it).
     *
     * @param contextType an FDC3 type ({@code fdc3.instrument}), or null for every type
     * @return removes the listener; the last one of a type removed, the agent is no longer listened to for it
     */
    public HandlerRegistration addContextListener(final String contextType, final Consumer<JsonObject> listener) {
        Objects.requireNonNull(listener, "listener");
        final String type = contextType == null ? ANY_TYPE : contextType;
        final List<Consumer<JsonObject>> listeners = contextListeners.computeIfAbsent(type, t -> new ArrayList<>(1));
        if (listeners.isEmpty()) callTerminalMethod("listenContext", type);
        listeners.add(listener);
        return () -> {
            final List<Consumer<JsonObject>> current = contextListeners.get(type);
            if (current == null || !current.remove(listener) || !current.isEmpty()) return;
            contextListeners.remove(type);
            callTerminalMethod("unlistenContext", type);
        };
    }

    /** Raises an intent; the agent picks the application that handles it. */
    public void raiseIntent(final String intent, final JsonObject context) {
        raiseIntent(intent, context, null);
    }

    /** Raises an intent, then calls back with the application that handled it and its result, or the error. */
    public void raiseIntent(final String intent, final JsonObject context, final Consumer<IntentResult> callback) {
        Objects.requireNonNull(intent, "intent");
        final int id = ++nextRaise;
        if (callback != null) raised.put(id, callback);
        callTerminalMethod("raiseIntent", id, intent, requireContext(context).toString());
    }

    /**
     * Handles an intent raised by another application: the handler's value is the intent's result,
     * {@link JsonValue#NULL} for none. One handler per intent, as in FDC3.
     *
     * @return removes the handler; the agent no longer routes the intent here
     */
    public HandlerRegistration addIntentListener(final String intent, final Function<JsonObject, JsonValue> handler) {
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(handler, "handler");
        if (intentHandlers.putIfAbsent(intent, handler) != null) {
            throw new IllegalStateException("The intent " + intent + " has a handler already: remove it first");
        }
        callTerminalMethod("listenIntent", intent);
        return () -> {
            if (intentHandlers.remove(intent, handler)) callTerminalMethod("unlistenIntent", intent);
        };
    }

    private static JsonObject requireContext(final JsonObject context) {
        Objects.requireNonNull(context, "context");
        if (!(context.get("type") instanceof JsonString)) throw new IllegalArgumentException("An FDC3 context has a type: " + context);
        return context;
    }

    @Override
    public void onClientData(final JsonObject event) {
        if (destroy) return;
        if (!event.containsKey(NATIVE)) {
            super.onClientData(event);
            return;
        }
        final JsonObject message = event.getJsonObject(NATIVE);
        if (message.containsKey("state")) onState(message);
        else if (message.containsKey("intent")) onIntent(message); // before "context": an intent carries its context
        else if (message.containsKey("context")) onContext(message.getString("listener"), message.getJsonObject("context"));
        else if (message.containsKey("raised")) onRaised(message);
        else if (message.containsKey("channel")) onChannel(stringOrNull(message, "channel"));
        else super.onClientData(event);
    }

    private void onState(final JsonObject message) {
        if ("ready".equals(message.getString("state"))) {
            info = message.getJsonObject("info");
            userChannels = message.getJsonArray("userChannels").getValuesAs(JsonObject.class);
            currentChannel = stringOrNull(message, "channel");
            state = State.READY;
        } else {
            state = State.UNAVAILABLE;
            log.info("No FDC3 desktop agent in the page of UIContext #{}", UIContext.get().getID());
        }
        List.copyOf(stateListeners).forEach(listener -> listener.accept(state));
    }

    private void onContext(final String type, final JsonObject context) {
        final List<Consumer<JsonObject>> listeners = contextListeners.get(type);
        if (listeners != null) List.copyOf(listeners).forEach(listener -> listener.accept(context));
    }

    private void onIntent(final JsonObject message) {
        final int request = message.getInt("request");
        final Function<JsonObject, JsonValue> handler = intentHandlers.get(message.getString("intent"));
        if (handler == null) {
            callTerminalMethod("intentFailed", request, "No handler");
            return;
        }
        try {
            final JsonValue result = handler.apply(message.getJsonObject("context"));
            callTerminalMethod("intentResult", request, result == null || result == JsonValue.NULL ? "null" : result.toString());
        } catch (final RuntimeException e) {
            log.error("The handler of the intent {} failed", message.getString("intent"), e);
            callTerminalMethod("intentFailed", request, String.valueOf(e.getMessage()));
        }
    }

    private void onRaised(final JsonObject message) {
        final Consumer<IntentResult> callback = raised.remove(message.getInt("raised"));
        if (callback == null) return;
        final JsonValue result = message.containsKey("result") ? message.get("result") : JsonValue.NULL;
        callback.accept(new IntentResult(stringOrNull(message, "app"), result, stringOrNull(message, "error")));
    }

    private void onChannel(final String channel) {
        currentChannel = channel;
        List.copyOf(channelListeners).forEach(listener -> listener.accept(channel));
    }

    private static String stringOrNull(final JsonObject message, final String key) {
        return message.get(key) instanceof JsonString s ? s.getString() : null;
    }
}
