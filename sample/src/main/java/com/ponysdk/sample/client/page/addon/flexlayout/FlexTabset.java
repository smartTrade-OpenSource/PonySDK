package com.ponysdk.sample.client.page.addon.flexlayout;

import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

/**
 * Builder for a tabset container that holds multiple {@link FlexTab} entries.
 *
 * <pre>{@code
 * FlexTabset ts = FlexTabset.create().weight(60).tab(FlexTab.create("Editor")).maxChildren(10);
 * }</pre>
 */
public class FlexTabset {
    private int weight = 100;
    private final List<FlexTab> tabs = new ArrayList<>();
    private int maxChildren = -1;

    private FlexTabset() {}

    /** Creates a new tabset builder with default weight of 100. */
    public static FlexTabset create() { return new FlexTabset(); }

    /** Sets the relative weight of this tabset within its parent row/column. */
    public FlexTabset weight(final int weight) { this.weight = weight; return this; }
    /** Adds a tab to this tabset. */
    public FlexTabset tab(final FlexTab tab) { tabs.add(tab); return this; }
    /** Sets the maximum number of tabs allowed in this tabset. */
    public FlexTabset maxChildren(final int max) { this.maxChildren = max; return this; }

    /** Serializes this tabset to a JSON string for the FlexLayout model. */

    public String toJson() {
        final JsonObjectBuilder b = Json.createObjectBuilder()
            .add("type", "tabset")
            .add("weight", weight);
        if (maxChildren > 0) b.add("maxChildren", maxChildren);
        final JsonArrayBuilder arr = Json.createArrayBuilder();
        for (final FlexTab t : tabs) {
            final JsonObjectBuilder tb = Json.createObjectBuilder()
                .add("type", "tab")
                .add("name", t.getName());
            if (t.getComponent() != null) tb.add("component", t.getComponent());
            tb.add("enableClose", t.isEnableClose());
            tb.add("enableDrag", t.isEnableDrag());
            arr.add(tb);
        }
        b.add("children", arr);
        return b.build().toString();
    }
}
