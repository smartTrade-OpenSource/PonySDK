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

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.AlreadyDestroyedApplication;
import com.ponysdk.core.server.context.CommunicationSanityChecker;
import com.ponysdk.core.server.context.PObjectCache;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.RecordingEncoder;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.server.websocket.WebsocketEncoder;
import org.eclipse.jetty.websocket.api.StatusCode;
import com.ponysdk.core.ui.basic.PCookies;
import com.ponysdk.core.ui.basic.PHistory;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.eventbus.*;
import com.ponysdk.core.ui.statistic.TerminalDataReceiver;
import com.ponysdk.core.useragent.UserAgent;
import com.ponysdk.core.writer.ModelWriter;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.spi.JsonProvider;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

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

    private static final int INITIAL_STREAM_MAP_CAPACITY = 2;

    private static final Logger log = LoggerFactory.getLogger(UIContext.class);
    private static final ThreadLocal<UIContext> currentContext = new ThreadLocal<>();
    private static final AtomicInteger uiContextCount = new AtomicInteger();
    private static final String DEFAULT_PROVIDER = "org.glassfish.json.JsonProviderImpl";

    /** Ends the suspensions that time out, and destroys the contexts whose recording overflowed. */
    private static final ScheduledThreadPoolExecutor reconnectionTimer;

    static {
        reconnectionTimer = new ScheduledThreadPoolExecutor(1, runnable -> {
            final Thread thread = new Thread(runnable, UIContext.class.getName() + "-reconnection");
            thread.setDaemon(true);
            return thread;
        });
        reconnectionTimer.setRemoveOnCancelPolicy(true);
    }

    private final int ID;

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Object> attributes = new HashMap<>();

    private final PObjectCache pObjectCache = new PObjectCache();
    private int objectCounter = 1;

    private Map<Integer, StreamHandler> streamListenerByID;
    private int streamRequestCounter = 0;

    private final PHistory history = new PHistory();
    private final com.ponysdk.core.ui.eventbus.EventBus rootEventBus = new com.ponysdk.core.ui.eventbus.EventBus();
    private final com.ponysdk.core.ui.eventbus2.EventBus newEventBus = new com.ponysdk.core.ui.eventbus2.EventBus();

    private final PCookies cookies = new PCookies();

    private final Set<ContextDestroyListener> destroyListeners = new HashSet<>();
    @Deprecated(forRemoval = true, since = "v2.8.0")
    private final TxnContext context;
    private final Set<DataListener> listeners = ConcurrentHashMap.newKeySet();

    private TerminalDataReceiver terminalDataReceiver;

    private boolean alive = true;
    private int closeStatusCode = StatusCode.NO_CODE;

    // Transparent reconnection: while suspended, the context waits for its terminal to come back on a new WebSocket
    private volatile boolean suspended = false;
    /** Records the protocol writes while suspended; null otherwise. */
    private RecordingEncoder recordingEncoder;
    /** Ends this suspension if no terminal comes back in time. */
    private ScheduledFuture<?> suspensionTimeout;
    private CommunicationSanityChecker communicationSanityChecker;
    private WebSocket.Listener webSocketListener;

    private final Latency roundtripLatency = new Latency(10);
    private final Latency networkLatency = new Latency(10);
    private final Latency terminalLatency = new Latency(10);

    private final ApplicationConfiguration configuration;
    private volatile WebSocket socket;
    private final ServletUpgradeRequest request;

    private long lastReceivedTime = System.currentTimeMillis();

    private final JsonProvider jsonProvider;

    private final ModelWriter modelWriter;

    public UIContext(final WebSocket socket, final TxnContext context, final ApplicationConfiguration configuration,
                     final ServletUpgradeRequest request) {
        this.ID = uiContextCount.incrementAndGet();
        this.socket = socket;
        this.configuration = configuration;
        this.request = request;
        this.context = context;
        this.modelWriter = context.getWriter();

        JsonProvider provider;
        try {
            provider = (JsonProvider) Class.forName(DEFAULT_PROVIDER).getDeclaredConstructor().newInstance();
        } catch (final Throwable t) {
            provider = JsonProvider.provider();
        }

        jsonProvider = provider;
    }

    /**
     * Returns the current UIContext
     *
     * @return The current UIContext
     */
    public static UIContext get() {
        return currentContext.get();
    }

    /**
     * Removed the current UIContext
     */
    public static void remove() {
        currentContext.remove();
    }

    /**
     * Sets the current UIContext
     *
     * @param uiContext the UIContext
     */
    public static void setCurrent(final UIContext uiContext) {
        currentContext.set(uiContext);
    }

    /**
     * Gets the default {@link com.ponysdk.core.ui.eventbus.EventBus}
     *
     * @return the root event bus
     */
    public com.ponysdk.core.ui.eventbus.EventBus getRootEventBus() {
        return rootEventBus;
    }

    /**
     * Gets the default {@link com.ponysdk.core.ui.eventbus2.EventBus}
     *
     * @return the new event bus
     */
    public com.ponysdk.core.ui.eventbus2.EventBus getNewEventBus() {
        return newEventBus;
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
     * @param listener the data listener
     * @return {@code true} if this set did not already contain the specified element
     */
    public boolean addDataListener(final DataListener listener) {
        return listeners.add(listener);
    }

    /**
     * Removes the {@link DataListener} from UIContext
     *
     * @param listener the data listener
     * @return {@code true} if this set contained the specified element
     */
    public boolean removeDataListener(final DataListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Executes a {@link Runnable} that represents a task in a graphical context
     * This method locks the UIContext
     *
     * @param runnable the tasks
     */
    public boolean execute(final Runnable runnable) {
        if (!isAlive()) return false;
        if (log.isDebugEnabled()) log.debug("Pushing to #{}", this);
        if (UIContext.get() != this) {
            acquire();
            try {
                final Txn txn = Txn.get();
                txn.begin(context);
                try {
                    runnable.run();
                    txn.commit();
                    return true;
                } catch (final Throwable e) {
                    log.error("Cannot process client instruction", e);
                    txn.rollback();
                    return false;
                }
            } finally {
                release();
            }
        } else {
            runnable.run();
            return false;
        }
    }

    /**
     * Stimulates all {@link DataListener} with a list of object
     *
     * @param data list of object
     */
    public boolean pushToClient(final List<Object> data) {
        if (isAlive() && data != null && !listeners.isEmpty()) {
            return execute(() -> {
                try {
                    listeners.forEach(listener -> data.forEach(listener::onData));
                } catch (final Throwable e) {
                    log.error("Cannot send data", e);
                }
            });
        } else {
            return false;
        }
    }


    /**
     * Stimulates all {@link DataListener} with an object
     *
     * @param data the object
     */
    public boolean pushToClient(final Object data) {
        if (isAlive() && data != null && !listeners.isEmpty()) {
            return execute(() -> {
                try {
                    listeners.forEach(listener -> listener.onData(data));
                } catch (final Throwable e) {
                    log.error("Cannot send data", e);
                }
            });
        } else {
            return false;
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
            final ValueType valueType = jsonValue.getValueType();
            if (ValueType.NUMBER == valueType) {
                objectID = ((JsonNumber) jsonValue).intValue();
            } else if (ValueType.STRING == valueType) {
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
                        final int parentObjectID = jsonObject.getJsonNumber(ClientToServerModel.PARENT_OBJECT_ID.toStringValue())
                                .intValue();
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
     * Locks the current UIContext
     */
    public void acquire() {
        lock.lock();
        currentContext.set(this);
    }

    /**
     * Unlock the current UIContext
     */
    public void release() {
        // A nested release (the lock taken again by the thread that holds it) keeps the current context: the
        // outer holder carries on with it
        if (lock.getHoldCount() <= 1) UIContext.remove();
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
     * Closes the current UIContext
     */
    public void close() {
        // While suspended, after what was recorded: the lost WebSocket is not written to
        final WebsocketEncoder target = suspended && recordingEncoder != null ? recordingEncoder : socket;
        target.beginObject();
        target.encode(ServerToClientModel.DESTROY_CONTEXT, null);
        target.endObject();
    }

    /**
     * force flush instructions to the server
     */
    public void flush() {
        if(isAlive()) socket.flush();
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
     * Destroys the current UIContext when the {@link com.ponysdk.core.server.websocket.WebSocket} is closed
     * <p>
     * This method locks the UIContext
     */
    public void onDestroy(final int statusCode) {
        //we used to avoid calling socket.close() there, but sometimes jetty does not close the WS
        //when there is an exception in a listener => always call close since it is a no-op on a closed WS
        this.closeStatusCode = statusCode;
        destroy();
    }

    /**
     * Destroys the current UIContext when the {@link Application} is destroyed
     * <p>
     * This method locks the UIContext
     */
    void destroyFromApplication() {
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
     * Destroys the UIContext
     * <p>
     * This method locks the UIContext
     */
    public void destroy() {
        if (!isAlive()) return;
        acquire();
        try {
            doDestroy();
            context.deregisterUIContext(ID);
            socket.close();
        } finally {
            release();
        }
    }

    /**
     * Disconnects and destroys the UIContext
     * <p>
     * This method locks the UIContext
     */
    public void disconnect() {
        if (!isAlive()) return;
        acquire();
        try {
            this.closeStatusCode = StatusCode.NORMAL;
            doDestroy();
            context.deregisterUIContext(ID);
            socket.disconnect();
        } finally {
            release();
        }
    }

    /**
     * Destroys effectively the UIContext
     */
    private void doDestroy() {
        alive = false;
        endSuspension();
        destroyListeners.forEach(listener -> {
            try {
                listener.onBeforeDestroy(this);
            } catch (final AlreadyDestroyedApplication e) {
                if (log.isDebugEnabled()) log.debug("Exception while destroying UIContext #" + getID(), e);
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
     * This method locks the UIContext
     */
    public void sendRoundTrip() {
        acquire();
        try {
            socket.sendRoundTrip();
        } catch (final Throwable e) {
            log.error("Cannot send server round trip to UIContext #" + getID(), e);
        } finally {
            release();
        }
    }

    /**
     * Returns the alive state of the current UIContext
     *
     * @return the alive state
     */
    public boolean isAlive() {
        return alive;
    }

    public int getCloseStatusCode() {
        return closeStatusCode;
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

    public HttpSession getSession() {
        return request.getSession();
    }

    public <T> T getApplicationAttribute(final String name) {
        return context.getAttribute(name);
    }

    public void setApplicationAttribute(final String name, final Object value) {
        context.setAttribute(name, value);
    }

    public String getApplicationId() {
        return context.getId();
    }

    public void setWebSocketListener(final WebSocket.Listener listener) {
        this.webSocketListener = listener;
        socket.setListener(listener);
    }

    public void setCommunicationSanityChecker(final CommunicationSanityChecker communicationSanityChecker) {
        this.communicationSanityChecker = communicationSanityChecker;
    }

    /**
     * The WebSocket of this UIContext was closed or failed.
     * <p>
     * With transparent reconnection enabled ({@link ApplicationConfiguration#getReconnectionTimeoutMs()}) and a
     * close that does not say the page is gone, the context is suspended: it waits for its terminal to come back on
     * a new WebSocket. Otherwise it is destroyed.
     * <p>
     * A close from a WebSocket that is no longer this context's (the one a reconnection replaced) is ignored.
     *
     * @param from the WebSocket that was closed
     * @param statusCode the close status code
     */
    public void onConnectionLost(final WebSocket from, final int statusCode) {
        if (!isAlive() || from != socket || suspended) return;
        if (isReconnectionEnabled() && isResumable(statusCode)) {
            acquire();
            try {
                if (!isAlive() || from != socket || suspended) return;
                this.closeStatusCode = statusCode;
                notifyConnectionLost();
                suspend();
            } finally {
                release();
            }
        } else {
            onDestroy(statusCode);
        }
    }

    /**
     * The communication sanity checker found no message from the terminal for too long: the connection is dropped,
     * and the context suspended or destroyed as {@link #onConnectionLost(WebSocket, int)} does.
     */
    public void onCommunicationLost() {
        if (!isAlive() || suspended) return;
        if (isReconnectionEnabled()) {
            final WebSocket lost = socket;
            onConnectionLost(lost, StatusCode.ABNORMAL);
            lost.disconnect();
        } else {
            disconnect();
        }
    }

    private boolean isReconnectionEnabled() {
        return configuration.getReconnectionTimeoutMs() > 0;
    }

    /**
     * A normal close (1000) or a close with no code (1005, what the terminal sends when its page unloads) mean no
     * terminal will come back: the context is destroyed. 1001 (going away) is resumable: proxies send it too, when
     * they restart, while a page that unloads sends 1005 first.
     */
    private static boolean isResumable(final int statusCode) {
        return statusCode != StatusCode.NORMAL && statusCode != StatusCode.NO_CODE;
    }

    private void notifyConnectionLost() {
        final ApplicationConfiguration.ReconnectionListener listener = configuration.getReconnectionListener();
        if (listener == null) return;
        try {
            listener.onConnectionLost(this);
        } catch (final Throwable e) {
            log.error("ReconnectionListener failed for UIContext #{}", ID, e);
        }
    }

    /**
     * Suspends this UIContext: its widgets and its application stay as they are. {@link #execute(Runnable)} still
     * runs, but what it writes is recorded instead of sent, until {@link #resume(WebSocket, byte[])} or the timeout.
     * <p>
     * Called under the UIContext lock, so that no transaction is cut in two between the old WebSocket and the
     * recording.
     */
    private void suspend() {
        final long timeoutMs = configuration.getReconnectionTimeoutMs();
        if (communicationSanityChecker != null) communicationSanityChecker.stop();
        final RecordingEncoder recorder = new RecordingEncoder(configuration.getMaxRecordingEntries(), (count, max) -> {
            final RecordingEncoder overflowed = recordingEncoder;
            log.warn("UIContext #{} cannot be resumed: its recording holds {} entries, more than {}, it is destroyed. Held: {}",
                ID, count, max, overflowed != null ? overflowed.describe() : "?");
            // Not from inside the write that overflowed
            reconnectionTimer.execute(this::destroy);
        });
        recordingEncoder = recorder;
        modelWriter.setEncoder(recorder);
        suspended = true;
        suspensionTimeout = reconnectionTimer.schedule(() -> {
            // Under the lock: a resume running now must not be undone once it is done
            acquire();
            try {
                if (!isAlive() || !suspended || recordingEncoder != recorder) return; // resumed, or another suspension
                log.info("UIContext #{} was not resumed within {} ms, it is destroyed", ID, timeoutMs);
                destroy();
            } finally {
                release();
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);
        log.info("UIContext #{} suspended, waiting {} ms for its terminal to reconnect", ID, timeoutMs);
    }

    /** Releases what a suspension holds. Called under the UIContext lock. */
    private void endSuspension() {
        suspended = false;
        if (suspensionTimeout != null) {
            suspensionTimeout.cancel(false);
            suspensionTimeout = null;
        }
        if (recordingEncoder != null) {
            recordingEncoder.clear();
            recordingEncoder = null;
            modelWriter.setEncoder(socket);
        }
    }

    /**
     * The bytes from the given offset that the WebSocket this context lost had sent, or null if they are not all
     * held any more.
     */
    public byte[] getUnreceivedBytes(final long offset) {
        acquire(); // the lost WebSocket's buffer was written by the thread that held the context
        try {
            return socket.getBytesFrom(offset);
        } finally {
            release();
        }
    }

    /**
     * Resumes this suspended UIContext on a new WebSocket: sends first the bytes the terminal had not received from
     * the lost WebSocket, then what was recorded while suspended, compacted, then a {@code RECONNECT_CONTEXT} block.
     *
     * @param newSocket the WebSocket the terminal reconnected with
     * @param unreceivedBytes from {@link #getUnreceivedBytes(long)}
     * @return false if the context could not be resumed: destroyed, no longer suspended, its recording overflowed, or
     *         the replay failed (the context is then left to the caller to destroy)
     */
    public boolean resume(final WebSocket newSocket, final byte[] unreceivedBytes) {
        acquire();
        try {
            if (!isAlive() || !suspended) return false;
            final RecordingEncoder recorder = recordingEncoder;
            if (recorder.isOverflowed()) return false;
            recordingEncoder = null; // endSuspension must not put the lost socket back
            endSuspension();

            final Txn txn = Txn.get();
            txn.begin(context);
            try {
                final int recorded = recorder.size();
                recorder.compact();
                log.info("UIContext #{} resumed: {} bytes not received sent again, {} recorded entries replayed ({} before compaction)",
                    ID, unreceivedBytes.length, recorder.size(), recorded);

                this.socket = newSocket;
                context.setSocket(newSocket);
                modelWriter.setEncoder(newSocket);
                if (webSocketListener != null) newSocket.setListener(webSocketListener);
                onMessageReceived();

                newSocket.putRaw(unreceivedBytes);
                recorder.replayTo(newSocket);
                newSocket.encode(ServerToClientModel.RECONNECT_CONTEXT, ID);
                newSocket.endObject();
                txn.commit();
            } catch (final Throwable e) {
                // Neither suspended nor connected any more: the caller destroys it, once it has told the terminal
                log.error("UIContext #{} could not be resumed", ID, e);
                txn.rollback();
                return false;
            } finally {
                recorder.clear();
            }
            if (communicationSanityChecker != null) communicationSanityChecker.start();
            return true;
        } finally {
            release();
        }
    }

    public boolean isSuspended() {
        return suspended;
    }

    /** The context's transaction context: a resumed context keeps its own, not the new WebSocket's. */
    public TxnContext getTxnContext() {
        return context;
    }

    /**
     * Gets the {@link Application} of the UIContext
     *
     * @return The Application
     */
    public Application getApplication() {
        return context.getApplication();
    }

    public ModelWriter getWriter() {
        return modelWriter;
    }

    public JsonProvider getJsonProvider() {
        return jsonProvider;
    }

    public ServletUpgradeRequest getRequest() {
        return request;
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

    private static final class Latency {

        private int index = 0;
        private final long[] values;

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
