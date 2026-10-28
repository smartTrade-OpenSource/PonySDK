/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.client.page.addon;

import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PWidget;

import jakarta.json.Json;

/**
 * Outlines a widget in a colour: an addon whose creation arguments are a JSON object
 * ({@link PAddOnComposite#PAddOnComposite(PWidget, jakarta.json.JsonObject)}), on a widget that may already show.
 * Its script (sample.js) refuses an empty colour.
 */
public class HighlightAddOn extends PAddOnComposite<PWidget> {

    public HighlightAddOn(final PWidget widget, final String color) {
        super(widget, Json.createObjectBuilder().add("color", color).build());
    }
}
