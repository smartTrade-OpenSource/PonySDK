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

package com.ponysdk.core.server.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.AlreadyDestroyedApplication;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.ui.basic.PCookies;
import com.ponysdk.core.ui.basic.PHistory;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.eventbus.BroadcastEventHandler;
import com.ponysdk.core.ui.eventbus.Event;
import com.ponysdk.core.ui.eventbus.EventHandler;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.ui.eventbus.RootEventBus;
import com.ponysdk.core.ui.eventbus.StreamHandler;
import com.ponysdk.core.ui.eventbus2.EventBus;
import com.ponysdk.core.ui.statistic.TerminalDataReceiver;
import com.ponysdk.core.writer.ModelWriter;

/**
 * <p>
 * Provides a way to identify a user across more than one page request or visit to a Web site and to
 * store information about that user.
 * </p>
 * <p>
 * There is ONE unique UIContext for each screen displayed. Each UIContext is bound to the current
 * {@link Application} .
 * </p>
 */
public class UIContext {

    private static final ThreadLocal<UIContext> currentContext = new ThreadLocal<>();

    private static final Logger log = LoggerFactory.getLogger(UIContext.class);

    private static final AtomicInteger uiContextCount = new AtomicInteger();

    private final int ID;
    private final Application application;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Object> attributes = new HashMap<>();

    private final PObjectWeakHashMap pObjectWeakReferences = new PObjectWeakHashMap();
    private int objectCounter = 1;

    private final Map<Integer, StreamHandler> streamListenerByID = new HashMap<>();
    private int streamRequestCounter = 0;

    private final PHistory history = new PHistory();
    private final RootEventBus rootEventBus = new RootEventBus();
    private final EventBus newEventBus = new EventBus();

    private final PCookies cookies = new PCookies();

    private final List<ContextDestroyListener> destroyListeners = new ArrayList<>();
    private final TxnContext context;
    private final Set<DataListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private TerminalDataReceiver terminalDataReceiver;

    private boolean living = true;

    private final ConcurrentBoundedLinkedQueue<Long> pings = new ConcurrentBoundedLinkedQueue<>(10);

    /**
     * The default constructor
     *
     * @param context
     *            the transaction context
     */
    public UIContext(final TxnContext context) {
        this.application = context.getApplication();
        this.ID = uiContextCount.incrementAndGet();
        this.context = context;
    }

    /**
     * Returns the current UIContext
     *
     * @return The current UIContext
     */
    public static final UIContext get() {
        return currentContext.get();
    }

    /**
     * Removed the current UIContext
     */
    public static final void remove() {
        currentContext.remove();
    }

    /**
     * Sets the current UIContext
     *
     * @param uiContext
     *            the UIContext
     */
    public static final void setCurrent(final UIContext uiContext) {
        currentContext.set(uiContext);
    }

    /**
     * Adds {@link EventHandler} to the {@link RootEventBus}
     *
     * @param type
     *            the event type
     * @param handler
     *            the event handler
     * @return the HandlerRegistration in order to remove the EventHandler
     * @see #fireEvent(Event)
     */
    public static final HandlerRegistration addHandler(final Event.Type type, final EventHandler handler) {
        return getRootEventBus().addHandler(type, handler);
    }

    /**
     * Fires an {@link Event} on the {@link RootEventBus}
     * Only {@link EventHandler}s added before fires event will be stimulated
     *
     * @param event
     *            the fired event
     * @see #addHandler(BroadcastEventHandler)
     */
    public static final void fireEvent(final Event<? extends EventHandler> event) {
        getRootEventBus().fireEvent(event);
    }

    /**
     * Removes {@link EventHandler} from the {@link RootEventBus}
     *
     * @param type
     *            the event type
     * @param handler
     *            the event handler
     * @see #addHandler(com.ponysdk.core.ui.eventbus.Event.Type, EventHandler)
     */
    public static final void removeHandler(final Event.Type type, final EventHandler handler) {
        getRootEventBus().removeHandler(type, handler);
    }

    /**
     * Adds {@link EventHandler} to the {@link RootEventBus} with a specific source
     * Use {@link #fireEventFromSource(Event, Object)} to stimulate this event handler
     *
     * @param type
     *            the event type
     * @param source
     *            the source
     * @param handler
     *            the event handler
     * @return the {@link HandlerRegistration} in order to remove the {@link EventHandler}
     * @see #fireEventFromSource(Event, Object)
     */
    public static final HandlerRegistration addHandlerToSource(final Event.Type type, final Object source,
                                                               final EventHandler handler) {
        return getRootEventBus().addHandlerToSource(type, source, handler);
    }

    /**
     * Fires an {@link Event} on the {@link RootEventBus} with a specific source
     * Only {@link EventHandler}s added before fires event will be stimulated
     *
     * @param event
     *            the fired event
     * @param source
     *            the source
     * @see #addHandlerToSource(com.ponysdk.core.ui.eventbus.Event.Type, Object, EventHandler)
     */
    public static final void fireEventFromSource(final Event<? extends EventHandler> event, final Object source) {
        getRootEventBus().fireEventFromSource(event, source);
    }

    /**
     * Removes handler from the {@link RootEventBus} with a specific source
     *
     * @param type
     *            the event type
     * @param source
     *            the source
     * @param handler
     *            the event handler
     * @see #addHandlerToSource(com.ponysdk.core.ui.eventbus.Event.Type, Object, EventHandler)
     */
    public static final void removeHandlerFromSource(final Event.Type type, final Object source, final EventHandler handler) {
        getRootEventBus().removeHandlerFromSource(type, source, handler);
    }

    /**
     * Adds a {@link BroadcastEventHandler} to the {@link RootEventBus} that receive all the events
     * All call to {@link #fireEvent(Event)} or {@link #fireEventFromSource(Event, Object)} will stimulate this event
     * handler
     *
     * @param handler
     *            the broadcast event handler
     * @see #fireEvent(Event)
     * @see #fireEventFromSource(Event, Object)
     */
    public static final void addHandler(final BroadcastEventHandler handler) {
        getRootEventBus().addHandler(handler);
    }

    /**
     * Removes broadcast handler from the {@link RootEventBus}
     *
     * @param handler
     *            the broadcast event handler
     * @see #addHandler(BroadcastEventHandler)
     */
    public static final void removeHandler(final BroadcastEventHandler handler) {
        getRootEventBus().removeHandler(handler);
    }

    /**
     * Gets the default {@link RootEventBus}
     *
     * @return the root event bus
     */
    public static final RootEventBus getRootEventBus() {
        return get().rootEventBus;
    }

    /**
     * Gets the default {@link EventBus}
     *
     * @return the new event bus
     */
    public static final EventBus getNewEventBus() {
        return get().newEventBus;
    }

    /**
     * Gets the ID of the UIContext
     *
     * @return the ID
     */
    public int getID() {
        return ID;
    }

    /**
     * Adds a {@link DataListener} to the UIContext
     *
     * @param listener
     *            the data listener
     */
    public boolean addDataListener(final DataListener listener) {
        return listeners.add(listener);
    }

    /**
     * Removes the {@link DataListener} from UIContext
     *
     * @param listener
     *            the data listener
     */
    public boolean removeDataListener(final DataListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Executes a {@link Runnable} that represents a task in a graphical context
     *
     * This method locks the UIContext
     *
     * @param runnable
     *            the tasks
     */
    public void execute(final Runnable runnable) {
        if (log.isDebugEnabled()) log.debug("Pushing to #" + this);
        if (UIContext.get() != this) {
            begin();
            try {
                final Txn txn = Txn.get();
                txn.begin(context);
                try {
                    runnable.run();
                    txn.commit();
                } catch (final Throwable e) {
                    log.error("Cannot process client instruction", e);
                    txn.rollback();
                }
            } finally {
                end();
            }
        } else {
            runnable.run();
        }
    }

    /**
     * Stimulates all {@link DataListener} with a list of object
     *
     * @param data
     *            list of object
     */
    public void pushToClient(final List<Object> data) {
        execute(() -> {
            if (listeners.isEmpty()) return;
            try {
                listeners.forEach(listener -> data.forEach(listener::onData));
            } catch (final Throwable e) {
                log.error("Cannot send data", e);
            }
        });
    }

    /**
     * Stimulates all {@link DataListener} with an object
     *
     * @param data
     *            the object
     */
    public void pushToClient(final Object data) {
        execute(() -> {
            if (listeners.isEmpty()) return;
            try {
                listeners.forEach(listener -> listener.onData(data));
            } catch (final Throwable e) {
                log.error("Cannot send data", e);
            }
        });
    }

    /**
     * Sends data to the targetted {@link PObject} from {@link JsonObject} instruction
     * Called from terminal side
     *
     * @param jsonObject
     *            the JSON instructions
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
     * @param terminalDataReceiver
     *            the terminal data receiver
     */
    public void setClientDataOutput(final TerminalDataReceiver terminalDataReceiver) {
        this.terminalDataReceiver = terminalDataReceiver;
    }

    /**
     * Locks the current UIContext
     */
    public void begin() {
        lock.lock();
        UIContext.setCurrent(this);
    }

    /**
     * Unlock the current UIContext
     */
    public void end() {
        UIContext.remove();
        lock.unlock();
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
     * @param pObject
     *            the pObject
     * @see #getObject(int)
     */
    public void registerObject(final PObject pObject) {
        pObjectWeakReferences.put(pObject.getID(), pObject);
    }

    /**
     * Gets the {@link PObject} with a specific object ID
     *
     * @param objectID
     *            the object ID of the searched {@link PObject}
     * @return the {@link PObject} or null if not found
     * @see #registerObject(PObject)
     */
    public <T> T getObject(final int objectID) {
        return (T) pObjectWeakReferences.get(objectID);
    }

    /**
     * Registers a {@link StreamHandler} that will be called on the terminal side
     *
     * @param streamListener
     *            the stream handler
     */
    public void stackStreamRequest(final StreamHandler streamListener) {
        final int streamRequestID = nextStreamRequestID();

        final ModelWriter writer = Txn.get().getWriter();
        writer.beginObject();
        writer.write(ServerToClientModel.TYPE_ADD_HANDLER, -1);
        writer.write(ServerToClientModel.HANDLER_TYPE, HandlerModel.HANDLER_STREAM_REQUEST.getValue());
        writer.write(ServerToClientModel.STREAM_REQUEST_ID, streamRequestID);
        writer.write(ServerToClientModel.APPLICATION_ID, getApplication().getId());
        writer.endObject();

        streamListenerByID.put(streamRequestID, streamListener);
    }

    /**
     * Registers a {@link StreamHandler} that will be called on a specific {@link com.ponysdk.core.terminal.ui.PTObject}
     *
     * @param streamListener
     *            the stream handler
     * @param objectID
     *            the object ID of a {@link PObject}
     */
    public void stackEmbededStreamRequest(final StreamHandler streamListener, final int objectID) {
        final int streamRequestID = nextStreamRequestID();

        final ModelWriter writer = Txn.get().getWriter();
        writer.beginObject();
        final PObject pObject = getObject(objectID);
        if (!PWindow.isMain(pObject.getWindow())) writer.write(ServerToClientModel.WINDOW_ID, pObject.getWindow().getID());
        if (pObject.getFrame() != null) writer.write(ServerToClientModel.FRAME_ID, pObject.getFrame().getID());
        writer.write(ServerToClientModel.TYPE_ADD_HANDLER, objectID);
        writer.write(ServerToClientModel.HANDLER_TYPE, HandlerModel.HANDLER_EMBEDED_STREAM_REQUEST.getValue());
        writer.write(ServerToClientModel.STREAM_REQUEST_ID, streamRequestID);
        writer.write(ServerToClientModel.APPLICATION_ID, getApplication().getId());
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
     * @param streamID
     *            the stream ID
     * @return the removed stream handler or null if not found
     */
    public StreamHandler removeStreamListener(final int streamID) {
        return streamListenerByID.remove(streamID);
    }

    /**
     * Closes the current UIContext
     */
    public void close() {
        final ModelWriter writer = Txn.get().getWriter();
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
     * @param name
     *            the name to which the object is bound; cannot be null
     * @param value
     *            the object to be bound
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
     * @param name
     *            the name of the object to remove from this session
     */
    public Object removeAttribute(final String name) {
        return attributes.remove(name);
    }

    /**
     * Returns the object bound with the specified name in this session, or <code>null</code> if no
     * object is bound under the name.
     *
     * @param name
     *            a string specifying the name of the object
     * @return the object with the specified name
     */
    public <T> T getAttribute(final String name) {
        return (T) attributes.get(name);
    }

    /**
     * Destroys the current UIContext when the {@link com.ponysdk.core.server.servlet.WebSocket} is closed
     *
     * This method locks the UIContext
     */
    public void onDestroy() {
        begin();
        try {
            doDestroy();
        } finally {
            end();
        }
    }

    /**
     * Destroys the current UIContext when the {@link Application} is destroyed
     *
     * This method locks the UIContext
     */
    void destroyFromApplication() {
        begin();
        try {
            living = false;
            destroyListeners.forEach(listener -> {
                try {
                    listener.onBeforeDestroy(this);
                } catch (final AlreadyDestroyedApplication e) {
                    if (log.isDebugEnabled()) log.debug("Exception while destroying UIContext #" + getID(), e);
                } catch (final Exception e) {
                    log.error("Exception while destroying UIContext #" + getID(), e);
                }
            });
            context.close();
        } finally {
            end();
        }
    }

    /**
     * Destroys the UIContext
     *
     * This method locks the UIContext
     */
    public void destroy() {
        begin();
        try {
            doDestroy();
            context.close();
        } finally {
            end();
        }
    }

    /**
     * Destroys effectively the UIContext
     */
    private void doDestroy() {
        living = false;
        destroyListeners.forEach(listener -> {
            try {
                listener.onBeforeDestroy(this);
            } catch (final AlreadyDestroyedApplication e) {
                if (log.isDebugEnabled()) log.debug("Exception while destroying UIContext #" + getID(), e);
            } catch (final Exception e) {
                log.error("Exception while destroying UIContext #" + getID(), e);
            }
        });
        application.unregisterUIContext(ID);
    }

    /**
     * Adds a {@link ContextDestroyListener} that will be stimulated when the UIContext is destroyed
     *
     * @param listener
     *            the context destroy listener
     * @since {@link #destroy()}
     */
    public void addContextDestroyListener(final ContextDestroyListener listener) {
        destroyListeners.add(listener);
    }

    /**
     * Sends the heart beat
     *
     * This method locks the UIContext
     */
    public void sendHeartBeat() {
        begin();
        try {
            context.sendHeartBeat();
        } catch (final Throwable e) {
            log.error("Cannot send server heart beat to UIContext #" + getID(), e);
        } finally {
            end();
        }
    }

    /**
     * Sends the round trip
     *
     * This method locks the UIContext
     */
    public void sendRoundTrip() {
        begin();
        try {
            context.sendRoundTrip();
        } catch (final Throwable e) {
            log.error("Cannot send server round trip to UIContext #" + getID(), e);
        } finally {
            end();
        }
    }

    /**
     * Returns the living state of the current UIContext
     *
     * @return the living state
     */
    public boolean isLiving() {
        return living;
    }

    /**
     * Gets the {@link TxnContext} of the UIContext
     *
     * @return the TxnContext
     */
    public TxnContext getContext() {
        return context;
    }

    /**
     * Gets the {@link Application} of the UIContext
     *
     * @return The Application
     */
    public Application getApplication() {
        return application;
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
        return "UIContext [ID=" + ID + ", living=" + living + ", ApplicationID=" + application.getId() + "]";
    }

    /**
     * Adds a ping value
     *
     * @param pingValue
     *            the ping value
     */
    public void addPingValue(final long pingValue) {
        pings.add(pingValue);
    }

    /**
     * Gets an average latency from the last 10 measurements
     *
     * @return the lantency
     */
    public double getLatency() {
        return pings.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    /**
     * A {@link ConcurrentLinkedQueue} that is bounded with a size limit
     *
     * @param <E>
     *            the type of elements held in this collection
     */
    private static final class ConcurrentBoundedLinkedQueue<E> extends ConcurrentLinkedQueue<E> {

        private final int limit;

        /**
         * The default constructor
         *
         * @param limit
         *            the size limit of the list
         */
        public ConcurrentBoundedLinkedQueue(final int limit) {
            this.limit = limit;
        }

        @Override
        public boolean add(final E e) {
            if (this.size() == this.limit) this.remove();
            return super.add(e);
        }
    }

}
