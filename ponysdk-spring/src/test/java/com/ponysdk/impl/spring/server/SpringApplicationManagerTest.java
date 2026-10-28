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
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.impl.spring.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.support.GenericApplicationContext;

import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.ContextDestroyListener;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.activity.InitializingActivity;
import com.ponysdk.core.ui.main.EntryPoint;

public class SpringApplicationManagerTest {

    private static final String CLIENT_CONFIG = "com/ponysdk/impl/spring/server/client_application.xml";

    private static final List<String> initialized = new CopyOnWriteArrayList<>();
    private static final List<String> destroyed = new CopyOnWriteArrayList<>();

    public static final class RecordingEntryPoint implements EntryPoint, DisposableBean {

        @Override
        public void start(final UIContext uiContext) {
        }

        @Override
        public void destroy() {
            destroyed.add("entryPoint");
        }
    }

    public static final class RecordingActivity implements InitializingActivity {

        private final String name;

        public RecordingActivity(final String name) {
            this.name = name;
        }

        @Override
        public void afterContextInitialized() {
            initialized.add(name);
        }
    }

    private SpringApplicationManager manager;
    private ApplicationConfiguration configuration;

    @BeforeEach
    public void setUp() {
        initialized.clear();
        destroyed.clear();
        configuration = new ApplicationConfiguration();
        manager = new SpringApplicationManager();
        manager.setConfiguration(configuration);
    }

    private static GenericApplicationContext serverContext(final String... activeProfiles) {
        final GenericApplicationContext context = new GenericApplicationContext();
        context.getEnvironment().setActiveProfiles(activeProfiles);
        return context;
    }

    @Test
    public void theSessionsSpringContextLivesAsLongAsItsUIContext() {
        configuration.setClientConfigFile(CLIENT_CONFIG);
        manager.setApplicationContext(serverContext());
        manager.start();
        final UIContext uiContext = Mockito.mock(UIContext.class);
        final ArgumentCaptor<ContextDestroyListener> listener = ArgumentCaptor.forClass(ContextDestroyListener.class);
        UIContext.setCurrent(uiContext);
        try {
            manager.initializeEntryPoint();
        } finally {
            UIContext.remove();
        }

        assertEquals(List.of(), destroyed);
        Mockito.verify(uiContext).addContextDestroyListener(listener.capture());
        listener.getValue().onBeforeDestroy(uiContext);
        assertEquals(List.of("entryPoint"), destroyed);
    }

    @Test
    public void theConfiguredClientFileGivesTheEntryPointAndInitializesItsActivities() {
        configuration.setClientConfigFile(CLIENT_CONFIG);
        manager.setApplicationContext(serverContext());
        manager.start();

        assertInstanceOf(RecordingEntryPoint.class, manager.initializeEntryPoint());
        assertEquals(List.of("always"), initialized);
    }

    @Test
    public void theServerActiveProfilesAreActiveInTheClientContext() {
        configuration.setClientConfigFile(CLIENT_CONFIG);
        manager.setApplicationContext(serverContext("blue"));
        manager.start();

        manager.initializeEntryPoint();

        assertEquals(List.of("always", "blue"), initialized);
    }

    @Test
    public void eachSessionGetsItsOwnEntryPoint() {
        configuration.setClientConfigFile(CLIENT_CONFIG);
        manager.setApplicationContext(serverContext());
        manager.start();

        assertNotSame(manager.initializeEntryPoint(), manager.initializeEntryPoint());
        assertEquals(List.of("always", "always"), initialized);
    }

    @Test
    public void withoutAClientFileTheDefaultFilesAreLoaded() {
        manager.setApplicationContext(serverContext());
        manager.start();

        assertInstanceOf(RecordingEntryPoint.class, manager.initializeEntryPoint());
        assertEquals(List.of("include", "application"), initialized);
    }

    @Test
    public void anEmptyClientFileFallsBackToTheDefaultFiles() {
        configuration.setClientConfigFile("");
        manager.setApplicationContext(serverContext());
        manager.start();

        manager.initializeEntryPoint();

        assertEquals(List.of("include", "application"), initialized);
    }

    @Test
    public void aManagerNotCreatedBySpringStillBuildsTheClientContext() {
        configuration.setClientConfigFile(CLIENT_CONFIG);
        manager.start();

        assertInstanceOf(RecordingEntryPoint.class, manager.initializeEntryPoint());
        assertEquals(List.of("always"), initialized);
    }

}
