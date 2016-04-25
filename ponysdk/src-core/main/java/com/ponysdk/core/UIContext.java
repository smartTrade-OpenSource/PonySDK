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

package com.ponysdk.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.event.BroadcastEventHandler;
import com.ponysdk.core.event.Event;
import com.ponysdk.core.event.Event.Type;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.event.EventHandler;
import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.core.event.RootEventBus;
import com.ponysdk.core.event.StreamHandler;
import com.ponysdk.core.security.Permission;
import com.ponysdk.core.servlet.CommunicationSanityChecker;
import com.ponysdk.core.statistic.TerminalDataReceiver;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.core.stm.TxnContext;
import com.ponysdk.core.tools.ListenerCollection;
import com.ponysdk.ui.server.basic.DataListener;
import com.ponysdk.ui.server.basic.PCookies;
import com.ponysdk.ui.server.basic.PHistory;
import com.ponysdk.ui.server.basic.PObject;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

/**
 * <p>
 * Provides a way to identify a user across more than one page request or visit
 * to a Web site and to store information about that user.
 * </p>
 * <p>
 * There is ONE unique UIContext for each screen displayed. Each UIContext is
 * bound to the current {@link Application} .
 * </p>
 */
public class UIContext {

    private static ThreadLocal<UIContext> currentContext = new ThreadLocal<>();

    private static final Logger log = LoggerFactory.getLogger(UIContext.class);

    private int objectCounter = 1;

    private int streamRequestCounter = 0;

    private final WeakHashMap weakReferences = new WeakHashMap();

    private final Map<Integer, StreamHandler> streamListenerByID = new HashMap<>();

    private Map<String, Permission> permissions = new HashMap<>();

    private final PHistory history = new PHistory();

    private final EventBus rootEventBus = new RootEventBus();

    private final PCookies cookies = new PCookies();

    private final Application application;

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private final ReentrantLock lock = new ReentrantLock();

    private int lastReceived = -1;
    private int nextSent = 0;
    private long lastSyncErrorTimestamp = 0;
    private final Map<Integer, JsonObject> incomingMessageQueue = new HashMap<>();

    private static final AtomicInteger ponyUIContextIDcount = new AtomicInteger();

    private final int uiContextID;

    private final CommunicationSanityChecker communicationSanityChecker;

    private final List<UIContextListener> uiContextListeners = new ArrayList<>();

    private TerminalDataReceiver terminalDataReceiver;

    private boolean destroyed = false;

    private final TxnContext context;

    private final ListenerCollection<DataListener> listenerCollection = new ListenerCollection<>();

    public UIContext(final Application application, final TxnContext context) {
        this.application = application;
        this.uiContextID = ponyUIContextIDcount.incrementAndGet();
        this.context = context;

        this.context.setUIContext(this);
        this.application.registerUIContext(this);

        this.communicationSanityChecker = new CommunicationSanityChecker(this);
        this.communicationSanityChecker.start();
    }

    public int getUiContextID() {
        return uiContextID;
    }

    public void addDataListener(final DataListener listener) {
        listenerCollection.register(listener);
    }

    public void removeDataListener(final DataListener listener) {
        listenerCollection.unregister(listener);
    }

    public void execute(final Runnable runnable) {
        if (log.isDebugEnabled())
            log.debug("Pushing to #" + this);
        if (UIContext.get() != this) {
            begin();
            try {
                final Txn txn = Txn.get();
                txn.begin(context);
                try {
                    // final Long receivedSeqNum = checkClientMessage(data,
                    // uiContext);

                    // if (receivedSeqNum != null) {
                    runnable.run();

                    // final List<JsonObject> datas =
                    // uiContext.expungeIncomingMessageQueue(receivedSeqNum);
                    // for (final JsonObject jsoObject : datas) {
                    // process(uiContext, jsoObject);
                    // }
                    // }

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

    public void pushToClient(final List<Object> data) {
        execute(() -> fireOnData(data));
    }

    public void pushToClient(final Object data) {
        execute(() -> fireOnData(data));
    }

    private void fireOnData(final List<Object> data) {
        if (listenerCollection.isEmpty())
            return;
        try {
            for (final DataListener listener : listenerCollection) {
                for (final Object object : data) {
                    listener.onData(object);
                }
            }
        } catch (final Throwable e) {
            log.error("Cannot send data", e);
        }
    }

    private void fireOnData(final Object data) {
        if (listenerCollection.isEmpty())
            return;
        try {
            for (final DataListener listener : listenerCollection) {
                listener.onData(data);
            }
        } catch (final Throwable e) {
            log.error("Cannot send data", e);
        }
    }

    public void fireClientData(final JsonObject jsonObject) {
        if (jsonObject.containsKey(ClientToServerModel.TYPE_CLOSE.toStringValue())) {
            destroy();
        } else if (jsonObject.containsKey(ClientToServerModel.TYPE_HISTORY.toStringValue())) {
            if (history != null) {
                history.fireHistoryChanged(jsonObject.getString(ClientToServerModel.TYPE_HISTORY.toStringValue()));
            }
        } else if (jsonObject.containsKey(ClientToServerModel.ERROR_MSG.toStringValue())) {
            log.error(jsonObject.getString(ClientToServerModel.ERROR_MSG.toStringValue()));
        } else {
            final int objectID = jsonObject.getJsonNumber(ClientToServerModel.OBJECT_ID.toStringValue()).intValue();

            final PObject object = weakReferences.get(objectID);

            if (object == null) {
                log.error("unknown reference from the browser. Unable to execute instruction: " + jsonObject);

                if (jsonObject.containsKey(ClientToServerModel.PARENT_OBJECT_ID.toStringValue())) {
                    final int parentObjectID = jsonObject.getJsonNumber(ClientToServerModel.PARENT_OBJECT_ID.toStringValue())
                            .intValue();
                    final PObject gcObject = weakReferences.get(parentObjectID);
                    log.warn("" + gcObject);
                }

                return;
            }

            // if (jsonObject.containsKey(Model.TYPE_EVENT.toStringValue())) {
            if (terminalDataReceiver != null) {
                terminalDataReceiver.onDataReceived(object, jsonObject);
            }
            object.onClientData(jsonObject);
            // }
        }
    }

    public void setClientDataOutput(final TerminalDataReceiver clientDataOutput) {
        this.terminalDataReceiver = clientDataOutput;
    }

    public void begin() {
        lock.lock();
        UIContext.setCurrent(this);
    }

    public void end() {
        // getContext().flush();
        UIContext.remove();
        lock.unlock();
    }

    public int nextID() {
        return objectCounter++;
    }

    public int nextStreamRequestID() {
        return streamRequestCounter++;
    }

    public void registerObject(final PObject object) {
        weakReferences.put(object.getID(), object);
    }

    // public void assignParentID(final int objectID, final int parentID) {
    // weakReferences.assignParentID(objectID, parentID);
    // }

    @SuppressWarnings("unchecked")
    public <T> T getObject(final int objectID) {
        return (T) weakReferences.get(objectID);
    }

    // public Session getSession() {
    // return application.getSession();
    // }

    public StreamHandler removeStreamListener(final int streamID) {
        return streamListenerByID.remove(streamID);
    }

    public void stackStreamRequest(final StreamHandler streamListener) {
        final int streamRequestID = UIContext.get().nextStreamRequestID();

        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        parser.parse(ServerToClientModel.TYPE_ADD_HANDLER, HandlerModel.HANDLER_STREAM_REQUEST_HANDLER.getValue());
        parser.parse(ServerToClientModel.OBJECT_ID, 0);
        parser.parse(ServerToClientModel.STREAM_REQUEST_ID, streamRequestID);
        parser.endObject();

        streamListenerByID.put(streamRequestID, streamListener);
    }

    public void stackEmbededStreamRequest(final StreamHandler streamListener, final int objectID) {
        final int streamRequestID = UIContext.get().nextStreamRequestID();

        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        parser.parse(ServerToClientModel.TYPE_ADD_HANDLER, HandlerModel.HANDLER_EMBEDED_STREAM_REQUEST_HANDLER.getValue());
        parser.parse(ServerToClientModel.OBJECT_ID, 0);
        parser.parse(ServerToClientModel.STREAM_REQUEST_ID, streamRequestID);
        parser.endObject();

        streamListenerByID.put(streamRequestID, streamListener);
    }

    public PHistory getHistory() {
        return history;
    }

    private EventBus getEventBus() {
        return rootEventBus;
    }

    public PCookies getCookies() {
        return cookies;
    }

    public static UIContext get() {
        return currentContext.get();
    }

    public static void remove() {
        currentContext.remove();
    }

    public static void setCurrent(final UIContext uiContext) {
        currentContext.set(uiContext);
    }

    public static <H extends EventHandler> HandlerRegistration addHandler(final Type<H> type, final H handler) {
        return get().getEventBus().addHandler(type, handler);
    }

    public static <H extends EventHandler> void removeHandler(final Type<H> type, final H handler) {
        get().getEventBus().removeHandler(type, handler);
    }

    public static <H extends EventHandler> HandlerRegistration addHandlerToSource(final Type<H> type,
            final Object source, final H handler) {
        return get().getEventBus().addHandlerToSource(type, source, handler);
    }

    public static <H extends EventHandler> void removeHandlerFromSource(final Type<H> type, final Object source,
            final H handler) {
        get().getEventBus().removeHandlerFromSource(type, source, handler);
    }

    public static void fireEvent(final Event<?> event) {
        get().getEventBus().fireEvent(event);
    }

    public static void fireEventFromSource(final Event<?> event, final Object source) {
        get().getEventBus().fireEventFromSource(event, source);
    }

    public static void addHandler(final BroadcastEventHandler handler) {
        get().getEventBus().addHandler(handler);
    }

    public static void removeHandler(final BroadcastEventHandler handler) {
        get().getEventBus().removeHandler(handler);
    }

    public static EventBus getRootEventBus() {
        return get().getEventBus();
    }

    public void close() {
        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        parser.parse(ServerToClientModel.TYPE_CLOSE, null);
        parser.endObject();
    }

    public boolean hasPermission(final String key) {
        return permissions.containsKey(key);
    }

    public void setPermissions(final Map<String, Permission> permissions) {
        this.permissions = permissions;
    }

    /**
     * Binds an object to this session, using the name specified. If an object
     * of the same name is already bound to the session, the object is replaced.
     * <p>
     * If the value passed in is null, this has the same effect as calling
     * <code>removeAttribute()<code>.
     *
     * @param name
     *            the name to which the object is bound; cannot be null
     * @param value
     *            the object to be bound
     */
    public void setAttribute(final String name, final Object value) {
        if (value == null)
            removeAttribute(name);
        else
            attributes.put(name, value);
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
     * Returns the object bound with the specified name in this session, or
     * <code>null</code> if no object is bound under the name.
     *
     * @param name
     *            a string specifying the name of the object
     * @return the object with the specified name
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(final String name) {
        return (T) attributes.get(name);
    }

    public void setApplicationAttribute(final String name, final Object value) {
        this.application.setAttribute(name, value);
    }

    public Application getApplication() {
        return application;
    }

    @SuppressWarnings("unchecked")
    public <T> T getApplicationAttribute(final String name) {
        return (T) this.application.getAttribute(name);
    }

    public void notifyMessageReceived() {
        communicationSanityChecker.onMessageReceived();
    }

    public boolean updateIncomingSeqNum(final int receivedSeqNum) {
        notifyMessageReceived();

        final int previous = lastReceived;
        if (previous + 1 != receivedSeqNum) {
            if (lastSyncErrorTimestamp <= 0)
                lastSyncErrorTimestamp = System.currentTimeMillis();
            return false;
        }
        lastReceived = receivedSeqNum;
        lastSyncErrorTimestamp = -1;

        return true;
    }

    public int getAndIncrementNextSentSeqNum() {
        return nextSent++;
    }

    public void stackIncomingMessage(final int receivedSeqNum, final JsonObject data) {
        incomingMessageQueue.put(receivedSeqNum, data);
    }

    public List<JsonObject> expungeIncomingMessageQueue(final int receivedSeqNum) {
        if (incomingMessageQueue.isEmpty())
            return Collections.emptyList();

        final List<JsonObject> datas = new ArrayList<>();
        int expected = receivedSeqNum + 1;
        while (incomingMessageQueue.containsKey(expected)) {
            datas.add(incomingMessageQueue.remove(expected));
            lastReceived = expected;
            expected++;
        }

        if (log.isDebugEnabled()) {
            log.debug("Message synchronized from #{} to #{}", receivedSeqNum, lastReceived);
        }
        return datas;
    }

    public long getLastSyncErrorTimestamp() {
        return lastSyncErrorTimestamp;
    }

    public void destroy() {
        // log.info("Destroying UIContext ViewID #{} from the Session #{}",
        // uiContextID,
        // application.getSession().getId());
        destroyed = true;

        communicationSanityChecker.stop();
        application.unregisterUIContext(uiContextID);

        for (final UIContextListener listener : uiContextListeners) {
            listener.onUIContextDestroyed(this);
        }

        // log.info("UIContext destroyed ViewID #{} from the Session #{}",
        // uiContextID,
        // application.getSession().getId());
    }

    public void sendHeartBeat() {
        begin();
        try {
            // final Txn txn = Txn.get();
            // txn.begin(context);
            try {
                context.sendHeartBeat();
                // txn.commit();
            } catch (final Throwable e) {
                log.error("Cannot send server heartbeat to client", e);
                // txn.rollback();
            }
        } finally {
            end();
        }
    }

    public void addUIContextListener(final UIContextListener listener) {
        uiContextListeners.add(listener);
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public TxnContext getContext() {
        return context;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (uiContextID ^ uiContextID >>> 32);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final UIContext other = (UIContext) obj;
        if (uiContextID != other.uiContextID)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "UIContext [" + application + ", uiContextID=" + uiContextID + ", destroyed=" + destroyed + "]";
    }

}
