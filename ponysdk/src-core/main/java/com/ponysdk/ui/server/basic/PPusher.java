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

package com.ponysdk.ui.server.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.Parser;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.socket.ConnectionListener;
import com.ponysdk.core.socket.WebSocket;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.core.stm.TxnSocketContext;
import com.ponysdk.core.tools.ListenerCollection;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.Model;

/**
 * Push data to terminal using the WebSocket.
 */
public class PPusher extends PObject implements ConnectionListener {

    private static final Logger log = LoggerFactory.getLogger(PPusher.class);

    public static final String PUSHER = "com.ponysdk.ui.server.basic.PPusher";

    private WebSocket websocket;

    private final UIContext uiContext;

    private final List<ConnectionListener> connectionListeners = new ArrayList<>();
    private final ListenerCollection<DataListener> listenerCollection = new ListenerCollection<>();

    private PusherState pusherState = PusherState.STOPPED;

    private final TxnSocketContext txnContext;

    private final int pollingDelay;

    private final int ping;

    public enum PusherState {
        STOPPED, INITIALIZING, STARTED
    }

    private PPusher(final int pollingDelay, final int ping) {
        this.pollingDelay = pollingDelay;
        this.ping = ping;
        this.txnContext = new TxnSocketContext();
        this.pusherState = PusherState.INITIALIZING;
        this.uiContext = UIContext.get();

        init();
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        parser.parse(Model.FIXDELAY, pollingDelay);
        parser.parse(Model.PINGDELAY, ping);
    }

    public void initialize(final WebSocket websocket) {
        this.websocket = websocket;
        this.websocket.addConnectionListener(this);
        this.txnContext.setSocket(websocket);
    }

    public static PPusher initialize(final int pollingDelay, final int ping) {
        if (UIContext.get() == null) throw new RuntimeException("It's not possible to instanciate a pusher in a new Thread.");
        PPusher pusher = UIContext.get().getAttribute(PUSHER);
        if (pusher != null) return pusher;
        pusher = new PPusher(pollingDelay, ping);
        UIContext.get().setAttribute(PUSHER, pusher);
        return pusher;
    }

    public static PPusher initialize() {
        return initialize(1000, 5 * 1000);
    }

    public static PPusher get() {
        if (UIContext.get() == null) throw new RuntimeException("It's not possible to instanciate a pusher in a new Thread.");

        final PPusher pusher = UIContext.get().getAttribute(PUSHER);
        if (pusher == null) { throw new RuntimeException("The pusher must be initialize before using it"); }
        return pusher;
    }

    public void close() {
        if (websocket == null) return;
        websocket.close();
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.PUSHER;
    }

    public UIContext getUiContext() {
        return uiContext;
    }

    public TxnSocketContext getTxContext() {
        return txnContext;
    }

    public void begin() {
        uiContext.acquire();
        UIContext.setCurrent(uiContext);
    }

    public void end() {
        UIContext.remove();
        uiContext.release();
    }

    @Override
    public void onClientData(final JsonObject event) {
        if (event.containsKey(Model.ERROR_MSG)) {
            log.warn("Failed to open websocket connection. Falling back to polling.");
            txnContext.switchToPollingMode();
            doOpen();
        } else if (event.containsKey(Model.POLL)) {
            txnContext.flushNow();
        }
    }

    public PusherState getPusherState() {
        return pusherState;
    }

    public void addConnectionListener(final ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    public void removeConnectionListener(final ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    public void addDataListener(final DataListener listener) {
        listenerCollection.register(listener);
    }

    public void removeDataListener(final DataListener listener) {
        listenerCollection.unregister(listener);
    }

    public void pushBatchToClient(final Collection<Object> collection) {
        if (pusherState != PusherState.STARTED) {
            if (log.isDebugEnabled()) log.debug("Pusher not started. Skipping message #" + collection);
            return;
        }

        if (UIContext.get() == null) {
            begin();
            try {
                if (listenerCollection.isEmpty()) return;
                final Txn txn = Txn.get();
                txn.begin(txnContext);
                try {
                    for (final Object data : collection) {
                        for (final DataListener listener : listenerCollection) {
                            listener.onData(data);
                        }
                    }
                    txn.commit();
                } catch (final Throwable e) {
                    log.error("Cannot process open socket", e);
                    txn.rollback();
                }
            } finally {
                end();
            }
        } else {
            if (listenerCollection.isEmpty()) return;

            for (final Object data : collection) {
                for (final DataListener listener : listenerCollection) {
                    listener.onData(data);
                }
            }
        }
    }

    public void pushToClient(final Object data) {
        if (pusherState != PusherState.STARTED) {
            if (log.isDebugEnabled()) log.debug("Pusher not started. Skipping message #" + data);
            return;
        }

        if (UIContext.get() == null) {
            begin();
            try {
                if (listenerCollection.isEmpty()) return;
                final Txn txn = Txn.get();
                txn.begin(txnContext);
                try {
                    for (final DataListener listener : listenerCollection) {
                        listener.onData(data);
                    }
                    txn.commit();
                } catch (final Throwable e) {
                    log.error("Cannot process open socket", e);
                    txn.rollback();
                }
            } finally {
                end();
            }
        } else {
            if (listenerCollection.isEmpty()) return;

            for (final DataListener listener : listenerCollection) {
                listener.onData(data);
            }
        }
    }

    @Override
    public void onClose() {
        begin();
        try {
            final Txn txn = Txn.get();
            txn.begin(txnContext);
            try {
                doClose();
                txn.commit();
            } catch (final Throwable e) {
                log.error("Cannot process open socket", e);
                txn.rollback();
            }
        } finally {
            end();
        }
    }

    @Override
    public void onOpen() {
        begin();
        try {
            final Txn txn = Txn.get();
            txn.begin(txnContext);
            try {
                doOpen();
                txn.commit();
            } catch (final Throwable e) {
                log.error("Cannot process open socket", e);
                txn.rollback();
            }
        } finally {
            end();
        }
    }

    public void doOpen() {
        pusherState = PusherState.STARTED;
        for (final ConnectionListener listener : connectionListeners) {
            listener.onOpen();
        }
    }

    public void doClose() {
        pusherState = PusherState.STOPPED;
        for (final ConnectionListener listener : connectionListeners) {
            listener.onClose();
        }
    }

    public boolean execute(final Runnable runnable) {
        try {
            if (UIContext.get() == null) {
                begin();
                try {
                    final Txn txn = Txn.get();
                    txn.begin(txnContext);
                    try {
                        runnable.run();
                        txn.commit();
                    } catch (final Throwable e) {
                        log.error("Cannot process commmand", e);
                        txn.rollback();
                        return false;
                    }
                } finally {
                    end();
                }
            } else {
                runnable.run();
            }
        } catch (final Throwable e) {
            log.error("Cannot execute command : " + runnable, e);
            return false;
        }
        return true;
    }

}
