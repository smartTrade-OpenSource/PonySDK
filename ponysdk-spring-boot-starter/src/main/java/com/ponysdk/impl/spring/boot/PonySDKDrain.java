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

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.SmartLifecycle;

import com.ponysdk.core.server.application.Drain;

/**
 * Moves the live pages to the other servers when Spring stops the context (ADR-0017, ADR-0023). Its phase is just above
 * Boot's graceful shutdown of the web server: a lifecycle stops from the highest phase down, so the drain ends, the
 * clients told and their sockets closed, before the server stops accepting requests (the graceful phase) and before it
 * stops (the web server's own).
 */
final class PonySDKDrain implements SmartLifecycle {

    static final int PHASE = WebServerApplicationContext.GRACEFUL_SHUTDOWN_PHASE + 1;

    private static final Logger log = LoggerFactory.getLogger(PonySDKDrain.class);

    private final Duration window;
    private volatile boolean running;

    PonySDKDrain(final Duration window) {
        this.window = window;
    }

    @Override
    public void start() {
        running = true;
    }

    /** Runs on the thread that stops the context, in the phase's turn: the drain is bounded by its window. */
    @Override
    public void stop() {
        running = false;
        if (!window.isPositive()) return; // no drain time: the server stops at once, as PonySDKServer does
        try {
            Drain.run(window);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Drain interrupted, the server stops as it is");
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }

    /** A pause (a checkpoint of the JVM) is not a stop: it must not move the pages. */
    @Override
    public boolean isPauseable() {
        return false;
    }
}
