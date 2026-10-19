/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpSession;

import org.eclipse.jetty.ee11.websocket.server.JettyServerUpgradeRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.ponysdk.core.server.servlet.SessionManager;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.UpgradeRequestData;
import com.ponysdk.core.server.websocket.WebSocket;

/** ADR-0001: what a caller sees and may do through {@link SessionAdmin}. */
public class SessionAdminTest {

    private final List<Application> applications = new ArrayList<>();
    private final Map<UIContext, WebSocket> sockets = new HashMap<>();
    private ApplicationConfiguration config;

    /** Two tabs of one HTTP session, and a tab of another user. */
    private UIContext tab1, tab2, otherUser;

    @Before
    public void setUp() {
        config = new ApplicationConfiguration();
        config.setReconnectionTimeoutMs(5000);
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        config.setStringDictionaryEnabled(false);
        final Application mine = application("mine");
        tab1 = context(mine);
        tab2 = context(mine);
        otherUser = context(application("other"));
    }

    @After
    public void tearDown() {
        applications.forEach(SessionManager.get()::unregisterApplication);
        UIContext.remove();
    }

    @Test
    public void theDefaultPolicyShowsOnlyTheCallersHttpSession() {
        final List<Integer> ids = SessionAdmin.of(tab1).sessions().stream().map(SessionInfo::id).toList();
        assertEquals(List.of(tab1.getID(), tab2.getID()), ids);
    }

    @Test
    public void aSessionTheCallerMayNotSeeIsAnUnknownId() {
        final SessionAdmin admin = SessionAdmin.of(tab1);
        assertTrue(admin.session(otherUser.getID()).isEmpty());
        assertThrows(NoSuchElementException.class, () -> admin.dropConnection(otherUser.getID()));
        Mockito.verify(sockets.get(otherUser), Mockito.never()).drop();
    }

    @Test
    public void dropClosesTheTargetSocket() {
        SessionAdmin.of(tab1).dropConnection(tab2.getID());
        Mockito.verify(sockets.get(tab2)).drop();
    }

    @Test
    public void holdIsSetThenReleased() {
        final SessionAdmin admin = SessionAdmin.of(tab1);
        admin.holdReconnection(tab2.getID(), true);
        assertTrue(tab2.isReconnectionHeld());
        assertTrue(admin.session(tab2.getID()).orElseThrow().reconnectionHeld());
        admin.holdReconnection(tab2.getID(), false);
        assertFalse(tab2.isReconnectionHeld());
    }

    @Test
    public void anActionThePolicyRefusesThrowsAndDoesNothing() {
        config.setSessionAccessPolicy((caller, target, action) -> action == SessionAction.VIEW);
        assertThrows(SecurityException.class, () -> SessionAdmin.of(tab1).dropConnection(tab2.getID()));
        Mockito.verify(sockets.get(tab2), Mockito.never()).drop();
    }

    @Test
    public void anExplicitPolicyReachesOtherUsers() {
        config.setSessionAccessPolicy((caller, target, action) -> true);
        assertTrue(SessionAdmin.of(tab1).session(otherUser.getID()).isPresent());
    }

    @Test
    public void aSuspendedSessionShowsWhatItRecords() {
        final long before = System.currentTimeMillis();
        tab2.suspend(5000);
        final SessionInfo info = SessionAdmin.of(tab1).session(tab2.getID()).orElseThrow();
        assertEquals(SessionInfo.State.SUSPENDED, info.state());
        final SessionInfo.Suspension suspension = info.suspension();
        assertNotNull(suspension);
        assertTrue(suspension.since() >= before);
        assertEquals(5000, suspension.timeoutMs());
        assertEquals(config.getMaxRecordingEntries(), suspension.maxEntries());
        assertNull(SessionAdmin.of(tab1).session(tab1.getID()).orElseThrow().suspension());
    }

    @Test
    public void destroyEndsTheSession() throws InterruptedException {
        SessionAdmin.of(tab1).destroy(tab2.getID());
        final long deadline = System.currentTimeMillis() + 5_000; // destroyed on a thread of its own
        while (tab2.isAlive() && System.currentTimeMillis() < deadline) Thread.sleep(10);
        assertFalse(tab2.isAlive());
        assertTrue(SessionAdmin.of(tab1).session(tab2.getID()).isEmpty());
    }

    private Application application(final String id) {
        final Application application = new Application(id, Mockito.mock(HttpSession.class), config);
        SessionManager.get().registerApplication(application);
        applications.add(application);
        return application;
    }

    private UIContext context(final Application application) {
        final WebSocket socket = Mockito.mock(WebSocket.class);
        Mockito.when(socket.getUpgradeRequestData()).thenReturn(new UpgradeRequestData(Map.of(), "test", Mockito.mock(HttpSession.class)));
        final TxnContext txnContext = new TxnContext(socket);
        txnContext.setApplication(application);
        final UIContext context = new UIContext(socket, txnContext, config, Mockito.mock(JettyServerUpgradeRequest.class));
        application.registerUIContext(context);
        sockets.put(context, socket);
        return context;
    }
}
