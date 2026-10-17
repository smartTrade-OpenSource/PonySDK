package com.ponysdk.core.ui.flexlayout;

import static org.junit.Assert.*;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.StringReader;

import org.junit.Test;

public class FlexLayoutModelTest {

    private JsonObject parse(String json) {
        return Json.createReader(new StringReader(json)).readObject();
    }

    // FlexTab builder

    @Test
    public void flexTab_name() {
        FlexTab tab = FlexTab.create("Test");
        assertEquals("Test", tab.getName());
    }

    @Test
    public void flexTab_component() {
        FlexTab tab = FlexTab.create("T").component("MyComp");
        assertEquals("MyComp", tab.getComponent());
    }

    @Test
    public void flexTab_icon() {
        FlexTab tab = FlexTab.create("T").icon("fa-home");
        assertEquals("fa-home", tab.getIcon());
    }

    @Test
    public void flexTab_pinned() {
        FlexTab tab = FlexTab.create("T").pinned();
        assertFalse(tab.isEnableClose());
        assertFalse(tab.isEnableDrag());
    }

    @Test
    public void flexTab_toJson() {
        JsonObject o = parse(FlexTab.create("X").component("C").toJson());
        assertEquals("X", o.getString("name"));
        assertEquals("C", o.getString("component"));
        assertTrue(o.getBoolean("enableClose"));
        assertTrue(o.getBoolean("enableDrag"));
    }

    // FlexTabset builder

    @Test
    public void flexTabset_weight() {
        JsonObject o = parse(FlexTabset.create().weight(60).toJson());
        assertEquals(60, o.getInt("weight"));
    }

    @Test
    public void flexTabset_tabs() {
        String json = FlexTabset.create().tab(FlexTab.create("A")).tab(FlexTab.create("B")).toJson();
        JsonObject o = parse(json);
        assertEquals(2, o.getJsonArray("children").size());
    }

    @Test
    public void flexTabset_maxChildren() {
        JsonObject o = parse(FlexTabset.create().maxChildren(5).toJson());
        assertEquals(5, o.getInt("maxChildren"));
    }

    @Test
    public void flexTabset_noMaxChildren() {
        JsonObject o = parse(FlexTabset.create().toJson());
        assertFalse(o.containsKey("maxChildren"));
    }

    // FlexBorder factory methods

    @Test
    public void flexBorder_left() {
        assertEquals("left-top", FlexBorder.left().getSide());
    }

    @Test
    public void flexBorder_right() {
        assertEquals("right-top", FlexBorder.right().getSide());
    }

    @Test
    public void flexBorder_bottom() {
        assertEquals("bottom", FlexBorder.bottom().getSide());
    }

    @Test
    public void flexBorder_size() {
        JsonObject o = parse(FlexBorder.left().size(300).toJson());
        assertEquals(300, o.getInt("size"));
    }

    @Test
    public void flexBorder_tabStyle() {
        JsonObject o = parse(FlexBorder.left().tabStyle(TabStyle.ICON).toJson());
        assertEquals("icon", o.getString("tabStyle"));
    }

    @Test
    public void flexBorder_autoTabStyleOmitted() {
        JsonObject o = parse(FlexBorder.left().toJson());
        assertFalse(o.containsKey("tabStyle"));
    }

    // FlexLayoutModel

    @Test
    public void layoutModel_row() {
        JsonObject o = parse(FlexLayoutModel.row().toJson());
        assertEquals("row", o.getString("type"));
    }

    @Test
    public void layoutModel_column() {
        JsonObject o = parse(FlexLayoutModel.column().toJson());
        assertEquals("column", o.getString("type"));
    }

    @Test
    public void layoutModel_tabsets() {
        String json = FlexLayoutModel.row()
            .tabset(FlexTabset.create().tab(FlexTab.create("T1")))
            .tabset(FlexTabset.create().tab(FlexTab.create("T2")))
            .toJson();
        JsonObject o = parse(json);
        assertEquals(2, o.getJsonArray("children").size());
    }

    // FlexKeymap

    @Test
    public void keymap_defaults() {
        String json = FlexKeymap.defaults().toJson();
        JsonObject o = parse(json);
        assertTrue(o.containsKey("toggleLeft"));
        assertTrue(o.containsKey("closeAll"));
    }

    @Test
    public void keymap_bind() {
        String json = FlexKeymap.defaults().bind(FlexAction.UNDO, KeyBinding.alt("u")).toJson();
        JsonObject o = parse(json);
        JsonObject undo = o.getJsonObject("undo");
        assertTrue(undo.getBoolean("alt"));
        assertEquals("u", undo.getString("key"));
    }

    @Test
    public void keymap_unbind() {
        String json = FlexKeymap.defaults().unbind(FlexAction.REDO).toJson();
        JsonObject o = parse(json);
        assertTrue(o.isNull("redo"));
    }

    // KeyBinding factory methods

    @Test
    public void keyBinding_ctrl() {
        JsonObject o = parse(KeyBinding.ctrl("c").toJson());
        assertTrue(o.getBoolean("ctrl"));
        assertFalse(o.containsKey("shift"));
        assertFalse(o.containsKey("alt"));
        assertEquals("c", o.getString("key"));
    }

    @Test
    public void keyBinding_alt() {
        JsonObject o = parse(KeyBinding.alt("a").toJson());
        assertTrue(o.getBoolean("alt"));
        assertFalse(o.containsKey("ctrl"));
    }

    @Test
    public void keyBinding_shift() {
        JsonObject o = parse(KeyBinding.shift("s").toJson());
        assertTrue(o.getBoolean("shift"));
        assertFalse(o.containsKey("ctrl"));
    }

    @Test
    public void keyBinding_ctrlShift() {
        JsonObject o = parse(KeyBinding.ctrlShift("x").toJson());
        assertTrue(o.getBoolean("ctrl"));
        assertTrue(o.getBoolean("shift"));
    }

    @Test
    public void keyBinding_ctrlAlt() {
        JsonObject o = parse(KeyBinding.ctrlAlt("d").toJson());
        assertTrue(o.getBoolean("ctrl"));
        assertTrue(o.getBoolean("alt"));
    }

    @Test
    public void keyBinding_of() {
        JsonObject o = parse(KeyBinding.of(true, true, true, "f").toJson());
        assertTrue(o.getBoolean("ctrl"));
        assertTrue(o.getBoolean("shift"));
        assertTrue(o.getBoolean("alt"));
        assertEquals("f", o.getString("key"));
    }

    @Test
    public void keyBinding_key() {
        JsonObject o = parse(KeyBinding.key("Escape").toJson());
        assertFalse(o.containsKey("ctrl"));
        assertFalse(o.containsKey("shift"));
        assertFalse(o.containsKey("alt"));
        assertEquals("Escape", o.getString("key"));
    }

    // FlexTheme

    @Test
    public void theme_defaultEmpty() {
        assertEquals("", FlexTheme.DEFAULT.getCssClass());
    }

    @Test
    public void theme_cssValues() {
        assertEquals("fl-theme-light", FlexTheme.LIGHT.getCssClass());
        assertEquals("fl-theme-nord", FlexTheme.NORD.getCssClass());
        assertEquals("fl-theme-monokai", FlexTheme.MONOKAI.getCssClass());
    }

    // TabStyle enum

    @Test
    public void tabStyle_values() {
        assertEquals("auto", TabStyle.AUTO.getValue());
        assertEquals("icon", TabStyle.ICON.getValue());
        assertEquals("label", TabStyle.LABEL.getValue());
        assertEquals("iconLabel", TabStyle.ICON_LABEL.getValue());
    }
}
