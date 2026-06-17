package com.ponysdk.core.ui.flexlayout;

import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

/**
 * Builder for a tab entry in a FlexLayout tabset.
 *
 * <pre>{@code
 * FlexTab tab = FlexTab.create("Editor").component("code-editor").icon("fa-code").pinned();
 * }</pre>
 */
public final class FlexTab {
    private final String name;
    private String component;
    private String icon;
    private boolean enableClose = true;
    private boolean enableDrag = true;
    private String config;

    private FlexTab(final String name) { this.name = name; }

    /** Creates a new tab builder with the given display name. */
    public static FlexTab create(final String name) { return new FlexTab(name); }

    /** Sets the component type rendered in this tab's content area. */
    public FlexTab component(final String component) { this.component = component; return this; }
    /** Sets the icon CSS class for the tab header. */
    public FlexTab icon(final String icon) { this.icon = icon; return this; }
    /** Makes the tab non-closable and non-draggable. */
    public FlexTab pinned() { this.enableClose = false; this.enableDrag = false; return this; }

    /** Attaches arbitrary configuration data passed to the component. */
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

    /** Serializes this tab to a JSON string for the FlexLayout model. */
    String toJson() {
        final JsonObjectBuilder b = Json.createObjectBuilder().add("name", name);
        if (component != null) b.add("component", component);
        b.add("enableClose", enableClose);
        b.add("enableDrag", enableDrag);
        if (config != null) b.add("config", Json.createReader(new java.io.StringReader(config)).readObject());
        return b.build().toString();
    }
}
