/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.client.page.addon;

import java.nio.charset.StandardCharsets;

import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PWidget;

import jakarta.json.Json;

/**
 * Asks the server a question by an HTTP request of its own (AbstractAddon.sendDataToServer with a callback), and
 * writes the answer on its widget, as data-answer. Its script is in sample.js.
 */
public class AskAddOn extends PAddOnComposite<PWidget> {

    public AskAddOn(final PWidget widget, final String question) {
        super(widget, Json.createObjectBuilder().add("question", question).build());
        setAjaxHandler((request, response) -> {
            response.setContentType("text/plain");
            response.getOutputStream().write(("answer to " + request.getHeader("question")).getBytes(StandardCharsets.UTF_8));
        });
    }
}
