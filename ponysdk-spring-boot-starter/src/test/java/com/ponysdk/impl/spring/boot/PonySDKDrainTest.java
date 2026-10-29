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

package com.ponysdk.impl.spring.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.web.server.context.WebServerApplicationContext;

import com.ponysdk.core.server.application.Drain;

/** The lifecycle on its own: what Spring asks of it. */
class PonySDKDrainTest {

    @Test
    void itRunsInTheGracefulPhasesTurnAndIsNotPaused() {
        final PonySDKDrain drain = new PonySDKDrain(Duration.ofSeconds(1));
        assertEquals(WebServerApplicationContext.GRACEFUL_SHUTDOWN_PHASE + 1, drain.getPhase());
        assertFalse(drain.isPauseable(), "a checkpoint of the JVM is not a stop");
        assertFalse(drain.isRunning());
        drain.start();
        assertTrue(drain.isRunning());
        drain.stop();
        assertFalse(drain.isRunning());
    }

    @Test
    void aWindowOfZeroDoesNotDrain() {
        try (LingeringScreen lingering = new LingeringScreen()) {
            final PonySDKDrain drain = new PonySDKDrain(Duration.ZERO);
            drain.start();
            final long start = System.nanoTime();
            drain.stop();
            assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() < 500, "it would wait for the lingering screen");
        }
    }

    @Test
    void anInterruptedDrainKeepsTheInterruptAndLetsTheServerStop() {
        try (LingeringScreen lingering = new LingeringScreen()) {
            final PonySDKDrain drain = new PonySDKDrain(Duration.ofSeconds(5));
            drain.start();
            Thread.currentThread().interrupt();
            drain.stop();
            assertTrue(Thread.interrupted(), "the interrupt is the caller's to see");
            assertFalse(Drain.isDraining());
        }
    }

    @Test
    void theHealthIndicatorSaysUpAndOutOfServiceWhileDraining() throws Exception {
        final PonySDKHealthIndicator indicator = new PonySDKHealthIndicator();
        assertEquals(Status.UP, indicator.health().getStatus());

        try (LingeringScreen lingering = new LingeringScreen()) {
            final Thread drain = Thread.ofVirtual().start(() -> new PonySDKDrain(Duration.ofSeconds(2)).stop());
            final long deadline = System.currentTimeMillis() + 2_000;
            while (!Drain.isDraining() && System.currentTimeMillis() < deadline) Thread.sleep(10);
            assertEquals(Status.OUT_OF_SERVICE, indicator.health().getStatus());
            drain.join();
        }
        assertEquals(Status.UP, indicator.health().getStatus());
    }
}
