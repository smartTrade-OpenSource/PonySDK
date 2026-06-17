package com.ponysdk.core.ui.flexlayout;

import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;

/**
 * Root model representing the main content area of a FlexLayout (row or column of tabsets).
 *
 * <pre>{@code
 * FlexLayoutModel model = FlexLayoutModel.row()
 *     .tabset(FlexTabset.create().tab(FlexTab.create("Main")))
 *     .tabset(FlexTabset.create().tab(FlexTab.create("Side")));
 * }</pre>
 */
public final class FlexLayoutModel {
    private final List<FlexTabset> tabsets = new ArrayList<>();
    private final String direction;

    private FlexLayoutModel(final String direction) { this.direction = direction; }

    /** Creates a horizontal (row) layout model. */
    public static FlexLayoutModel row() { return new FlexLayoutModel("row"); }
    /** Creates a vertical (column) layout model. */
    public static FlexLayoutModel column() { return new FlexLayoutModel("column"); }

    /** Adds a tabset child to this layout model. */
    public FlexLayoutModel tabset(final FlexTabset ts) { tabsets.add(ts); return this; }

    /** Serializes this layout model to a JSON string. */

    String toJson() {
        final JsonObjectBuilder b = Json.createObjectBuilder().add("type", direction);
        final JsonArrayBuilder arr = Json.createArrayBuilder();
        for (final FlexTabset ts : tabsets) {
            arr.add(Json.createReader(new java.io.StringReader(ts.toJson())).readObject());
        }
        b.add("children", arr);
        return b.build().toString();
    }
}
