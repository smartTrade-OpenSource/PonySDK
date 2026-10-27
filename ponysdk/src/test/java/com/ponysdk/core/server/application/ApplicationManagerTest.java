/*
 * Copyright (c) 2017 PonySDK
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

package com.ponysdk.core.server.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.ponysdk.core.ui.basic.PHistory;
import com.ponysdk.core.ui.main.EntryPoint;

public class ApplicationManagerTest {

    @TempDir
    Path tmpDir;

    private static ApplicationManager manager(final EntryPoint entryPoint) {
        return new ApplicationManager() {

            @Override
            protected EntryPoint initializeEntryPoint() {
                return entryPoint;
            }

            @Override
            public void start() {
            }
        };
    }

    private static UIContext uiContextRunningTasksInline(final String historyToken, final PHistory history) {
        final UIContext uiContext = Mockito.mock(UIContext.class);
        Mockito.when(uiContext.execute(Mockito.any())).thenAnswer(invocation -> {
            invocation.<Runnable> getArgument(0).run();
            return true;
        });
        Mockito.when(uiContext.getHistoryToken()).thenReturn(historyToken);
        Mockito.when(uiContext.getHistory()).thenReturn(history);
        return uiContext;
    }

    @Test
    public void startApplicationReplaysTheHistoryTokenThenStartsTheEntryPoint() {
        final EntryPoint entryPoint = Mockito.mock(EntryPoint.class);
        final PHistory history = Mockito.mock(PHistory.class);
        final UIContext uiContext = uiContextRunningTasksInline("page=2", history);

        manager(entryPoint).startApplication(uiContext);

        final var order = Mockito.inOrder(history, entryPoint);
        order.verify(history).newItem("page=2", false);
        order.verify(entryPoint).start(uiContext);
    }

    @Test
    public void startApplicationWithoutHistoryTokenOnlyStartsTheEntryPoint() {
        final EntryPoint entryPoint = Mockito.mock(EntryPoint.class);
        final PHistory history = Mockito.mock(PHistory.class);

        final UIContext noToken = uiContextRunningTasksInline(null, history);
        manager(entryPoint).startApplication(noToken);
        final UIContext emptyToken = uiContextRunningTasksInline("", history);
        manager(entryPoint).startApplication(emptyToken);

        Mockito.verifyNoInteractions(history);
        Mockito.verify(entryPoint).start(noToken);
        Mockito.verify(entryPoint).start(emptyToken);
    }

    @Test
    public void noSharedDictionaryWithoutConfigurationOrPersistence() {
        final ApplicationManager manager = manager(null);
        assertNull(manager.getSharedDictionaryProvider(), "no configuration");

        final ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.setStringDictionaryPersistPath(tmpDir.toString());
        manager.setConfiguration(configuration);

        configuration.setStringDictionaryEnabled(false);
        assertNull(manager.getSharedDictionaryProvider(), "dictionary disabled");

        configuration.setStringDictionaryEnabled(true);
        configuration.setStringDictionaryPersistenceEnabled(false);
        assertNull(manager.getSharedDictionaryProvider(), "persistence disabled");

        manager.persistSharedDictionary();
        manager.shutdown();
        assertFalse(Files.exists(tmpDir.resolve(SharedDictionaryProvider.DEFAULT_FILE_NAME)));
    }

    @Test
    public void sharedDictionaryIsCreatedOnceAndPersistedToTheConfiguredPath() throws IOException {
        final ApplicationManager manager = manager(null);
        final ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.setStringDictionaryPersistPath(tmpDir.toString());
        configuration.setStringDictionaryPreSeedSize(8);
        manager.setConfiguration(configuration);

        final SharedDictionaryProvider provider = manager.getSharedDictionaryProvider();
        try {
            assertNotNull(provider);
            assertSame(provider, manager.getSharedDictionaryProvider());

            provider.recordBulkUsage(Map.of("persisted", 10L));
            manager.persistSharedDictionary();
            final Path file = tmpDir.resolve(SharedDictionaryProvider.DEFAULT_FILE_NAME);
            assertTrue(Files.readString(file).contains("\tpersisted"));

            Files.delete(file);
        } finally {
            manager.shutdown();
        }
        assertTrue(Files.exists(tmpDir.resolve(SharedDictionaryProvider.DEFAULT_FILE_NAME)), "shutdown persists");
    }

}
