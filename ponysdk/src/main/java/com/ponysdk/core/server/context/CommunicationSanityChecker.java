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
    private static final int CHECK_PERIOD = 1;

    private final ScheduledExecutorService checker = Executors.newSingleThreadScheduledExecutor();
    private Duration heartBeatPeriod;
    private Map<Integer, UIContext> contexts = new ConcurrentHashMap<>();

    public CommunicationSanityChecker(Duration duration) {
        this.heartBeatPeriod = duration;
    }

    public void registerUIContext(UIContext uiContext) {
        contexts.put(uiContext.getID(), uiContext);
        uiContext.addContextDestroyListener(this::removeUIContext);
    }

    public void start() {
        checker.scheduleWithFixedDelay(this::check, 0, CHECK_PERIOD, TimeUnit.SECONDS);
        log.info("Communication sanity checker is now started, period : {} seconds", CHECK_PERIOD);
    }

    private void check() {
        contexts.forEach(this::checkCommunicationState);
    }

    public void stop() {
        checker.shutdownNow();
        log.info("Communication sanity checker is now terminated");
    }

    private void removeUIContext(UIContext uiContext) {
        contexts.remove(uiContext.getID());
    }

    private void checkCommunicationState(Integer ID, UIContext uiContext) {
        try {
            if (uiContext.isAlive()) {
                final Instant now = Instant.now();
                final Duration duration = Duration.between(uiContext.getLastReceivedTime(), now);
                if (heartBeatPeriod.compareTo(duration) > 0) {
                    log.info("Close UIContext {}, no message received for {}", uiContext.getID(),heartBeatPeriod);
                    uiContext.destroy();
                }
            }
            //uiContext.sendHeartBeat();
            //uiContext.sendRoundTrip();
        } catch (final Throwable e) {
            log.error("Error while checking communication state on UIContext #{}", uiContext.getID(), e);
            //remove UIContext ?
        }
    }

}
