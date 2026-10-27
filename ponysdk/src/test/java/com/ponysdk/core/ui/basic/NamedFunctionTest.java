/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.server.websocket.WebsocketEncoder;
import com.ponysdk.core.ui.basic.PScript.ExecutionCallback;
import com.ponysdk.core.ui.formatter.TextFunction;

/**
 * JavaScript called by name, never sent as source (ADR-0015).
 */
public class NamedFunctionTest {

    /** The wire as "MODEL=value" strings. */
    private static final class Wire implements WebsocketEncoder {
        final List<String> entries = new ArrayList<>();
        @Override public int protocolVersion() { return com.ponysdk.core.model.ProtocolVersion.CURRENT; }
        @Override public void beginObject() {}
        @Override public void encode(final ServerToClientModel model, final Object value) {
            entries.add(model + "=" + (value instanceof Object[] array ? Arrays.deepToString(array) : value));
        }
        @Override public void endObject() {}
    }

    private Wire wire;
    private UIContext ctx;
    private PFlowPanel root;

    @BeforeEach
    public void start() {
        wire = new Wire();
        final WebSocket socket = Mockito.mock(WebSocket.class);
        Mockito.when(socket.getUpgradeRequestData()).thenReturn(
            new com.ponysdk.core.server.websocket.UpgradeRequestData(Map.of(), "test", Mockito.mock(jakarta.servlet.http.HttpSession.class)));
        final TxnContext txn = new TxnContext(socket);
        txn.getWriter().setEncoder(wire);
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        ctx = new UIContext(socket, txn, config);
        ctx.execute(() -> {
            root = Element.newPFlowPanel();
            PWindow.getMain().add(root);
        });
        wire.entries.clear();
    }

    @AfterEach
    public void tearDown() {
        UIContext.remove();
    }

    /** Runs in the context and returns what it threw, null if nothing. */
    private Throwable run(final Runnable task) {
        final Throwable[] thrown = new Throwable[1];
        ctx.execute(() -> {
            try {
                task.run();
            } catch (final Throwable t) {
                thrown[0] = t;
            }
        });
        return thrown[0];
    }

    @Test
    public void aScriptCallsAFunctionOfTheWindowWithItsArguments() {
        run(() -> PScript.call(PWindow.getMain(), "showToast", "saved", 3));
        assertTrue(wire.entries.contains("FUNCTION_NAME=showToast"), wire.entries.toString());
        assertTrue(wire.entries.contains("FUNCTION_ARGS=[saved, 3]"), wire.entries.toString());
        assertFalse(wire.entries.stream().anyMatch(e -> e.startsWith("EVAL")), "no source sent");
    }

    @Test
    public void aCallWithoutArgumentsSendsNone() {
        run(() -> PScript.call(PWindow.getMain(), "refresh"));
        assertTrue(wire.entries.contains("FUNCTION_NAME=refresh"));
        assertFalse(wire.entries.stream().anyMatch(e -> e.startsWith("FUNCTION_ARGS")), wire.entries.toString());
    }

    @Test
    public void aCallWithACallbackAndADelayCarriesBoth() {
        final ExecutionCallback callback = Mockito.mock(ExecutionCallback.class);
        run(() -> PScript.call(PWindow.getMain(), "measure", new Object[] { "grid" }, callback, Duration.ofMillis(250)));
        final int name = wire.entries.indexOf("FUNCTION_NAME=measure");
        assertEquals(List.of("FUNCTION_NAME=measure", "FUNCTION_ARGS=[grid]", "COMMAND_ID=1", "FIXDELAY=250"),
            wire.entries.subList(name, name + 4));
    }

    @Test
    public void aWidgetCallsAFunctionOnItsElement() {
        final PButton[] button = new PButton[1];
        run(() -> { button[0] = Element.newPButton("ok"); root.add(button[0]); });
        wire.entries.clear();
        run(() -> button[0].callTerminalFunction("highlight", "#ff0"));
        assertEquals(List.of("FUNCTION_NAME=highlight", "FUNCTION_ARGS=[#ff0]"),
            wire.entries.stream().filter(e -> e.startsWith("FUNCTION_")).toList());
    }

    @Test
    public void aFunctionalLabelNamesItsFunction() {
        run(() -> root.add(Element.newPFunctionalLabel(TextFunction.named(args -> args[0] + " ponies", "ponies"), 3)));
        assertTrue(wire.entries.contains("FUNCTION_NAME=ponies"), wire.entries.toString());
        assertFalse(wire.entries.stream().anyMatch(e -> e.startsWith("FUNCTION_CREATION")), "no source sent");
    }

    /** The 2.x APIs that sent JavaScript source, which the browser ran with eval, are gone. */
    @Test
    public void noApiSendsJavaScriptSource() {
        assertTrue(Arrays.stream(PScript.class.getMethods()).noneMatch(m -> m.getName().equals("execute")));
        assertTrue(Arrays.stream(PObject.class.getMethods()).noneMatch(m -> m.getName().equals("evalTerminalScript")));
        assertEquals(0, TextFunction.class.getConstructors().length, "TextFunction.named only");
        assertTrue(Arrays.stream(TextFunction.class.getMethods()).noneMatch(m -> m.getName().equals("getJsFunction")));
    }
}
