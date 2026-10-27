/*
 * Copyright (c) 2026 PonySDK
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
package com.ponysdk.core.ui.flexlayout;

import static com.ponysdk.core.ui.flexlayout.TerminalCallRecorder.fire;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.test.PSuite;

public class FlexLayoutPanelTest extends PSuite {

    private TerminalCallRecorder recorder;
    private FlexLayoutPanel panel;
    private FlexLayoutAddon addon;

    static final class Content extends PFlowPanel implements TabContent {

        @Override
        public String getTabName() {
            return "Report";
        }

        @Override
        public String getComponent() {
            return "report";
        }
    }

    @BeforeEach
    public void setUp() {
        recorder = TerminalCallRecorder.install();
        panel = attach(new FlexLayoutPanel());
        addon = panel.getAddon();
    }

    @AfterEach
    public void tearDown() {
        TerminalCallRecorder.uninstall();
    }

    private static FlexLayoutPanel attach(final FlexLayoutPanel panel) {
        PWindow.getMain().add(panel);
        return panel;
    }

    private static String id(final PWidget widget) {
        return String.valueOf(widget.getID());
    }

    private static JsonObject read(final String json) {
        return Json.createReader(new StringReader(json)).readObject();
    }

    // ─── Construction ────────────────────────────────────────────

    @Test
    public void typedConstructorSerializesModelThemeAndBorders() {
        final FlexLayoutModel model = FlexLayoutModel.row().tabset(FlexTabset.create().tab(FlexTab.create("A")));
        final FlexLayoutPanel typed = attach(new FlexLayoutPanel(model, FlexTheme.NORD,
            FlexBorder.left().size(300).tabStyle(TabStyle.ICON), FlexBorder.bottom()));

        final Object[] args = recorder.creationArgs(typed.getAddon());
        final JsonObject modelJson = read((String) args[0]);
        assertEquals("row", modelJson.getString("type"));
        assertEquals("A", modelJson.getJsonArray("children").getJsonObject(0)
            .getJsonArray("children").getJsonObject(0).getString("name"));
        assertEquals("fl-theme-nord", args[1]);
        final JsonArray borders = Json.createReader(new StringReader((String) args[2])).readArray();
        assertEquals(2, borders.size());
        final JsonObject left = borders.getJsonObject(0);
        assertEquals("left-top", left.getString("location"));
        assertEquals(300, left.getInt("size"));
        assertEquals("icon", left.getString("tabStyle"));
        assertFalse(borders.getJsonObject(1).containsKey("tabStyle"));
        assertSame(typed.getAddon().getPanel(), typed.asWidget());
    }

    @Test
    public void typedConstructorWithoutAnything() {
        final FlexLayoutPanel typed = attach(new FlexLayoutPanel((FlexLayoutModel) null, null));
        assertArrayEquals(new Object[] { null, null, null }, recorder.creationArgs(typed.getAddon()));
        assertArrayEquals(new Object[] { null, null, null }, recorder.creationArgs(addon));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void deprecatedStringConstructors() {
        final FlexLayoutPanel a = attach(new FlexLayoutPanel("{\"type\":\"row\"}", "th"));
        assertArrayEquals(new Object[] { "{\"type\":\"row\"}", "th", null }, recorder.creationArgs(a.getAddon()));

        final FlexLayoutPanel b = attach(new FlexLayoutPanel(null, null, "[{\"location\":\"bottom\"}]"));
        assertArrayEquals(new Object[] { null, null, "[{\"location\":\"bottom\"}]" }, recorder.creationArgs(b.getAddon()));
    }

    // ─── Tabs ────────────────────────────────────────────────────

    @Test
    public void addTabGeneratesSequentialIds() {
        final PLabel a = Element.newPLabel();
        final PLabel b = Element.newPLabel();
        final PLabel c = Element.newPLabel();

        assertEquals("tab_1", panel.addTab("A", a));
        recorder.assertLast(addon, "addTab", "tab_1", "A", id(a), null, null);
        assertEquals("tab_2", panel.addTab(FlexTab.create("B").icon("ignored"), b));
        recorder.assertLast(addon, "addTab", "tab_2", "B", id(b), null, null);
        assertEquals("tab_3", panel.addTab("C", c, "ts1"));
        recorder.assertLast(addon, "addTab", "tab_3", "C", id(c), "ts1", null);

        final Content content = new Content();
        assertEquals("tab_4", panel.addTab(content));
        recorder.assertLast(addon, "addTab", "tab_4", "Report", id(content), null, null);

        panel.removeTab("tab_1");
        recorder.assertLast(addon, "removeTab", "tab_1");
        assertFalse(addon.getTabWidgets().containsKey("tab_1"));
    }

    @Test
    public void borderTabs() {
        final PLabel a = Element.newPLabel();
        final PLabel b = Element.newPLabel();
        final PLabel c = Element.newPLabel();

        assertEquals("tab_1", panel.addBorderTab("left", "A", a));
        recorder.assertLast(addon, "addBorderTab", "left", "tab_1", "A", id(a), null, null);
        assertEquals("tab_2", panel.addBorderTab("right", "B", b, "ico"));
        recorder.assertLast(addon, "addBorderTab", "right", "tab_2", "B", id(b), null, "ico");
        assertEquals("tab_3", panel.addBorderTab(FlexBorder.rightBottom(), FlexTab.create("C").icon("i"), c));
        recorder.assertLast(addon, "addBorderTab", "right-bottom", "tab_3", "C", id(c), null, "i");

        panel.removeBorderTab("left", "tab_1");
        recorder.assertLast(addon, "removeBorderTab", "left", "tab_1");
        panel.selectBorderTab("right", "tab_2");
        recorder.assertLast(addon, "selectBorderTab", "right", "tab_2");
        panel.moveToBorder("tab_2", "bottom");
        recorder.assertLast(addon, "moveToBorder", "tab_2", "bottom");
        panel.moveFromBorder("bottom", "tab_2");
        recorder.assertLast(addon, "moveFromBorder", "bottom", "tab_2", null);
        panel.toggleBorder("left");
        recorder.assertLast(addon, "toggleBorder", "left");
        panel.popOutBorder("left");
        recorder.assertLast(addon, "popOutBorder", "left");
    }

    // ─── Pop-out ─────────────────────────────────────────────────

    @Test
    public void popOutDefaults() {
        panel.popOut("t1", "T");
        recorder.assertLast(addon, "popOut", "t1", "T", 100, 100, 400, 300, "float");
        panel.popOut("t1", "T", 1, 2, 3, 4, "window");
        recorder.assertLast(addon, "popOut", "t1", "T", 1, 2, 3, 4, "window");
        panel.popOutToWindow("t1", "T");
        recorder.assertLast(addon, "popOut", "t1", "T", 100, 100, 500, 400, "window");
        panel.popIn("t1");
        recorder.assertLast(addon, "popIn", "t1");
    }

    @Test
    public void popOutStateAndHandlers() {
        final List<String> events = new ArrayList<>();
        panel.setOnPopOut(t -> events.add("out:" + t));
        panel.setOnPopIn(t -> events.add("in:" + t));
        final String tabId = panel.addTab("A", Element.newPLabel());

        fire(addon, Json.createObjectBuilder().add("type", "popOut").add("tabId", tabId));
        assertTrue(panel.isPopOut(tabId));
        fire(addon, Json.createObjectBuilder().add("type", "popIn").add("tabId", tabId));
        assertFalse(panel.isPopOut(tabId));

        assertEquals(List.of("out:" + tabId, "in:" + tabId), events);
    }

    // ─── Widget factory ──────────────────────────────────────────

    @Test
    public void componentFactoryAttachesWidgetsOnExternalDrop() {
        final List<String> requested = new ArrayList<>();
        final List<PWidget> built = new ArrayList<>();
        panel.setWidgetFactory((String component) -> {
            requested.add(component);
            if ("none".equals(component)) return null;
            final PLabel label = Element.newPLabel(component);
            built.add(label);
            return label;
        });

        fire(addon, Json.createObjectBuilder().add("type", "externalDrop").add("tabId", "x1").add("component", "chart"));
        recorder.assertLast(addon, "attachWidget", "x1", id(built.get(0)));
        assertSame(built.get(0), addon.getTabWidgets().get("x1"));

        recorder.clear();
        fire(addon, Json.createObjectBuilder().add("type", "externalDrop").add("tabId", "x2").add("component", "none"));
        assertTrue(recorder.calls(addon).isEmpty());

        // The component recorded on drop lets a window pop-out rebuild the widget
        fire(addon, Json.createObjectBuilder().add("type", "popOut").add("tabId", "x1").add("mode", "window"));
        assertTrue(panel.isPopOut("x1"));
        assertEquals(List.of("chart", "none", "chart"), requested);
    }

    @Test
    public void nullComponentFactoryIgnoresExternalDrops() {
        panel.setWidgetFactory((java.util.function.Function<String, PWidget>) null);
        recorder.clear();
        fire(addon, Json.createObjectBuilder().add("type", "externalDrop").add("tabId", "x1").add("component", "chart"));
        assertTrue(recorder.calls(addon).isEmpty());
        assertFalse(addon.getTabWidgets().containsKey("x1"));
    }

    @Test
    public void instanceFactoryReceivesConfigOrTabIdAsInstanceKey() {
        final List<String> requested = new ArrayList<>();
        panel.setWidgetFactory((String component, String key) -> {
            requested.add(component + "|" + key);
            return "none".equals(component) ? null : Element.newPLabel();
        });

        fire(addon, Json.createObjectBuilder().add("type", "externalDrop").add("tabId", "x1").add("component", "c").add("config", "cfg"));
        fire(addon, Json.createObjectBuilder().add("type", "externalDrop").add("tabId", "x2").add("component", "c"));
        fire(addon, Json.createObjectBuilder().add("type", "externalDrop").add("tabId", "x3").add("component", "none"));

        assertEquals(List.of("c|cfg", "c|x2", "none|x3"), requested);
        assertEquals(java.util.Set.of("x1", "x2"), addon.getTabWidgets().keySet());
        assertEquals(List.of("attachWidget", "attachWidget"), recorder.methods(addon));
    }

    // ─── Drag sources ────────────────────────────────────────────

    @Test
    public void dragSourcesCarryATabDefinition() {
        final PLabel source = Element.newPLabel();
        panel.registerDragSource(source, "Chart", "chart");
        JsonObject def = read((String) recorder.last(addon).args[1]);
        assertEquals(id(source), recorder.last(addon).args[0]);
        assertEquals("Chart", def.getString("name"));
        assertEquals("chart", def.getString("component"));
        assertTrue(def.getBoolean("enableClose"));
        assertFalse(def.containsKey("config"));

        panel.registerDragSource(source, "Chart", "chart", "{\"symbol\":\"EUR\"}");
        def = read((String) recorder.last(addon).args[1]);
        assertEquals("EUR", def.getJsonObject("config").getString("symbol"));

        panel.registerDragSourceByClass("drag", "Grid", "grid");
        assertEquals("registerDragSourceByClass", recorder.last(addon).method);
        assertEquals("drag", recorder.last(addon).args[0]);
        assertEquals("grid", read((String) recorder.last(addon).args[1]).getString("component"));

        panel.registerDragSourceByClass("drag", "Grid", "grid", "{\"rows\":3}");
        assertEquals(3, read((String) recorder.last(addon).args[1]).getJsonObject("config").getInt("rows"));
    }

    // ─── Persistence and events ──────────────────────────────────

    @Test
    @SuppressWarnings("deprecation")
    public void persistenceAndEvents() {
        final List<String> received = new ArrayList<>();

        panel.saveModel(m -> received.add("saved:" + m));
        recorder.assertLast(addon, "getModel");
        fire(addon, Json.createObjectBuilder().add("type", "modelSnapshot").add("model", "m"));

        panel.loadModel("{}");
        recorder.assertLast(addon, "loadModel", "{}");
        panel.loadModel("{}", "v3");
        assertEquals("v3", addon.getModelVersion());

        panel.setOnModelChange(m -> received.add("changed:" + m));
        recorder.assertLast(addon, "enableModelChangeNotification", true);
        fire(addon, Json.createObjectBuilder().add("type", "modelChange").add("model", "m2"));

        panel.setOnAction(a -> received.add("action:" + a.getString("name")));
        recorder.assertLast(addon, "enableActionNotification", true);
        fire(addon, Json.createObjectBuilder().add("type", "action").add("name", "close"));

        panel.setOnTabClosed(t -> received.add("closed:" + t));
        fire(addon, Json.createObjectBuilder().add("type", "tabClosed").add("tabId", "t9"));

        panel.setOnTabSelected(t -> received.add("selected:" + t));
        fire(addon, Json.createObjectBuilder().add("type", "tabSelected").add("tabId", "t9"));
        panel.setOnTabVisible(t -> received.add("visible:" + t));
        fire(addon, Json.createObjectBuilder().add("type", "tabVisible").add("tabId", "t9"));
        panel.setOnTabHidden(t -> received.add("hidden:" + t));
        fire(addon, Json.createObjectBuilder().add("type", "tabHidden").add("tabId", "t9"));

        panel.setOnActionBroadcast(a -> received.add("broadcast:" + a.getString("name")));
        fire(addon, Json.createObjectBuilder().add("type", "action").add("name", "move"));

        panel.getTabConfig("t9", c -> received.add("config:" + c));
        recorder.assertLast(addon, "getTabConfig", "t9");
        fire(addon, Json.createObjectBuilder().add("type", "tabConfig").add("config", "{}"));

        panel.getLayoutSummary(s -> received.add("summary:" + s));
        fire(addon, Json.createObjectBuilder().add("type", "layoutSummary").add("data", "S"));
        panel.getOpenTabsRaw(s -> received.add("raw:" + s));
        fire(addon, Json.createObjectBuilder().add("type", "openTabs").add("data", "[]"));

        assertEquals(List.of("saved:m", "changed:m2", "action:close", "closed:t9", "selected:t9", "visible:t9",
            "hidden:t9", "action:move", "broadcast:move", "config:{}", "summary:S", "raw:[]"), received);
    }

    @Test
    public void migrationHandlerIsUsedOnRehydrate() {
        final List<String> drops = new ArrayList<>();
        panel.setWidgetFactory((String component) -> {
            drops.add(component);
            return Element.newPLabel();
        });
        panel.setMigrationHandler((component, version) -> component + "-" + version);
        panel.loadModel("{}", "2");

        fire(addon, Json.createObjectBuilder().add("type", "rehydrate").add("tabs", "[{\"tabId\":\"t\",\"component\":\"grid\"}]"));

        assertEquals(List.of("grid-2"), drops);
    }

    @Test
    public void openTabsAreParsed() {
        final List<List<FlexTabInfo>> answers = new ArrayList<>();
        panel.getOpenTabs(answers::add);
        recorder.assertLast(addon, "getOpenTabs");

        fire(addon, Json.createObjectBuilder().add("type", "openTabs").add("data",
            "[{\"id\":\"t1\",\"name\":\"One\",\"component\":\"c\",\"tabsetId\":\"ts\",\"selected\":true},{\"id\":\"t2\"}]"));

        assertEquals(1, answers.size());
        final List<FlexTabInfo> tabs = answers.get(0);
        assertEquals(2, tabs.size());
        assertEquals("t1", tabs.get(0).getId());
        assertEquals("One", tabs.get(0).getName());
        assertEquals("c", tabs.get(0).getComponent());
        assertEquals("ts", tabs.get(0).getTabsetId());
        assertTrue(tabs.get(0).isSelected());
        assertEquals("t2", tabs.get(1).getId());
        assertEquals(null, tabs.get(1).getName());
        assertFalse(tabs.get(1).isSelected());
    }

    // ─── Appearance and features ─────────────────────────────────

    @Test
    @SuppressWarnings("deprecation")
    public void appearanceAndFeatures() {
        panel.setTheme(FlexTheme.MONOKAI);
        recorder.assertLast(addon, "setTheme", "fl-theme-monokai");
        panel.setTheme("custom");
        recorder.assertLast(addon, "setTheme", "custom");

        panel.setKeymap(FlexKeymap.defaults().unbind(FlexAction.UNDO));
        final JsonObject keymap = read((String) recorder.last(addon).args[0]);
        assertTrue(keymap.isNull("undo"));
        assertTrue(keymap.getJsonObject("redo").getBoolean("ctrl"));
        panel.setKeymap("{}");
        recorder.assertLast(addon, "setKeymap", "{}");

        panel.setBlockedActions(FlexAction.RENAME_TAB, FlexAction.COMMAND_PALETTE);
        recorder.assertLast(addon, "setBlockedActions", "[\"renameTab\",\"commandPalette\"]");
        panel.setBlockedActions("a", "b");
        recorder.assertLast(addon, "setBlockedActions", "[\"a\",\"b\"]");
        panel.setBlockedTabs("t1");
        recorder.assertLast(addon, "setBlockedTabs", "[\"t1\"]");

        panel.applyRemoteAction("{}");
        recorder.assertLast(addon, "applyRemoteAction", "{}");
        panel.showCommandPalette();
        recorder.assertLast(addon, "showCommandPalette");
        panel.setCommandPaletteItems(List.of("x"));
        recorder.assertLast(addon, "setCommandPaletteItems", "[\"x\"]");
        panel.setTabGroup("t1", "g", "red");
        recorder.assertLast(addon, "setTabGroup", "t1", "g", "red");
        panel.showNotification("m", "warn", 10);
        recorder.assertLast(addon, "showNotification", "m", "warn", 10);
        panel.setMaxTabs("ts", 2);
        recorder.assertLast(addon, "setMaxTabs", "ts", 2);
        panel.setLocked(false);
        recorder.assertLast(addon, "setLocked", false);
        panel.setTabConfig("t1", "{\"k\":1}");
        recorder.assertLast(addon, "setTabConfig", "t1", "{\"k\":1}");
    }

    @Test
    public void pinnedTabAndStatusBar() {
        final PLabel pinned = Element.newPLabel();
        final String pinId = panel.addPinnedTab("Home", pinned);
        assertSame(pinned, addon.getTabWidgets().get(pinId));
        recorder.assertLast(addon, "addPinnedTab", pinId, "Home", id(pinned), null);

        final PLabel status = Element.newPLabel();
        panel.setStatusBarWidget(status);
        recorder.assertLast(addon, "setStatusBar", id(status));

        recorder.clear();
        panel.setStatusBarWidget(null);
        assertTrue(recorder.calls(addon).isEmpty());
    }
}
