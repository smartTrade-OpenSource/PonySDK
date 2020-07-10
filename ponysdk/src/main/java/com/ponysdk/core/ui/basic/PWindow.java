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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.ui.basic.event.PCloseEvent;
import com.ponysdk.core.ui.basic.event.PCloseHandler;
import com.ponysdk.core.ui.basic.event.POpenEvent;
import com.ponysdk.core.ui.basic.event.POpenHandler;
import com.ponysdk.core.ui.basic.event.PVisibilityEvent;
import com.ponysdk.core.ui.basic.event.PVisibilityEvent.PVisibilityHandler;
import com.ponysdk.core.ui.formatter.TextFunction;
import com.ponysdk.core.util.SetUtils;
import com.ponysdk.core.writer.ModelWriter;

public class PWindow extends PObject {

    private static final String CANONICAL_NAME = PWindow.class.getCanonicalName();

    private Set<POpenHandler> openHandlers;
    private Set<PCloseHandler> closeHandlers;
    private Set<PWindow> subWindows;

    private String url;
    private String name;
    private String features;
    private boolean relative = false;
    private final Location location;

    private Map<String, PRootPanel> panelByZone = new HashMap<>(8);

    private Map<TextFunction, PFunction> functions;

    private PWindow parent;

    private List<PVisibilityHandler> visibilityHandlers;
    private boolean shown = true;

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

    protected PWindow(final PWindow parent, final boolean relative, final String url, final String name, final String features) {
        this(relative, url, name, features);
        if (parent != null) {
            parent.addWindow(this);
            this.parent = parent;
        }
    }

    @Override
    void init() {
        if (initialized) return;

        if (stackedInstructions != null) {
            stackedInstructions.values().forEach(Runnable::run);
            stackedInstructions = null;
        }

        initialized = true;

        panelByZone.forEach((key, value) -> value.attach(this, null));
        if (initializeListeners != null) initializeListeners.forEach(listener -> listener.onInitialize(this));

        UIContext.get().addContextDestroyListener(uiContext -> onDestroy());
    }

    public static PWindow getMain() {
        PWindow mainWindow = UIContext.get().getAttribute(CANONICAL_NAME);
        if (mainWindow == null) {
            mainWindow = new PMainWindow();
            UIContext.get().setAttribute(CANONICAL_NAME, mainWindow);
        }
        return mainWindow;
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.WINDOW;
    }

    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        super.enrichForCreation(writer);
        writer.write(ServerToClientModel.RELATIVE, relative);
        writer.write(ServerToClientModel.URL, url);
        writer.write(ServerToClientModel.NAME, name);
        writer.write(ServerToClientModel.FEATURES, features);
    }

    public void open() {
        if (destroy) return;
        if (!initialized) {
            final ModelWriter writer = UIContext.get().getWriter();
            writer.beginObject(window);
            writer.write(ServerToClientModel.TYPE_CREATE, ID);
            writer.write(ServerToClientModel.WIDGET_TYPE, getWidgetType().getValue());
            enrichForCreation(writer);
            writer.endObject();
            UIContext.get().registerObject(this);

            PWindowManager.preregisterWindow(this);

            writeUpdate(callback -> callback.write(ServerToClientModel.OPEN));
            Txn.get().flush();
        }
    }

    /**
     * Opens the Print Dialog to print the current document.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/Window/print">MDN</a>
     */
    public void print() {
        saveUpdate(writer -> writer.write(ServerToClientModel.PRINT));
    }

    /**
     * Resizes the current window by a certain amount.
     *
     * @param xDelta is the number of pixels to grow the window horizontally.
     * @param yDelta is the number of pixels to grow the window vertically.
     * @see <a href="https://developer.mozilla.org/fr/docs/Web/API/Window/resizeBy">MDN</a>
     */
    public void resizeBy(final float xDelta, final float yDelta) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.RESIZE_BY_X, (double) xDelta);
            writer.write(ServerToClientModel.RESIZE_BY_Y, (double) yDelta);
        });
    }

    /**
     * Dynamically resizes window.
     *
     * @param width is an integer representing the new outerWidth in pixels (including scroll bars,
     *            title bars, etc).
     * @param height is an integer value representing the new outerHeight in pixels (including scroll
     *            bars, title bars, etc).
     * @see <a href="https://developer.mozilla.org/fr/docs/Web/API/Window/resizeTo">MDN</a>
     */
    public void resizeTo(final int width, final int height) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.RESIZE_TO_WIDTH, width);
            writer.write(ServerToClientModel.RESIZE_TO_HEIGHT, height);
        });
    }

    /**
     * Moves the current window by a specified amount.
     *
     * @param deltaX is the amount of pixels to move the window horizontally.
     * @param deltaY is the amount of pixels to move the window vertically.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/window/moveBy">MDN</a>
     */
    public void moveBy(final float deltaX, final float deltaY) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.MOVE_BY_X, (double) deltaX);
            writer.write(ServerToClientModel.MOVE_BY_Y, (double) deltaY);
        });
    }

    /**
     * Moves the window to the specified coordinates.
     *
     * @param x is the horizontal coordinate to be moved to.
     * @param y is the vertical coordinate to be moved to.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/window/moveTo">MDN</a>
     */
    public void moveTo(final float x, final float y) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.MOVE_TO_X, (double) x);
            writer.write(ServerToClientModel.MOVE_TO_Y, (double) y);
        });
    }

    /**
     * Makes a request to bring the window to the front. It may fail due to user settings and the window isn't
     * guaranteed to be frontmost before this method returns.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/Window/focus">MDN</a>
     */
    public void focus() {
        saveUpdate(writer -> writer.write(ServerToClientModel.FOCUS, true));
    }

    /**
     * The Location.reload() method reloads the resource from the current URL
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/Location/reload">MDN</a>
     */
    public void reload() {
        saveUpdate(writer -> writer.write(ServerToClientModel.RELOAD));
    }

    /**
     * The Window.close() method closes the current window, or the window on which it was called.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/Window/close">MDN</a>
     */
    public void close() {
        if (!destroy) saveUpdate(writer -> writer.write(ServerToClientModel.CLOSE));
    }

    public void setTitle(final String title) {
        saveUpdate(ServerToClientModel.WINDOW_TITLE, title);
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
                for (final Iterator<POpenHandler> iter = openHandlers.iterator(); iter.hasNext();) {
                    final POpenHandler handler = iter.next();
                    iter.remove();
                    handler.onOpen(e);
                }
            }
        } else if (event.containsKey(ClientToServerModel.HANDLER_CLOSE.toStringValue())) {
            PWindowManager.unregisterWindow(this);
            if (subWindows != null) {
                for (final Iterator<PWindow> iter = subWindows.iterator(); iter.hasNext();) {
                    final PWindow window = iter.next();
                    iter.remove();
                    window.close();
                }
            }
            if (closeHandlers != null) {
                final PCloseEvent e = new PCloseEvent(this);
                for (final Iterator<PCloseHandler> iter = closeHandlers.iterator(); iter.hasNext();) {
                    final PCloseHandler handler = iter.next();
                    iter.remove();
                    handler.onClose(e);
                }
            }
            onDestroy();
        } else if (event.containsKey(ClientToServerModel.HANDLER_DESTROY.toStringValue())) {
            onDestroy();
        } else if (event.containsKey(ClientToServerModel.HANDLER_DOCUMENT_VISIBILITY.toStringValue())) {
            shown = event.getBoolean(ClientToServerModel.HANDLER_DOCUMENT_VISIBILITY.toStringValue());
            if (visibilityHandlers != null) {
                final PVisibilityEvent visibilityEvent = new PVisibilityEvent(this, shown);
                visibilityHandlers.forEach(handler -> handler.onVisibility(visibilityEvent));
            }
        } else {
            super.onClientData(event);
        }
    }

    public void addOpenHandler(final POpenHandler handler) {
        if (openHandlers == null) openHandlers = SetUtils.newArraySet(4);
        openHandlers.add(handler);
    }

    public boolean removeOpenHandler(final POpenHandler handler) {
        return openHandlers != null && openHandlers.remove(handler);
    }

    public void addCloseHandler(final PCloseHandler handler) {
        if (closeHandlers == null) closeHandlers = SetUtils.newArraySet(4);
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
            if (isInitialized()) rootPanel.attach(this, null);
        }
        return rootPanel;
    }

    public boolean isOpened() {
        return initialized && !destroy;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public void clear() {
        panelByZone.forEach((key, value) -> value.clear());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (panelByZone != null) {
            panelByZone.forEach((key, value) -> value.onDestroy());
            panelByZone = null;
        }
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
        if (subWindows == null) subWindows = SetUtils.newArraySet(4);
        subWindows.add(window);
    }

    private Object removeWindow(final PWindow window) {
        return subWindows != null && subWindows.remove(window);
    }

    public PWindow getParent() {
        return parent;
    }

    PFunction getPFunction(final TextFunction function) {
        return safeFunctions().computeIfAbsent(function, this::createPFunction);
    }

    private Map<TextFunction, PFunction> safeFunctions() {
        if (functions == null) {
            functions = new HashMap<>();
        }
        return functions;
    }

    private PFunction createPFunction(final TextFunction function) {
        final PFunction pf = new PFunction(function);
        pf.attach(this, null);
        return pf;
    }

    /**
     * Add visibility handler
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Events/visibilitychange">MDN</a>
     */
    public void addVisibilityHandler(final PVisibilityHandler visibilityHandler) {
        if (visibilityHandlers == null) visibilityHandlers = new ArrayList<>();
        visibilityHandlers.add(visibilityHandler);
    }

    /**
     * Remove visibility handler
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Events/visibilitychange">MDN</a>
     */
    public void removeVisibilityHandler(final PVisibilityHandler visibilityHandler) {
        if (visibilityHandlers != null) {
            visibilityHandlers.remove(visibilityHandler);
            if (visibilityHandlers.isEmpty()) saveRemoveHandler(HandlerModel.HANDLER_VISIBILITY);
        }
    }

    /**
     * The opposite of document.hidden property that returns a Boolean value indicating if the page is considered hidden
     * or not.
     *
     * @see <a href="https://developer.mozilla.org/fr/docs/Web/API/Document/hidden">MDN</a>
     */
    public boolean isShown() {
        return shown;
    }

    @Override
    public String toString() {
        return super.toString() + ", name = " + name;
    }

    public static final class Location {

        private final PWindow window;

        Location(final PWindow window) {
            this.window = window;
        }

        public void replace(final String url) {
            window.saveUpdate(writer -> writer.write(ServerToClientModel.WINDOW_LOCATION_REPLACE, url));
        }
    }

    public static final class Parameter {

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

    private static final class PMainWindow extends PWindow {

        @Override
        final void init() {
            final ModelWriter writer = UIContext.get().getWriter();
            writer.beginObject(window);
            writer.write(ServerToClientModel.TYPE_CREATE, ID);
            writer.write(ServerToClientModel.WIDGET_TYPE, getWidgetType().getValue());
            writer.endObject();
            UIContext.get().registerObject(this);
            initialized = true;

            UIContext.get().addContextDestroyListener(uiContext -> onDestroy());
        }

        @Override
        public final void open() {
            // Already open
        }

        @Override
        public final void close() {
            // should destroy the main window ??
        }

        @Override
        protected WidgetType getWidgetType() {
            return WidgetType.BROWSER;
        }

    }

    protected String dumpDOM() {
        String DOM = "<body>";
        for (PRootPanel panel : panelByZone.values()) {
            DOM += panel.dumpDOM();
        }
        DOM += "</body>";
        return DOM;
    }

}
