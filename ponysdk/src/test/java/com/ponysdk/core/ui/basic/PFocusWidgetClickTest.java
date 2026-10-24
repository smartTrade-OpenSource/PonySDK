/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

/** The terminal disables a clicked focus widget until the server says the click is processed. */
public class PFocusWidgetClickTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private static JsonObject click() {
        return Json.createObjectBuilder()
            .add(ClientToServerModel.DOM_HANDLER_TYPE.toStringValue(), DomHandlerType.CLICK.getValue()).build();
    }

    @Test
    public void oneEndOfProcessingPerClickWhateverTheNumberOfHandlers() {
        final PButton button = Element.newPButton("b");
        PWindow.getMain().add(button);
        final int[] calls = {0};
        button.addClickHandler(e -> calls[0]++);
        button.addClickHandler(e -> calls[0]++);
        writer.clear();

        button.onClientData(click());

        assertEquals(2, calls[0]);
        assertEquals(1, writer.count(ServerToClientModel.END_OF_PROCESSING));
    }

    @Test
    public void theFlagsAtClickTimeDecide() {
        final PButton button = Element.newPButton("b");
        PWindow.getMain().add(button);
        button.setEnabledOnRequest(true); // the terminal keeps it enabled: nothing to end
        button.addClickHandler(e -> {});
        button.setEnabledOnRequest(false); // now the terminal disables it on click: it must be given back
        writer.clear();

        button.onClientData(click());

        assertEquals(1, writer.count(ServerToClientModel.END_OF_PROCESSING));
    }

    @Test
    public void aClickHandlerCanBeRemoved() {
        final PButton button = Element.newPButton("b");
        PWindow.getMain().add(button);
        final int[] calls = {0};
        final PClickHandler handler = e -> calls[0]++;
        button.addClickHandler(handler);

        button.removeDomHandler(handler, PClickEvent.TYPE);
        button.onClientData(click());

        assertEquals(0, calls[0], "removed: not called");
    }
}
