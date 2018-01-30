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

package com.ponysdk.core.server.servlet;

import com.ponysdk.core.server.context.UIContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommunicationSanityChecker {

    private static final Logger log = LoggerFactory.getLogger(CommunicationSanityChecker.class);

    private static final int CHECK_PERIOD = 1000;


    private static final Executor checker = Executors.newSingleThreadExecutor();

    private final AtomicBoolean started = new AtomicBoolean(false);
    private Duration heartBeatPeriod;
    private ScheduledFuture<?> sanityChecker;
    private CommunicationState currentState;
    private long suspectTime = -1;

    private enum CommunicationState {
        OK,
        SUSPECT,
        KO
    }

    public CommunicationSanityChecker(Duration duration) {
        this.heartBeatPeriod = duration;
    }

    public void registerUIContext(UIContext uiContext){
        uiContext.addContextDestroyListener(context -> stop());
    }

    public void start() {
        if (!started.get()) {
            currentState = CommunicationState.OK;
            sanityChecker = checker.scheduleWithFixedDelay(this::checkCommunicationState, 10, CHECK_PERIOD, TimeUnit.MILLISECONDS);
            started.set(true);
            log.info("Start communication sanity checker on UIContext #{} with period: {} ms", uiContext.getID(), heartBeatPeriod);
        }
    }

    public void stop() {
        if (started.get()) {
            if (sanityChecker != null) {
                sanityChecker.cancel(false);
                sanityCheckerTimer.remove(sanityChecker);
                sanityChecker = null;
            }
            started.set(false);
            log.info("Stop communication sanity checker on UIContext #{}", uiContext.getID());
        }
    }

    private boolean isCommunicationSuspectedToBeNonFunctional(final long now) {
        // No message have been received or sent during the HeartbeatPeriod
        return (now - lastReceivedTime) >= heartBeatPeriod;
    }

    private void checkCommunicationState() {
        try {
            final long now = System.currentTimeMillis();
            switch (currentState) {
                case OK:
                    if (isCommunicationSuspectedToBeNonFunctional(now)) {
                        suspectTime = now;
                        currentState = CommunicationState.SUSPECT;
                        if (log.isDebugEnabled()) log.debug(
                                "No message have been received on UIContext #{}, communication suspected to be non functional, sending heartbeat...",
                                uiContext.getID());
                    }
                    break;
                case SUSPECT:
                    if (lastReceivedTime < suspectTime) {
                        if (now - suspectTime >= heartBeatPeriod) {
                            // No message have been received since we suspected the
                            // communication to be non functional
                            log.info(
                                    "No message have been received on UIContext #{} since we suspected the communication to be non functional, context will be destroyed",
                                    uiContext.getID());
                            currentState = CommunicationState.KO;
                            stop();
                            uiContext.destroy();
                        }
                    } else {
                        currentState = CommunicationState.OK;
                        suspectTime = -1;
                    }
                    break;
                case KO:
                default:
                    break;
            }

            uiContext.sendHeartBeat();
            uiContext.sendRoundTrip();
        } catch (final Throwable e) {
            log.error("Error while checking communication state on UIContext #{}", uiContext.getID(), e);
        }
    }

}
