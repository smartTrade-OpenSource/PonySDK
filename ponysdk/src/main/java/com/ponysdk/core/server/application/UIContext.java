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

import com.ponysdk.core.server.metrics.PonySDKMetrics;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.AlreadyDestroyedApplication;
import com.ponysdk.core.server.context.PObjectCache;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.RecordingEncoder;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.basic.PCookies;
import com.ponysdk.core.ui.basic.PHistory;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.eventbus.*;
import com.ponysdk.core.ui.statistic.TerminalDataReceiver;
import com.ponysdk.core.useragent.UserAgent;
import com.ponysdk.core.writer.ModelWriter;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;
import jakarta.json.spi.JsonProvider;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
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
    private static final String DEFAULT_PROVIDER = "org.eclipse.parsson.JsonProviderImpl";

    /** Shared JsonProvider instance — Parsson is thread-safe, no need to create one per session. */
    private static final JsonProvider SHARED_JSON_PROVIDER;
    static {
        JsonProvider p;
        try {
            p = (JsonProvider) Class.forName(DEFAULT_PROVIDER).getDeclaredConstructor().newInstance();
        } catch (final Throwable t) {
            p = JsonProvider.provider();
        }
        SHARED_JSON_PROVIDER = p;
    }

    // Cached JSON keys to avoid repeated toStringValue() calls
    private static final String KEY_TYPE_HISTORY = ClientToServerModel.TYPE_HISTORY.toStringValue();
    private static final String KEY_OBJECT_ID = ClientToServerModel.OBJECT_ID.toStringValue();
    private static final String KEY_PARENT_OBJECT_ID = ClientToServerModel.PARENT_OBJECT_ID.toStringValue();

    private final int ID;

    private final ReentrantLock lock = new ReentrantLock();
    private Map<String, Object> attributes;

    private final PObjectCache pObjectCache = new PObjectCache();
    private int objectCounter = 1;

    private Map<Integer, StreamHandler> streamListenerByID;
    private int streamRequestCounter = 0;

    private final PHistory history = new PHistory();
    private final com.ponysdk.core.ui.eventbus.EventBus rootEventBus = new com.ponysdk.core.ui.eventbus.EventBus();
    private final com.ponysdk.core.ui.eventbus2.EventBus newEventBus = new com.ponysdk.core.ui.eventbus2.EventBus();

    private final PCookies cookies = new PCookies();

    private Set<ContextDestroyListener> destroyListeners;
    @Deprecated(forRemoval = true, since = "v2.8.0")
    private final TxnContext context;
    private final Set<DataListener> listeners = new CopyOnWriteArraySet<>();

    private TerminalDataReceiver terminalDataReceiver;

    private boolean alive = true;
    private volatile boolean suspended = false;  // volatile: read by timeout thread + resume

    /** Records all protocol operations during suspension for replay on reconnection. */
    private RecordingEncoder recordingEncoder;

    private Latency roundtripLatency;
    private Latency networkLatency;
    private Latency terminalLatency;

    private final ApplicationConfiguration configuration;
    private WebSocket socket;
    private final JettyServerUpgradeRequest request;

    // Cached from request at construction time — the underlying Jetty request
    // is recycled after the WebSocket upgrade completes, so we must snapshot
    // any data we need before that happens.
    private final Map<String, List<String>> cachedParameterMap;
    private final String cachedUserAgent;
    private final HttpSession cachedHttpSession;

    private long lastReceivedTime = System.currentTimeMillis();

    private final ModelWriter modelWriter;

    private final StringDictionary stringDictionary;

    /** Optional OTel metrics — null if not configured. */
    private PonySDKMetrics metrics;

    public UIContext(final WebSocket socket, final TxnContext context, final ApplicationConfiguration configuration,
                     final JettyServerUpgradeRequest request) {
        this(socket, context, configuration, request, null);
    }

    public UIContext(final WebSocket socket, final TxnContext context, final ApplicationConfiguration configuration,
                     final JettyServerUpgradeRequest request, final SharedDictionaryProvider sharedDictionaryProvider) {
        this.ID = uiContextCount.incrementAndGet();
        this.socket = socket;
        this.configuration = configuration;
        this.request = request;
        this.context = context;
        this.modelWriter = context.getWriter();

        // Use pre-cached request data from WebSocket (snapshotted during HTTP upgrade,
        // before Jetty 12 recycles the underlying request)
        this.cachedParameterMap = socket != null ? socket.getCachedParameterMap() : Map.of();
        this.cachedUserAgent = socket != null ? socket.getCachedUserAgent() : null;
        this.cachedHttpSession = socket != null ? socket.getCachedHttpSession() : null;

        // Initialize StringDictionary based on configuration
        if (configuration.isStringDictionaryEnabled()) {
            this.stringDictionary = new StringDictionary(
                configuration.getStringDictionaryMaxSize(),
                configuration.getStringDictionaryMinLength()
            );
            // Pre-seed from shared provider (learned from previous sessions)
            this.stringDictionary.initFromSharedProvider(sharedDictionaryProvider);
        } else {
            this.stringDictionary = null;
        }
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
        if (UIContext.get() != this) {
            final long lockStart = (metrics != null) ? System.currentTimeMillis() : 0;
            acquire();
            final long runStart = System.currentTimeMillis();
            if (metrics != null) metrics.recordLockWait(runStart - lockStart);
            try {
                final Txn txn = Txn.get();
                txn.begin(context);
                try {
                    runnable.run();
                    txn.commit();
                    if (metrics != null) metrics.recordExecute(System.currentTimeMillis() - runStart);
                    return true;
                } catch (final Throwable e) {
                    log.error("Cannot process client instruction for UIContext #{}", ID, e);
                    txn.rollback();
                    if (metrics != null) metrics.recordExecute(System.currentTimeMillis() - runStart);
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
        if (jsonObject.containsKey(KEY_TYPE_HISTORY)) {
            history.fireHistoryChanged(jsonObject.getString(KEY_TYPE_HISTORY));
        } else {
            final JsonValue jsonValue = jsonObject.get(KEY_OBJECT_ID);
            final int objectID = switch (jsonValue.getValueType()) {
                case NUMBER -> ((JsonNumber) jsonValue).intValue();
                case STRING -> Integer.parseInt(((JsonString) jsonValue).getString());
                default -> {
                    log.error("unknown reference from the browser. Unable to execute instruction: {}", jsonObject);
                    yield -1;
                }
            };
            if (objectID == -1) return;

            //Cookies
            if (objectID == 0) {
                cookies.onClientData(jsonObject);
            } else {
                final PObject object = getObject(objectID);

                if (object == null) {
                    log.error("unknown reference from the browser. Unable to execute instruction: {}", jsonObject);

                    if (jsonObject.containsKey(KEY_PARENT_OBJECT_ID)) {
                        final int parentObjectID = jsonObject.getJsonNumber(KEY_PARENT_OBJECT_ID)
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
        socket.beginObject();
        socket.encode(ServerToClientModel.DESTROY_CONTEXT, null);
        socket.endObject();
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
        else {
            if (attributes == null) attributes = new HashMap<>(4);
            attributes.put(name, value);
        }
    }

    /**
     * Removes the object bound with the specified name from this session. If
     * the session does not have an object bound with the specified name, this
     * method does nothing.
     *
     * @param name the name of the object to remove from this session
     */
    public Object removeAttribute(final String name) {
        return attributes != null ? attributes.remove(name) : null;
    }

    /**
     * Returns the object bound with the specified name in this session, or <code>null</code> if no
     * object is bound under the name.
     *
     * @param name a string specifying the name of the object
     * @return the object with the specified name
     */
    public <T> T getAttribute(final String name) {
        return attributes != null ? (T) attributes.get(name) : null;
    }

    /**
     * Destroys the current UIContext when the {@link com.ponysdk.core.server.websocket.WebSocket} is closed
     * <p>
     * This method locks the UIContext
     */
    public void onDestroy() {
        //we used to avoid calling socket.close() there, but sometimes jetty does not close the WS
        //when there is an exception in a listener => always call close since it is a no-op on a closed WS
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
            doDestroy();
            context.deregisterUIContext(ID);
            socket.disconnect();
        } finally {
            release();
        }
    }

    /**
     * Destroys effectively the UIContext.
     * <p>
     * Cleanup order:
     * <ol>
     *   <li>Sets {@code alive=false} and {@code suspended=false}</li>
     *   <li>Clears the {@link RecordingEncoder} and resets {@link ModelWriter} to the raw socket</li>
     *   <li>Flushes {@link StringDictionary} frequency data to the shared provider</li>
     *   <li>Fires all {@link ContextDestroyListener}s (set is nulled before iteration)</li>
     * </ol>
     * Must be called under the UIContext lock.
     */
    private void doDestroy() {
        alive = false;
        suspended = false;

        // Clear and release the recording encoder to avoid retaining buffered entries
        final RecordingEncoder rec = recordingEncoder;
        recordingEncoder = null;
        if (rec != null) {
            rec.clear();
            // Reset ModelWriter encoder so it no longer references the recorder
            context.getWriter().setEncoder(socket);
        }

        // Flush string dictionary frequency data to shared provider
        if (stringDictionary != null) {
            stringDictionary.flushToSharedProvider();
        }

        if (destroyListeners != null) {
            final Set<ContextDestroyListener> toNotify = destroyListeners;
            destroyListeners = null; // release the set early to avoid retaining listeners
            toNotify.forEach(listener -> {
                try {
                    listener.onBeforeDestroy(this);
                } catch (final AlreadyDestroyedApplication e) {
                    if (log.isDebugEnabled()) log.debug("Exception while destroying UIContext #" + getID(), e);
                } catch (final Exception e) {
                    log.error("Exception while destroying UIContext #" + getID(), e);
                }
            });
        }
    }

    /**
     * Adds a {@link ContextDestroyListener} that will be notified when the UIContext is destroyed.
     * No-op if the UIContext is already destroyed.
     *
     * @param listener the context destroy listener
     * @see #destroy()
     */
    public void addContextDestroyListener(final ContextDestroyListener listener) {
        final Set<ContextDestroyListener> dl = destroyListeners;
        if (dl == null) {
            if (!alive) return; // already destroyed — ignore silently
            destroyListeners = new HashSet<>(4);
        }
        if (destroyListeners != null) destroyListeners.add(listener);
    }

    /**
     * Removes a {@link ContextDestroyListener}.
     *
     * @param listener the context destroy listener
     * @see #destroy()
     */
    public void removeContextDestroyListener(final ContextDestroyListener listener) {
        final Set<ContextDestroyListener> dl = destroyListeners;
        if (dl != null) dl.remove(listener);
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
        final List<String> historyTokens = this.cachedParameterMap.get(ClientToServerModel.TYPE_HISTORY.toStringValue());
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
        return UserAgent.parseUserAgentString(cachedUserAgent);
    }

    public HttpSession getSession() {
        return cachedHttpSession;
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
        socket.setListener(listener);
    }

    /**
     * Gets the {@link Application} of the UIContext
     *
     * @return The Application
     */
    public Application getApplication() {
        return context.getApplication();
    }

    public void setMetrics(final PonySDKMetrics metrics) {
        this.metrics = metrics;
    }

    public PonySDKMetrics getMetrics() {
        return metrics;
    }

    /**
     * Suspends this UIContext on WebSocket disconnect, keeping all widget state intact.
     * Called only when transparent reconnection is enabled.
     * <p>
     * During suspension, {@link #execute(Runnable)} still runs runnables (mutating PObject state)
     * but writes go to a {@link RecordingEncoder} — buffered, not sent over the wire.
     * On {@link #resume(WebSocket)}, the buffer is compacted (consecutive updates merged,
     * create+remove cancelled) and replayed on the new WebSocket in a single flush.
     *
     * @param timeoutMs how long to wait for reconnection before destroying (ms)
     */
    public void suspend(final long timeoutMs) {
        suspended = true;
        final int maxEntries = configuration.getMaxRecordingEntries();
        recordingEncoder = new RecordingEncoder(maxEntries, (count, max) -> {
            log.error("UIContext #{} recording buffer overflow ({} > {}) — destroying session",
                    ID, count, max);
            // Schedule destroy outside the encode call to avoid re-entrancy.
            // Don't touch 'suspended' here — destroy() → doDestroy() handles all state cleanup.
            Thread.ofVirtual().start(this::destroy);
        });
        // Swap to RecordingEncoder — all protocol writes are buffered
        context.getWriter().setEncoder(recordingEncoder);
        log.info("UIContext #{} suspended, waiting for reconnection (timeout={}ms)", ID, timeoutMs);
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(timeoutMs);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (suspended) {
                log.warn("UIContext #{} reconnection timeout — destroying", ID);
                destroy();
            }
        });
    }

    /**
     * Resumes a suspended UIContext with a new WebSocket.
     * Compacts the recorded buffer (merges updates, cancels create+remove pairs),
     * replays it on the new socket, then signals the client with {@code RECONNECT_CONTEXT}.
     */
    public void resume(final WebSocket newSocket) {
        if (!suspended) throw new IllegalStateException("UIContext #" + ID + " is not suspended");
        if (!alive) throw new IllegalStateException("UIContext #" + ID + " is already destroyed");
        suspended = false;

        final RecordingEncoder recorder = this.recordingEncoder;
        this.recordingEncoder = null;

        // If the recorder overflowed, the UIContext is already being destroyed — abort
        if (recorder == null || recorder.isOverflowed()) {
            log.warn("UIContext #{} resume aborted — recorder overflowed or null", ID);
            return;
        }

        // Compact: merge consecutive updates, cancel create+remove pairs
        final int beforeSize = recorder.size();
        recorder.compact();
        log.info("UIContext #{} resumed — replaying {} entries (compacted from {})",
                ID, recorder.size(), beforeSize);

        // Swap back to real encoder and socket for flush path
        context.getWriter().setEncoder(newSocket);
        context.setSocket(newSocket);
        this.socket = newSocket;

        // Reset last received time so CommunicationSanityChecker doesn't immediately kill us
        onMessageReceived();

        acquire();
        try {
            final Txn txn = Txn.get();
            txn.begin(context);
            try {
                // Replay all buffered operations in order
                recorder.replayTo(newSocket);

                // Signal the client that reconnection succeeded
                final ModelWriter writer = getWriter();
                writer.beginObject(PWindow.getMain());
                writer.write(ServerToClientModel.RECONNECT_CONTEXT, ID);
                writer.endObject();

                txn.commit();
            } catch (final Throwable e) {
                log.error("Error during UIContext #{} resume replay", ID, e);
                txn.rollback();
            } finally {
                // Release recorder entries now that replay is done
                recorder.clear();
            }
        } finally {
            release();
        }
    }

    public boolean isSuspended() {
        return suspended;
    }

    public ModelWriter getWriter() {
        return modelWriter;
    }

    public JsonProvider getJsonProvider() {
        return SHARED_JSON_PROVIDER;
    }

    public JettyServerUpgradeRequest getRequest() {
        return request;
    }

    /**
     * Returns the cached parameter map from the original upgrade request.
     * Safe to call after WebSocket upgrade (unlike getRequest().getParameterMap()).
     */
    public Map<String, List<String>> getParameterMap() {
        return cachedParameterMap;
    }

    /**
     * Gets the StringDictionary for protocol optimization.
     *
     * @return The StringDictionary, or null if disabled
     */
    public StringDictionary getStringDictionary() {
        return stringDictionary;
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
        if (roundtripLatency == null) roundtripLatency = new Latency(10);
        roundtripLatency.add(value);
    }

    /**
     * Gets an average roundtrip latency from the last 10 measurements
     *
     * @return the lantency
     */
    public double getRoundtripLatency() {
        return roundtripLatency != null ? roundtripLatency.getValue() : 0;
    }

    /**
     * Adds a network latency value
     *
     * @param value the value
     */
    public void addNetworkLatencyValue(final long value) {
        if (networkLatency == null) networkLatency = new Latency(10);
        networkLatency.add(value);
    }

    /**
     * Gets an average network latency from the last 10 measurements
     *
     * @return the lantency
     */
    public double getNetworkLatency() {
        return networkLatency != null ? networkLatency.getValue() : 0;
    }

    /**
     * Adds a terminal latency value
     *
     * @param value the ping value
     */
    public void addTerminalLatencyValue(final long value) {
        if (terminalLatency == null) terminalLatency = new Latency(10);
        terminalLatency.add(value);
    }

    /**
     * Gets an average terminal latency from the last 10 measurements
     *
     * @return the lantency
     */
    public double getTerminalLatency() {
        return terminalLatency != null ? terminalLatency.getValue() : 0;
    }

    private static final class Latency {

        private int index = 0;
        private long sum = 0;
        private final long[] values;

        private Latency(final int size) {
            values = new long[size];
        }

        public void add(final long value) {
            sum -= values[index];
            values[index] = value;
            sum += value;
            if (++index >= values.length) index = 0;
        }

        public double getValue() {
            return (double) sum / values.length;
        }
    }

}
