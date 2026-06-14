package com.ponysdk.sample.client.page.addon.flexlayout;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

/**
 * Immutable snapshot of a tab's runtime state, parsed from JSON received from the client.
 *
 * <pre>{@code
 * List&lt;FlexTabInfo&gt; tabs = FlexTabInfo.fromJson(jsonArray);
 * tabs.forEach(t -&gt; System.out.println(t.getName()));
 * }</pre>
 */
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

    /** Returns the unique tab identifier. */
    public String getId() { return id; }
    /** Returns the display name of the tab. */
    public String getName() { return name; }
    /** Returns the component type rendered in this tab. */
    public String getComponent() { return component; }
    /** Returns the ID of the tabset containing this tab. */
    public String getTabsetId() { return tabsetId; }
    /** Returns whether this tab is currently selected/active. */
    public boolean isSelected() { return selected; }

    /** Parses a JSON array string into a list of FlexTabInfo instances. */
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
