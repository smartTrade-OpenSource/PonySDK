package com.ponysdk.sample.client.page.addon;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;

/**
 * PAddOnComposite wrapping the FlexLayout JS library.
 * <p>
 * Provides a dockable panel layout with:
 * <ul>
 *   <li>Drag/drop tab management (reorder, split, move between tabsets)</li>
 *   <li>Programmatic tab add/remove with PWidget content</li>
 *   <li>External drag sources that create new tabs with server-instantiated widgets</li>
 *   <li>Layout persistence (save/load model as JSON)</li>
 *   <li>Change notifications for all layout modifications</li>
 * </ul>
 */
public class FlexLayoutAddon extends PAddOnComposite<PFlowPanel> {

    private static final String EVT_TAB_CLOSED = "tabClosed";
    private static final String EVT_EXTERNAL_DROP = "externalDrop";
    private static final String EVT_MODEL_CHANGE = "modelChange";
    private static final String EVT_MODEL_SNAPSHOT = "modelSnapshot";
    private static final String EVT_ACTION = "action";
    private static final String EVT_POP_OUT = "popOut";
    private static final String EVT_POP_IN = "popIn";
    private static final String EVT_POP_OUT_STATE = "popOutState";
    private static final String EVT_REHYDRATE = "rehydrate";

    private final Map<String, PWidget> tabWidgets = new HashMap<>();
    private final Map<String, PopOutInfo> popOutTabs = new HashMap<>();
    private ExternalDropHandler onExternalDrop;
    private Consumer<String> onTabClosed;
    private Consumer<String> onModelChange;
    private Consumer<JsonObject> onAction;
    private Consumer<String> onPopOut;
    private Consumer<String> onPopIn;
    private Consumer<String> modelSnapshotCallback;
    private Consumer<String> popOutStateCallback;
    private final Map<String, String> tabComponents = new HashMap<>();
    private java.util.function.BiFunction<String, String, PWidget> widgetFactory;

    public FlexLayoutAddon() {
        this(null, null, null);
    }

    public FlexLayoutAddon(final String modelJson, final String theme) {
        this(modelJson, theme, null);
    }

    public FlexLayoutAddon(final String modelJson, final String theme, final String bordersJson) {
        super(Element.newPFlowPanel(), buildArgs(modelJson, theme, bordersJson));
        setTerminalHandler(event -> handleClientEvent(event.getData()));
    }

    // ─── Tab Management ──────────────────────────────────────────

    /**
     * Add a tab hosting a PWidget in the first available tabset.
     */
    public void addTab(final String tabId, final String tabName, final PWidget content) {
        addTab(tabId, tabName, content, null);
    }

    /**
     * Add a tab hosting a PWidget in a specific tabset.
     */
    public void addTab(final String tabId, final String tabName, final PWidget content, final String tabsetId) {
        widget.add(content);
        tabWidgets.put(tabId, content);
        callTerminalMethod("addTab", tabId, tabName, String.valueOf(content.getID()), tabsetId);
    }

    /**
     * Attach a widget to an existing tab (created by external drag).
     */
    public void attachWidgetToTab(final String tabId, final PWidget content) {
        attachWidgetToTab(tabId, content, null);
    }

    public void attachWidgetToTab(final String tabId, final PWidget content, final String component) {
        widget.add(content);
        tabWidgets.put(tabId, content);
        if (component != null) tabComponents.put(tabId, component);
        callTerminalMethod("attachWidget", tabId, String.valueOf(content.getID()));
    }

    /**
     * Remove a tab by ID.
     */
    public void removeTab(final String tabId) {
        callTerminalMethod("removeTab", tabId);
        final PWidget removed = tabWidgets.remove(tabId);
        tabComponents.remove(tabId);
        if (removed != null) removed.removeFromParent();
        // Close PWindow if tab was popped out
        final PopOutInfo info = popOutTabs.remove(tabId);
        if (info != null && info.pWindow != null) info.pWindow.close();
    }

    // ─── Sidebar / Border Management ─────────────────────────────

    /**
     * Add a tab to a sidebar (border panel).
     * @param side "left", "right", or "bottom"
     */
    public void addBorderTab(final String side, final String tabId, final String tabName, final PWidget content) {
        addBorderTab(side, tabId, tabName, content, -1, null);
    }

    public void addBorderTab(final String side, final String tabId, final String tabName, final PWidget content, final int index) {
        addBorderTab(side, tabId, tabName, content, index, null);
    }

    public void addBorderTab(final String side, final String tabId, final String tabName, final PWidget content, final int index, final String icon) {
        widget.add(content);
        tabWidgets.put(tabId, content);
        callTerminalMethod("addBorderTab", side, tabId, tabName, String.valueOf(content.getID()), index >= 0 ? index : null, icon);
    }

    /**
     * Remove a tab from a sidebar.
     */
    public void removeBorderTab(final String side, final String tabId) {
        callTerminalMethod("removeBorderTab", side, tabId);
        final PWidget removed = tabWidgets.remove(tabId);
        tabComponents.remove(tabId);
        if (removed != null) removed.removeFromParent();
    }

    /**
     * Toggle-select a border tab (opens/closes the sidebar panel).
     */
    public void selectBorderTab(final String side, final String tabId) {
        callTerminalMethod("selectBorderTab", side, tabId);
    }

    /**
     * Move a tab from the main layout into a sidebar.
     */
    public void moveToBorder(final String tabId, final String side) {
        callTerminalMethod("moveToBorder", tabId, side);
    }

    /**
     * Move a tab from a sidebar back into the main layout.
     */
    public void moveFromBorder(final String side, final String tabId, final String tabsetId) {
        callTerminalMethod("moveFromBorder", side, tabId, tabsetId);
    }

    /**
     * Toggle visibility of a sidebar (add/remove border).
     */
    public void toggleBorder(final String side) {
        callTerminalMethod("toggleBorder", side);
    }

    /**
     * Set the tab display style for a sidebar.
     * @param tabStyle "auto" (icon if available, else label), "icon", "label", or "iconLabel"
     */
    public void setBorderTabStyle(final String side, final String tabStyle) {
        callTerminalMethod("setBorderTabStyle", side, tabStyle);
    }

    // ─── Model Persistence ───────────────────────────────────────

    /**
     * Load a complete model JSON, replacing the current layout.
     * Destroys all existing tabs, widgets, and pop-out windows.
     */
    public void loadModel(final String modelJson) {
        // Close all pop-out windows
        for (final PopOutInfo info : popOutTabs.values()) {
            if (info.pWindow != null) info.pWindow.close();
        }
        popOutTabs.clear();
        // Remove all widgets
        for (final PWidget w : tabWidgets.values()) {
            w.removeFromParent();
        }
        tabWidgets.clear();
        tabComponents.clear();
        // Load new model on client
        callTerminalMethod("loadModel", modelJson);
    }

    /**
     * Request the current layout model JSON from the client.
     * The callback is invoked once with the serialized JSON.
     */
    public void getModel(final Consumer<String> callback) {
        this.modelSnapshotCallback = callback;
        callTerminalMethod("getModel");
    }

    // ─── Change Notifications ────────────────────────────────────

    /**
     * Enable/disable model change notifications (sends full model JSON — heavy).
     */
    public void setModelChangeEnabled(final boolean enabled) {
        callTerminalMethod("enableModelChangeNotification", enabled);
    }

    /**
     * Enable/disable lightweight action notifications (sends only the action performed — ~80 bytes).
     * Much more efficient than full model change for real-time tracking.
     * Use with {@link #setOnAction(Consumer)}.
     */
    public void setActionNotificationEnabled(final boolean enabled) {
        callTerminalMethod("enableActionNotification", enabled);
    }

    /**
     * Listener for lightweight action events. Each action contains:
     * <ul>
     *   <li>"a" — action type (SELECT_TAB, CLOSE_TAB, MOVE_TAB, MAXIMIZE_TOGGLE, ADD_TAB, etc.)</li>
     *   <li>"t" — tab ID</li>
     *   <li>"ts" — tabset ID</li>
     *   <li>"to" — target ID (for MOVE_TAB)</li>
     *   <li>"l" — location (top, left, right, bottom, center)</li>
     *   <li>"n" — name (for RENAME_TAB / ADD_TAB)</li>
     *   <li>"i" — insert index</li>
     * </ul>
     * ~60x smaller than full model change notifications.
     * Requires {@link #setActionNotificationEnabled(boolean)} true.
     */
    public void setOnAction(final Consumer<JsonObject> handler) {
        this.onAction = handler;
    }

    /**
     * Set the debounce delay (in ms) for model change notifications.
     * Default is 250ms. Set to 0 for immediate (every change).
     * Higher values reduce WebSocket traffic during continuous resize operations.
     */
    public void setModelChangeDebounce(final int delayMs) {
        callTerminalMethod("setModelChangeDebounce", delayMs);
    }

    /**
     * Listener called on every layout change (requires {@link #setModelChangeEnabled(boolean)} true).
     */
    public void setOnModelChange(final Consumer<String> handler) {
        this.onModelChange = handler;
    }

    /**
     * Listener called when a tab is closed by the user.
     */
    public void setOnTabClosed(final Consumer<String> handler) {
        this.onTabClosed = handler;
    }

    /**
     * Listener called when an external drag creates a new tab.
     */
    public void setOnExternalDrop(final ExternalDropHandler handler) {
        this.onExternalDrop = handler;
    }

    @FunctionalInterface
    public interface ExternalDropHandler {
        void onDrop(String tabId, String component, String config);
    }

    // ─── Appearance ──────────────────────────────────────────────

    /**
     * Set the theme class (e.g. "fl-theme-light", "fl-theme-gray").
     */
    public void setTheme(final String theme) {
        callTerminalMethod("setTheme", theme);
    }

    // ─── Pop-out / Pop-in ────────────────────────────────────────

    /**
     * Pop a tab out into a floating window (mode "float") or a browser window (mode "window").
     */
    public void popOut(final String tabId, final String title, final int x, final int y, final int w, final int h, final String mode) {
        callTerminalMethod("popOut", tabId, title, x, y, w, h, mode);
    }

    /**
     * Pop a tab out as a floating div (default mode).
     */
    public void popOut(final String tabId, final String title, final int x, final int y, final int w, final int h) {
        popOut(tabId, title, x, y, w, h, "float");
    }

    /**
     * Pop a tab out into a separate browser window.
     */
    public void popOutToWindow(final String tabId, final String title, final int x, final int y, final int w, final int h) {
        popOut(tabId, title, x, y, w, h, "window");
    }

    /**
     * Pop a floating window back into the layout as a tab.
     */
    public void popIn(final String tabId) {
        callTerminalMethod("popIn", tabId);
    }

    /**
     * Request current pop-out state (positions, sizes). Callback receives JSON.
     */
    public void getPopOutState(final Consumer<String> callback) {
        this.popOutStateCallback = callback;
        callTerminalMethod("getPopOutState");
    }

    /**
     * Whether a tab is currently popped out.
     */
    public boolean isPopOut(final String tabId) {
        return popOutTabs.containsKey(tabId);
    }

    public void setOnPopOut(final Consumer<String> handler) { this.onPopOut = handler; }
    public void setOnPopIn(final Consumer<String> handler) { this.onPopIn = handler; }

    public void setWidgetFactory(final java.util.function.BiFunction<String, String, PWidget> factory) {
        this.widgetFactory = factory;
    }

    // ─── Drag Source Registration ────────────────────────────────

    /**
     * Register a PWidget as a drag source by its ID.
     */
    public void registerDragSource(final PWidget source, final String tabDefJson) {
        callTerminalMethod("registerDragSource", String.valueOf(source.getID()), tabDefJson);
    }

    /**
     * Register all elements matching a CSS class as drag sources.
     */
    public void registerDragSourceByClass(final String cssClass, final String tabDefJson) {
        callTerminalMethod("registerDragSourceByClass", cssClass, tabDefJson);
    }

    // ─── Accessors ───────────────────────────────────────────────

    public PFlowPanel getPanel() {
        return widget;
    }

    public Map<String, PWidget> getTabWidgets() {
        return tabWidgets;
    }

    // ─── Lifecycle ───────────────────────────────────────────────

    @Override
    public void onDestroy() {
        tabWidgets.clear();
        popOutTabs.clear();
        onExternalDrop = null;
        onTabClosed = null;
        onModelChange = null;
        onAction = null;
        onPopOut = null;
        onPopIn = null;
        modelSnapshotCallback = null;
        popOutStateCallback = null;
        tabComponents.clear();
        widgetFactory = null;
        super.onDestroy();
    }

    // ─── Internals ───────────────────────────────────────────────

    static class PopOutInfo {
        final String tabId;
        final String tabsetId;
        final int tabIdx;
        final String mode;
        final String title;
        PWindow pWindow;
        PopOutInfo(String tabId, String tabsetId, int tabIdx, String mode, String title) {
            this.tabId = tabId; this.tabsetId = tabsetId; this.tabIdx = tabIdx; this.mode = mode; this.title = title;
        }
    }

    // ─── Internals ───────────────────────────────────────────────

    private static JsonObject buildArgs(final String modelJson, final String theme, final String bordersJson) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        if (modelJson != null) {
            builder.add("model", Json.createReader(new StringReader(modelJson)).readObject());
        }
        if (theme != null) {
            builder.add("theme", theme);
        }
        if (bordersJson != null) {
            builder.add("borders", Json.createReader(new StringReader(bordersJson)).readArray());
        }
        return builder.build();
    }

    private void handleClientEvent(final JsonObject data) {
        final String type = data.getString("type", "");
        switch (type) {
            case EVT_TAB_CLOSED:
                handleTabClosed(data);
                break;
            case EVT_EXTERNAL_DROP:
                handleExternalDrop(data);
                break;
            case EVT_MODEL_CHANGE:
                handleModelChange(data);
                break;
            case EVT_MODEL_SNAPSHOT:
                handleModelSnapshot(data);
                break;
            case EVT_ACTION:
                handleActionEvent(data);
                break;
            case EVT_POP_OUT:
                handlePopOut(data);
                break;
            case EVT_POP_IN:
                handlePopIn(data);
                break;
            case EVT_POP_OUT_STATE:
                handlePopOutState(data);
                break;
            case EVT_REHYDRATE:
                handleRehydrate(data);
                break;
            default:
                break;
        }
    }

    private void handleTabClosed(final JsonObject data) {
        final String tabId = data.getString("tabId", null);
        if (tabId == null) return;
        final PWidget removed = tabWidgets.remove(tabId);
        tabComponents.remove(tabId);
        if (removed != null) removed.removeFromParent();
        if (onTabClosed != null) onTabClosed.accept(tabId);
    }

    private void handleExternalDrop(final JsonObject data) {
        final String tabId = data.getString("tabId", null);
        final String component = data.getString("component", null);
        final String config = data.getString("config", null);
        if (onExternalDrop != null && tabId != null) {
            onExternalDrop.onDrop(tabId, component, config);
        }
    }

    private void handleModelChange(final JsonObject data) {
        final String model = data.getString("model", null);
        if (onModelChange != null && model != null) {
            onModelChange.accept(model);
        }
    }

    private void handleModelSnapshot(final JsonObject data) {
        final String model = data.getString("model", null);
        if (modelSnapshotCallback != null && model != null) {
            modelSnapshotCallback.accept(model);
            modelSnapshotCallback = null;
        }
    }

    private void handleActionEvent(final JsonObject data) {
        if (onAction != null) onAction.accept(data);
    }

    private void handlePopOut(final JsonObject data) {
        final String tabId = data.getString("tabId", null);
        if (tabId == null) return;
        final String mode = data.getString("mode", "float");
        final PWidget w = tabWidgets.get(tabId);
        final PopOutInfo info = new PopOutInfo(tabId, data.getString("tabsetId", null), data.getInt("tabIdx", 0), mode, data.getString("title", tabId));
        popOutTabs.put(tabId, info);

        if ("window".equals(mode) && widgetFactory != null) {
            final String component = tabComponents.get(tabId);
            if (component != null) {
                final String title = data.getString("title", tabId);
                final int width = data.getInt("w", 500);
                final int height = data.getInt("h", 400);
                final PWindow pWindow = Element.newPWindow(title,
                    "resizable=yes,scrollbars=yes,height=" + height + ",width=" + width);
                info.pWindow = pWindow;
                pWindow.addOpenHandler(event -> {
                    final com.ponysdk.core.ui.basic.PButton popInBtn = Element.newPButton("\u23CE Pop back in");
                    popInBtn.setStyleProperty("margin", "6px");
                    popInBtn.setStyleProperty("padding", "4px 12px");
                    popInBtn.setStyleProperty("cursor", "pointer");
                    popInBtn.addClickHandler(e -> pWindow.close());
                    pWindow.add(popInBtn);
                    final PWidget newWidget = widgetFactory.apply(component, tabId);
                    if (newWidget != null) pWindow.add(newWidget);
                });
                pWindow.addCloseHandler(event -> popInFromWindow(tabId));
                pWindow.open();
            }
        }

        if (w instanceof TabContent) ((TabContent) w).onPopOut();
        if (onPopOut != null) onPopOut.accept(tabId);
    }

    private void handlePopIn(final JsonObject data) {
        final String tabId = data.getString("tabId", null);
        if (tabId == null) return;
        final PopOutInfo info = popOutTabs.remove(tabId);
        if (info == null) return;
        // Window mode pop-in is handled server-side (PWindow.close triggers popInFromWindow)
        final PWidget w = tabWidgets.get(tabId);
        if (w instanceof TabContent) ((TabContent) w).onPopIn();
        if (onPopIn != null) onPopIn.accept(tabId);
    }

    private void popInFromWindow(final String tabId) {
        final PopOutInfo info = popOutTabs.remove(tabId);
        if (info == null) return;
        final String component = tabComponents.get(tabId);
        if (widgetFactory != null && component != null) {
            final PWidget newWidget = widgetFactory.apply(component, tabId);
            if (newWidget != null) {
                widget.add(newWidget);
                tabWidgets.put(tabId, newWidget);
                callTerminalMethod("addTab", tabId, info.title, String.valueOf(newWidget.getID()), info.tabsetId);
            }
        }
        if (onPopIn != null) onPopIn.accept(tabId);
    }

    private void handlePopOutState(final JsonObject data) {
        final String state = data.getString("state", null);
        if (popOutStateCallback != null && state != null) {
            popOutStateCallback.accept(state);
            popOutStateCallback = null;
        }
    }

    private void handleRehydrate(final JsonObject data) {
        final String tabsJson = data.getString("tabs", null);
        if (tabsJson == null || onExternalDrop == null) return;
        final javax.json.JsonArray tabs = Json.createReader(new StringReader(tabsJson)).readArray();
        for (int i = 0; i < tabs.size(); i++) {
            final JsonObject tab = tabs.getJsonObject(i);
            final String tabId = tab.getString("tabId", null);
            final String component = tab.getString("component", null);
            final String config = tab.isNull("config") ? null : tab.getString("config", null);
            if (tabId != null && component != null) {
                onExternalDrop.onDrop(tabId, component, config);
            }
        }
    }
}
