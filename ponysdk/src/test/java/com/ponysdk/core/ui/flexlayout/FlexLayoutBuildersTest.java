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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.basic.PWidget;

public class FlexLayoutBuildersTest {

    private static JsonObject read(final String json) {
        return Json.createReader(new StringReader(json)).readObject();
    }

    // ─── FlexTab ─────────────────────────────────────────────────

    @Test
    public void tabDefaults() {
        final FlexTab tab = FlexTab.create("Editor");
        assertEquals("Editor", tab.getName());
        assertNull(tab.getComponent());
        assertNull(tab.getIcon());
        assertTrue(tab.isEnableClose());
        assertTrue(tab.isEnableDrag());
        assertEquals("{\"name\":\"Editor\",\"enableClose\":true,\"enableDrag\":true}", tab.toJson());
    }

    @Test
    public void tabWithEverything() {
        final Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("symbol", "EUR");
        cfg.put("depth", 5);
        cfg.put("live", true);
        cfg.put("ignored", new Object());
        final FlexTab tab = FlexTab.create("Book").component("book").icon("fa-book").pinned().config(cfg);

        assertFalse(tab.isEnableClose());
        assertFalse(tab.isEnableDrag());
        final JsonObject json = read(tab.toJson());
        assertEquals("book", json.getString("component"));
        assertEquals("fa-book", json.getString("icon"));
        assertFalse(json.getBoolean("enableClose"));
        assertFalse(json.getBoolean("enableDrag"));
        final JsonObject config = json.getJsonObject("config");
        assertEquals("EUR", config.getString("symbol"));
        assertEquals(5.0, config.getJsonNumber("depth").doubleValue());
        assertTrue(config.getBoolean("live"));
        assertFalse(config.containsKey("ignored"));
    }

    // ─── FlexTabset / FlexLayoutModel ────────────────────────────

    @Test
    public void tabsetSerializesItsTabs() {
        final FlexTabset tabset = FlexTabset.create().weight(60).maxChildren(4)
            .tab(FlexTab.create("A").component("a"))
            .tab(FlexTab.create("B").pinned());

        final JsonObject json = read(tabset.toJson());
        assertEquals("tabset", json.getString("type"));
        assertEquals(60, json.getInt("weight"));
        assertEquals(4, json.getInt("maxChildren"));
        final JsonArray children = json.getJsonArray("children");
        assertEquals(2, children.size());
        assertEquals("tab", children.getJsonObject(0).getString("type"));
        assertEquals("a", children.getJsonObject(0).getString("component"));
        assertTrue(children.getJsonObject(0).getBoolean("enableClose"));
        assertFalse(children.getJsonObject(1).containsKey("component"));
        assertFalse(children.getJsonObject(1).getBoolean("enableDrag"));
    }

    @Test
    public void tabsetDefaults() {
        final JsonObject json = read(FlexTabset.create().toJson());
        assertEquals(100, json.getInt("weight"));
        assertFalse(json.containsKey("maxChildren"));
        assertTrue(json.getJsonArray("children").isEmpty());
        assertFalse(read(FlexTabset.create().maxChildren(0).toJson()).containsKey("maxChildren"));
    }

    @Test
    public void tabsetKeepsTabIconAndConfig() {
        final FlexTab tab = FlexTab.create("A").icon("fa-a").config(Map.of("k", "v"));
        final JsonObject child = read(FlexTabset.create().tab(tab).toJson()).getJsonArray("children").getJsonObject(0);
        assertEquals("fa-a", child.getString("icon"));
        assertEquals("v", child.getJsonObject("config").getString("k"));
    }

    @Test
    public void modelRowAndColumn() {
        final JsonObject row = read(FlexLayoutModel.row().tabset(FlexTabset.create()).tabset(FlexTabset.create().weight(20)).toJson());
        assertEquals("row", row.getString("type"));
        assertEquals(2, row.getJsonArray("children").size());
        assertEquals(20, row.getJsonArray("children").getJsonObject(1).getInt("weight"));

        final JsonObject column = read(FlexLayoutModel.column().toJson());
        assertEquals("column", column.getString("type"));
        assertTrue(column.getJsonArray("children").isEmpty());
    }

    // ─── FlexBorder ──────────────────────────────────────────────

    @Test
    public void borderSides() {
        assertEquals("left-top", FlexBorder.left().getSide());
        assertEquals("left-top", FlexBorder.leftTop().getSide());
        assertEquals("left-bottom", FlexBorder.leftBottom().getSide());
        assertEquals("right-top", FlexBorder.right().getSide());
        assertEquals("right-top", FlexBorder.rightTop().getSide());
        assertEquals("right-bottom", FlexBorder.rightBottom().getSide());
        assertEquals("bottom", FlexBorder.bottom().getSide());
    }

    @Test
    public void borderJson() {
        final FlexBorder border = FlexBorder.bottom();
        assertEquals(TabStyle.AUTO, border.getTabStyle());
        assertEquals("{\"type\":\"border\",\"location\":\"bottom\",\"size\":220}", border.toJson());

        border.size(150).tabStyle(TabStyle.ICON_LABEL);
        assertEquals(TabStyle.ICON_LABEL, border.getTabStyle());
        final JsonObject json = read(border.toJson());
        assertEquals(150, json.getInt("size"));
        assertEquals("iconLabel", json.getString("tabStyle"));
    }

    // ─── KeyBinding / FlexKeymap ─────────────────────────────────

    @Test
    public void keyBindings() {
        assertEquals("{\"key\":\"a\"}", KeyBinding.key("a").toJson());
        assertEquals("{\"ctrl\":true,\"key\":\"a\"}", KeyBinding.ctrl("a").toJson());
        assertEquals("{\"ctrl\":true,\"shift\":true,\"key\":\"a\"}", KeyBinding.ctrlShift("a").toJson());
        assertEquals("{\"ctrl\":true,\"alt\":true,\"key\":\"a\"}", KeyBinding.ctrlAlt("a").toJson());
        assertEquals("{\"ctrl\":true,\"shift\":true,\"alt\":true,\"key\":\"a\"}", KeyBinding.ctrlShiftAlt("a").toJson());
        assertEquals("{\"alt\":true,\"key\":\"a\"}", KeyBinding.alt("a").toJson());
        assertEquals("{\"shift\":true,\"key\":\"a\"}", KeyBinding.shift("a").toJson());
        assertEquals("{\"shift\":true,\"alt\":true,\"key\":\"a\"}", KeyBinding.shiftAlt("a").toJson());
        assertEquals("{\"ctrl\":true,\"alt\":true,\"key\":\"F2\"}", KeyBinding.of(true, false, true, "F2").toJson());
        assertEquals("{\"key\":\"F2\"}", KeyBinding.of(false, false, false, "F2").toJson());
    }

    @Test
    public void defaultKeymap() {
        final JsonObject json = read(FlexKeymap.defaults().toJson());
        assertEquals(List.of("toggleLeft", "toggleRight", "toggleBottom", "closeAll", "undo", "redo", "closeActiveTab",
            "commandPalette"), List.copyOf(json.keySet()));
        assertEquals(read(KeyBinding.ctrlShift("b").toJson()), json.getJsonObject("toggleLeft"));
        assertEquals(read(KeyBinding.ctrl("z").toJson()), json.getJsonObject("undo"));
    }

    @Test
    public void keymapBindAndUnbind() {
        final JsonObject json = read(FlexKeymap.defaults()
            .bind(FlexAction.UNDO, KeyBinding.alt("u"))
            .bind(FlexAction.RENAME_TAB, KeyBinding.key("F2"))
            .unbind(FlexAction.CLOSE_ALL)
            .toJson());
        assertEquals("u", json.getJsonObject("undo").getString("key"));
        assertTrue(json.getJsonObject("undo").getBoolean("alt"));
        assertEquals("F2", json.getJsonObject("renameTab").getString("key"));
        assertTrue(json.isNull("closeAll"));
    }

    // ─── FlexTabInfo ─────────────────────────────────────────────

    @Test
    public void tabInfoFromEmptyArray() {
        assertTrue(FlexTabInfo.fromJson("[]").isEmpty());
    }

    // ─── Enums and interfaces ────────────────────────────────────

    @Test
    public void enumValues() {
        assertEquals("", FlexTheme.DEFAULT.getCssClass());
        assertEquals("fl-theme-light", FlexTheme.LIGHT.getCssClass());
        assertEquals("fl-theme-underline", FlexTheme.UNDERLINE.getCssClass());
        assertEquals(11, FlexTheme.values().length);
        assertEquals("auto", TabStyle.AUTO.getValue());
        assertEquals("icon", TabStyle.ICON.getValue());
        assertEquals("label", TabStyle.LABEL.getValue());
        assertEquals("toggleLeft", FlexAction.TOGGLE_LEFT.getKey());
        assertEquals("closeActiveTab", FlexAction.CLOSE_ACTIVE_TAB.getKey());
    }

    @Test
    public void interfaceDefaults() {
        final TabContent content = new TabContent() {

            @Override
            public PWidget asWidget() {
                return null;
            }

            @Override
            public String getTabName() {
                return "T";
            }

            @Override
            public String getComponent() {
                return "c";
            }
        };
        assertTrue(content.isClosable());
        assertTrue(content.isPopOutable());
        // Lifecycle hooks are no-ops by default
        content.onPopOut();
        content.onPopIn();
        content.onActivated();
        content.onDeactivated();

        final FlexLayoutAware aware = () -> "Title";
        assertEquals("Title", aware.getFlexTitle());
        assertNull(aware.getFlexIcon());
    }
}
