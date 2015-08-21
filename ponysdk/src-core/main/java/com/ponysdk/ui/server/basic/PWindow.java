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

import javax.json.JsonObject;

import com.ponysdk.core.Parser;
import com.ponysdk.core.tools.ListenerCollection;
import com.ponysdk.ui.server.basic.event.PCloseEvent;
import com.ponysdk.ui.server.basic.event.PCloseHandler;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.Model;

public class PWindow extends PObject {

    public static final long MAIN = -1L;

    private final List<Runnable> postedCommands = new ArrayList<>();
    private final ListenerCollection<PCloseHandler> closeHandlers = new ListenerCollection<>();

    private final boolean loaded = false;

    private final String url;

    private final String name;

    private final String features;

    public PWindow(final String url, final String name, final String features) {
        this.url = url;
        this.name = name;
        this.features = features;

        init();
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        parser.parse(Model.URL, url);
        parser.parse(Model.NAME, name);
        parser.parse(Model.FEATURES, features);
    }

    public void open() {
        saveUpdate(Model.OPEN, true);
    }

    public void close() {
        saveUpdate(Model.CLOSE, true);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.WINDOW;
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (instruction.containsKey(Model.HANDLER_CLOSE_HANDLER.getKey())) {
            fireClose();
            return;
        }

        super.onClientData(instruction);
    }

    public void addCloseHandler(final PCloseHandler handler) {
        closeHandlers.add(handler);
    }

    public void removeCloseHandler(final PCloseHandler handler) {
        closeHandlers.remove(handler);
    }

    private void fireClose() {
        final PCloseEvent e = new PCloseEvent(this);
        for (final PCloseHandler h : closeHandlers) {
            h.onClose(e);
        }
    }

    protected void postOpenerCommand(final Runnable runnable) {
        postedCommands.add(runnable);
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void addWidget(final IsPWidget widget) {
        PRootLayoutPanel.get().add(widget);
    }

    protected void onLoad() {}

}
