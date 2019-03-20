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

package com.ponysdk.core.server.context;

import com.ponysdk.core.server.websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommunicationSanityChecker {

    private static final Logger log = LoggerFactory.getLogger(CommunicationSanityChecker.class);
    private static final int DELAY = 1;

    private final ScheduledExecutorService checker = Executors.newSingleThreadScheduledExecutor();
    private Duration heartBeatPeriod;

    private Map<String, WebSocket> sockets = new ConcurrentHashMap<>();

    public CommunicationSanityChecker(Duration duration) {
        this.heartBeatPeriod = duration;
    }

    public void registerSession(WebSocket socket) {
        sockets.put(socket.getSessionID(), socket);
    }

    public void start() {
        checker.scheduleWithFixedDelay(this::check, 0, DELAY, TimeUnit.SECONDS);
        log.info("Communication sanity checker is now started, period : {} seconds", DELAY);
    }

    private void check() {
        sockets.forEach(this::checkCommunicationState);
    }

    public void stop() {
        checker.shutdownNow();
        log.info("Communication sanity checker is now terminated");
    }

    private void checkCommunicationState(String ID, WebSocket socket) {
        try {
            if (socket.isSessionOpen()) {
                final Duration duration = Duration.between(socket.getLastReceivedTime(), Instant.now()).abs();
                if (heartBeatPeriod.compareTo(duration) < 0) {
                    log.info("Session {} will be closed, no message received for {} seconds", socket, heartBeatPeriod.getSeconds());
                    closeSession(socket);
                }
            }
        } catch (final Exception e) {
            log.error("Error while checking communication state on session {}", socket, e);
            closeSession(socket);
        }
    }

    private void closeSession(WebSocket socket) {
        if (socket == null) return;
        try {
            socket.close();
        } catch (Exception t) {
            log.error("Cannot close the session {}", socket, t);
        } finally {
            sockets.remove(socket.getSessionID());
        }
    }

}
