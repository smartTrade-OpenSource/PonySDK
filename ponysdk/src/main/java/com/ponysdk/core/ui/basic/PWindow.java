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
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.ui.basic.event.PCloseEvent;
import com.ponysdk.core.ui.basic.event.PCloseHandler;
import com.ponysdk.core.ui.basic.event.POpenEvent;
import com.ponysdk.core.ui.basic.event.POpenHandler;
import com.ponysdk.core.writer.ModelWriter;

public class PWindow extends PObject {

    private Set<POpenHandler> openHandlers;
    private Set<PCloseHandler> closeHandlers;
    private Set<PWindow> subWindows;

    private String url;
    private String name;
    private String features;
    private boolean relative = false;
    private final Location location;

    private Map<String, PRootPanel> panelByZone = new HashMap<>();

    PWindow() {
        this.location = new Location(this);
        this.window = this;
        init();
    }
    // return
    // "resizable=yes,location=0,status=0,scrollbars=0,height=800,width=1200,title="
    // + getName();

    // TODO nciaravola => feature + relative should be include in an Option Pojo
    protected PWindow(final boolean relative, final String url, final String name, final String features) {
        this.window = getMain();
        this.url = url;
        this.name = name;
        this.features = features;
        this.relative = relative;
        this.location = new Location(this);
    }

    protected PWindow(final PWindow parentWindow, final boolean relative, final String url, final String name, final String features) {
        this(relative, url, name, features);
        if (parentWindow != null) parentWindow.addWindow(this);
    }

    @Override
    void init() {
        if (initialized) return;

        if (stackedInstructions != null) {
            while (!stackedInstructions.isEmpty()) {
                stackedInstructions.poll().run();
            }
        }
        initialized = true;

        panelByZone.forEach((key, value) -> value.attach(this));
        if (initializeListener != null) initializeListener.onInitialize(this);
    }

    public static PWindow getMain() {
        PWindow mainWindow = UIContext.get().getAttribute(PWindow.class.getCanonicalName());
        if (mainWindow == null) {
            mainWindow = new PMainWindow();
            UIContext.get().setAttribute(PWindow.class.getCanonicalName(), mainWindow);
        }
        return mainWindow;
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.WINDOW;
    }

    @Override
    protected void enrichOnInit(final ModelWriter writer) {
        super.enrichOnInit(writer);
        writer.write(ServerToClientModel.RELATIVE, relative);
        writer.write(ServerToClientModel.URL, url);
        writer.write(ServerToClientModel.NAME, name);
        writer.write(ServerToClientModel.FEATURES, features);
    }

    public void open() {
        if (destroy) return;
        if (!initialized) {
            final ModelWriter writer = Txn.getWriter();
            writer.beginObject();
            if (window != PWindow.getMain()) writer.write(ServerToClientModel.WINDOW_ID, window.getID());
            writer.write(ServerToClientModel.TYPE_CREATE, ID);
            writer.write(ServerToClientModel.WIDGET_TYPE, getWidgetType().getValue());
            enrichOnInit(writer);
            writer.endObject();
            UIContext.get().registerObject(this);

            PWindowManager.preregisterWindow(this);

            writeUpdate(callback -> callback.write(ServerToClientModel.OPEN));
            Txn.get().flush();
        }
    }

    public void print() {
        saveUpdate(writer -> writer.write(ServerToClientModel.PRINT));
    }

    public void close() {
        if (!destroy) saveUpdate(writer -> writer.write(ServerToClientModel.CLOSE));
    }

    public void setTitle(final String title) {
        saveUpdate(writer -> writer.write(ServerToClientModel.WINDOW_TITLE, title));
    }

    public Location getLocation() {
        return location;
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
            if (subWindows != null) {
                subWindows.forEach(window -> window.close());
                subWindows.clear();
            }
            if (closeHandlers != null) {
                final PCloseEvent e = new PCloseEvent(this);
                closeHandlers.forEach(handler -> handler.onClose(e));
                closeHandlers.clear();
            }
            onDestroy();
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

    public void add(final IsPWidget widget) {
        add(null, widget);
    }

    public void add(final String id, final IsPWidget widget) {
        if (!destroy) ensureRootPanel(id).add(widget);
    }

    private PRootPanel ensureRootPanel(final String zoneID) {
        PRootPanel rootPanel = panelByZone.get(zoneID);
        if (rootPanel == null) {
            rootPanel = new PRootPanel(zoneID);
            panelByZone.put(zoneID, rootPanel);
            if (isInitialized()) rootPanel.attach(this);
        }
        return rootPanel;
    }

    public boolean isOpened() {
        return initialized && !destroy;
    }

    public String getUrl() {
        return url;
    }

    public void clear() {
        panelByZone.forEach((key, value) -> value.clear());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        panelByZone.forEach((key, value) -> value.onDestroy());
        panelByZone = null;
    }

    public PRootPanel getPRootPanel() {
        return ensureRootPanel(null);
    }

    public PRootPanel getPRootPanel(final String zoneID) {
        return ensureRootPanel(zoneID);
    }

    public static boolean isMain(final PWindow window) {
        return getMain() == window;
    }

    private void addWindow(final PWindow window) {
        if (subWindows == null) subWindows = Collections.newSetFromMap(new ConcurrentHashMap<>());
        window.addCloseHandler(event -> removeWindow(window));
        subWindows.add(window);
    }

    private Object removeWindow(final PWindow window) {
        return subWindows != null && subWindows.remove(window);
    }

    @Override
    public String toString() {
        return super.toString() + ", name = " + name;
    }

    public class Location {

        private final PWindow window;

        Location(final PWindow window) {
            this.window = window;
        }

        public void replace(final String url) {
            window.saveUpdate(writer -> writer.write(ServerToClientModel.WINDOW_LOCATION_REPLACE, url));
        }
    }

    public class Parameter {

        private String title;
        private Integer fullScreen;
        private Integer height = 100;
        private Integer width = 100;
        private Double left;
        private Double top;
        private Boolean menuBar;
        private Boolean scrollbars;
        private Boolean status;
        private Boolean titlebar;
        private Boolean toolbar;

        public Integer getFullScreen() {
            return fullScreen;
        }

        public void setFullScreen(final Integer fullScreen) {
            this.fullScreen = fullScreen;
        }

        public Integer getHeight() {
            return height;
        }

        public void setHeight(final Integer height) {
            this.height = height;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(final Integer width) {
            this.width = width;
        }

        public Double getLeft() {
            return left;
        }

        public void setLeft(final Double left) {
            this.left = left;
        }

        public Double getTop() {
            return top;
        }

        public void setTop(final Double top) {
            this.top = top;
        }

        public Boolean getMenuBar() {
            return menuBar;
        }

        public void setMenuBar(final Boolean menuBar) {
            this.menuBar = menuBar;
        }

        public Boolean getScrollbars() {
            return scrollbars;
        }

        public void setScrollbars(final Boolean scrollbars) {
            this.scrollbars = scrollbars;
        }

        public Boolean getStatus() {
            return status;
        }

        public void setStatus(final Boolean status) {
            this.status = status;
        }

        public Boolean getTitlebar() {
            return titlebar;
        }

        public void setTitlebar(final Boolean titlebar) {
            this.titlebar = titlebar;
        }

        public Boolean getToolbar() {
            return toolbar;
        }

        public void setToolbar(final Boolean toolbar) {
            this.toolbar = toolbar;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(final String title) {
            this.title = title;
        }

    }

}
