package com.ponysdk.sample.client.page.addon.flexlayout;

import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

public class FlexLayoutModel {
    private final List<FlexTabset> tabsets = new ArrayList<>();
    private final String direction;

    private FlexLayoutModel(final String direction) { this.direction = direction; }

    public static FlexLayoutModel row() { return new FlexLayoutModel("row"); }
    public static FlexLayoutModel column() { return new FlexLayoutModel("column"); }

    public FlexLayoutModel tabset(final FlexTabset ts) { tabsets.add(ts); return this; }

    public String toJson() {
        final JsonObjectBuilder b = Json.createObjectBuilder().add("type", direction);
        final JsonArrayBuilder arr = Json.createArrayBuilder();
        for (final FlexTabset ts : tabsets) {
            arr.add(Json.createReader(new java.io.StringReader(ts.toJson())).readObject());
        }
        b.add("children", arr);
        return b.build().toString();
    }
}
