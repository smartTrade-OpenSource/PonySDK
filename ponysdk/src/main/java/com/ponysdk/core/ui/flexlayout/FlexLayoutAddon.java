package com.ponysdk.core.ui.flexlayout;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
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
 * For internal use. Use {@link FlexLayoutPanel} instead.
 * <p>
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
    private static final String EVT_TAB_SELECTED = "tabSelected";
    private static final String EVT_TAB_CONFIG = "tabConfig";
    private static final String EVT_OPEN_TABS = "openTabs";
    private static final String EVT_LAYOUT_SUMMARY = "layoutSummary";
    private static final String EVT_TAB_VISIBLE = "tabVisible";
    private static final String EVT_TAB_HIDDEN = "tabHidden";

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
    private String modelVersion;
    private BiFunction<String, String, String> migrationHandler;
    private Consumer<JsonObject> onActionBroadcast;
    private Consumer<String> onTabSelected;
    private Consumer<String> tabConfigCallback;
    private Consumer<String> openTabsCallback;
    private Consumer<String> layoutSummaryCallback;
    private Consumer<String> onTabVisible;
    private Consumer<String> onTabHidden;

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

    public void addTab(final String tabId, final String tabName, final PWidget content) {
        addTab(tabId, tabName, content, null);
    }

    public void addTab(final String tabId, final String tabName, final PWidget content, final String tabsetId) {
        widget.add(content);
        tabWidgets.put(tabId, content);
        callTerminalMethod("addTab", tabId, tabName, String.valueOf(content.getID()), tabsetId);
    }

    public void attachWidgetToTab(final String tabId, final PWidget content) {
        attachWidgetToTab(tabId, content, null);
    }

    public void attachWidgetToTab(final String tabId, final PWidget content, final String component) {
        widget.add(content);
        tabWidgets.put(tabId, content);
        if (component != null) tabComponents.put(tabId, component);
        callTerminalMethod("attachWidget", tabId, String.valueOf(content.getID()));
    }

    public void removeTab(final String tabId) {
        callTerminalMethod("removeTab", tabId);
        final PWidget removed = tabWidgets.remove(tabId);
        tabComponents.remove(tabId);
        if (removed != null) removed.removeFromParent();
        final PopOutInfo info = popOutTabs.remove(tabId);
        if (info != null && info.pWindow != null) info.pWindow.close();
    }

    // ─── Sidebar / Border Management ─────────────────────────────

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

    public void removeBorderTab(final String side, final String tabId) {
        callTerminalMethod("removeBorderTab", side, tabId);
        final PWidget removed = tabWidgets.remove(tabId);
        tabComponents.remove(tabId);
        if (removed != null) removed.removeFromParent();
    }

    public void selectBorderTab(final String side, final String tabId) {
        callTerminalMethod("selectBorderTab", side, tabId);
    }

    public void moveToBorder(final String tabId, final String side) {
        callTerminalMethod("moveToBorder", tabId, side);
    }

    public void moveFromBorder(final String side, final String tabId, final String tabsetId) {
        callTerminalMethod("moveFromBorder", side, tabId, tabsetId);
    }

    public void toggleBorder(final String side) {
        callTerminalMethod("toggleBorder", side);
    }

    public void setBorderTabStyle(final String side, final String tabStyle) {
        callTerminalMethod("setBorderTabStyle", side, tabStyle);
    }

    public void isBorderVisible(final String side, final Consumer<Boolean> callback) {
        getModel(model -> {
            final boolean visible = model != null && model.contains("\"side\":\"" + side + "\"")
                && !model.contains("\"side\":\"" + side + "\",\"hidden\":true");
            callback.accept(visible);
        });
    }

    // ─── Model Persistence ───────────────────────────────────────

    public void loadModel(final String modelJson) {
        for (final PopOutInfo info : popOutTabs.values()) {
            if (info.pWindow != null) info.pWindow.close();
        }
        popOutTabs.clear();
        for (final PWidget w : tabWidgets.values()) {
            w.removeFromParent();
        }
        tabWidgets.clear();
        tabComponents.clear();
        callTerminalMethod("loadModel", modelJson);
    }

    public void getModel(final Consumer<String> callback) {
        this.modelSnapshotCallback = callback;
        callTerminalMethod("getModel");
    }

    // ─── Change Notifications ────────────────────────────────────

    public void setModelChangeEnabled(final boolean enabled) {
        callTerminalMethod("enableModelChangeNotification", enabled);
    }

    public void setActionNotificationEnabled(final boolean enabled) {
        callTerminalMethod("enableActionNotification", enabled);
    }

    public void setOnAction(final Consumer<JsonObject> handler) {
        this.onAction = handler;
    }

    public void setModelChangeDebounce(final int delayMs) {
        callTerminalMethod("setModelChangeDebounce", delayMs);
    }

    public void setOnModelChange(final Consumer<String> handler) {
        this.onModelChange = handler;
    }

    public void setOnTabClosed(final Consumer<String> handler) {
        this.onTabClosed = handler;
    }

    public void setOnExternalDrop(final ExternalDropHandler handler) {
        this.onExternalDrop = handler;
    }

    @FunctionalInterface
    public interface ExternalDropHandler {
        void onDrop(String tabId, String component, String config);
    }

    // ─── Appearance ──────────────────────────────────────────────

    public void setTheme(final String theme) {
        callTerminalMethod("setTheme", theme);
    }

    // ─── Pop-out / Pop-in ────────────────────────────────────────

    public void popOut(final String tabId, final String title, final int x, final int y, final int w, final int h, final String mode) {
        callTerminalMethod("popOut", tabId, title, x, y, w, h, mode);
    }

    public void popOut(final String tabId, final String title, final int x, final int y, final int w, final int h) {
        popOut(tabId, title, x, y, w, h, "float");
    }

    public void popOutToWindow(final String tabId, final String title, final int x, final int y, final int w, final int h) {
        popOut(tabId, title, x, y, w, h, "window");
    }

    public void popIn(final String tabId) {
        callTerminalMethod("popIn", tabId);
    }

    public void getPopOutState(final Consumer<String> callback) {
        this.popOutStateCallback = callback;
        callTerminalMethod("getPopOutState");
    }

    public boolean isPopOut(final String tabId) {
        return popOutTabs.containsKey(tabId);
    }

    public void setOnPopOut(final Consumer<String> handler) { this.onPopOut = handler; }
    public void setOnPopIn(final Consumer<String> handler) { this.onPopIn = handler; }

    public void setWidgetFactory(final java.util.function.BiFunction<String, String, PWidget> factory) {
        this.widgetFactory = factory;
    }

    // ─── Drag Source Registration ────────────────────────────────

    public void registerDragSource(final PWidget source, final String tabDefJson) {
        callTerminalMethod("registerDragSource", String.valueOf(source.getID()), tabDefJson);
    }

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

    // ─── Feature Toggles ─────────────────────────────────────────

    public void setBorderMinSize(final String side, final int minSize) {
        callTerminalMethod("setBorderMinSize", side, minSize);
    }

    public void setBorderMaxSize(final String side, final int maxSize) {
        callTerminalMethod("setBorderMaxSize", side, maxSize);
    }

    public void setBadge(final String tabId, final String badge) {
        callTerminalMethod("setBadge", tabId, badge, null);
    }

    public void setBadge(final String tabId, final String badge, final String color) {
        callTerminalMethod("setBadge", tabId, badge, color);
    }

    public void enableUndoRedo(final boolean enabled) {
        callTerminalMethod("enableUndoRedo", enabled);
    }

    public void enableKeyboardShortcuts(final boolean enabled) {
        callTerminalMethod("enableKeyboardShortcuts", enabled);
    }

    public void setKeymap(final String keymapJson) {
        callTerminalMethod("setKeymap", keymapJson);
    }

    public void enableTouchGestures(final boolean enabled) {
        callTerminalMethod("enableTouchGestures", enabled);
    }

    public void enableContextMenu(final boolean enabled) {
        callTerminalMethod("enableContextMenu", enabled);
    }

    public void undo() {
        callTerminalMethod("undo");
    }

    public void redo() {
        callTerminalMethod("redo");
    }

    public void enableAutoSave(final boolean enabled) {
        callTerminalMethod("enableAutoSave", enabled);
    }

    public void reorderBorderTab(final String side, final String tabId, final int newIndex) {
        callTerminalMethod("reorderBorderTab", side, tabId, newIndex);
    }

    public void maximizeBorder(final String side) {
        callTerminalMethod("maximizeBorder", side);
    }

    // ─── Feature 1: Model Migration API ─────────────────────────

    public void loadModel(final String modelJson, final String version) {
        this.modelVersion = version;
        loadModel(modelJson);
    }

    public void setMigrationHandler(final BiFunction<String, String, String> handler) {
        this.migrationHandler = handler;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    // ─── Feature 3: Sidebar Pop-out ─────────────────────────────

    public void popOutBorder(final String side) {
        callTerminalMethod("popOutBorder", side);
    }

    // ─── Feature 4: Collaboration API ───────────────────────────

    public void applyRemoteAction(final String actionJson) {
        callTerminalMethod("applyRemoteAction", actionJson);
    }

    public void setOnActionBroadcast(final Consumer<JsonObject> handler) {
        this.onActionBroadcast = handler;
        setActionNotificationEnabled(true);
    }

    // ─── Feature 7: Command Palette ─────────────────────────────

    public void showCommandPalette() {
        callTerminalMethod("showCommandPalette");
    }

    public void setCommandPaletteItems(final List<String> items) {
        final javax.json.JsonArrayBuilder arr = Json.createArrayBuilder();
        for (final String item : items) arr.add(item);
        callTerminalMethod("setCommandPaletteItems", arr.build().toString());
    }

    // ─── Feature 8: Pinned Tabs ─────────────────────────────────

    public void addPinnedTab(final String tabName, final PWidget content) {
        addPinnedTab(tabName, content, null);
    }

    public void addPinnedTab(final String tabName, final PWidget content, final String tabsetId) {
        final String tabId = "pin_" + System.identityHashCode(content);
        widget.add(content);
        tabWidgets.put(tabId, content);
        callTerminalMethod("addPinnedTab", tabId, tabName, String.valueOf(content.getID()), tabsetId);
    }

    // ─── Feature 9: Tab Groups ──────────────────────────────────

    public void setTabGroup(final String tabId, final String groupName, final String color) {
        callTerminalMethod("setTabGroup", tabId, groupName, color);
    }

    // ─── Feature 10: Status Bar ─────────────────────────────────

    public void setStatusBarWidget(final PWidget w) {
        if (w == null) return;
        widget.add(w);
        callTerminalMethod("setStatusBar", String.valueOf(w.getID()));
    }

    // ─── Feature 11: Notification/Toast ─────────────────────────

    public void showNotification(final String message, final String type, final int durationMs) {
        callTerminalMethod("showNotification", message, type, durationMs);
    }

    // ─── Feature: onTabSelect callback ──────────────────────────

    public void setOnTabSelected(final Consumer<String> handler) {
        this.onTabSelected = handler;
    }

    // ─── Feature: Max tabs per tabset ───────────────────────────

    public void setMaxTabs(final String tabsetId, final int max) {
        callTerminalMethod("setMaxTabs", tabsetId, max);
    }

    // ─── Feature: Layout lock ───────────────────────────────────

    public void setLocked(final boolean locked) {
        callTerminalMethod("setLocked", locked);
    }

    // ─── Feature: Tab metadata ──────────────────────────────────

    public void setTabConfig(final String tabId, final String configJson) {
        callTerminalMethod("setTabConfig", tabId, configJson);
    }

    public void getTabConfig(final String tabId, final Consumer<String> callback) {
        this.tabConfigCallback = callback;
        callTerminalMethod("getTabConfig", tabId);
    }

    // ─── Hook 1: Blocked actions/tabs ─────────────────────────────

    public void setBlockedActions(final FlexAction... actions) {
        final String[] keys = new String[actions.length];
        for (int i = 0; i < actions.length; i++) keys[i] = actions[i].getKey();
        setBlockedActions(keys);
    }

    public void setBlockedActions(final String... actions) {
        final javax.json.JsonArrayBuilder arr = Json.createArrayBuilder();
        for (final String a : actions) arr.add(a);
        callTerminalMethod("setBlockedActions", arr.build().toString());
    }

    public void setBlockedTabs(final String... tabIds) {
        final javax.json.JsonArrayBuilder arr = Json.createArrayBuilder();
        for (final String id : tabIds) arr.add(id);
        callTerminalMethod("setBlockedTabs", arr.build().toString());
    }

    // ─── Hook 2: getOpenTabs / getLayoutSummary ─────────────────

    public void getOpenTabs(final Consumer<String> callback) {
        this.openTabsCallback = callback;
        callTerminalMethod("getOpenTabs");
    }

    public void getLayoutSummary(final Consumer<String> callback) {
        this.layoutSummaryCallback = callback;
        callTerminalMethod("getLayoutSummary");
    }

    // ─── Hook 3: Tab visibility ─────────────────────────────────

    public void setOnTabVisible(final Consumer<String> handler) {
        this.onTabVisible = handler;
    }

    public void setOnTabHidden(final Consumer<String> handler) {
        this.onTabHidden = handler;
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
            case EVT_TAB_SELECTED:
                handleTabSelected(data);
                break;
            case EVT_TAB_CONFIG:
                handleTabConfig(data);
                break;
            case EVT_OPEN_TABS:
                handleOpenTabs(data);
                break;
            case EVT_LAYOUT_SUMMARY:
                handleLayoutSummaryEvt(data);
                break;
            case EVT_TAB_VISIBLE:
                handleTabVisible(data);
                break;
            case EVT_TAB_HIDDEN:
                handleTabHidden(data);
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
        if (onActionBroadcast != null) onActionBroadcast.accept(data);
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
            String component = tab.getString("component", null);
            final String config = tab.containsKey("config") && !tab.isNull("config") ? tab.getString("config", null) : null;
            if (tabId == null || component == null) continue;
            if (migrationHandler != null && modelVersion != null) {
                component = migrationHandler.apply(component, modelVersion);
                if (component == null) continue;
            }
            onExternalDrop.onDrop(tabId, component, config);
        }
    }

    private void handleTabSelected(final JsonObject data) {
        final String tabId = data.getString("tabId", null);
        if (onTabSelected != null && tabId != null) onTabSelected.accept(tabId);
    }

    private void handleTabConfig(final JsonObject data) {
        final String config = data.getString("config", null);
        if (tabConfigCallback != null && config != null) {
            tabConfigCallback.accept(config);
            tabConfigCallback = null;
        }
    }

    private void handleOpenTabs(final JsonObject data) {
        final String d = data.getString("data", null);
        if (openTabsCallback != null && d != null) {
            openTabsCallback.accept(d);
            openTabsCallback = null;
        }
    }

    private void handleLayoutSummaryEvt(final JsonObject data) {
        final String d = data.getString("data", null);
        if (layoutSummaryCallback != null && d != null) {
            layoutSummaryCallback.accept(d);
            layoutSummaryCallback = null;
        }
    }

    private void handleTabVisible(final JsonObject data) {
        final String tabId = data.getString("tabId", null);
        if (onTabVisible != null && tabId != null) onTabVisible.accept(tabId);
    }

    private void handleTabHidden(final JsonObject data) {
        final String tabId = data.getString("tabId", null);
        if (onTabHidden != null && tabId != null) onTabHidden.accept(tabId);
    }
}
