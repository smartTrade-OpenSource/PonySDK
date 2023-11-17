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

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.concurrent.UIContext;
import com.ponysdk.core.server.websocket.WebsocketEncoder;
import com.ponysdk.core.ui.basic.PWindow;

import java.lang.ref.WeakReference;

public class ModelWriter {

    private final WebsocketEncoder encoder;

    private WeakReference<PWindow> currentWindow;

    public ModelWriter(final WebsocketEncoder encoder) {
        this.encoder = encoder;
    }

    public void beginObject(final PWindow window) {
        encoder.beginObject();

        if (currentWindow == null || !window.equals(currentWindow.get())) {
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
        if (UIContext.get().isAlive()) {
            encoder.encode(model, value);
        }
    }

    public void endObject() {
        if (UIContext.get().isAlive()) {
            encoder.endObject();
        }
    }

    public PWindow getCurrentWindow() {
        return currentWindow != null ? currentWindow.get() : null;
    }

}
