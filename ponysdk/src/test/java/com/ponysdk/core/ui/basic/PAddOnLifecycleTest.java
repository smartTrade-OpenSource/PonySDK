/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.Assert.assertTrue;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.ee11.websocket.server.JettyServerUpgradeRequest;
import org.junit.After;
import org.junit.Test;
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

    @After
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
        final UIContext ctx = new UIContext(socket, new TxnContext(socket), config,
            Mockito.mock(JettyServerUpgradeRequest.class));
        final WeakReference<?>[] ref = new WeakReference<?>[1];

        ctx.execute(() -> {
            final Addon addon = new Addon();
            addon.attach(PWindow.getMain());
            addon.destroy();
            ref[0] = new WeakReference<>(addon);
        });

        assertTrue("collectable once destroyed and no longer referenced", WidgetLifecycleLeakTest.collected(ref[0]));
    }
}
