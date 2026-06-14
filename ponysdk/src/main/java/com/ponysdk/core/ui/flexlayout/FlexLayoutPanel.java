package com.ponysdk.core.ui.flexlayout;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PWidget;

/**
 * High-level facade for FlexLayout integration in PonySDK.
 * <p>
 * Usage:
 * <pre>
 * FlexLayoutPanel layout = new FlexLayoutPanel(modelJson, "fl-theme-light");
 * layout.setWidgetFactory(component -&gt; createWidget(component));
 * layout.registerDragSourceByClass("my-drag-src", "Panel", "myComponent");
 * layout.setOnModelChange(json -&gt; save(json));
 * container.add(layout.asWidget());
 * </pre>
 */
public class FlexLayoutPanel implements IsPWidget {

    private final FlexLayoutAddon addon;
    private final AtomicInteger tabCounter = new AtomicInteger(0);
    private Function<String, PWidget> widgetFactory;

    // ─── Typed constructors ──────────────────────────────────────

    public FlexLayoutPanel(final FlexLayoutModel model, final FlexTheme theme, final FlexBorder... borders) {
        final String modelJson = model != null ? model.toJson() : null;
        final String themeStr = theme != null ? theme.getCssClass() : null;
        final String bordersJson = borders.length > 0 ? buildBordersJson(borders) : null;
        this.addon = new FlexLayoutAddon(modelJson, themeStr, bordersJson);
    }

    // ─── Backward-compatible constructors (deprecated) ───────────

    public FlexLayoutPanel() {
        this(null, null, (String) null);
    }

    @Deprecated
    public FlexLayoutPanel(final String modelJson, final String theme) {
        this(modelJson, theme, null);
    }

    @Deprecated
    public FlexLayoutPanel(final String modelJson, final String theme, final String bordersJson) {
        this.addon = new FlexLayoutAddon(modelJson, theme, bordersJson);
    }

    @Override
    public PWidget asWidget() {
        return addon.getPanel();
    }

    // ─── Tab Management ──────────────────────────────────────────

    public String addTab(final String tabName, final PWidget content) {
        return addTab(tabName, content, null);
    }

    public String addTab(final FlexTab tab, final PWidget content) {
        return addTab(tab.getName(), content, null);
    }

    public String addTab(final TabContent content) {
        return addTab(content.getTabName(), content.asWidget(), null);
    }

    public String addTab(final String tabName, final PWidget content, final String tabsetId) {
        final String tabId = "tab_" + tabCounter.incrementAndGet();
        addon.addTab(tabId, tabName, content, tabsetId);
        return tabId;
    }

    public void removeTab(final String tabId) {
        addon.removeTab(tabId);
    }

    // ─── Sidebar / Border Management ─────────────────────────────

    public String addBorderTab(final String side, final String tabName, final PWidget content) {
        return addBorderTab(side, tabName, content, null);
    }

    public String addBorderTab(final String side, final String tabName, final PWidget content, final String icon) {
        final String tabId = "tab_" + tabCounter.incrementAndGet();
        addon.addBorderTab(side, tabId, tabName, content, -1, icon);
        return tabId;
    }

    public String addBorderTab(final FlexBorder border, final FlexTab tab, final PWidget content) {
        final String tabId = "tab_" + tabCounter.incrementAndGet();
        addon.addBorderTab(border.getSide(), tabId, tab.getName(), content, -1, tab.getIcon());
        return tabId;
    }

    public void removeBorderTab(final String side, final String tabId) {
        addon.removeBorderTab(side, tabId);
    }

    public void selectBorderTab(final String side, final String tabId) {
        addon.selectBorderTab(side, tabId);
    }

    public void moveToBorder(final String tabId, final String side) {
        addon.moveToBorder(tabId, side);
    }

    public void moveFromBorder(final String side, final String tabId) {
        addon.moveFromBorder(side, tabId, null);
    }

    public void toggleBorder(final String side) {
        addon.toggleBorder(side);
    }

    // ─── Pop-out / Pop-in ────────────────────────────────────────

    public void popOut(final String tabId, final String title) {
        addon.popOut(tabId, title, 100, 100, 400, 300, "float");
    }

    public void popOut(final String tabId, final String title, final int x, final int y, final int w, final int h, final String mode) {
        addon.popOut(tabId, title, x, y, w, h, mode);
    }

    public void popOutToWindow(final String tabId, final String title) {
        addon.popOutToWindow(tabId, title, 100, 100, 500, 400);
    }

    public void popIn(final String tabId) {
        addon.popIn(tabId);
    }

    public boolean isPopOut(final String tabId) {
        return addon.isPopOut(tabId);
    }

    public void setOnPopOut(final Consumer<String> handler) { addon.setOnPopOut(handler); }
    public void setOnPopIn(final Consumer<String> handler) { addon.setOnPopIn(handler); }

    // ─── External Drag Sources ───────────────────────────────────

    public void setWidgetFactory(final Function<String, PWidget> factory) {
        this.widgetFactory = factory;
        addon.setWidgetFactory((component, config) -> factory.apply(component));
        addon.setOnExternalDrop((tabId, component, config) -> {
            if (this.widgetFactory == null) return;
            final PWidget w = this.widgetFactory.apply(component);
            if (w != null) addon.attachWidgetToTab(tabId, w, component);
        });
    }

    public void setWidgetFactory(final BiFunction<String, String, PWidget> factory) {
        this.widgetFactory = component -> factory.apply(component, null);
        addon.setWidgetFactory(factory);
        addon.setOnExternalDrop((tabId, component, config) -> {
            final String instanceKey = config != null ? config : tabId;
            final PWidget w = factory.apply(component, instanceKey);
            if (w != null) addon.attachWidgetToTab(tabId, w, component);
        });
    }

    public void registerDragSource(final PWidget source, final String tabName, final String component) {
        addon.registerDragSource(source, buildTabDef(tabName, component, null));
    }

    public void registerDragSource(final PWidget source, final String tabName, final String component, final String configJson) {
        addon.registerDragSource(source, buildTabDef(tabName, component, configJson));
    }

    public void registerDragSourceByClass(final String cssClass, final String tabName, final String component) {
        addon.registerDragSourceByClass(cssClass, buildTabDef(tabName, component, null));
    }

    public void registerDragSourceByClass(final String cssClass, final String tabName, final String component, final String configJson) {
        addon.registerDragSourceByClass(cssClass, buildTabDef(tabName, component, configJson));
    }

    // ─── Persistence ─────────────────────────────────────────────

    public void saveModel(final Consumer<String> callback) {
        addon.getModel(callback);
    }

    public void loadModel(final String modelJson) {
        addon.loadModel(modelJson);
    }

    // ─── Events ──────────────────────────────────────────────────

    public void setOnModelChange(final Consumer<String> listener) {
        addon.setModelChangeEnabled(true);
        addon.setOnModelChange(listener);
    }

    public void setOnAction(final Consumer<JsonObject> listener) {
        addon.setActionNotificationEnabled(true);
        addon.setOnAction(listener);
    }

    public void setOnTabClosed(final Consumer<String> handler) {
        addon.setOnTabClosed(handler);
    }

    // ─── Appearance ──────────────────────────────────────────────

    public void setTheme(final FlexTheme theme) {
        addon.setTheme(theme.getCssClass());
    }

    public void setTheme(final String theme) {
        addon.setTheme(theme);
    }

    // ─── Typed Keymap API ────────────────────────────────────────

    public void setKeymap(final FlexKeymap keymap) {
        addon.setKeymap(keymap.toJson());
    }

    @Deprecated
    public void setKeymap(final String keymapJson) {
        addon.setKeymap(keymapJson);
    }

    // ─── Typed getOpenTabs ───────────────────────────────────────

    public void getOpenTabs(final Consumer<List<FlexTabInfo>> callback) {
        addon.getOpenTabs(json -> callback.accept(FlexTabInfo.fromJson(json)));
    }

    @Deprecated
    public void getOpenTabsRaw(final Consumer<String> callback) {
        addon.getOpenTabs(callback);
    }

    public void getLayoutSummary(final Consumer<String> callback) {
        addon.getLayoutSummary(callback);
    }

    // ─── Typed Blocked Actions ───────────────────────────────────

    public void setBlockedActions(final FlexAction... actions) {
        final String[] keys = new String[actions.length];
        for (int i = 0; i < actions.length; i++) keys[i] = actions[i].getKey();
        addon.setBlockedActions(keys);
    }

    @Deprecated
    public void setBlockedActions(final String... actions) {
        addon.setBlockedActions(actions);
    }

    public void setBlockedTabs(final String... tabIds) {
        addon.setBlockedTabs(tabIds);
    }

    // ─── Feature 1: Model Migration ─────────────────────────────

    public void loadModel(final String modelJson, final String version) {
        addon.loadModel(modelJson, version);
    }

    public void setMigrationHandler(final BiFunction<String, String, String> handler) {
        addon.setMigrationHandler(handler);
    }

    // ─── Feature 3: Sidebar Pop-out ─────────────────────────────

    public void popOutBorder(final String side) {
        addon.popOutBorder(side);
    }

    // ─── Feature 4: Collaboration ───────────────────────────────

    public void applyRemoteAction(final String actionJson) {
        addon.applyRemoteAction(actionJson);
    }

    public void setOnActionBroadcast(final Consumer<JsonObject> handler) {
        addon.setOnActionBroadcast(handler);
    }

    // ─── Feature 7: Command Palette ─────────────────────────────

    public void showCommandPalette() {
        addon.showCommandPalette();
    }

    public void setCommandPaletteItems(final List<String> items) {
        addon.setCommandPaletteItems(items);
    }

    // ─── Feature 8: Pinned Tabs ─────────────────────────────────

    public String addPinnedTab(final String tabName, final PWidget content) {
        addon.addPinnedTab(tabName, content);
        return "pin_" + System.identityHashCode(content);
    }

    // ─── Feature 9: Tab Groups ──────────────────────────────────

    public void setTabGroup(final String tabId, final String groupName, final String color) {
        addon.setTabGroup(tabId, groupName, color);
    }

    // ─── Feature 10: Status Bar ─────────────────────────────────

    public void setStatusBarWidget(final PWidget w) {
        if (w == null) return;
        addon.setStatusBarWidget(w);
    }

    // ─── Feature 11: Notifications ──────────────────────────────

    public void showNotification(final String message, final String type, final int durationMs) {
        addon.showNotification(message, type, durationMs);
    }

    // ─── Feature: onTabSelect callback ──────────────────────────

    public void setOnTabSelected(final Consumer<String> handler) {
        addon.setOnTabSelected(handler);
    }

    // ─── Feature: Max tabs per tabset ───────────────────────────

    public void setMaxTabs(final String tabsetId, final int max) {
        addon.setMaxTabs(tabsetId, max);
    }

    // ─── Feature: Layout lock ───────────────────────────────────

    public void setLocked(final boolean locked) {
        addon.setLocked(locked);
    }

    // ─── Feature: Tab metadata ──────────────────────────────────

    public void setTabConfig(final String tabId, final String configJson) {
        addon.setTabConfig(tabId, configJson);
    }

    public void getTabConfig(final String tabId, final Consumer<String> callback) {
        addon.getTabConfig(tabId, callback);
    }

    // ─── Hook 3: Tab visibility ─────────────────────────────────

    public void setOnTabVisible(final Consumer<String> handler) {
        addon.setOnTabVisible(handler);
    }

    public void setOnTabHidden(final Consumer<String> handler) {
        addon.setOnTabHidden(handler);
    }

    // ─── Internal ────────────────────────────────────────────────

    public FlexLayoutAddon getAddon() {
        return addon;
    }

    private static String buildTabDef(final String tabName, final String component, final String configJson) {
        final javax.json.JsonObjectBuilder b = Json.createObjectBuilder()
            .add("name", tabName)
            .add("component", component)
            .add("enableClose", true);
        if (configJson != null) b.add("config", Json.createReader(new java.io.StringReader(configJson)).readObject());
        return b.build().toString();
    }

    private static String buildBordersJson(final FlexBorder[] borders) {
        final javax.json.JsonArrayBuilder arr = Json.createArrayBuilder();
        for (final FlexBorder border : borders) {
            arr.add(Json.createReader(new java.io.StringReader(border.toJson())).readObject());
        }
        return arr.build().toString();
    }
}
