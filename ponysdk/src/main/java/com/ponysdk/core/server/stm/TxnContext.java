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
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.writer.ModelWriter;

public class TxnContext implements TxnListener {

    private final WebSocket socket;
    private final ModelWriter modelWriter;

    private boolean flushNow = false;
    private Application application;

    public TxnContext(final WebSocket socket) {
        this.socket = socket;
        this.modelWriter = new ModelWriter(socket);
    }

    public ModelWriter getWriter() {
        return modelWriter;
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
        // Nothing to do
    }

    @Override
    public void afterFlush(final TxnContext txnContext) {
        // Nothing to do
    }

    public WebSocket getSocket() {
        return socket;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(final Application application) {
        this.application = application;
    }

    public <T> T getAttribute(final String name) {
        return application != null ? application.getAttribute(name) : null;
    }

    public void setAttribute(final String name, final Object value) {
        if (application != null) application.setAttribute(name, value);
    }

    public String getId() {
        return application != null ? application.getId() : null;
    }

    public void registerUIContext(final UIContext uiContext) {
        if (application != null) application.registerUIContext(uiContext);
    }

    public void deregisterUIContext(final int ID) {
        if (application != null) application.deregisterUIContext(ID);
    }

    @Override
    public String toString() {
        return "TxnContext{" + "flushNow=" + flushNow + ", modelWriter=" + modelWriter + '}';
    }

}
