/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.sample.client.playground;

import static org.assertj.core.api.Assertions.*;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.test.ModelWriterForTest;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for Debouncer.
 * <p>
 * Tests debouncing behavior including delay, cancellation, and rapid calls.
 * </p>
 * <p>
 * Requirements: 8.3, 8.4
 * </p>
 */
class DebouncerTest {

    @BeforeEach
    void setUp() {
        final WebSocket socket = Mockito.mock(WebSocket.class);
        final ServletUpgradeRequest request = Mockito.mock(ServletUpgradeRequest.class);
        final TxnContext context = Mockito.spy(new TxnContext(socket));
        final ModelWriter modelWriter = new ModelWriterForTest();
        final Application application = Mockito.mock(Application.class);
        context.setApplication(application);
        final ApplicationConfiguration configuration = Mockito.mock(ApplicationConfiguration.class);
        Txn.get().begin(context);
        final UIContext uiContext = Mockito.spy(new UIContext(socket, context, configuration, request));
        Mockito.when(uiContext.getWriter()).thenReturn(modelWriter);
        UIContext.setCurrent(uiContext);
    }

    @AfterEach
    void tearDown() {
        Txn.get().commit();
    }

    @Test
    void testActionDelayedBySpecifiedDuration() throws InterruptedException {
        // Given: A debouncer with 100ms delay
        final Debouncer debouncer = new Debouncer(100);
        final AtomicInteger counter = new AtomicInteger(0);

        // When: An action is debounced
        debouncer.debounce(counter::incrementAndGet);

        // Then: Action should not execute immediately
        assertThat(counter.get())
            .as("Action should not execute immediately")
            .isEqualTo(0);

        // When: We wait for the delay to expire
        Thread.sleep(150);

        // Then: Action should have executed
        assertThat(counter.get())
            .as("Action should execute after delay")
            .isEqualTo(1);
    }

    @Test
    void testRapidCallsCancelPreviousPendingActions() throws InterruptedException {
        // Given: A debouncer with 100ms delay
        final Debouncer debouncer = new Debouncer(100);
        final AtomicInteger counter = new AtomicInteger(0);

        // When: Multiple rapid calls are made
        debouncer.debounce(counter::incrementAndGet);
        Thread.sleep(30);
        debouncer.debounce(counter::incrementAndGet);
        Thread.sleep(30);
        debouncer.debounce(counter::incrementAndGet);

        // Then: Only the last action should execute
        Thread.sleep(150);
        assertThat(counter.get())
            .as("Only the last debounced action should execute")
            .isEqualTo(1);
    }

    @Test
    void testCancelMethodPreventsExecution() throws InterruptedException {
        // Given: A debouncer with 100ms delay
        final Debouncer debouncer = new Debouncer(100);
        final AtomicInteger counter = new AtomicInteger(0);

        // When: An action is debounced then canceled
        debouncer.debounce(counter::incrementAndGet);
        debouncer.cancel();

        // Then: Action should not execute
        Thread.sleep(150);
        assertThat(counter.get())
            .as("Canceled action should not execute")
            .isEqualTo(0);
    }

    @Test
    void testCancelWithNoPendingActionHasNoEffect() {
        // Given: A debouncer with no pending action
        final Debouncer debouncer = new Debouncer(100);

        // When: Cancel is called
        // Then: Should not throw exception
        assertThatCode(debouncer::cancel)
            .as("Cancel with no pending action should not throw")
            .doesNotThrowAnyException();
    }

    @Test
    void testNegativeDelayThrowsException() {
        // When: Creating debouncer with negative delay
        // Then: Should throw IllegalArgumentException
        assertThatThrownBy(() -> new Debouncer(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("delayMs must not be negative");
    }

    @Test
    void testNullActionThrowsException() {
        // Given: A debouncer
        final Debouncer debouncer = new Debouncer(100);

        // When: Debouncing null action
        // Then: Should throw IllegalArgumentException
        assertThatThrownBy(() -> debouncer.debounce(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("action must not be null");
    }
}
