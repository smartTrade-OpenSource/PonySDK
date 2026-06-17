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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Test;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.UIContext;

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

    @After
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
        assertTrue("registered application must be listed", manager.getApplications().contains(application));
    }

    @Test
    public void testUnregisterRemovesApplication() {
        final String id = "app-" + UUID.randomUUID();
        final Application application = app(id);

        manager.registerApplication(application);
        assertSame(application, manager.getApplication(id));

        manager.unregisterApplication(application);
        assertNull("unregistered application must no longer be found", manager.getApplication(id));
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
        verify(listener).onApplicationDestroyed(application);
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
    public void testGetUIContextGlobal_findsAcrossApplications() {
        final Application application = register("app-" + UUID.randomUUID());
        final UIContext ctx = mock(UIContext.class);
        final int uiId = 123_456 + (int) (Math.random() * 1000);
        when(application.getUIContext(uiId)).thenReturn(ctx);

        @SuppressWarnings("deprecation")
        final UIContext found = manager.getUIContext(uiId);
        assertSame(ctx, found);
    }

    @Test
    public void testCountUIContexts_sumsAcrossApplicationsByDelta() {
        final int before = manager.countUIContexts();

        final Application application = register("app-" + UUID.randomUUID());
        when(application.countUIContexts()).thenReturn(3);

        assertEquals(before + 3, manager.countUIContexts());
    }
}
