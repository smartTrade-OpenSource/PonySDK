package com.ponysdk.sample.client.page.addon.flexlayout;

import java.util.Map;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

public class FlexTab {
    private final String name;
    private String component;
    private String icon;
    private boolean enableClose = true;
    private boolean enableDrag = true;
    private String config;

    private FlexTab(final String name) { this.name = name; }

    public static FlexTab create(final String name) { return new FlexTab(name); }

    public FlexTab component(final String component) { this.component = component; return this; }
    public FlexTab icon(final String icon) { this.icon = icon; return this; }
    public FlexTab pinned() { this.enableClose = false; this.enableDrag = false; return this; }

    public FlexTab config(final Map<String, Object> cfg) {
        final JsonObjectBuilder b = Json.createObjectBuilder();
        for (final Map.Entry<String, Object> e : cfg.entrySet()) {
            if (e.getValue() instanceof String) b.add(e.getKey(), (String) e.getValue());
            else if (e.getValue() instanceof Number) b.add(e.getKey(), ((Number) e.getValue()).doubleValue());
            else if (e.getValue() instanceof Boolean) b.add(e.getKey(), (Boolean) e.getValue());
        }
        this.config = b.build().toString();
        return this;
    }

    public String getName() { return name; }
    public String getComponent() { return component; }
    public String getIcon() { return icon; }
    public boolean isEnableClose() { return enableClose; }
    public boolean isEnableDrag() { return enableDrag; }

    public String toJson() {
        final JsonObjectBuilder b = Json.createObjectBuilder().add("name", name);
        if (component != null) b.add("component", component);
        b.add("enableClose", enableClose);
        b.add("enableDrag", enableDrag);
        if (config != null) b.add("config", Json.createReader(new java.io.StringReader(config)).readObject());
        return b.build().toString();
    }
}
