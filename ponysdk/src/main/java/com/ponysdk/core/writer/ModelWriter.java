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

package com.ponysdk.core.writer;

import java.lang.ref.WeakReference;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.websocket.WebsocketEncoder;
import com.ponysdk.core.ui.basic.PWindow;

public class ModelWriter {

    private WebsocketEncoder encoder;

    private WeakReference<PWindow> currentWindow;

    public ModelWriter(final WebsocketEncoder encoder) {
        this.encoder = encoder;
    }

    /**
     * Swaps the underlying encoder — used during transparent WebSocket reconnection
     * to reattach a live UIContext to a new WebSocket session.
     */
    public void setEncoder(final WebsocketEncoder encoder) {
        this.encoder = encoder;
        this.currentWindow = null; // force WINDOW_ID re-emission on next write
    }

    public void beginObject(final PWindow window) {
        encoder.beginObject();

        if (currentWindow == null || currentWindow.get() != window) {
            currentWindow = new WeakReference<>(window);
            encoder.encode(ServerToClientModel.WINDOW_ID, window.getID());
        }
    }

    public void write(final ServerToClientModel model) {
        write(model, null);
    }

    /**
     * @param value The type can be primitives, String or Object[]
     */
    public void write(final ServerToClientModel model, final Object value) {
        final UIContext ctx = UIContext.get();
        if (ctx != null && ctx.isAlive()) {
            encoder.encode(model, value);
        }
    }

    public void endObject() {
        final UIContext ctx = UIContext.get();
        if (ctx != null && ctx.isAlive()) {
            encoder.endObject();
        }
    }

    public PWindow getCurrentWindow() {
        return currentWindow != null ? currentWindow.get() : null;
    }

}
