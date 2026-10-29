/*
 * Copyright (c) 2026 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ponysdk.core.server.servlet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ContextDestroyListener;
import com.ponysdk.core.server.application.ContextKeys;
import com.ponysdk.core.server.application.UIContext;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Non-regression tests for {@link SessionManager} — the registry of live applications and the
 * session-scoped UIContext lookup. The scoped lookup {@link SessionManager#getUIContext(String, int)}
 * is security-sensitive (it prevents cross-session IDOR access), so its contract is pinned here.
 *
 * <p>{@link SessionManager} is a process-wide singleton; each test registers applications with
 * unique ids and unregisters them in {@link #cleanup()}, and uses delta assertions for the global
 * aggregates so pre-existing state from other tests cannot make these flaky.
 */
public class SessionManagerTest {

    private final SessionManager manager = SessionManager.get();
    private final List<Application> registered = new ArrayList<>();

    @AfterEach
    public void cleanup() {
        registered.forEach(manager::unregisterApplication);
        registered.clear();
    }

    private Application app(final String id) {
        final Application application = mock(Application.class);
        when(application.getId()).thenReturn(id);
        return application;
    }

    private Application register(final String id) {
        final Application application = app(id);
        manager.registerApplication(application);
        registered.add(application);
        return application;
    }

    @Test
    public void testRegisterAndGetApplication() {
        final String id = "app-" + UUID.randomUUID();
        final Application application = register(id);

        assertSame(application, manager.getApplication(id));
        assertTrue(manager.getApplications().contains(application), "registered application must be listed");
    }

    @Test
    public void testUnregisterRemovesApplication() {
        final String id = "app-" + UUID.randomUUID();
        final Application application = app(id);

        manager.registerApplication(application);
        assertSame(application, manager.getApplication(id));

        manager.unregisterApplication(application);
        assertNull(manager.getApplication(id), "unregistered application must no longer be found");
        assertFalse(manager.getApplications().contains(application));
    }

    @Test
    public void testListenersNotifiedOnCreateAndDestroy() {
        final ApplicationListener listener = mock(ApplicationListener.class);
        manager.addApplicationListener(listener);

        final Application application = register("app-" + UUID.randomUUID());
        verify(listener).onApplicationCreated(application);

        manager.unregisterApplication(application);
        registered.remove(application);
        // the status-code overload is the one SessionManager calls (its default delegates to the 1-arg one)
        verify(listener).onApplicationDestroyed(org.mockito.ArgumentMatchers.eq(application), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    public void testAListenerOfTheOneArgumentDestroyIsNotifiedToo() {
        final List<Application> destroyed = new ArrayList<>();
        manager.addApplicationListener(new ApplicationListener() {

            @Override
            public void onApplicationCreated(final Application application) {
                // not observed here
            }

            @Override
            public void onApplicationDestroyed(final Application application) {
                destroyed.add(application);
            }
        });
        final Application application = register("app-" + UUID.randomUUID());

        manager.unregisterApplication(application);
        registered.remove(application);

        assertTrue(destroyed.contains(application));
    }

    @Test
    public void testGetUIContextScoped_returnsContextOfMatchingApplication() {
        final String id = "app-" + UUID.randomUUID();
        final Application application = register(id);
        final UIContext ctx = mock(UIContext.class);
        when(application.getUIContext(7)).thenReturn(ctx);

        assertSame(ctx, manager.getUIContext(id, 7));
    }

    @Test
    public void testGetUIContextScoped_nullApplicationIdReturnsNull() {
        // IDOR guard: a missing/unknown caller session must never resolve a context.
        assertNull(manager.getUIContext(null, 7));
    }

    @Test
    public void testGetUIContextScoped_unknownApplicationReturnsNull() {
        assertNull(manager.getUIContext("missing-" + UUID.randomUUID(), 7));
    }

    @Test
    public void testCountUIContexts_sumsAcrossApplicationsByDelta() {
        final int before = manager.countUIContexts();

        final Application application = register("app-" + UUID.randomUUID());
        when(application.countUIContexts()).thenReturn(3);

        assertEquals(before + 3, manager.countUIContexts());
    }

    // ---- the contexts of embedded terminals, found by key (ADR-0009) ----

    private static UIContext keyedContext(final int id, final List<ContextDestroyListener> listeners) {
        final UIContext uiContext = mock(UIContext.class);
        final ContextKeys keys = ContextKeys.create();
        when(uiContext.getID()).thenReturn(id);
        when(uiContext.getKeys()).thenReturn(keys);
        org.mockito.Mockito.doAnswer(invocation -> listeners.add(invocation.getArgument(0)))
            .when(uiContext).addContextDestroyListener(org.mockito.ArgumentMatchers.any());
        return uiContext;
    }

    private static HttpServletRequest requestWith(final String header, final String parameter) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(ClientToServerModel.CONTEXT_KEY.name())).thenReturn(header);
        when(request.getParameter(ClientToServerModel.STREAM_KEY.toStringValue())).thenReturn(parameter);
        return request;
    }

    @Test
    public void aContextWithKeysIsFoundByThemUntilDestroyed() {
        final List<ContextDestroyListener> listeners = new ArrayList<>();
        final UIContext uiContext = keyedContext(-1001, listeners);
        manager.registerKeys(uiContext);
        final String[] keys = uiContext.getKeys().encoded().split("\\.");

        assertSame(uiContext, manager.getUIContextByKey(-1001, keys[0]));
        assertNull(manager.getUIContextByKey(-1001, keys[1]), "the stream key proves a stream only");
        assertNull(manager.getUIContextByKey(-1002, keys[0]), "another id");
        assertSame(uiContext, manager.getUIContext(requestWith(keys[0], null), -1001, false), "K in its header");
        assertNull(manager.getUIContext(requestWith(null, keys[1]), -1001, false), "S is not for AJAX nor uploads");
        assertSame(uiContext, manager.getUIContext(requestWith(null, keys[1]), -1001, true), "S for a pending stream");
        assertNull(manager.getUIContext(requestWith("wrong", "wrong"), -1001, true));

        listeners.forEach(listener -> listener.onBeforeDestroy(uiContext));
        assertNull(manager.getUIContextByKey(-1001, keys[0]), "gone with its context");
    }

    @Test
    public void aPageContextHasNoKeys() {
        final UIContext uiContext = mock(UIContext.class);
        when(uiContext.getID()).thenReturn(-1003);
        manager.registerKeys(uiContext);
        assertNull(manager.getUIContext(requestWith("any", "any"), -1003, true));
    }
}
