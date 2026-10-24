/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.UpgradeRequestData;
import com.ponysdk.core.server.websocket.WebSocket;

/**
 * A real context, not {@link com.ponysdk.test.PSuite}: its context is a Mockito spy, whose invocation history
 * keeps every registered object reachable.
 */
public class PAddOnLifecycleTest {

    private static final class Addon extends PAddOn {
        Addon() {
            super(1);
        }
    }

    @AfterEach
    public void tearDown() {
        UIContext.remove();
    }

    @Test
    public void aDestroyedAddOnIsNotHeldByItsWindow() {
        final WebSocket socket = Mockito.mock(WebSocket.class);
        Mockito.when(socket.getUpgradeRequestData())
            .thenReturn(new UpgradeRequestData(Map.of(), "test", Mockito.mock(jakarta.servlet.http.HttpSession.class)));
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        final UIContext ctx = new UIContext(socket, new TxnContext(socket), config);
        final WeakReference<?>[] ref = new WeakReference<?>[1];

        ctx.execute(() -> {
            final Addon addon = new Addon();
            addon.attach(PWindow.getMain());
            addon.destroy();
            ref[0] = new WeakReference<>(addon);
        });

        assertTrue(WidgetLifecycleLeakTest.collected(ref[0]), "collectable once destroyed and no longer referenced");
    }
}
