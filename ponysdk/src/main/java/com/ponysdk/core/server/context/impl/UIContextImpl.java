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

package com.ponysdk.core.server.context.impl;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.concurrent.ScheduledTaskHandler;
import com.ponysdk.core.server.concurrent.Scheduler;
import com.ponysdk.core.server.concurrent.SchedulingContext;
import com.ponysdk.core.server.context.api.ContextDestroyListener;
import com.ponysdk.core.server.context.History;
import com.ponysdk.core.server.context.PObjectCache;
import com.ponysdk.core.server.context.api.UIContext;
import com.ponysdk.core.server.monitoring.Monitor;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.basic.PCookies;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.eventbus.*;
import com.ponysdk.core.ui.statistic.TerminalDataReceiver;
import com.ponysdk.core.useragent.UserAgent;
import com.ponysdk.core.writer.ModelWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpSession;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class UIContextImpl implements UIContext {

    private static final int INITIAL_STREAM_MAP_CAPACITY = 2;

    private static final Logger log = LoggerFactory.getLogger(UIContextImpl.class);
    private static final ThreadLocal<UIContextImpl> currentContext = new ThreadLocal<>();
    private static final AtomicInteger uiContextCount = new AtomicInteger();

    private final int id;

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private final PObjectCache pObjectCache = new PObjectCache();
    private final SchedulingContext schedulingContext;
    private int objectCounter = 1;

    private Map<Integer, StreamHandler> streamListenerByID;
    private int streamRequestCounter = 0;

    private final History history;
    private final com.ponysdk.core.ui.eventbus.EventBus rootEventBus = new com.ponysdk.core.ui.eventbus.EventBus();
    private final com.ponysdk.core.ui.eventbus2.EventBus newEventBus = new com.ponysdk.core.ui.eventbus2.EventBus();

    private final PCookies cookies = new PCookies();

    private final Set<ContextDestroyListener> destroyListeners = new HashSet<>();

    private static Scheduler scheduler;

    static {
        AtomicInteger count = new AtomicInteger();
        int threadCount = Integer.parseInt(System.getProperty("PONY_SCHEDULER_THREAD_POOL", "4"));
        ThreadFactory threadFactory = r -> new Thread(r, "P_SCHEDULER-" + count.getAndIncrement());
        scheduler = new Scheduler(threadCount, threadFactory);
    }

    private TerminalDataReceiver terminalDataReceiver;

    private boolean alive = true;

    private final Monitor monitor = new Monitor();

    private final WebSocket socket;

    private final ModelWriter modelWriter;

    public UIContextImpl(final WebSocket socket) {
        this.id = uiContextCount.incrementAndGet();
        this.socket = socket;
        this.modelWriter = new ModelWriter(socket);
        this.schedulingContext = scheduler.createContext(Long.toString(id));


        final List<String> historyTokens = socket.getRequest().getParameterMap().get(ClientToServerModel.TYPE_HISTORY.toStringValue());
        final String historyToken = historyTokens != null && !historyTokens.isEmpty() ? historyTokens.get(0) : null;
        history = new History(historyToken);
    }

    /**
     * Returns the current UIContextImpl
     *
     * @return The current UIContextImpl
     */
    public static UIContextImpl get() {
        UIContextImpl uiContext = currentContext.get();
        if (uiContext == null) {
            throw new IllegalMonitorStateException("PScheduler should be used when an application thread needs to update the GUI.");
        }
        return uiContext;
    }

    private static void remove() {
        currentContext.remove();
    }

    public static void setCurrent(final UIContextImpl uiContext) {
        currentContext.set(uiContext);
    }

    /**
     * Adds {@link EventHandler} to the {@link com.ponysdk.core.ui.eventbus.EventBus}
     *
     * @param type    the event type
     * @param handler the event handler
     * @return the HandlerRegistration in order to remove the EventHandler
     * @see #fireEvent(Event)
     */
    public static HandlerRegistration addHandler(final Event.Type type, final EventHandler handler) {
        return get().rootEventBus.addHandler(type, handler);
    }

    /**
     * Fires an {@link Event} on the {@link com.ponysdk.core.ui.eventbus.EventBus}
     * Only {@link EventHandler}s added before fires event will be stimulated
     *
     * @param event the fired event
     * @see #addHandler(BroadcastEventHandler)
     */
    public void fireEvent(final Event<? extends EventHandler> event) {
        get().fireEvent0(event);
    }

    private void fireEvent0(final Event<? extends EventHandler> event) {
        rootEventBus.fireEvent(event);
    }

    /**
     * Removes {@link EventHandler} from the {@link com.ponysdk.core.ui.eventbus.EventBus}
     *
     * @param type    the event type
     * @param handler the event handler
     * @see #addHandler(com.ponysdk.core.ui.eventbus.Event.Type, EventHandler)
     */
    public static void removeHandler(final Event.Type type, final EventHandler handler) {
        get().rootEventBus.removeHandler(type, handler);
    }

    /**
     * Adds {@link EventHandler} to the {@link com.ponysdk.core.ui.eventbus.EventBus} with a specific source
     * Use {@link #fireEventFromSource(Event, Object)} to stimulate this event handler
     *
     * @param type    the event type
     * @param source  the source
     * @param handler the event handler
     * @return the {@link HandlerRegistration} in order to remove the {@link EventHandler}
     * @see #fireEventFromSource(Event, Object)
     */
    public static HandlerRegistration addHandlerToSource(final Event.Type type, final Object source, final EventHandler handler) {
        return get().rootEventBus.addHandlerToSource(type, source, handler);
    }

    /**
     * Fires an {@link Event} on the {@link com.ponysdk.core.ui.eventbus.EventBus} with a specific source
     * Only {@link EventHandler}s added before fires event will be stimulated
     *
     * @param event  the fired event
     * @param source the source
     * @see #addHandlerToSource(com.ponysdk.core.ui.eventbus.Event.Type, Object, EventHandler)
     */
    public void fireEventFromSource(final Event<? extends EventHandler> event, final Object source) {
        get().rootEventBus.fireEventFromSource(event, source);
    }

    /**
     * Removes handler from the {@link com.ponysdk.core.ui.eventbus.EventBus} with a specific source
     *
     * @param type    the event type
     * @param source  the source
     * @param handler the event handler
     * @see #addHandlerToSource(com.ponysdk.core.ui.eventbus.Event.Type, Object, EventHandler)
     */
    public static void removeHandlerFromSource(final Event.Type type, final Object source, final EventHandler handler) {
        get().rootEventBus.removeHandlerFromSource(type, source, handler);
    }

    /**
     * Adds a {@link BroadcastEventHandler} to the {@link com.ponysdk.core.ui.eventbus.EventBus} that receive all the
     * events
     * All call to {@link #fireEvent(Event)} or {@link #fireEventFromSource(Event, Object)} will stimulate this event
     * handler
     *
     * @param handler the broadcast event handler
     * @see #fireEvent(Event)
     * @see #fireEventFromSource(Event, Object)
     */
    public static void addHandler(final BroadcastEventHandler handler) {
        get().rootEventBus.addHandler(handler);
    }

    /**
     * Removes broadcast handler from the {@link com.ponysdk.core.ui.eventbus.EventBus}
     *
     * @param handler the broadcast event handler
     * @see #addHandler(BroadcastEventHandler)
     */
    public static void removeHandler(final BroadcastEventHandler handler) {
        get().rootEventBus.removeHandler(handler);
    }

    /**
     * Gets the default {@link com.ponysdk.core.ui.eventbus.EventBus}
     *
     * @return the root event bus
     */
    public static com.ponysdk.core.ui.eventbus.EventBus getRootEventBus() {
        return get().rootEventBus;
    }

    /**
     * Gets the default {@link com.ponysdk.core.ui.eventbus2.EventBus}
     *
     * @return the new event bus
     */
    public static com.ponysdk.core.ui.eventbus2.EventBus getNewEventBus() {
        return get().newEventBus;
    }

    public int getID() {
        return id;
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
     * Locks the current UIContextImpl
     */
    private void acquire() {
        lock.lock();
        currentContext.set(this);
    }

    private void release() {
        UIContextImpl.remove();
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
     * Registers a {@link PObject} in the UIContextImpl
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
        writer.write(ServerToClientModel.HANDLER_TYPE, HandlerModel.HANDLER_EMBEDED_STREAM_REQUEST.getValue());
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
     * Removes the {@link StreamHandler} with a specific ID in the current UIContextImpl
     *
     * @param streamID the stream ID
     * @return the removed stream handler or null if not found
     */
    public StreamHandler removeStreamListener(final int streamID) {
        return streamListenerByID != null ? streamListenerByID.remove(streamID) : null;
    }

    /**
     * Closes the current UIContextImpl
     */
    public void close() {
        socket.encode(ServerToClientModel.DESTROY_CONTEXT, null);
        socket.endObject();
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

    /**
     * Destroys the current UIContextImpl when the {@link com.ponysdk.core.server.websocket.WebSocket} is closed
     * <p>
     * This method locks the UIContextImpl
     */
    public void onDestroy() {
        if (!isAlive()) return;

        schedulingContext.destroy();

        acquire();
        try {
            doDestroy();
        } finally {
            release();
        }
    }

    /**
     * Destroys the UIContextImpl
     * <p>
     * This method locks the UIContextImpl
     */
    public void destroy() {
        if (!isAlive()) return;
        acquire();
        try {
            doDestroy();
            socket.close();
        } finally {
            release();
        }
    }

    /**
     * Destroys effectively the UIContextImpl
     */
    private void doDestroy() {
        alive = false;
        destroyListeners.forEach(listener -> {
            try {
                listener.onBeforeDestroy(this);
            } catch (final Exception e) {
                log.error("Exception while destroying UIContextImpl #" + getID(), e);
            }
        });
    }

    /**
     * Adds a {@link ContextDestroyListener} that will be stimulated when the UIContextImpl is destroyed
     *
     * @param listener the context destroy listener
     * @since {@link #destroy()}
     */
    public void addContextDestroyListener(final ContextDestroyListener listener) {
        destroyListeners.add(listener);
    }

    /**
     * Removes a {@link ContextDestroyListener}
     *
     * @param listener the context destroy listener
     * @since {@link #destroy()}
     */
    public void removeContextDestroyListener(final ContextDestroyListener listener) {
        destroyListeners.remove(listener);
    }

    /**
     * Sends the round trip
     * <p>
     * This method locks the UIContextImpl
     */
    public void sendRoundTrip() {
        acquire();
        try {
            socket.sendRoundTrip();
        } finally {
            release();
        }
    }

    /**
     * Returns the alive state of the current UIContextImpl
     *
     * @return the alive state
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * Gets the {@link History} of the UIContextImpl
     *
     * @return the History
     */
    public History getHistory() {
        return history;
    }

    /**
     * Gets the {@link PCookies} of the UIContextImpl
     *
     * @return the PCookies
     */
    public PCookies getCookies() {
        return cookies;
    }

    /**
     * Get a cookie from the {@link PCookies} of the UIContextImpl
     *
     * @return the cookie
     */
    public String getCookie(final String name) {
        return getCookies().getCookie(name);
    }

    public UserAgent getUserAgent() {

        return UserAgent.parseUserAgentString(socket.getRequest().getHeader("User-Agent"));
    }

    public HttpSession getSession() {
        return socket.getRequest().getSession();
    }

    public <T> T getApplicationAttribute(final String name) {
        return application.getAttribute(name);
    }

    public void setWebSocketListener(final WebSocket.Listener listener) {
        socket.setListener(listener);
    }

    public ModelWriter getWriter() {
        return modelWriter;
    }

    public SchedulingContext getSchedulingContext() {
        return schedulingContext;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final UIContextImpl uiContext = (UIContextImpl) o;
        return id == uiContext.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "UIContextImpl [id=" + id + ", alive=" + alive + "]";
    }


    public static SchedulingContext createContext(UIContextImpl uiContext) {
        return scheduler.createContext(Long.toString(uiContext.getID()));
    }

    public void execute(final Runnable task) {
        schedulingContext.execute(() -> run(task));
    }

    public ScheduledTaskHandler execute(final Runnable task, Duration delay) {
        return schedulingContext.executeLater(delay.toMillis(), () -> run(task));
    }

    public ScheduledTaskHandler repeat(final Runnable task, Duration period) {
        return schedulingContext.schedule(period.toMillis(), () -> run(task));
    }


    private void run(Runnable task) {
        acquire();
        try {
            task.run();

            //TODO nciaravola
            //flush();
        } finally {
            release();
        }
    }
}
