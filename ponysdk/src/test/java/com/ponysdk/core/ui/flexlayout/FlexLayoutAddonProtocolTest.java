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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.json.Json;
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

public class FlexLayoutAddonProtocolTest extends PSuite {

    private static final String UNPARKED = "fl-pony-unparked";

    private TerminalCallRecorder recorder;
    private FlexLayoutAddon addon;

    static final class AwareLabel extends PFlowPanel implements FlexLayoutAware {

        private final String icon;

        AwareLabel(final String icon) {
            this.icon = icon;
        }

        @Override
        public String getFlexTitle() {
            return "Aware";
        }

        @Override
        public String getFlexIcon() {
            return icon != null ? icon : FlexLayoutAware.super.getFlexIcon();
        }
    }

    static final class Content extends PFlowPanel implements TabContent {

        int popOuts;
        int popIns;

        @Override
        public String getTabName() {
            return "Content";
        }

        @Override
        public String getComponent() {
            return "content";
        }

        @Override
        public void onPopOut() {
            popOuts++;
        }

        @Override
        public void onPopIn() {
            popIns++;
        }
    }

    @BeforeEach
    public void setUp() {
        recorder = TerminalCallRecorder.install();
        addon = attach(new FlexLayoutAddon());
    }

    @AfterEach
    public void tearDown() {
        TerminalCallRecorder.uninstall();
    }

    private static FlexLayoutAddon attach(final FlexLayoutAddon addon) {
        PWindow.getMain().add(addon.getPanel());
        return addon;
    }

    private static String id(final PWidget widget) {
        return String.valueOf(widget.getID());
    }

    private static JsonObject read(final String json) {
        return Json.createReader(new StringReader(json)).readObject();
    }

    // ─── Creation ────────────────────────────────────────────────

    @Test
    public void constructorWritesModelThemeAndBordersAsCreationArguments() {
        final FlexLayoutAddon created = attach(new FlexLayoutAddon("{\"type\":\"row\"}", "fl-theme-nord",
            "[{\"type\":\"border\",\"location\":\"bottom\"}]"));

        // Model and borders travel verbatim; the terminal parses them once
        assertArrayEquals(new Object[] { "{\"type\":\"row\"}", "fl-theme-nord", "[{\"type\":\"border\",\"location\":\"bottom\"}]" },
            recorder.creationArgs(created));
        assertTrue(created.getPanel().getParent() != null);
    }

    @Test
    public void defaultConstructorWritesEmptyCreationArguments() {
        assertArrayEquals(new Object[] { null, null, null }, recorder.creationArgs(addon));
        assertSame(addon.getPanel(), addon.asWidget());
    }

    @Test
    public void twoArgumentConstructorHasNoBorders() {
        final FlexLayoutAddon created = attach(new FlexLayoutAddon(null, "fl-theme-gray"));
        assertArrayEquals(new Object[] { null, "fl-theme-gray", null }, recorder.creationArgs(created));
    }

    @Test
    public void callsBeforeAttachAreReplayedOnAttach() {
        final FlexLayoutAddon detached = new FlexLayoutAddon();
        detached.undo();
        assertTrue(recorder.calls(detached).isEmpty());

        attach(detached);
        recorder.assertLast(detached, "undo");
    }

    // ─── Tabs ────────────────────────────────────────────────────

    @Test
    public void addTabParksContentAndSendsItsId() {
        final PLabel content = Element.newPLabel("x");
        addon.addTab("t1", "Tab 1", content);

        recorder.assertLast(addon, "addTab", "t1", "Tab 1", id(content), null, null);
        assertSame(addon.getPanel(), content.getParent());
        assertTrue(content.hasStyleName(UNPARKED));
        assertEquals(id(content), content.getProperty("id"));
        assertSame(content, addon.getTabWidgets().get("t1"));
    }

    @Test
    public void addTabWithTabsetAndIcon() {
        final PLabel a = Element.newPLabel();
        addon.addTab("t1", "A", a, "ts1");
        recorder.assertLast(addon, "addTab", "t1", "A", id(a), "ts1", null);

        final PLabel b = Element.newPLabel();
        addon.addTab("t2", "B", b, "ts1", "icon-b");
        recorder.assertLast(addon, "addTab", "t2", "B", id(b), "ts1", "icon-b");
    }

    @Test
    public void addTabFromLayoutAwareWidgetUsesItsTitleAndIcon() {
        final AwareLabel withIcon = new AwareLabel("ico");
        addon.addTab("t1", withIcon);
        recorder.assertLast(addon, "addTab", "t1", "Aware", id(withIcon), null, "ico");

        final AwareLabel noIcon = new AwareLabel(null);
        addon.addTab("t2", noIcon, "ts2");
        recorder.assertLast(addon, "addTab", "t2", "Aware", id(noIcon), "ts2", null);
    }

    @Test
    public void attachWidgetToTab() {
        final PLabel a = Element.newPLabel();
        addon.attachWidgetToTab("t1", a);
        recorder.assertLast(addon, "attachWidget", "t1", id(a));
        assertSame(a, addon.getTabWidgets().get("t1"));
        assertTrue(a.hasStyleName(UNPARKED));
    }

    @Test
    public void removeTabReleasesTheWidget() {
        final PLabel content = Element.newPLabel();
        addon.addTab("t1", "Tab 1", content);

        addon.removeTab("t1");

        recorder.assertLast(addon, "removeTab", "t1");
        assertNull(content.getParent());
        assertTrue(addon.getTabWidgets().isEmpty());
    }

    @Test
    public void removeUnknownTabOnlyNotifiesTheTerminal() {
        addon.removeTab("missing");
        recorder.assertLast(addon, "removeTab", "missing");
    }

    // ─── Borders ─────────────────────────────────────────────────

    @Test
    public void addBorderTabVariants() {
        final PLabel a = Element.newPLabel();
        addon.addBorderTab("left", "b1", "B1", a);
        recorder.assertLast(addon, "addBorderTab", "left", "b1", "B1", id(a), null, null);

        final PLabel b = Element.newPLabel();
        addon.addBorderTab("right", "b2", "B2", b, 2);
        recorder.assertLast(addon, "addBorderTab", "right", "b2", "B2", id(b), 2, null);

        final PLabel c = Element.newPLabel();
        addon.addBorderTab("bottom", "b3", "B3", c, 0, "ico");
        recorder.assertLast(addon, "addBorderTab", "bottom", "b3", "B3", id(c), 0, "ico");

        assertEquals(3, addon.getTabWidgets().size());
        assertTrue(c.hasStyleName(UNPARKED));
    }

    @Test
    public void addBorderTabFromLayoutAwareWidget() {
        final AwareLabel a = new AwareLabel("i");
        addon.addBorderTab("left", "b1", a);
        recorder.assertLast(addon, "addBorderTab", "left", "b1", "Aware", id(a), null, "i");

        final AwareLabel b = new AwareLabel(null);
        addon.addBorderTab("left", "b2", b, 1);
        recorder.assertLast(addon, "addBorderTab", "left", "b2", "Aware", id(b), 1, null);
    }

    @Test
    public void removeBorderTabReleasesTheWidget() {
        final PLabel a = Element.newPLabel();
        addon.addBorderTab("left", "b1", "B1", a);

        addon.removeBorderTab("left", "b1");
        recorder.assertLast(addon, "removeBorderTab", "left", "b1");
        assertNull(a.getParent());
        assertFalse(addon.getTabWidgets().containsKey("b1"));

        addon.removeBorderTab("left", "missing");
        recorder.assertLast(addon, "removeBorderTab", "left", "missing");
    }

    @Test
    public void borderCommands() {
        addon.selectBorderTab("left", "b1");
        recorder.assertLast(addon, "selectBorderTab", "left", "b1");
        addon.moveToBorder("t1", "right");
        recorder.assertLast(addon, "moveToBorder", "t1", "right");
        addon.moveFromBorder("right", "t1", "ts1");
        recorder.assertLast(addon, "moveFromBorder", "right", "t1", "ts1");
        addon.toggleBorder("bottom");
        recorder.assertLast(addon, "toggleBorder", "bottom");
        addon.setBorderTabStyle("left", "icon");
        recorder.assertLast(addon, "setBorderTabStyle", "left", "icon");
        addon.setBorderMinSize("left", 50);
        recorder.assertLast(addon, "setBorderMinSize", "left", 50);
        addon.setBorderSize("left", 300);
        recorder.assertLast(addon, "setBorderSize", "left", 300);
        addon.setBorderMaxSize("left", 600);
        recorder.assertLast(addon, "setBorderMaxSize", "left", 600);
        addon.reorderBorderTab("left", "b1", 3);
        recorder.assertLast(addon, "reorderBorderTab", "left", "b1", 3);
        addon.maximizeBorder("left");
        recorder.assertLast(addon, "maximizeBorder", "left");
        addon.popOutBorder("right");
        recorder.assertLast(addon, "popOutBorder", "right");
    }

    @Test
    public void isBorderVisibleReadsTheModelSnapshot() {
        final List<Boolean> answers = new ArrayList<>();

        addon.isBorderVisible("left", answers::add);
        recorder.assertLast(addon, "getModel");
        fire(addon, Json.createObjectBuilder().add("type", "modelSnapshot").add("model", "{\"side\":\"left\"}"));

        addon.isBorderVisible("left", answers::add);
        fire(addon, Json.createObjectBuilder().add("type", "modelSnapshot").add("model", "{\"side\":\"left\",\"hidden\":true}"));

        addon.isBorderVisible("left", answers::add);
        fire(addon, Json.createObjectBuilder().add("type", "modelSnapshot").add("model", "{\"side\":\"right\"}"));

        assertEquals(List.of(true, false, false), answers);
    }

    // ─── Model ───────────────────────────────────────────────────

    @Test
    public void loadModelReleasesEveryTabWidget() {
        final PLabel a = Element.newPLabel();
        final PLabel b = Element.newPLabel();
        addon.addTab("t1", "A", a);
        addon.attachWidgetToTab("t2", b, "comp");

        addon.loadModel("{\"layout\":{}}");

        recorder.assertLast(addon, "loadModel", "{\"layout\":{}}");
        assertNull(a.getParent());
        assertNull(b.getParent());
        assertTrue(addon.getTabWidgets().isEmpty());
        assertNull(addon.getModelVersion());
    }

    @Test
    public void loadModelWithVersionKeepsTheVersion() {
        addon.loadModel("{}", "v2");
        assertEquals("v2", addon.getModelVersion());
        recorder.assertLast(addon, "loadModel", "{}");
    }

    @Test
    public void modelSnapshotCallbackIsOneShot() {
        final List<String> snapshots = new ArrayList<>();
        addon.getModel(snapshots::add);
        recorder.assertLast(addon, "getModel");

        fire(addon, Json.createObjectBuilder().add("type", "modelSnapshot"));
        fire(addon, Json.createObjectBuilder().add("type", "modelSnapshot").add("model", "m1"));
        fire(addon, Json.createObjectBuilder().add("type", "modelSnapshot").add("model", "m2"));

        assertEquals(List.of("m1"), snapshots);
    }

    @Test
    public void modelChangeNotifications() {
        final List<String> models = new ArrayList<>();
        fire(addon, Json.createObjectBuilder().add("type", "modelChange").add("model", "ignored"));

        addon.setModelChangeEnabled(true);
        recorder.assertLast(addon, "enableModelChangeNotification", true);
        addon.setModelChangeDebounce(250);
        recorder.assertLast(addon, "setModelChangeDebounce", 250);
        addon.setOnModelChange(models::add);

        fire(addon, Json.createObjectBuilder().add("type", "modelChange").add("model", "m1"));
        fire(addon, Json.createObjectBuilder().add("type", "modelChange"));
        fire(addon, Json.createObjectBuilder().add("type", "modelChange").add("model", "m2"));

        assertEquals(List.of("m1", "m2"), models);
    }

    // ─── Client events ───────────────────────────────────────────

    @Test
    public void unknownOrUntypedEventsAreIgnored() {
        final List<String> closed = new ArrayList<>();
        addon.setOnTabClosed(closed::add);

        fire(addon, Json.createObjectBuilder().add("type", "somethingElse").add("tabId", "t1"));
        fire(addon, Json.createObjectBuilder().add("tabId", "t1"));

        assertTrue(closed.isEmpty());
    }

    @Test
    public void tabClosedReleasesWidgetAndNotifies() {
        final List<String> closed = new ArrayList<>();
        final PLabel a = Element.newPLabel();
        addon.attachWidgetToTab("t1", a, "comp");

        fire(addon, Json.createObjectBuilder().add("type", "tabClosed").add("tabId", "t1"));
        assertNull(a.getParent());
        assertTrue(addon.getTabWidgets().isEmpty());

        addon.setOnTabClosed(closed::add);
        fire(addon, Json.createObjectBuilder().add("type", "tabClosed"));
        fire(addon, Json.createObjectBuilder().add("type", "tabClosed").add("tabId", "unknown"));
        assertEquals(List.of("unknown"), closed);
    }

    @Test
    public void externalDropIsForwarded() {
        final List<String> drops = new ArrayList<>();
        fire(addon, Json.createObjectBuilder().add("type", "externalDrop").add("tabId", "t0"));

        addon.setOnExternalDrop((tabId, component, config) -> drops.add(tabId + "|" + component + "|" + config));
        fire(addon, Json.createObjectBuilder().add("type", "externalDrop").add("tabId", "t1").add("component", "c").add("config", "k"));
        fire(addon, Json.createObjectBuilder().add("type", "externalDrop").add("tabId", "t2"));
        fire(addon, Json.createObjectBuilder().add("type", "externalDrop").add("component", "c"));

        assertEquals(List.of("t1|c|k", "t2|null|null"), drops);
    }

    @Test
    public void actionEventsReachBothActionAndBroadcastHandlers() {
        final List<JsonObject> actions = new ArrayList<>();
        final List<JsonObject> broadcasts = new ArrayList<>();
        fire(addon, Json.createObjectBuilder().add("type", "action"));

        addon.setActionNotificationEnabled(false);
        recorder.assertLast(addon, "enableActionNotification", false);
        addon.setOnAction(actions::add);
        fire(addon, Json.createObjectBuilder().add("type", "action").add("name", "a1"));

        addon.setOnActionBroadcast(broadcasts::add);
        recorder.assertLast(addon, "enableActionNotification", true);
        fire(addon, Json.createObjectBuilder().add("type", "action").add("name", "a2"));

        assertEquals(2, actions.size());
        assertEquals("a2", actions.get(1).getString("name"));
        assertEquals(1, broadcasts.size());
        assertEquals("a2", broadcasts.get(0).getString("name"));
    }

    @Test
    public void tabSelectedVisibleAndHiddenAreForwarded() {
        final List<String> events = new ArrayList<>();
        fire(addon, Json.createObjectBuilder().add("type", "tabSelected").add("tabId", "t0"));
        fire(addon, Json.createObjectBuilder().add("type", "tabVisible").add("tabId", "t0"));
        fire(addon, Json.createObjectBuilder().add("type", "tabHidden").add("tabId", "t0"));

        addon.setOnTabSelected(t -> events.add("selected:" + t));
        addon.setOnTabVisible(t -> events.add("visible:" + t));
        addon.setOnTabHidden(t -> events.add("hidden:" + t));
        fire(addon, Json.createObjectBuilder().add("type", "tabSelected").add("tabId", "t1"));
        fire(addon, Json.createObjectBuilder().add("type", "tabVisible").add("tabId", "t1"));
        fire(addon, Json.createObjectBuilder().add("type", "tabHidden").add("tabId", "t1"));
        fire(addon, Json.createObjectBuilder().add("type", "tabSelected"));
        fire(addon, Json.createObjectBuilder().add("type", "tabVisible"));
        fire(addon, Json.createObjectBuilder().add("type", "tabHidden"));

        assertEquals(List.of("selected:t1", "visible:t1", "hidden:t1"), events);
    }

    @Test
    public void oneShotQueryCallbacks() {
        final List<String> answers = new ArrayList<>();

        addon.getTabConfig("t1", c -> answers.add("config:" + c));
        recorder.assertLast(addon, "getTabConfig", "t1");
        addon.getOpenTabs(d -> answers.add("open:" + d));
        recorder.assertLast(addon, "getOpenTabs");
        addon.getLayoutSummary(d -> answers.add("summary:" + d));
        recorder.assertLast(addon, "getLayoutSummary");
        addon.getPopOutState(s -> answers.add("state:" + s));
        recorder.assertLast(addon, "getPopOutState");

        for (final String type : List.of("tabConfig", "openTabs", "layoutSummary", "popOutState")) {
            fire(addon, Json.createObjectBuilder().add("type", type));
        }
        fire(addon, Json.createObjectBuilder().add("type", "tabConfig").add("config", "c1"));
        fire(addon, Json.createObjectBuilder().add("type", "openTabs").add("data", "d1"));
        fire(addon, Json.createObjectBuilder().add("type", "layoutSummary").add("data", "s1"));
        fire(addon, Json.createObjectBuilder().add("type", "popOutState").add("state", "p1"));
        // Second answers find no callback any more
        fire(addon, Json.createObjectBuilder().add("type", "tabConfig").add("config", "c2"));
        fire(addon, Json.createObjectBuilder().add("type", "openTabs").add("data", "d2"));
        fire(addon, Json.createObjectBuilder().add("type", "layoutSummary").add("data", "s2"));
        fire(addon, Json.createObjectBuilder().add("type", "popOutState").add("state", "p2"));

        assertEquals(List.of("config:c1", "open:d1", "summary:s1", "state:p1"), answers);
    }

    // ─── Rehydrate ───────────────────────────────────────────────

    @Test
    public void rehydrateRecreatesTabsThroughTheExternalDropHandler() {
        final List<String> drops = new ArrayList<>();
        final String tabs = "[{\"tabId\":\"t1\",\"component\":\"c1\",\"config\":\"k1\"},"
                + "{\"tabId\":\"t2\",\"component\":\"c2\",\"config\":null},"
                + "{\"tabId\":\"t3\",\"component\":\"c3\"},"
                + "{\"component\":\"c4\"},"
                + "{\"tabId\":\"t5\"}]";

        fire(addon, Json.createObjectBuilder().add("type", "rehydrate").add("tabs", tabs));
        assertTrue(drops.isEmpty());

        addon.setOnExternalDrop((tabId, component, config) -> drops.add(tabId + "|" + component + "|" + config));
        fire(addon, Json.createObjectBuilder().add("type", "rehydrate"));
        fire(addon, Json.createObjectBuilder().add("type", "rehydrate").add("tabs", tabs));

        assertEquals(List.of("t1|c1|k1", "t2|c2|null", "t3|c3|null"), drops);
    }

    @Test
    public void rehydrateMigratesComponentsOfAVersionedModel() {
        final List<String> drops = new ArrayList<>();
        final String tabs = "[{\"tabId\":\"t1\",\"component\":\"old\"},{\"tabId\":\"t2\",\"component\":\"gone\"}]";
        addon.setOnExternalDrop((tabId, component, config) -> drops.add(tabId + "|" + component));
        addon.setMigrationHandler((component, version) -> "gone".equals(component) ? null : component + "@" + version);

        // No version loaded: no migration
        fire(addon, Json.createObjectBuilder().add("type", "rehydrate").add("tabs", tabs));
        addon.loadModel("{}", "v1");
        fire(addon, Json.createObjectBuilder().add("type", "rehydrate").add("tabs", tabs));

        assertEquals(List.of("t1|old", "t2|gone", "t1|old@v1"), drops);
    }

    // ─── Pop-out ─────────────────────────────────────────────────

    @Test
    public void popOutCommands() {
        addon.popOut("t1", "T", 1, 2, 3, 4, "float");
        recorder.assertLast(addon, "popOut", "t1", "T", 1, 2, 3, 4, "float");
        addon.popOut("t1", "T", 1, 2, 3, 4);
        recorder.assertLast(addon, "popOut", "t1", "T", 1, 2, 3, 4, "float");
        addon.popOutToWindow("t1", "T", 1, 2, 3, 4);
        recorder.assertLast(addon, "popOut", "t1", "T", 1, 2, 3, 4, "window");
        addon.popOutActiveFloat();
        recorder.assertLast(addon, "popOutActiveFloat");
        addon.popOutActiveWindow();
        recorder.assertLast(addon, "popOutActiveWindow");
        addon.maximizeActiveTabset();
        recorder.assertLast(addon, "maximizeActiveTabset");
        addon.popIn("t1");
        recorder.assertLast(addon, "popIn", "t1");
    }

    @Test
    public void floatPopOutAndPopInNotifyTabContentAndHandlers() {
        final List<String> events = new ArrayList<>();
        final Content content = new Content();
        addon.addTab("t1", "T", content);
        addon.setOnPopOut(t -> events.add("out:" + t));
        addon.setOnPopIn(t -> events.add("in:" + t));

        fire(addon, Json.createObjectBuilder().add("type", "popOut"));
        fire(addon, Json.createObjectBuilder().add("type", "popOut").add("tabId", "t1"));
        assertTrue(addon.isPopOut("t1"));
        assertEquals(1, content.popOuts);
        assertSame(content, addon.getTabWidgets().get("t1"));

        fire(addon, Json.createObjectBuilder().add("type", "popIn"));
        fire(addon, Json.createObjectBuilder().add("type", "popIn").add("tabId", "unknown"));
        fire(addon, Json.createObjectBuilder().add("type", "popIn").add("tabId", "t1"));
        assertFalse(addon.isPopOut("t1"));
        assertEquals(1, content.popIns);

        assertEquals(List.of("out:t1", "in:t1"), events);
    }

    @Test
    public void floatPopOutOfPlainWidgetWithoutHandlers() {
        addon.addTab("t1", "T", Element.newPLabel());
        fire(addon, Json.createObjectBuilder().add("type", "popOut").add("tabId", "t1").add("mode", "float"));
        assertTrue(addon.isPopOut("t1"));
        fire(addon, Json.createObjectBuilder().add("type", "popIn").add("tabId", "t1"));
        assertFalse(addon.isPopOut("t1"));
    }

    @Test
    public void windowPopOutWithoutFactoryIsRolledBack() {
        final PLabel content = Element.newPLabel();
        addon.attachWidgetToTab("t1", content, "comp");

        fire(addon, Json.createObjectBuilder().add("type", "popOut").add("tabId", "t1").add("mode", "window"));

        recorder.assertLast(addon, "restorePopOut", "t1", null);
        assertFalse(addon.isPopOut("t1"));
        assertSame(content, addon.getTabWidgets().get("t1"));
    }

    @Test
    public void windowPopOutWithoutComponentIsRolledBack() {
        addon.setWidgetFactory((component, tabId) -> Element.newPLabel());
        addon.addTab("t1", "T", Element.newPLabel());

        fire(addon, Json.createObjectBuilder().add("type", "popOut").add("tabId", "t1").add("mode", "window"));

        recorder.assertLast(addon, "restorePopOut", "t1", null);
        assertFalse(addon.isPopOut("t1"));
    }

    @Test
    public void windowPopOutRebuildsTheWidgetAndPopInRestoresTheTab() {
        final List<String> built = new ArrayList<>();
        final List<PWidget> widgets = new ArrayList<>();
        final List<String> events = new ArrayList<>();
        final FlexLayoutAddon themed = attach(new FlexLayoutAddon(null, "theme-a"));
        themed.setWidgetFactory((component, tabId) -> {
            built.add(component + "|" + tabId);
            final PLabel label = Element.newPLabel(component);
            widgets.add(label);
            return label;
        });
        themed.setOnPopOut(t -> events.add("out:" + t));
        themed.setOnPopIn(t -> events.add("in:" + t));
        final Content original = new Content();
        themed.attachWidgetToTab("t1", original, "comp");

        fire(themed, Json.createObjectBuilder().add("type", "popOut").add("tabId", "t1").add("mode", "window")
            .add("tabsetId", "ts1").add("tabIdx", 2).add("title", "Title").add("w", 640).add("h", 480));

        assertTrue(themed.isPopOut("t1"));
        assertNull(original.getParent());
        assertFalse(themed.getTabWidgets().containsKey("t1"));
        assertEquals(List.of("comp|t1"), built);
        final PWidget windowWidget = widgets.get(0);
        final PWidget wrapper = (PWidget) windowWidget.getParent();
        assertNotNull(wrapper);
        assertTrue(wrapper.hasStyleName("theme-a"));

        themed.setTheme("theme-b");
        recorder.assertLast(themed, "setTheme", "theme-b");
        assertTrue(wrapper.hasStyleName("theme-b"));
        assertFalse(wrapper.hasStyleName("theme-a"));

        // popIn on a window pop-out goes through closing the PWindow, not through the terminal
        recorder.clear();
        themed.popIn("t1");

        assertFalse(recorder.methods(themed).contains("popIn"));
        assertFalse(themed.isPopOut("t1"));
        assertEquals(List.of("comp|t1", "comp|t1"), built);
        final PWidget restored = themed.getTabWidgets().get("t1");
        assertSame(widgets.get(1), restored);
        assertSame(themed.getPanel(), restored.getParent());
        recorder.assertLast(themed, "restorePopOut", "t1", id(restored));
        assertEquals(List.of("out:t1", "in:t1"), events);
    }

    @Test
    public void windowPopOutUsesFactoryResultEvenWhenNull() {
        final AtomicReference<String> lastTab = new AtomicReference<>();
        addon.setWidgetFactory((component, tabId) -> {
            lastTab.set(tabId);
            return null;
        });
        addon.attachWidgetToTab("t1", Element.newPLabel(), "comp");

        fire(addon, Json.createObjectBuilder().add("type", "popOut").add("tabId", "t1").add("mode", "window"));
        assertTrue(addon.isPopOut("t1"));
        assertEquals("t1", lastTab.get());

        // The terminal asks for the pop-in: the window closes and the tab comes back empty
        fire(addon, Json.createObjectBuilder().add("type", "popIn").add("tabId", "t1"));
        assertFalse(addon.isPopOut("t1"));
        recorder.assertLast(addon, "restorePopOut", "t1", null);
        assertFalse(addon.getTabWidgets().containsKey("t1"));
    }

    @Test
    public void removeTabClosesItsPopOutWindow() {
        addon.setWidgetFactory((component, tabId) -> Element.newPLabel());
        addon.attachWidgetToTab("t1", Element.newPLabel(), "comp");
        fire(addon, Json.createObjectBuilder().add("type", "popOut").add("tabId", "t1").add("mode", "window"));
        assertTrue(addon.isPopOut("t1"));

        addon.removeTab("t1");

        assertFalse(addon.isPopOut("t1"));
        assertTrue(recorder.methods(addon).contains("removeTab"));
    }

    @Test
    public void loadModelClosesPopOutWindows() {
        addon.setWidgetFactory((component, tabId) -> Element.newPLabel());
        addon.attachWidgetToTab("t1", Element.newPLabel(), "comp");
        addon.addTab("t2", "T2", Element.newPLabel());
        fire(addon, Json.createObjectBuilder().add("type", "popOut").add("tabId", "t1").add("mode", "window"));
        fire(addon, Json.createObjectBuilder().add("type", "popOut").add("tabId", "t2"));

        recorder.deferWindowClose();

        addon.loadModel("{}");

        assertFalse(addon.isPopOut("t1"));
        assertFalse(addon.isPopOut("t2"));
        recorder.assertLast(addon, "loadModel", "{}");

        // The window close arrives after the new model: the old tab must not be restored into it
        recorder.deliverWindowCloses();
        recorder.assertLast(addon, "loadModel", "{}");
    }

    @Test
    public void setThemeWithoutPopOutsOrPreviousTheme() {
        addon.addTab("t1", "T", Element.newPLabel());
        fire(addon, Json.createObjectBuilder().add("type", "popOut").add("tabId", "t1"));
        addon.setTheme("x");
        recorder.assertLast(addon, "setTheme", "x");
        addon.setTheme(null);
        recorder.assertLast(addon, "setTheme", new Object[] { null });
    }

    @Test
    public void setThemeToEmptyRemovesThePreviousThemeFromPopOutWindows() {
        final List<PWidget> widgets = new ArrayList<>();
        addon.setWidgetFactory((component, tabId) -> {
            final PLabel label = Element.newPLabel();
            widgets.add(label);
            return label;
        });
        addon.attachWidgetToTab("t1", Element.newPLabel(), "comp");
        fire(addon, Json.createObjectBuilder().add("type", "popOut").add("tabId", "t1").add("mode", "window"));
        final PWidget wrapper = (PWidget) widgets.get(0).getParent();
        assertEquals(0, wrapper.getStyleNames().count());

        addon.setTheme("t-a");
        assertTrue(wrapper.hasStyleName("t-a"));
        addon.setTheme("");
        assertFalse(wrapper.hasStyleName("t-a"));
    }

    // ─── Drag sources and feature toggles ────────────────────────

    @Test
    public void registerDragSources() {
        final PLabel source = Element.newPLabel();
        addon.registerDragSource(source, "{\"name\":\"n\"}");
        recorder.assertLast(addon, "registerDragSource", id(source), "{\"name\":\"n\"}");
        assertEquals(id(source), source.getProperty("id"));

        addon.registerDragSourceByClass("drag", "{}");
        recorder.assertLast(addon, "registerDragSourceByClass", "drag", "{}");
    }

    @Test
    public void featureToggles() {
        addon.setBadge("t1", "3");
        recorder.assertLast(addon, "setBadge", "t1", "3", null);
        addon.setBadge("t1", "3", "red");
        recorder.assertLast(addon, "setBadge", "t1", "3", "red");
        addon.enableUndoRedo(true);
        recorder.assertLast(addon, "enableUndoRedo", true);
        addon.enableKeyboardShortcuts(false);
        recorder.assertLast(addon, "enableKeyboardShortcuts", false);
        addon.setKeymap("{}");
        recorder.assertLast(addon, "setKeymap", "{}");
        addon.enableTouchGestures(true);
        recorder.assertLast(addon, "enableTouchGestures", true);
        addon.enableContextMenu(true);
        recorder.assertLast(addon, "enableContextMenu", true);
        addon.undo();
        recorder.assertLast(addon, "undo");
        addon.redo();
        recorder.assertLast(addon, "redo");
        addon.enableAutoSave(true);
        recorder.assertLast(addon, "enableAutoSave", true);
        addon.applyRemoteAction("{\"a\":1}");
        recorder.assertLast(addon, "applyRemoteAction", "{\"a\":1}");
        addon.showCommandPalette();
        recorder.assertLast(addon, "showCommandPalette");
        addon.setCommandPaletteItems(List.of("a", "b"));
        recorder.assertLast(addon, "setCommandPaletteItems", "[\"a\",\"b\"]");
        addon.setTabGroup("t1", "g", "blue");
        recorder.assertLast(addon, "setTabGroup", "t1", "g", "blue");
        addon.showNotification("hello", "info", 1000);
        recorder.assertLast(addon, "showNotification", "hello", "info", 1000);
        addon.setMaxTabs("ts1", 5);
        recorder.assertLast(addon, "setMaxTabs", "ts1", 5);
        addon.setLocked(true);
        recorder.assertLast(addon, "setLocked", true);
        addon.configureButtons("{}");
        recorder.assertLast(addon, "configureButtons", "{}");
        addon.setPopoutFloatVisible(false);
        recorder.assertLast(addon, "configureButtons", "{\"popoutFloat\":{\"visible\":false}}");
        addon.setPopoutWindowVisible(true);
        recorder.assertLast(addon, "configureButtons", "{\"popoutWindow\":{\"visible\":true}}");
        addon.setTabConfig("t1", "{}");
        recorder.assertLast(addon, "setTabConfig", "t1", "{}");
        addon.setIconMode("text", null);
        recorder.assertLast(addon, "setIconMode", "text", null);
        addon.setIconModeAttribute();
        recorder.assertLast(addon, "setIconMode", "attribute", "ui-icon");
    }

    @Test
    public void blockedActionsAndTabs() {
        addon.setBlockedActions(FlexAction.UNDO, FlexAction.CLOSE_ALL);
        recorder.assertLast(addon, "setBlockedActions", "[\"undo\",\"closeAll\"]");
        addon.setBlockedActions("x");
        recorder.assertLast(addon, "setBlockedActions", "[\"x\"]");
        addon.setBlockedTabs("t1", "t2");
        recorder.assertLast(addon, "setBlockedTabs", "[\"t1\",\"t2\"]");
    }

    @Test
    public void pinnedTabAndStatusBar() {
        final PLabel pinned = Element.newPLabel();
        addon.addPinnedTab("Pinned", pinned);
        final String pinId = "pin_" + System.identityHashCode(pinned);
        recorder.assertLast(addon, "addPinnedTab", pinId, "Pinned", id(pinned), null);
        assertSame(pinned, addon.getTabWidgets().get(pinId));

        final PLabel other = Element.newPLabel();
        addon.addPinnedTab("Other", other, "ts1");
        recorder.assertLast(addon, "addPinnedTab", "pin_" + System.identityHashCode(other), "Other", id(other), "ts1");

        final PLabel status = Element.newPLabel();
        addon.setStatusBarWidget(status);
        recorder.assertLast(addon, "setStatusBar", id(status));
        assertSame(addon.getPanel(), status.getParent());

        recorder.clear();
        addon.setStatusBarWidget(null);
        assertTrue(recorder.calls(addon).isEmpty());
    }

    @Test
    public void onDestroyDropsTabsAndHandlers() {
        final List<String> closed = new ArrayList<>();
        addon.setOnTabClosed(closed::add);
        addon.addTab("t1", "T", Element.newPLabel());

        addon.onDestroy();

        assertTrue(addon.getTabWidgets().isEmpty());
        assertFalse(addon.isPopOut("t1"));
        fire(addon, Json.createObjectBuilder().add("type", "tabClosed").add("tabId", "t1"));
        assertTrue(closed.isEmpty());
    }
}
