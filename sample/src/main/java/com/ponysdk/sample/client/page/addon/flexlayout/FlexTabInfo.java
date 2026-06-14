package com.ponysdk.sample.client.page.addon.flexlayout;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class FlexTabInfo {
    private final String id;
    private final String name;
    private final String component;
    private final String tabsetId;
    private final boolean selected;

    FlexTabInfo(final String id, final String name, final String component, final String tabsetId, final boolean selected) {
        this.id = id;
        this.name = name;
        this.component = component;
        this.tabsetId = tabsetId;
        this.selected = selected;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getComponent() { return component; }
    public String getTabsetId() { return tabsetId; }
    public boolean isSelected() { return selected; }

    public static List<FlexTabInfo> fromJson(final String json) {
        final List<FlexTabInfo> result = new ArrayList<>();
        final JsonArray arr = Json.createReader(new StringReader(json)).readArray();
        for (int i = 0; i < arr.size(); i++) {
            final JsonObject o = arr.getJsonObject(i);
            result.add(new FlexTabInfo(
                o.getString("id", null),
                o.getString("name", null),
                o.getString("component", null),
                o.getString("tabsetId", null),
                o.getBoolean("selected", false)
            ));
        }
        return result;
    }
}
