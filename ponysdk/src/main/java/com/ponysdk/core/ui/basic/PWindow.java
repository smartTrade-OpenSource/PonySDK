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

package com.ponysdk.core.ui.basic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.servlet.WebsocketEncoder;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.ui.basic.event.PCloseEvent;
import com.ponysdk.core.ui.basic.event.PCloseHandler;
import com.ponysdk.core.ui.basic.event.POpenEvent;
import com.ponysdk.core.ui.basic.event.POpenHandler;

public class PWindow extends PObject {

    static final int EMPTY_WINDOW_ID = -1;

    private Set<POpenHandler> openHandlers;
    private Set<PCloseHandler> closeHandlers;

    private String url;
    private String name;
    private String features;
    private boolean relative = false;

    private Map<String, PRootPanel> panelByZone = new HashMap<>();

    private PWindow() {
        init();
    }

    //TODO nciaravola => feature + relative should be include in an Option Pojo
    protected PWindow(final boolean relative, final String url, final String name, final String features) {
        this.windowID = getMain().getID();
        this.url = url;
        this.name = name;
        this.features = features;
        this.relative = relative;
    }

    @Override
    void init() {
        if (stackedInstructions != null) {
            while (!stackedInstructions.isEmpty()) {
                stackedInstructions.poll().run();
            }
        }
        if (attachListener != null) attachListener.onAttach();
        initialized = true;

        panelByZone.forEach((key, value) -> value.attach(getID()));
    }

    public static PWindow getMain() {
        PWindow mainWindow = UIContext.get().getAttribute(PWindow.class.getCanonicalName());
        if (mainWindow == null) {
            mainWindow = new PWindow() {

                @Override
                public void open() {
                    // Already open
                }

                @Override
                public void close() {
                    // Never be close
                }

                @Override
                public void print() {
                    PScript.execute(this, "window.print()");
                }

            };
            UIContext.get().setAttribute(PWindow.class.getCanonicalName(), mainWindow);
        }
        return mainWindow;
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.WINDOW;
    }

    @Override
    protected void enrichOnInit(final WebsocketEncoder parser) {
        super.enrichOnInit(parser);
        parser.encode(ServerToClientModel.RELATIVE, relative);
        parser.encode(ServerToClientModel.URL, url);
        parser.encode(ServerToClientModel.NAME, name);
        parser.encode(ServerToClientModel.FEATURES, features);
    }

    public void open() {
        if (destroy) return;
        if (!initialized) {
            final WebsocketEncoder parser = Txn.get().getEncoder();
            parser.beginObject();
            parser.encode(ServerToClientModel.TYPE_CREATE, ID);
            parser.encode(ServerToClientModel.WIDGET_TYPE, getWidgetType().getValue());
            enrichOnInit(parser);
            parser.endObject();
            UIContext.get().registerObject(this);

            PWindowManager.preregisterWindow(this);

            writeUpdate(writer -> writer.writeModel(ServerToClientModel.OPEN));
            Txn.get().flush();
        }
    }

    public void print() {
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.PRINT));
    }

    public void close() {
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.CLOSE));
    }

    public void setTitle(final String title) {
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.WINDOW_TITLE, title));
    }

    @Override
    public void onClientData(final JsonObject event) {
        if (event.containsKey(ClientToServerModel.HANDLER_OPEN.toStringValue())) {
            url = event.getString(ClientToServerModel.HANDLER_OPEN.toStringValue());
            PWindowManager.registerWindow(this);

            init();

            if (openHandlers != null) {
                final POpenEvent e = new POpenEvent(this);
                openHandlers.forEach(handler -> handler.onOpen(e));
                openHandlers.clear();
            }
        } else if (event.containsKey(ClientToServerModel.HANDLER_CLOSE.toStringValue())) {
            PWindowManager.unregisterWindow(this);
            if (closeHandlers != null) {
                final PCloseEvent e = new PCloseEvent(this);
                closeHandlers.forEach(handler -> handler.onClose(e));
                closeHandlers.clear();
            }
            destroy();
        } else {
            super.onClientData(event);
        }
    }

    public void addOpenHandler(final POpenHandler handler) {
        if (openHandlers == null) openHandlers = Collections.newSetFromMap(new ConcurrentHashMap<>());
        openHandlers.add(handler);
    }

    public boolean removeOpenHandler(final POpenHandler handler) {
        return openHandlers != null && openHandlers.remove(handler);
    }

    public void addCloseHandler(final PCloseHandler handler) {
        if (closeHandlers == null) closeHandlers = Collections.newSetFromMap(new ConcurrentHashMap<>());
        closeHandlers.add(handler);
    }

    public boolean removeCloseHandler(final PCloseHandler handler) {
        return closeHandlers != null && closeHandlers.remove(handler);
    }

    public void add(final String id, final IsPWidget widget) {
        ensureRootPanel(id).add(widget);
    }

    private PRootPanel ensureRootPanel(final String zoneID) {
        PRootPanel rootPanel = panelByZone.get(zoneID);
        if (rootPanel == null) {
            rootPanel = new PRootPanel(zoneID);
            panelByZone.put(zoneID, rootPanel);
            if (initialized) {
                rootPanel.attach(ID);
            }
        }
        return rootPanel;
    }

    public void add(final IsPWidget widget) {
        ensureRootPanel(null).add(widget);
    }

    public boolean isOpened() {
        return initialized;
    }

    public String getUrl() {
        return url;
    }

    public void clear() {
        panelByZone.forEach((key, value) -> value.clear());
    }

    @Override
    public void destroy() {
        super.destroy();
        panelByZone.forEach((key, value) -> value.destroy());
        panelByZone = null;
    }

    public PRootPanel getPRootPanel() {
        return ensureRootPanel(null);
    }

    public PRootPanel getPRootPanel(final String zoneID) {
        return ensureRootPanel(zoneID);
    }

    public static class TargetAttribut {

        static final String BLANK = "_blank";
        static final String PARENT = "_parent";
        static final String SELF = "_self";
        static final String TOP = "_top";
    }

}
