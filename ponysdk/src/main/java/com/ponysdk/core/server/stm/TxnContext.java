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

package com.ponysdk.core.server.stm;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.servlet.WebSocket;
import com.ponysdk.core.server.servlet.WebsocketEncoder;
import com.ponysdk.core.writer.ModelWriter;

import java.util.HashMap;
import java.util.Map;

public class TxnContext implements TxnListener {

    private final WebSocket socket;
    private final ModelWriter modelWriter;
    private final Map<String, Object> parameters = new HashMap<>();

    private boolean flushNow = false;
    private Application application;

    private UIContext uiContext;

    public TxnContext(final WebSocket socket) {
        this.socket = socket;
        this.modelWriter = new ModelWriter(socket);
    }

    void flush() {
        socket.flush();
    }

    @Override
    public void beforeFlush(final TxnContext txnContext) {
        if (!flushNow) return;

        flushNow = false;

        Txn.get().flush();
    }

    @Override
    public void beforeRollback() {
    }

    @Override
    public void afterFlush(final TxnContext txnContext) {
    }

    public ModelWriter getWriter() {
        return modelWriter;
    }

    public WebsocketEncoder getEncoder() {
        return socket;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(final Application application) {
        this.application = application;
    }

    public void setAttribute(final String name, final Object value) {
        parameters.put(name, value);
    }

    public Object getAttribute(final String name) {
        return parameters.get(name);
    }

    public String getHistoryToken() {
        return null;
    }

    public UIContext getUIContext() {
        return uiContext;
    }

    public void setUIContext(final UIContext uiContext) {
        this.uiContext = uiContext;
    }

    public void sendHeartBeat() {
        socket.sendHeartBeat();
    }

    public void sendRoundTrip() {
        socket.sendRoundTrip();
    }

    public void close() {
        socket.close();
    }

    public void release() {
        socket.release();
    }

    @Override
    public String toString() {
        return "TxnContext{" +
                "flushNow=" + flushNow +
                ", application=" + application +
                ", uiContext=" + uiContext +
                '}';
    }
}
