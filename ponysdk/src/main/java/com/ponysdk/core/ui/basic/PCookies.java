/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.ui.basic;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.util.SetUtils;
import com.ponysdk.core.writer.ModelWriter;

import javax.json.JsonObject;
import java.util.Objects;
import java.util.Set;

public class PCookies {

    private static final int ID = 0; // reserved

    private boolean isInitialized;

    private String value;

    private Set<PValueChangeHandler<String>> valueChangeHandlers;

    private Set<PObject.InitializeListener> initializeListeners;

    public String get() {
        return value;
    }

    public void set(final String value) {
        if (Objects.equals(this.value, value)) return;

        this.value = value;

        final ModelWriter writer = UIContext.get().getWriter();
        writer.beginObject(PWindow.getMain());
        writer.write(ServerToClientModel.TYPE_UPDATE, ID);
        writer.write(ServerToClientModel.ADD_COOKIE, value);
        writer.endObject();
    }

    public void onClientData(final JsonObject event) {
        if (!isInitialized) {
            isInitialized = true;
            if (initializeListeners != null) initializeListeners.forEach(listener -> listener.onInitialize(this));
        }

        String value = event.getJsonString(ClientToServerModel.COOKIES.toStringValue()).getString();

        if (Objects.equals(value, this.value)) return;

        this.value = value;

        if (valueChangeHandlers != null) {
            valueChangeHandlers.forEach(h -> h.onValueChange(new PValueChangeEvent<String>(this, value)));
        }

    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void addInitializeListener(final PObject.InitializeListener listener) {
        if (initializeListeners == null) initializeListeners = SetUtils.newArraySet(4);
        initializeListeners.add(listener);
    }

    public void addValueChangeHandler(final PValueChangeHandler<String> handler) {
        if (valueChangeHandlers == null) valueChangeHandlers = SetUtils.newArraySet(4);
        this.valueChangeHandlers.add(handler);
    }
}
