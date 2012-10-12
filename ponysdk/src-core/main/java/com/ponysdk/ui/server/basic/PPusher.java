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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.instruction.Instruction;
import com.ponysdk.core.socket.ConnectionListener;
import com.ponysdk.core.socket.WebSocket;
import com.ponysdk.core.tools.ListenerCollection;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

public class PPusher extends PObject implements ConnectionListener {

    private static final Logger log = LoggerFactory.getLogger(PPusher.class);

    public static final String PUSHER = "com.ponysdk.ui.server.basic.PPusher";

    private WebSocket websocket;
    private final UIContext uiContext;

    private final List<ConnectionListener> connectionListeners = new ArrayList<ConnectionListener>();
    private final ListenerCollection<DataListener> listenerCollection = new ListenerCollection<DataListener>();

    private final List<Instruction> updates = new ArrayList<Instruction>();

    private boolean polling = false;
    private PusherState pusherState = PusherState.STOPPED;

    public enum PusherState {
        STOPPED, INITIALIZING, STARTED
    }

    private PPusher(final int pollingDelay) {
        super();

        create.put(PROPERTY.FIXDELAY, pollingDelay);

        pusherState = PusherState.INITIALIZING;
        uiContext = UIContext.get();
    }

    public void initialize(final WebSocket websocket) {
        this.websocket = websocket;
        this.websocket.addConnectionListener(this);
    }

    public static PPusher initialize(final int pollingDelay) {
        if (UIContext.get() == null) throw new RuntimeException("It's not possible to instanciate a pusher in a new Thread.");
        if (UIContext.get().getAttribute(PUSHER) != null) return get();
        final PPusher pusher = new PPusher(pollingDelay);
        UIContext.get().setAttribute(PUSHER, pusher);
        return pusher;
    }

    public static PPusher initialize() {
        return initialize(1000);
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

    public void begin() {
        uiContext.acquire();
        UIContext.setCurrent(uiContext);
    }

    public void end() {
        UIContext.remove();
        uiContext.release();
    }

    public void flush() throws IOException, JSONException {
        if (polling) {
            final Collection<Instruction> instructions = uiContext.clearPendingInstructions();
            updates.addAll(instructions);
            return;
        }

        final JSONObject jsonObject = new JSONObject();
        if (!uiContext.flushInstructions(jsonObject)) return;

        jsonObject.put(APPLICATION.SEQ_NUM, uiContext.getAndIncrementNextSentSeqNum());
        websocket.send(jsonObject.toString());
    }

    @Override
    public void onClientData(final JSONObject event) throws JSONException {
        if (event.has(PROPERTY.ERROR_MSG)) {
            log.warn("Failed to open websocket connection. Falling back to polling.");
            pusherState = PusherState.STARTED;
            polling = true;
        } else if (event.has(PROPERTY.POLL)) {
            for (final Instruction instruction : updates) {
                getUIContext().stackInstruction(instruction);
            }
            updates.clear();
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

        begin();
        try {
            if (listenerCollection.isEmpty()) return;

            for (final DataListener listener : listenerCollection) {
                listener.onData(data);
            }
            PPusher.get().flush();
        } catch (final Exception exception) {
            log.error("Cannot push data", exception);
        } finally {
            PPusher.get().end();
        }
    }

    @Override
    public void onClose() {
        pusherState = PusherState.STOPPED;
        for (final ConnectionListener listener : connectionListeners) {
            listener.onClose();
        }
    }

    @Override
    public void onOpen() {
        pusherState = PusherState.STARTED;
        for (final ConnectionListener listener : connectionListeners) {
            listener.onOpen();
        }
    }

}
