package com.ponysdk.sample.client.page.addon;

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

    public FlexLayoutPanel() {
        this(null, null, null);
    }

    public FlexLayoutPanel(final String modelJson, final String theme) {
        this(modelJson, theme, null);
    }

    public FlexLayoutPanel(final String modelJson, final String theme, final String bordersJson) {
        this.addon = new FlexLayoutAddon(modelJson, theme, bordersJson);
    }

    @Override
    public PWidget asWidget() {
        return addon.getPanel();
    }

    // ─── Tab Management ──────────────────────────────────────────

    /**
     * Add a tab with a PWidget as content. Returns the generated tab ID.
     */
    public String addTab(final String tabName, final PWidget content) {
        return addTab(tabName, content, null);
    }

    /**
     * Add a tab using a TabContent implementation. Tab name and properties are derived from the interface.
     */
    public String addTab(final TabContent content) {
        return addTab(content.getTabName(), content.asWidget(), null);
    }

    /**
     * Add a tab to a specific tabset. Returns the generated tab ID.
     */
    public String addTab(final String tabName, final PWidget content, final String tabsetId) {
        final String tabId = "tab_" + tabCounter.incrementAndGet();
        addon.addTab(tabId, tabName, content, tabsetId);
        return tabId;
    }

    /**
     * Remove a tab by its ID.
     */
    public void removeTab(final String tabId) {
        addon.removeTab(tabId);
    }

    // ─── Sidebar / Border Management ─────────────────────────────

    /**
     * Add a tab to a sidebar. Returns the generated tab ID.
     * @param side "left", "right", or "bottom"
     */
    public String addBorderTab(final String side, final String tabName, final PWidget content) {
        return addBorderTab(side, tabName, content, null);
    }

    /**
     * Add a tab to a sidebar with an icon. Returns the generated tab ID.
     * @param icon emoji or character displayed in the strip (null for label only)
     */
    public String addBorderTab(final String side, final String tabName, final PWidget content, final String icon) {
        final String tabId = "tab_" + tabCounter.incrementAndGet();
        addon.addBorderTab(side, tabId, tabName, content, -1, icon);
        return tabId;
    }

    /**
     * Remove a tab from a sidebar.
     */
    public void removeBorderTab(final String side, final String tabId) {
        addon.removeBorderTab(side, tabId);
    }

    /**
     * Toggle-select a border tab (opens/closes the sidebar panel).
     */
    public void selectBorderTab(final String side, final String tabId) {
        addon.selectBorderTab(side, tabId);
    }

    /**
     * Move a tab from the main layout into a sidebar.
     */
    public void moveToBorder(final String tabId, final String side) {
        addon.moveToBorder(tabId, side);
    }

    /**
     * Move a tab from a sidebar back into the main layout.
     */
    public void moveFromBorder(final String side, final String tabId) {
        addon.moveFromBorder(side, tabId, null);
    }

    /**
     * Toggle visibility of a sidebar.
     */
    public void toggleBorder(final String side) {
        addon.toggleBorder(side);
    }

    // ─── Pop-out / Pop-in ────────────────────────────────────────

    /**
     * Pop a tab out into a floating div overlay (stays in the same browser window).
     */
    public void popOut(final String tabId, final String title) {
        addon.popOut(tabId, title, 100, 100, 400, 300, "float");
    }

    /**
     * Pop a tab out with explicit position and size.
     * @param mode "float" (overlay div) or "window" (new browser window)
     */
    public void popOut(final String tabId, final String title, final int x, final int y, final int w, final int h, final String mode) {
        addon.popOut(tabId, title, x, y, w, h, mode);
    }

    /**
     * Pop a tab out into a separate browser window.
     * Closing the window automatically pops the tab back in.
     */
    public void popOutToWindow(final String tabId, final String title) {
        addon.popOutToWindow(tabId, title, 100, 100, 500, 400);
    }

    /**
     * Pop a floating window back into the layout.
     */
    public void popIn(final String tabId) {
        addon.popIn(tabId);
    }

    /**
     * Whether a tab is currently popped out.
     */
    public boolean isPopOut(final String tabId) {
        return addon.isPopOut(tabId);
    }

    /**
     * Listen for pop-out events.
     */
    public void setOnPopOut(final Consumer<String> handler) { addon.setOnPopOut(handler); }

    /**
     * Listen for pop-in events.
     */
    public void setOnPopIn(final Consumer<String> handler) { addon.setOnPopIn(handler); }

    // ─── External Drag Sources ───────────────────────────────────

    /**
     * Set the factory that creates PWidgets when external drag creates a tab.
     * The factory receives the "component" identifier and returns a PWidget (or null to skip).
     */
    public void setWidgetFactory(final Function<String, PWidget> factory) {
        this.widgetFactory = factory;
        addon.setWidgetFactory((component, config) -> factory.apply(component));
        addon.setOnExternalDrop((tabId, component, config) -> {
            if (this.widgetFactory == null) return;
            final PWidget w = this.widgetFactory.apply(component);
            if (w != null) addon.attachWidgetToTab(tabId, w, component);
        });
    }

    /**
     * Set a factory that receives both the component type AND the instance config JSON.
     * Use this when you need to recreate a specific instance (e.g. a blotter with a particular channel).
     */
    public void setWidgetFactory(final BiFunction<String, String, PWidget> factory) {
        this.widgetFactory = component -> factory.apply(component, null);
        addon.setWidgetFactory(factory);
        addon.setOnExternalDrop((tabId, component, config) -> {
            // Use config if available (rehydration), otherwise use tabId as instance key
            final String instanceKey = config != null ? config : tabId;
            final PWidget w = factory.apply(component, instanceKey);
            if (w != null) addon.attachWidgetToTab(tabId, w, component);
        });
    }

    /**
     * Register a PWidget as a drag source.
     */
    public void registerDragSource(final PWidget source, final String tabName, final String component) {
        addon.registerDragSource(source, buildTabDef(tabName, component, null));
    }

    public void registerDragSource(final PWidget source, final String tabName, final String component, final String configJson) {
        addon.registerDragSource(source, buildTabDef(tabName, component, configJson));
    }

    /**
     * Register all elements with a CSS class as drag sources.
     */
    public void registerDragSourceByClass(final String cssClass, final String tabName, final String component) {
        addon.registerDragSourceByClass(cssClass, buildTabDef(tabName, component, null));
    }

    public void registerDragSourceByClass(final String cssClass, final String tabName, final String component, final String configJson) {
        addon.registerDragSourceByClass(cssClass, buildTabDef(tabName, component, configJson));
    }

    // ─── Persistence ─────────────────────────────────────────────

    /**
     * Save the current layout model as JSON (async callback).
     */
    public void saveModel(final Consumer<String> callback) {
        addon.getModel(callback);
    }

    /**
     * Load a layout from a previously saved JSON string.
     */
    public void loadModel(final String modelJson) {
        addon.loadModel(modelJson);
    }

    // ─── Events ──────────────────────────────────────────────────

    /**
     * Listen for all layout changes (sends full model JSON — heavy, ~5-10KB).
     * For real-time tracking, prefer {@link #setOnAction} (~80 bytes per event).
     */
    public void setOnModelChange(final Consumer<String> listener) {
        addon.setModelChangeEnabled(true);
        addon.setOnModelChange(listener);
    }

    /**
     * Listen for lightweight action events (~80 bytes).
     * Ideal for real-time persistence, undo/redo, or multi-user sync.
     */
    public void setOnAction(final Consumer<JsonObject> listener) {
        addon.setActionNotificationEnabled(true);
        addon.setOnAction(listener);
    }

    /**
     * Listen for tab close events.
     */
    public void setOnTabClosed(final Consumer<String> handler) {
        addon.setOnTabClosed(handler);
    }

    // ─── Appearance ──────────────────────────────────────────────

    /**
     * Set the FlexLayout theme (e.g. "fl-theme-light", "fl-theme-gray", "fl-theme-rounded").
     */
    public void setTheme(final String theme) {
        addon.setTheme(theme);
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
        addon.setStatusBarWidget(w);
    }

    // ─── Feature 11: Notifications ──────────────────────────────

    public void showNotification(final String message, final String type, final int durationMs) {
        addon.showNotification(message, type, durationMs);
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
}
