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

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.ContextDestroyListener;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.basic.PCookies;
import com.ponysdk.core.ui.basic.PHistory;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.eventbus.*;
import com.ponysdk.core.ui.eventbus2.EventBus;
import com.ponysdk.core.ui.statistic.TerminalDataReceiver;
import com.ponysdk.core.writer.ModelWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.websocket.server.HandshakeRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * Provides a way to identify a user across more than one page request or visit to a Web site and to
 * store information about that user.
 * </p>
 */
public class UIContext {

    private static final Logger log = LoggerFactory.getLogger(UIContext.class);
    private static final ThreadLocal<UIContext> currentContext = new ThreadLocal<>();
    private static final AtomicInteger uiContextCount = new AtomicInteger();

    private final int ID;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Object> attributes = new HashMap<>();
    private final WebSocket socket;

    private final PObjectWeakHashMap pObjectWeakReferences = new PObjectWeakHashMap();
    private final ModelWriter writer;
    private final HandshakeRequest request;

    private int objectCounter = 1;

    private final Map<Integer, StreamHandler> streamListenerByID = new HashMap<>();
    private int streamRequestCounter = 0;

    private final PHistory history = new PHistory();
    private final RootEventBus rootEventBus = new RootEventBus();
    private final EventBus newEventBus = new EventBus();
    private final PCookies cookies = new PCookies();

    private final Set<ContextDestroyListener> destroyListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<DataListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private TerminalDataReceiver terminalDataReceiver;

    private volatile boolean alive = true;

    private final Latency latency = new Latency(10);

    public UIContext(final WebSocket socket, HandshakeRequest request, ApplicationConfiguration option) {
        this.ID = uiContextCount.incrementAndGet();
        this.socket = socket;
        this.writer = new ModelWriter(socket);
        this.request = request;
    }

    public static UIContext get() {
        return currentContext.get();
    }

    /**
     * Adds {@link EventHandler} to the {@link RootEventBus}
     *
     * @param type    the event type
     * @param handler the event handler
     * @return the HandlerRegistration in order to remove the EventHandler
     * @see #fireEvent(Event)
     */
    public static HandlerRegistration addHandler(final Event.Type type, final EventHandler handler) {
        return getRootEventBus().addHandler(type, handler);
    }

    /**
     * Fires an {@link Event} on the {@link RootEventBus}
     * Only {@link EventHandler}s added before fires event will be stimulated
     *
     * @param event the fired event
     * @see #addHandler(BroadcastEventHandler)
     */
    public static void fireEvent(final Event<? extends EventHandler> event) {
        getRootEventBus().fireEvent(event);
    }

    /**
     * Removes {@link EventHandler} from the {@link RootEventBus}
     *
     * @param type    the event type
     * @param handler the event handler
     * @see #addHandler(com.ponysdk.core.ui.eventbus.Event.Type, EventHandler)
     */
    public static void removeHandler(final Event.Type type, final EventHandler handler) {
        getRootEventBus().removeHandler(type, handler);
    }

    /**
     * Adds {@link EventHandler} to the {@link RootEventBus} with a specific source
     * Use {@link #fireEventFromSource(Event, Object)} to stimulate this event handler
     *
     * @param type    the event type
     * @param source  the source
     * @param handler the event handler
     * @return the {@link HandlerRegistration} in order to remove the {@link EventHandler}
     * @see #fireEventFromSource(Event, Object)
     */
    public static HandlerRegistration addHandlerToSource(final Event.Type type, final Object source, final EventHandler handler) {
        return getRootEventBus().addHandlerToSource(type, source, handler);
    }

    /**
     * Fires an {@link Event} on the {@link RootEventBus} with a specific source
     * Only {@link EventHandler}s added before fires event will be stimulated
     *
     * @param event  the fired event
     * @param source the source
     * @see #addHandlerToSource(com.ponysdk.core.ui.eventbus.Event.Type, Object, EventHandler)
     */
    public static void fireEventFromSource(final Event<? extends EventHandler> event, final Object source) {
        getRootEventBus().fireEventFromSource(event, source);
    }

    /**
     * Removes handler from the {@link RootEventBus} with a specific source
     *
     * @param type    the event type
     * @param source  the source
     * @param handler the event handler
     * @see #addHandlerToSource(com.ponysdk.core.ui.eventbus.Event.Type, Object, EventHandler)
     */
    public static void removeHandlerFromSource(final Event.Type type, final Object source, final EventHandler handler) {
        getRootEventBus().removeHandlerFromSource(type, source, handler);
    }

    /**
     * Adds a {@link BroadcastEventHandler} to the {@link RootEventBus} that receive all the events
     * All call to {@link #fireEvent(Event)} or {@link #fireEventFromSource(Event, Object)} will stimulate this event
     * handler
     *
     * @param handler the broadcast event handler
     * @see #fireEvent(Event)
     * @see #fireEventFromSource(Event, Object)
     */
    public static void addHandler(final BroadcastEventHandler handler) {
        getRootEventBus().addHandler(handler);
    }

    /**
     * Removes broadcast handler from the {@link RootEventBus}
     *
     * @param handler the broadcast event handler
     * @see #addHandler(BroadcastEventHandler)
     */
    public static void removeHandler(final BroadcastEventHandler handler) {
        getRootEventBus().removeHandler(handler);
    }

    /**
     * Gets the default {@link RootEventBus}
     *
     * @return the root event bus
     */
    public static RootEventBus getRootEventBus() {
        return get().rootEventBus;
    }

    /**
     * Gets the default {@link EventBus}
     *
     * @return the new event bus
     */
    public static EventBus getNewEventBus() {
        return get().newEventBus;
    }

    public int getID() {
        return ID;
    }

    public boolean addDataListener(final DataListener listener) {
        return listeners.add(listener);
    }

    public boolean removeDataListener(final DataListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Executes a {@link Runnable} that represents a task in a graphical context
     * <p>
     * This method locks the UIContext
     *
     * @param runnable the tasks
     */
    public boolean execute(final Runnable runnable) {
        if (!isAlive()) return false;
        if (log.isDebugEnabled()) log.debug("Pushing to #" + this);
        if (UIContext.get() != this) {
            acquire();
            try {
                runnable.run();
                flush();
                return true;
            } catch (final Throwable e) {
                log.error("Cannot process client instruction", e);
                return false;
            } finally {
                release();
            }
        } else {
            runnable.run();
            return true;
        }
    }


    //Recheck

    /**
     * Stimulates all {@link DataListener} with an object
     *
     * @param data the object
     */
    public void pushToClient(final Object data) {
        if (!isAlive()) return;
        if (data != null && !listeners.isEmpty()) {
            execute(() -> {
                try {
                    listeners.forEach(listener -> listener.onData(data));
                } catch (final Throwable e) {
                    log.error("Cannot send data", e);
                }
            });
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
            if (history != null) {
                history.fireHistoryChanged(jsonObject.getString(ClientToServerModel.TYPE_HISTORY.toStringValue()));
            }
        } else {
            final JsonValue jsonValue = jsonObject.get(ClientToServerModel.OBJECT_ID.toStringValue());
            int objectID;
            final ValueType valueType = jsonValue.getValueType();
            if (ValueType.NUMBER == valueType) {
                objectID = ((JsonNumber) jsonValue).intValue();
            } else if (ValueType.STRING == valueType) {
                objectID = Integer.parseInt(((JsonString) jsonValue).getString());
            } else {
                log.error("unknown reference from the browser. Unable to execute instruction: " + jsonObject);
                return;
            }

            //Cookies
            if (objectID == 0) {
                cookies.onClientData(jsonObject);
            } else {
                final PObject object = getObject(objectID);

                if (object == null) {
                    log.error("unknown reference from the browser. Unable to execute instruction: " + jsonObject);

                    if (jsonObject.containsKey(ClientToServerModel.PARENT_OBJECT_ID.toStringValue())) {
                        final int parentObjectID = jsonObject.getJsonNumber(ClientToServerModel.PARENT_OBJECT_ID.toStringValue())
                            .intValue();
                        final PObject gcObject = pObjectWeakReferences.get(parentObjectID);
                        log.warn(String.valueOf(gcObject));
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

    private void acquire() {
        lock.lock();
        currentContext.set(this);
    }

    private void release() {
        currentContext.remove();
        lock.unlock();
    }

    public void flush() {
        socket.flush();
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
        pObjectWeakReferences.put(pObject.getID(), pObject);
    }

    public PObject getObject(final int objectID) {
        return pObjectWeakReferences.get(objectID);
    }

    /**
     * Registers a {@link StreamHandler} that will be called on the terminal side
     *
     * @param streamListener the stream handler
     */
    public void stackStreamRequest(final StreamHandler streamListener) {
        final int streamRequestID = nextStreamRequestID();

        writer.beginObject();
        writer.write(ServerToClientModel.TYPE_ADD_HANDLER, -1);
        writer.write(ServerToClientModel.HANDLER_TYPE, HandlerModel.HANDLER_STREAM_REQUEST.getValue());
        writer.write(ServerToClientModel.STREAM_REQUEST_ID, streamRequestID);
        writer.endObject();

        streamListenerByID.put(streamRequestID, streamListener);
    }

    /**
     * Registers a {@link StreamHandler} that will be called on a specific {@link com.ponysdk.core.terminal.ui.PTObject}
     *
     * @param streamListener the stream handler
     * @param objectID       the object ID of a {@link PObject}
     */
    public void stackEmbeddedStreamRequest(final StreamHandler streamListener, final int objectID) {
        final int streamRequestID = nextStreamRequestID();

        writer.beginObject();
        final PObject pObject = getObject(objectID);
        if (!PWindow.isMain(pObject.getWindow()))
            writer.write(ServerToClientModel.WINDOW_ID, pObject.getWindow().getID());
        if (pObject.getFrame() != null) writer.write(ServerToClientModel.FRAME_ID, pObject.getFrame().getID());
        writer.write(ServerToClientModel.TYPE_ADD_HANDLER, objectID);
        writer.write(ServerToClientModel.HANDLER_TYPE, HandlerModel.HANDLER_EMBEDED_STREAM_REQUEST.getValue());
        writer.write(ServerToClientModel.STREAM_REQUEST_ID, streamRequestID);
        writer.endObject();

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
        return streamListenerByID.remove(streamID);
    }

    /**
     * Closes the current UIContext
     */
    public void close() {
        writer.beginObject();
        writer.write(ServerToClientModel.DESTROY_CONTEXT, null);
        writer.endObject();
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
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(final String name) {
        return (T) attributes.get(name);
    }

    /**
     * Destroys the UIContext
     * <p>
     * This method locks the UIContext
     */
    public void destroy() {
        if (!isAlive()) return;
        alive = false;
        doDestroy();
    }

    /**
     * Destroys effectively the UIContext
     */
    private void doDestroy() {
        destroyListeners.forEach(listener -> {
            try {
                listener.onBeforeDestroy(this);
            } catch (final Exception e) {
                log.error("Exception while destroying UIContext #" + getID(), e);
            }
        });
    }

    /**
     * Adds a {@link ContextDestroyListener} that will be stimulated when the UIContext is destroyed
     *
     * @param listener the context destroy listener
     * @since {@link #destroy()}
     */
    public void addContextDestroyListener(final ContextDestroyListener listener) {
        destroyListeners.add(listener);
    }

    /**
     * Sends the heart beat
     * <p>
     * This method locks the UIContext
     */
    public void sendHeartBeat() {
        acquire();
        try {
            socket.sendHeartBeat();
        } catch (final Throwable e) {
            log.error("Cannot send server heart beat to UIContext #" + getID(), e);
        } finally {
            release();
        }
    }

    /**
     * Sends the round trip
     * <p>
     * This method locks the UIContext
     */
    public void sendRoundTrip() {
        //begin();
        try {
            socket.sendRoundTrip();
        } catch (final Throwable e) {
            log.error("Cannot send server round trip to UIContext #" + getID(), e);
        } finally {
            //end();
        }
    }

    /**
     * Returns is UIContext alive ?
     *
     * @return alive state
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * Gets the {@link PHistory} of the UIContext
     *
     * @return the PHistory
     */
    public PHistory getHistory() {
        return history;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final UIContext uiContext = (UIContext) o;
        return ID == uiContext.ID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID);
    }

    @Override
    public String toString() {
        return "UIContext [ID=" + ID + ", alive=" + alive + "]";
    }

    /**
     * Adds a ping value
     *
     * @param pingValue the ping value
     */
    public void addPingValue(final long pingValue) {
        latency.add(pingValue);
    }

    /**
     * Gets an average latency from the last 10 measurements
     *
     * @return the lantency
     */
    public double getLatency() {
        return latency.getValue();
    }

    public ModelWriter getWriter() {
        return writer;
    }

    public static void beginObject() {
        get().writer.beginObject();
    }

    public static void write(final ServerToClientModel model) {
        get().writer.write(model, null);
    }

    public static void write(final ServerToClientModel model, final Object value) {
        get().writer.write(model, value);
    }

    public static void endObject() {
        get().writer.endObject();
    }

    public String getHistoryToken() {
        final List<String> historyTokens = this.request.getParameterMap().get(ClientToServerModel.TYPE_HISTORY.toStringValue());
        return !historyTokens.isEmpty() ? historyTokens.get(0) : null;
    }

    public boolean isDebugMode() {
        return false;
    }

    private static final class Latency {

        private int index = 0;
        private final long[] values;

        private Latency(final int size) {
            values = new long[size];
            for (int i = 0; i < values.length; i++) {
                values[i] = 0L;
            }
        }

        public void add(final long value) {
            values[index++] = value;
            if (index >= values.length) index = 0;
        }

        public double getValue() {
            double average = 0;
            for (final long value : values) {
                average += value;
            }
            return average / values.length;
        }
    }

}
