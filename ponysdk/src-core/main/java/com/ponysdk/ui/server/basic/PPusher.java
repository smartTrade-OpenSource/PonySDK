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
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.socket.ConnectionListener;
import com.ponysdk.core.socket.WebSocket;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.core.stm.TxnSocketContext;
import com.ponysdk.core.tools.ListenerCollection;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

public class PPusher extends PObject implements ConnectionListener {

    private static final Logger log = LoggerFactory.getLogger(PPusher.class);

    public static final String PUSHER = "com.ponysdk.ui.server.basic.PPusher";

    private WebSocket websocket;

    private final UIContext uiContext;

    private final List<ConnectionListener> connectionListeners = new ArrayList<ConnectionListener>();
    private final ListenerCollection<DataListener> listenerCollection = new ListenerCollection<DataListener>();

    private PusherState pusherState = PusherState.STOPPED;

    private final TxnSocketContext txnContext;

    public enum PusherState {
        STOPPED, INITIALIZING, STARTED
    }

    private PPusher(final int pollingDelay, final int maxIdleTime) {
        super();

        this.txnContext = new TxnSocketContext(maxIdleTime);

        create.put(PROPERTY.FIXDELAY, pollingDelay);

        this.pusherState = PusherState.INITIALIZING;
        this.uiContext = UIContext.get();
    }

    public void initialize(final WebSocket websocket) {
        this.websocket = websocket;
        this.websocket.addConnectionListener(this);
        this.txnContext.setSocket(websocket);
    }

    public static PPusher initialize(final int pollingDelay, final int maxIdleTime) {
        if (UIContext.get() == null) throw new RuntimeException("It's not possible to instanciate a pusher in a new Thread.");
        if (UIContext.get().getAttribute(PUSHER) != null) return get();
        final PPusher pusher = new PPusher(pollingDelay, maxIdleTime);
        UIContext.get().setAttribute(PUSHER, pusher);
        return pusher;
    }

    public static PPusher initialize() {
        return initialize(1000, 10 * 1000);
    }

    public static PPusher get() {
        if (UIContext.get() == null) throw new RuntimeException("It's not possible to instanciate a pusher in a new Thread.");

        final PPusher pusher = UIContext.get().getAttribute(PUSHER);
        if (pusher == null) { throw new RuntimeException("The pusher must be initialize before using it"); }
        return pusher;
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.PUSHER;
    }

    public void newCommand(final com.ponysdk.core.command.Command<?> command) {
        command.execute();
    }

    public UIContext getUiContext() {
        return uiContext;
    }

    TxnSocketContext getTxContext() {
        return txnContext;
    }

    @Override
    public void onClientData(final JSONObject event) throws JSONException {
        if (event.has(PROPERTY.ERROR_MSG)) {
            log.warn("Failed to open websocket connection. Falling back to polling.");
            txnContext.switchToPollingMode();
            onOpen();
        } else if (event.has(PROPERTY.POLL)) {
            txnContext.flushNow();
        }
    }

    public PusherState getPusherState() {
        return pusherState;
    }

    public void addConnectionListener(final ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    public void addDataListener(final DataListener listener) {
        listenerCollection.register(listener);
    }

    public void pushToClient(final Object data) {
        if (pusherState != PusherState.STARTED) {
            if (log.isDebugEnabled()) log.debug("Pusher not started. Skipping message #" + data);
            return;
        }

        if (UIContext.get() == null) {
            uiContext.acquire();
            UIContext.setCurrent(uiContext);
            try {
                if (listenerCollection.isEmpty()) return;
                final Txn txn = Txn.get();
                txn.begin(txnContext);
                try {
                    for (final DataListener listener : listenerCollection) {
                        listener.onData(data);
                    }
                    txn.commit();
                } catch (final Exception e) {
                    log.error("Cannot process open socket", e);
                    txn.rollback();
                }
            } finally {
                UIContext.remove();
                uiContext.release();
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
        uiContext.acquire();
        UIContext.setCurrent(uiContext);
        try {
            final Txn txn = Txn.get();
            txn.begin(txnContext);
            try {
                pusherState = PusherState.STOPPED;

                for (final ConnectionListener listener : connectionListeners) {
                    listener.onClose();
                }
                txn.commit();
            } catch (final Exception e) {
                log.error("Cannot process open socket", e);
                txn.rollback();
            }
        } finally {
            UIContext.remove();
            uiContext.release();
        }

    }

    @Override
    public void onOpen() {
        uiContext.acquire();
        UIContext.setCurrent(uiContext);
        try {
            final Txn txn = Txn.get();
            txn.begin(txnContext);
            try {
                pusherState = PusherState.STARTED;

                PusherCommandExecutor.execute(this, new PCommand() {

                    @Override
                    public void execute() {
                        for (final ConnectionListener listener : connectionListeners) {
                            listener.onOpen();
                        }
                    }
                });
                txn.commit();
            } catch (final Exception e) {
                log.error("Cannot process open socket", e);
                txn.rollback();
            }
        } finally {
            UIContext.remove();
            uiContext.release();
        }
    }

    public void execute(final PCommand command) {
        if (UIContext.get() == null) {
            uiContext.acquire();
            UIContext.setCurrent(uiContext);
            try {
                final Txn txn = Txn.get();
                txn.begin(txnContext);
                try {
                    PusherCommandExecutor.execute(this, command);
                    txn.commit();
                } catch (final Exception e) {
                    log.error("Cannot process open socket", e);
                    txn.rollback();
                }
            } finally {
                UIContext.remove();
                uiContext.release();
            }
        } else {
            PusherCommandExecutor.execute(this, command);
        }
    }

}
