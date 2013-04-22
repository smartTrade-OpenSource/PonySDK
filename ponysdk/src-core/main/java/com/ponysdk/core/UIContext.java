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
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.event.BroadcastEventHandler;
import com.ponysdk.core.event.Event;
import com.ponysdk.core.event.Event.Type;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.event.EventHandler;
import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.core.event.StreamHandler;
import com.ponysdk.core.instruction.AddHandler;
import com.ponysdk.core.instruction.Close;
import com.ponysdk.core.security.Permission;
import com.ponysdk.core.servlet.Session;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.server.basic.PCookies;
import com.ponysdk.ui.server.basic.PHistory;
import com.ponysdk.ui.server.basic.PObject;
import com.ponysdk.ui.server.basic.PPusher;
import com.ponysdk.ui.server.basic.PTimer;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.HISTORY;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;

public class UIContext {

    private static ThreadLocal<UIContext> currentContext = new ThreadLocal<UIContext>();

    private final Logger log = LoggerFactory.getLogger(UIContext.class);

    private long objectCounter = 1;

    private long streamRequestCounter = 0;

    private final WeakHashMap weakReferences = new WeakHashMap();

    private final Map<Long, PTimer> timers = new ConcurrentHashMap<Long, PTimer>();

    private final Map<Long, StreamHandler> streamListenerByID = new HashMap<Long, StreamHandler>();

    private Map<String, Permission> permissions = new HashMap<String, Permission>();

    private PHistory history;

    private EventBus rootEventBus;

    private final PCookies cookies = new PCookies();

    private final Application application;

    private final Map<String, Object> ponySessionAttributes = new ConcurrentHashMap<String, Object>();

    private final ReentrantLock lock = new ReentrantLock();

    private long lastReceived = -1;
    private long nextSent = 0;
    private final Map<Long, JSONObject> incomingMessageQueue = new HashMap<Long, JSONObject>();

    private long uiContextID;

    public UIContext(final Application ponyApplication) {
        this.application = ponyApplication;
    }

    void setUiContextID(final long uiContextID) {
        this.uiContextID = uiContextID;
    }

    public long getUiContextID() {
        return uiContextID;
    }

    void fireClientData(final JSONObject instruction) throws JSONException {
        if (instruction.has(TYPE.KEY)) {
            if (instruction.get(TYPE.KEY).equals(TYPE.KEY_.CLOSE)) {
                UIContext.get().invalidate();
                return;
            }

            if (instruction.get(TYPE.KEY).equals(TYPE.KEY_.HISTORY)) {
                if (history != null) {
                    history.fireHistoryChanged(instruction.getString(HISTORY.TOKEN));
                }
                return;
            }

        }

        final PObject object = weakReferences.get(instruction.getLong(PROPERTY.OBJECT_ID));

        if (object == null) {
            log.warn("unknown reference from the browser. Unable to execute instruction: " + instruction);
            try {
                if (instruction.has(PROPERTY.PARENT_ID)) {
                    final PObject parentOfGarbageObject = weakReferences.get(instruction.getLong(PROPERTY.PARENT_ID));
                    log.warn("parent: " + parentOfGarbageObject);
                }
            } catch (final Exception e) {}

            return;
        }
        if (instruction.has(TYPE.KEY)) {
            if (instruction.get(TYPE.KEY).equals(TYPE.KEY_.EVENT)) {
                object.onClientData(instruction);
            }
        }
    }

    public void acquire() {
        lock.lock();
    }

    public void release() {
        lock.unlock();
    }

    public long nextID() {
        return objectCounter++;
    }

    public PPusher getPusher() {
        return getAttribute(PPusher.PUSHER);
    }

    public long nextStreamRequestID() {
        return streamRequestCounter++;
    }

    public void registerObject(final PObject object) {
        weakReferences.put(object.getID(), object);
    }

    public void unRegisterObject(final PObject object) {
        timers.remove(object.getID());
        weakReferences.remove(object.getID());
    }

    public void assignParentID(final long objectID, final long parentID) {
        weakReferences.assignParentID(objectID, parentID);
    }

    @SuppressWarnings("unchecked")
    public <T> T getObject(final long objectID) {
        return (T) weakReferences.get(objectID);
    }

    public Session getSession() {
        return application.getSession();
    }

    public StreamHandler removeStreamListener(final Long streamID) {
        return streamListenerByID.remove(streamID);
    }

    public void stackStreamRequest(final StreamHandler streamListener) {
        final AddHandler addHandler = new AddHandler(0, HANDLER.KEY_.STREAM_REQUEST_HANDLER);
        final long streamRequestID = UIContext.get().nextStreamRequestID();
        addHandler.put(PROPERTY.STREAM_REQUEST_ID, streamRequestID);
        Txn.get().getTxnContext().save(addHandler);
        streamListenerByID.put(streamRequestID, streamListener);
    }

    public void stackEmbededStreamRequest(final StreamHandler streamListener, final long objectID) {
        final AddHandler addHandler = new AddHandler(objectID, HANDLER.KEY_.EMBEDED_STREAM_REQUEST_HANDLER);
        final long streamRequestID = UIContext.get().nextStreamRequestID();
        addHandler.put(PROPERTY.STREAM_REQUEST_ID, streamRequestID);
        Txn.get().getTxnContext().save(addHandler);
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

    public void setHistory(final PHistory history) {
        this.history = history;
    }

    public void setRootEventBus(final EventBus eventBus) {
        this.rootEventBus = eventBus;
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

    public static <H extends EventHandler> HandlerRegistration addHandlerToSource(final Type<H> type, final Object source, final H handler) {
        return get().getEventBus().addHandlerToSource(type, source, handler);
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

    public static EventBus getRootEventBus() {
        return get().getEventBus();
    }

    void invalidate() {
        application.getSession().invalidate();
    }

    public void close() {
        Txn.get().getTxnContext().save(new Close());
    }

    public boolean hasPermission(final String key) {
        return permissions.containsKey(key);
    }

    public void setPermissions(final Map<String, Permission> permissions) {
        this.permissions = permissions;
    }

    /**
     * Binds an object to this session, using the name specified. If an object of the same name is already
     * bound to the session, the object is replaced.
     * <p>
     * If the value passed in is null, this has the same effect as calling <code>removeAttribute()<code>.
     * 
     * @param name
     *            the name to which the object is bound; cannot be null
     * @param value
     *            the object to be bound
     */
    public void setAttribute(final String name, final Object value) {
        if (value == null) removeAttribute(name);
        else ponySessionAttributes.put(name, value);
    }

    /**
     * Removes the object bound with the specified name from this session. If the session does not have an
     * object bound with the specified name, this method does nothing.
     * 
     * @param name
     *            the name of the object to remove from this session
     */

    public Object removeAttribute(final String name) {
        return ponySessionAttributes.remove(name);
    }

    /**
     * Returns the object bound with the specified name in this session, or <code>null</code> if no object is
     * bound under the name.
     * 
     * @param name
     *            a string specifying the name of the object
     * @return the object with the specified name
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(final String name) {
        return (T) ponySessionAttributes.get(name);
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

    public boolean updateIncomingSeqNum(final long receivedSeqNum) {
        final long previous = lastReceived;
        if ((previous + 1) != receivedSeqNum) {
            log.error("Wrong seqnum received. Expecting #" + (previous + 1) + " but received #" + receivedSeqNum);
            return false;
        }
        lastReceived = receivedSeqNum;
        return true;
    }

    public long getAndIncrementNextSentSeqNum() {
        final long n = nextSent;
        nextSent++;
        return n;
    }

    public void stackIncomingMessage(final Long receivedSeqNum, final JSONObject data) {
        incomingMessageQueue.put(receivedSeqNum, data);
    }

    public List<JSONObject> expungeIncomingMessageQueue(final Long receivedSeqNum) {
        if (incomingMessageQueue.isEmpty()) return Collections.emptyList();

        final List<JSONObject> datas = new ArrayList<JSONObject>();
        long expected = receivedSeqNum + 1;
        while (incomingMessageQueue.containsKey(expected)) {
            datas.add(incomingMessageQueue.remove(expected));
            lastReceived = expected;
            expected++;
        }
        log.info("Message synchronized from #" + receivedSeqNum + " to #" + lastReceived);
        return datas;
    }

}
