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

package com.ponysdk.core.server.context;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.util.component.Container;
import org.eclipse.jetty.websocket.core.Extension;
import org.eclipse.jetty.websocket.core.ExtensionStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.websocket.PonyPerMessageDeflateExtension;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.basic.PCookies;
import com.ponysdk.core.ui.basic.PHistory;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.eventbus.StreamHandler;
import com.ponysdk.core.ui.statistic.TerminalDataReceiver;
import com.ponysdk.core.useragent.UserAgent;
import com.ponysdk.core.util.PObjectCache;
import com.ponysdk.core.writer.ModelWriter;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;

/**
 * <p>
 * Provides a way to identify a user across more than one page request or visit to a Web site and to
 * store information about that user.
 * </p>
 */
public class UIContextImpl implements UIContext {
    private static final ThreadLocal<UIContextImpl> currentContext = new ThreadLocal<>();
    private static final Logger log = LoggerFactory.getLogger(UIContextImpl.class);
    private static final Logger loggerIn = LoggerFactory.getLogger("WebSocket-IN");
    private static final Logger loggerOut = LoggerFactory.getLogger("WebSocket-OUT");
    private static final AtomicInteger uiContextCount = new AtomicInteger();
    private static final String DEFAULT_PROVIDER = "org.glassfish.json.JsonProviderImpl";
    private static final int INITIAL_STREAM_MAP_CAPACITY = 2;
    private static final String MSG_RECEIVED = "Message received from terminal : UIContext #{} on {} : {}";

    private final int ID = uiContextCount.incrementAndGet();
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Object> attributes = new HashMap<>();
    private final PObjectCache pObjectCache = new PObjectCache();
    private final PHistory history = new PHistory();
    private final com.ponysdk.core.ui.eventbus.EventBus rootEventBus = new com.ponysdk.core.ui.eventbus.EventBus();
    private final com.ponysdk.core.ui.eventbus2.EventBus eventBus = new com.ponysdk.core.ui.eventbus2.EventBus();
    private final PCookies cookies = new PCookies();
    private final Latency roundtripLatency = new Latency(10);
    private final Latency networkLatency = new Latency(10);
    private final Latency terminalLatency = new Latency(10);
    private final ApplicationManager applicationManager;
    private final JsonProvider jsonProvider;
    private final ModelWriter modelWriter;
    private final List<UIContextDestroyListener> uiContextDestroyListeners = new ArrayList<>();
    private int objectCounter = 1;
    private Map<Integer, StreamHandler> streamListenerByID;
    private int streamRequestCounter = 0;
    private TerminalDataReceiver terminalDataReceiver;
    private long lastReceivedTime = System.currentTimeMillis();
    private CommunicationSanityChecker communicationSanityChecker;
    private WebSocket.Listener listener;
    private UIContextInstructionListener monitor;
    private long lastSentPing;
    private final UIContextScheduler scheduler;

    public UIContextImpl(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
        this.scheduler = new UIContextScheduler(this);
        this.modelWriter = new ModelWriter(this);

        JsonProvider provider;
        try {
            provider = (JsonProvider) Class.forName(DEFAULT_PROVIDER).getDeclaredConstructor().newInstance();
        } catch (final Throwable t) {
            provider = JsonProvider.provider();
        }

        jsonProvider = provider;
    }

    public static UIContextImpl get() {
        return currentContext.get();
    }

    public int getID() {
        return ID;
    }

    @Override
    public void start() {
        communicationSanityChecker = new CommunicationSanityChecker(this);
        communicationSanityChecker.start();
        scheduler.forceExecute(() -> currentContext.set(this));
        scheduler.start();
    }

    @Override
    public void stop() {
        communicationSanityChecker.stop();
        scheduler.stop();

        //TODO

        if (session.isOpen()) return;
        try {
            log.info("Closing websocket programmatically for UIContext #{}", ID);
            session.close();
        } finally {
            onClosed();
        }
    }

    @Override
    public void processInstruction(final String message) {
        if (this.listener != null) listener.onIncomingText(message);
        try {
            onMessageReceived();
            if (monitor != null) monitor.onMessageReceived(this, message);

            final JsonObject jsonObject;
            try (final JsonReader reader = jsonProvider.createReader(new StringReader(message))) {
                jsonObject = reader.readObject();
            }

            if (jsonObject.containsKey(ClientToServerModel.HEARTBEAT_REQUEST.toStringValue())) {
                sendHeartbeat();
            } else if (jsonObject.containsKey(ClientToServerModel.TERMINAL_LATENCY.toStringValue())) {
                processRoundTripLatency(jsonObject);
            } else if (jsonObject.containsKey(ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue())) {
                processInstructions(jsonObject);
            } else if (jsonObject.containsKey(ClientToServerModel.ERROR_MSG.toStringValue())) {
                processTerminalLog(jsonObject, ClientToServerModel.ERROR_MSG);
            } else if (jsonObject.containsKey(ClientToServerModel.WARN_MSG.toStringValue())) {
                processTerminalLog(jsonObject, ClientToServerModel.WARN_MSG);
            } else if (jsonObject.containsKey(ClientToServerModel.INFO_MSG.toStringValue())) {
                processTerminalLog(jsonObject, ClientToServerModel.INFO_MSG);
            } else {
                log.error("Unknown message from terminal #{} : {}", ID, message);
            }

            if (monitor != null) monitor.onMessageProcessed(this, message);
        } catch (final Throwable e) {
            log.error("Cannot process message from terminal  #{} : {}", ID, message, e);
        } finally {
            if (monitor != null) monitor.onMessageUnprocessed(this, message);
        }
    }

    @Override
    public com.ponysdk.core.ui.eventbus2.EventBus getEventBus() {
        return eventBus;
    }

    public void registerDestroyListener(UIContextDestroyListener listener) {
        uiContextDestroyListeners.add(listener);
    }

    public void unregisterDestroyListener(UIContextDestroyListener listener) {
        uiContextDestroyListeners.remove(listener);
    }

    /**
     * Gets the default {@link com.ponysdk.core.ui.eventbus.EventBus}
     *
     * @return the root event bus
     */
    public com.ponysdk.core.ui.eventbus.EventBus getRootEventBus() {
        return rootEventBus;
    }

    @Override
    public void executeAsync(final Runnable task) {
        if (isAlive()) return;
        scheduler.execute(() -> exec(task));
    }
    
    @Override
    public void forceExecuteAsync(final Runnable task) {
        if (isAlive()) return;
        scheduler.forceExecute(() -> exec(task));
    }


    @Override
    public ScheduledTaskHandler executeLaterAsync(Duration delay, Runnable task) {
        if (isAlive()) return null;
        return scheduler.executeLater(delay.toNanos(), () -> exec(task));
    }

    @Override
    public ScheduledTaskHandler scheduleAsync(Duration period, Runnable task) {
        if (isAlive()) return null;
        return scheduler.schedule(period.toNanos(), () -> exec(task));
    }
    
    private void exec(Runnable runnable) {
    	lock.lock();
		try {
		    runnable.run();
		    flush();
		} catch (final Throwable e) {
		    log.error("Cannot process client instruction", e);
		} finally {
		    lock.unlock();
		}
    }

    /**
     * Sends data to the targeted {@link PObject} from {@link JsonObject} instruction
     * Called from terminal side
     *
     * @param jsonObject the JSON instructions
     */
    public void fireClientData(final JsonObject jsonObject) {
        if (jsonObject.containsKey(ClientToServerModel.TYPE_HISTORY.toStringValue())) {
            history.fireHistoryChanged(jsonObject.getString(ClientToServerModel.TYPE_HISTORY.toStringValue()));
        } else {
            final JsonValue jsonValue = jsonObject.get(ClientToServerModel.OBJECT_ID.toStringValue());
            int objectID;
            final JsonValue.ValueType valueType = jsonValue.getValueType();
            if (JsonValue.ValueType.NUMBER == valueType) {
                objectID = ((JsonNumber) jsonValue).intValue();
            } else if (JsonValue.ValueType.STRING == valueType) {
                objectID = Integer.parseInt(((JsonString) jsonValue).getString());
            } else {
                log.error("unknown reference from the browser. Unable to execute instruction: {}", jsonObject);
                return;
            }

            //Cookies
            if (objectID == 0) {
                cookies.onClientData(jsonObject);
            } else {
                final PObject object = getObject(objectID);

                if (object == null) {
                    log.error("unknown reference from the browser. Unable to execute instruction: {}", jsonObject);

                    if (jsonObject.containsKey(ClientToServerModel.PARENT_OBJECT_ID.toStringValue())) {
                        final int parentObjectID = jsonObject.getJsonNumber(ClientToServerModel.PARENT_OBJECT_ID.toStringValue()).intValue();
                        final PObject gcObject = getObject(parentObjectID);
                        if (log.isWarnEnabled()) log.warn(String.valueOf(gcObject));
                    }

                    return;
                }

                if (terminalDataReceiver != null) terminalDataReceiver.onDataReceived(object, jsonObject);

                object.onClientData(jsonObject);
            }
        }
    }

    /**
     * Sets a {@link TerminalDataReceiver}
     *
     * @param terminalDataReceiver the terminal data receiver
     */
    public void setTerminalDataReceiver(final TerminalDataReceiver terminalDataReceiver) {
        this.terminalDataReceiver = terminalDataReceiver;
    }

    /**
     * Generates the new {@link PObject} id
     *
     * @return the new ID
     */
    public int nextID() {
        return objectCounter++;
    }

    /**
     * Registers a {@link PObject} in the UIContext
     *
     * @param pObject the pObject
     * @see #getObject(int)
     */
    public void registerObject(final PObject pObject) {
        pObjectCache.add(pObject);
    }

    /**
     * Gets the {@link PObject} with a specific object ID
     *
     * @param objectID the object ID of the searched {@link PObject}
     * @return the {@link PObject} or null if not found
     * @see #registerObject(PObject)
     */
    public PObject getObject(final int objectID) {
        return pObjectCache.get(objectID);
    }

    /**
     * Registers a {@link StreamHandler} that will be called on the terminal side
     *
     * @param streamListener the stream handler
     */
    public void stackStreamRequest(final StreamHandler streamListener) {
        stackStreamRequest(streamListener, PWindow.getMain());
    }

    /**
     * Registers a {@link StreamHandler} that will be called on the terminal side
     *
     * @param streamListener the stream handler
     * @param window         Window target
     */
    public void stackStreamRequest(final StreamHandler streamListener, final PWindow window) {
        final int streamRequestID = nextStreamRequestID();

        final ModelWriter writer = getWriter();
        writer.beginObject(window);
        writer.write(ServerToClientModel.TYPE_ADD_HANDLER, 0);
        writer.write(ServerToClientModel.HANDLER_TYPE, HandlerModel.HANDLER_STREAM_REQUEST.getValue());
        writer.write(ServerToClientModel.STREAM_REQUEST_ID, streamRequestID);
        writer.endObject();

        if (streamListenerByID == null) streamListenerByID = new HashMap<>(INITIAL_STREAM_MAP_CAPACITY);
        streamListenerByID.put(streamRequestID, streamListener);
    }

    /**
     * Registers a {@link StreamHandler} that will be called on a specific {@link com.ponysdk.core.terminal.ui.PTObject}
     *
     * @param streamListener the stream handler
     * @param pObject        the {@link PObject}
     */
    public void stackEmbeddedStreamRequest(final StreamHandler streamListener, final PObject pObject) {
        final int streamRequestID = nextStreamRequestID();

        final ModelWriter writer = getWriter();
        writer.beginObject(pObject.getWindow());
        if (pObject.getFrame() != null) writer.write(ServerToClientModel.FRAME_ID, pObject.getFrame().getID());
        writer.write(ServerToClientModel.TYPE_ADD_HANDLER, pObject.getID());
        writer.write(ServerToClientModel.HANDLER_TYPE, HandlerModel.HANDLER_EMBEDDED_STREAM_REQUEST.getValue());
        writer.write(ServerToClientModel.STREAM_REQUEST_ID, streamRequestID);
        writer.endObject();

        if (streamListenerByID == null) streamListenerByID = new HashMap<>(INITIAL_STREAM_MAP_CAPACITY);
        streamListenerByID.put(streamRequestID, streamListener);
    }

    /**
     * Generates the new stream id
     *
     * @return the new ID
     */
    private int nextStreamRequestID() {
        return streamRequestCounter++;
    }

    /**
     * Removes the {@link StreamHandler} with a specific ID in the current UIContext
     *
     * @param streamID the stream ID
     * @return the removed stream handler or null if not found
     */
    public StreamHandler removeStreamListener(final int streamID) {
        return streamListenerByID != null ? streamListenerByID.remove(streamID) : null;
    }

    /**
     * Binds an object to this session, using the name specified. If an object of the same name is already bound to the
     * session, the object is replaced.
     * <p>
     * If the value passed in is null, this has the same effect as calling {@link #removeAttribute(String)}.
     *
     * @param name  the name to which the object is bound; cannot be null
     * @param value the object to be bound
     */
    public void setAttribute(final String name, final Object value) {
        if (value == null) removeAttribute(name);
        else attributes.put(name, value);
    }

    /**
     * Removes the object bound with the specified name from this session. If
     * the session does not have an object bound with the specified name, this
     * method does nothing.
     *
     * @param name the name of the object to remove from this session
     */
    public Object removeAttribute(final String name) {
        return attributes.remove(name);
    }

    /**
     * Returns the object bound with the specified name in this session, or <code>null</code> if no
     * object is bound under the name.
     *
     * @param name a string specifying the name of the object
     * @return the object with the specified name
     */
    public <T> T getAttribute(final String name) {
        return (T) attributes.get(name);
    }

    void onClosed() {
        unregisterUIContext();
    }

    /**
     * Sends the round trip
     * <p>
     * This method locks the UIContext
     */
    public void sendRoundTrip() {
        socket.sendRoundTrip();
    }

    /**
     * Returns the alive state of the current UIContext
     *
     * @return the alive state
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * Gets the {@link ApplicationConfiguration} of the UIContext
     *
     * @return The configuration
     */
    public ApplicationConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Gets the {@link PHistory} of the UIContext
     *
     * @return the PHistory
     */
    public PHistory getHistory() {
        return history;
    }

    public String getHistoryToken() {
        final List<String> historyTokens = this.request.getParameterMap().get(ClientToServerModel.TYPE_HISTORY.toStringValue());
        return historyTokens != null && !historyTokens.isEmpty() ? historyTokens.get(0) : null;
    }

    /**
     * Gets the {@link PCookies} of the UIContext
     *
     * @return the PCookies
     */
    public PCookies getCookies() {
        return cookies;
    }

    /**
     * Get a cookie from the {@link PCookies} of the UIContext
     *
     * @return the cookie
     */
    public String getCookie(final String name) {
        return getCookies().getCookie(name);
    }

    public void onMessageReceived() {
        lastReceivedTime = System.currentTimeMillis();
    }

    public long getLastReceivedTime() {
        return lastReceivedTime;
    }

    public UserAgent getUserAgent() {
        return UserAgent.parseUserAgentString(request.getHeader("User-Agent"));
    }

    public void setWebSocketListener(final WebSocket.Listener listener) {
        socket.setListener(listener);
    }

    public ModelWriter getWriter() {
        return modelWriter;
    }

    public JsonProvider getJsonProvider() {
        return jsonProvider;
    }

    public JettyServerUpgradeRequest getRequest() {
        return request;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final UIContextImpl uiContext = (UIContextImpl) o;
        return ID == uiContext.ID;
    }

    @Override
    public int hashCode() {
        return ID;
    }

    @Override
    public String toString() {
        return "UIContext [ID=" + ID + ", alive=" + alive + "]";
    }

    /**
     * Adds a roundtrip latency value
     *
     * @param value the value
     */
    public void addRoundtripLatencyValue(final long value) {
        roundtripLatency.add(value);
    }

    /**
     * Gets an average roundtrip latency from the last 10 measurements
     *
     * @return the lantency
     */
    public double getRoundtripLatency() {
        return roundtripLatency.getValue();
    }

    /**
     * Adds a network latency value
     *
     * @param value the value
     */
    public void addNetworkLatencyValue(final long value) {
        networkLatency.add(value);
    }

    /**
     * Gets an average network latency from the last 10 measurements
     *
     * @return the lantency
     */
    public double getNetworkLatency() {
        return networkLatency.getValue();
    }

    /**
     * Adds a terminal latency value
     *
     * @param value the ping value
     */
    public void addTerminalLatencyValue(final long value) {
        terminalLatency.add(value);
    }

    /**
     * Gets an average terminal latency from the last 10 measurements
     *
     * @return the lantency
     */
    public double getTerminalLatency() {
        return terminalLatency.getValue();
    }

    private void unregisterUIContext() {
        contexts.remove(getID());
        for (UIContextDestroyListener listener : uiContextDestroyListeners) {
            try {
                listener.onUIContextDestroyed(this);
            } catch (Exception e) {
                log.error("Exception while notifying the removal of UIContext#" + getID(), e);
            }
        }
        uiContextDestroyListeners.clear();
        communicationSanityChecker.stop();
    }

    private void processRoundTripLatency(final JsonObject jsonObject) {
        final long roundTripLatency = TimeUnit.MILLISECONDS.convert(System.nanoTime() - lastSentPing, TimeUnit.NANOSECONDS);
        log.trace("RoundTrip measurement : {} ms from terminal #{}", roundTripLatency, ID);
        addRoundtripLatencyValue(roundTripLatency);

        final long terminalLatency = jsonObject.getJsonNumber(ClientToServerModel.TERMINAL_LATENCY.toStringValue()).longValue();
        log.trace("Terminal measurement : {} ms from terminal #{}", terminalLatency, ID);
        addTerminalLatencyValue(terminalLatency);

        final long networkLatency = roundTripLatency - terminalLatency;
        log.trace("Network measurement : {} ms from terminal #{}", networkLatency, ID);
        addNetworkLatencyValue(networkLatency);
    }

    private void processInstructions(final JsonObject jsonObject) {
        final String applicationInstructions = ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue();
        loggerIn.trace("UIContext #{} : {}", ID, jsonObject);
        execute(() -> {
            final JsonArray appInstructions = jsonObject.getJsonArray(applicationInstructions);
            for (int i = 0; i < appInstructions.size(); i++) {
                fireClientData(appInstructions.getJsonObject(i));
            }
        });
    }

    private void processTerminalLog(final JsonObject json, final ClientToServerModel level) {
        final String message = json.getJsonString(level.toStringValue()).getString();
        String objectInformation = "";

        if (json.containsKey(ClientToServerModel.OBJECT_ID.toStringValue())) {
            final PObject object = getObject(json.getJsonNumber(ClientToServerModel.OBJECT_ID.toStringValue()).intValue());
            objectInformation = object == null ? "NA" : object.toString();
        }

        switch (level) {
            case INFO_MSG:
                if (log.isInfoEnabled()) log.info(MSG_RECEIVED, ID, objectInformation, message);
                break;
            case WARN_MSG:
                if (log.isWarnEnabled()) log.warn(MSG_RECEIVED, ID, objectInformation, message);
                break;
            case ERROR_MSG:
                if (log.isErrorEnabled()) log.error(MSG_RECEIVED, ID, objectInformation, message);
                break;
            default:
                log.error("Unknown log level during terminal log processing : {}", level);
        }
    }

    /**
     * Send round trip to the client
     */
    public void sendRoundTrip() {
        if (isAlive() && isSessionOpen()) {
            lastSentPing = System.nanoTime();
            beginObject();
            encode(ServerToClientModel.ROUNDTRIP_LATENCY, null);
            endObject();
            flush0();
        }
    }

    private void sendHeartbeat() {
        if (!isAlive() || !isSessionOpen()) return;
        beginObject();
        encode(ServerToClientModel.HEARTBEAT, null);
        endObject();
        flush0();
    }

    public void flush() {
        if (!session.isOpen()) return;
        try {
            websocketPusher.flush();
        } catch (final IOException e) {
            log.error("Can't write on the websocket for UIContext #{}, so we destroy the application", ID, e);
            close();
        }
    }

    @Override
    public void beginObject() {
        // Nothing to do
    }

    @Override
    public void endObject() {
        encode(ServerToClientModel.END, null);
    }

    @Override
    public void encode(final ServerToClientModel model, final Object value) {
        try {
            if (loggerOut.isTraceEnabled())
                loggerOut.trace("UIContext #{} : {} {}", ID, model, value);
            websocketPusher.encode(model, value);
            if (listener != null) listener.onOutgoingPonyFrame(model, value);
        } catch (final IOException e) {
            log.error("Cannot encode on the websocket for the UIContext #{}, UIContext will be close", ID, e);
            close();
        }
    }

    public void setMonitor(final UIContextInstructionListener monitor) {
        this.monitor = monitor;
    }

    public void setListener(final WebSocket.Listener listener) {
        this.listener = listener;
        this.websocketPusher.setWebSocketListener(listener);
        if (!(session instanceof Container)) {
            log.warn("Unrecognized session type {} for {}", session == null ? null : session.getClass(), uiContext);
            return;
        }
        final ExtensionStack extensionStack = ((Container) session).getBean(ExtensionStack.class);
        if (extensionStack == null) {
            log.warn("No Extension Stack for {}", uiContext);
            return;
        }

        PonyPerMessageDeflateExtension ponyExtension = null;

        for (Extension extension : extensionStack.getExtensions()) {
            if (extension.getClass().equals(PonyPerMessageDeflateExtension.class)) {
                ponyExtension = (PonyPerMessageDeflateExtension) extension;
                break;
            }
        }

        if (ponyExtension == null) {
            log.warn("Missing PonyPerMessageDeflateExtension from Extension Stack for {}", uiContext);
            return;
        }
        ponyExtension.setWebSocketListener(listener);
    }

    public interface Listener {

        void onOutgoingPonyFrame(ServerToClientModel model, Object value);

        void onOutgoingPonyFramesBytes(int bytes);

        void onOutgoingWebSocketFrame(int headerLength, int payloadLength);

        void onIncomingText(String text);

        void onIncomingWebSocketFrame(int headerLength, int payloadLength);

    }

    private static final class Latency {

        private final long[] values;
        private int index = 0;

        private Latency(final int size) {
            values = new long[size];
            Arrays.fill(values, 0);
        }

        public void add(final long value) {
            values[index++] = value;
            if (index >= values.length) index = 0;
        }

        public double getValue() {
            return Arrays.stream(values).average().orElse(0);
        }
    }
}
