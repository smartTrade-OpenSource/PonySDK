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

import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;

public class CommunicationSanityChecker {

    private static final Logger log = LoggerFactory.getLogger(CommunicationSanityChecker.class);

    private static final int CHECK_PERIOD = 1000;
    private static final int MAX_THREAD_CHECKER = Integer.parseInt(
        System.getProperty("communication.sanity.checker.thread.count", String.valueOf(Runtime.getRuntime().availableProcessors())));
    protected static final ScheduledThreadPoolExecutor sanityCheckerTimer = new ScheduledThreadPoolExecutor(MAX_THREAD_CHECKER,
        new ThreadFactory() {

            private int i = 0;

            @Override
            public Thread newThread(final Runnable r) {
                final Thread t = new Thread(r);
                t.setName(CommunicationSanityChecker.class.getName() + "-" + i++);
                t.setDaemon(true);
                return t;
            }
        });

    protected final AtomicBoolean started = new AtomicBoolean(false);
    private final UIContext uiContext;
    private long heartBeatPeriod;
    private RunnableScheduledFuture<?> sanityChecker;
    private CommunicationState currentState;
    private long suspectTime = -1;

    private static enum CommunicationState {
        OK,
        SUSPECT,
        KO
    }

    public CommunicationSanityChecker(final UIContext uiContext) {
        this.uiContext = uiContext;
        this.uiContext.addContextDestroyListener(context -> stop());
        final ApplicationConfiguration configuration = uiContext.getConfiguration();
        setHeartBeatPeriod(configuration.getHeartBeatPeriod(), configuration.getHeartBeatPeriodTimeUnit());
    }

    private boolean isStarted() {
        return started.get();
    }

    public void start() {
        if (!isStarted() && heartBeatPeriod > 0) {
            currentState = CommunicationState.OK;
            sanityChecker = (RunnableScheduledFuture<?>) sanityCheckerTimer.scheduleWithFixedDelay(() -> {
                try {
                    checkCommunicationState();
                } catch (final Throwable e) {
                    log.error("Error while checking communication state on UIContext #{}", uiContext.getID(), e);
                }
            }, 10, CHECK_PERIOD, TimeUnit.MILLISECONDS);
            started.set(true);
            log.info("Start communication sanity checker on UIContext #{} with period: {} ms", uiContext.getID(), heartBeatPeriod);
        }
    }

    public void stop() {
        if (isStarted()) {
            if (sanityChecker != null) {
                sanityChecker.cancel(false);
                sanityCheckerTimer.remove(sanityChecker);
                sanityChecker = null;
            }
            started.set(false);
            log.info("Stop communication sanity checker on UIContext #{}", uiContext.getID());
        }
    }

    public void setHeartBeatPeriod(final long heartbeat, final TimeUnit timeUnit) {
        heartBeatPeriod = TimeUnit.MILLISECONDS.convert(heartbeat, timeUnit);
    }

    private boolean isCommunicationSuspectedToBeNonFunctional(final long now) {
        // No message have been received or sent during the HeartbeatPeriod
        return now - uiContext.getLastReceivedTime() >= heartBeatPeriod;
    }

    private void checkCommunicationState() {
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
                if (uiContext.getLastReceivedTime() < suspectTime) {
                    if (now - suspectTime >= heartBeatPeriod) {
                        // No message have been received since we suspected the
                        // communication to be non functional
                        log.info(
                            "No message have been received on UIContext #{} since we suspected the communication to be non functional, context will be destroyed",
                            uiContext.getID());
                        currentState = CommunicationState.KO;
                        stop();
                        uiContext.disconnect();
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

        uiContext.sendRoundTrip();
    }

}
