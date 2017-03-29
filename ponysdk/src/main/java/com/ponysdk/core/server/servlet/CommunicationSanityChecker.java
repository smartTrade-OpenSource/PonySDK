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

import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.ApplicationManagerOption;
import com.ponysdk.core.server.application.UIContext;

public class CommunicationSanityChecker {

    protected static final Logger log = LoggerFactory.getLogger(CommunicationSanityChecker.class);

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
    private static final long HEARTBEAT_PERIOD_BLOCKED = TimeUnit.MINUTES.toMillis(10);

    protected final AtomicBoolean started = new AtomicBoolean(false);
    private final UIContext uiContext;
    private long heartBeatPeriod;
    private long lastReceivedTime;
    private RunnableScheduledFuture<?> sanityChecker;
    private CommunicationState currentState;
    private long suspectTime = -1;

    private long oldHeartBeatPeriod;

    public CommunicationSanityChecker(final UIContext uiContext) {
        this.uiContext = uiContext;
        final ApplicationManagerOption options = uiContext.getApplication().getOptions();
        setHeartBeatPeriod(options.getHeartBeatPeriod(), options.getHeartBeatPeriodTimeUnit());
        enableCommunicationChecker(!uiContext.getApplication().getOptions().isDebugMode());
    }

    private boolean isStarted() {
        return started.get();
    }

    public void start() {
        lastReceivedTime = System.currentTimeMillis();
        if (!isStarted()) {
            currentState = CommunicationState.OK;
            sanityChecker = (RunnableScheduledFuture<?>) sanityCheckerTimer.scheduleWithFixedDelay(() -> {
                try {
                    checkCommunicationState();
                } catch (final Throwable e) {
                    log.error("[{}] Error while checking communication state", uiContext, e);
                }
            }, 0, CHECK_PERIOD, TimeUnit.MILLISECONDS);
            started.set(true);
            log.info("Started. HeartbeatPeriod: {} ms, {}", uiContext, heartBeatPeriod);
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
            log.info("[{}] Stopped.", uiContext);
        }
    }

    public void onMessageReceived() {
        lastReceivedTime = System.currentTimeMillis();
    }

    public void setHeartBeatPeriod(final long heartbeat, final TimeUnit timeUnit) {
        heartBeatPeriod = TimeUnit.MILLISECONDS.convert(heartbeat, timeUnit);
        oldHeartBeatPeriod = heartBeatPeriod;
        if (heartBeatPeriod <= 0) throw new IllegalArgumentException("'HeartBeatPeriod' parameter must be gretter than 0");
    }

    private boolean isCommunicationSuspectedToBeNonFunctional(final long now) {
        // No message have been received or sent during the HeartbeatPeriod
        return now - lastReceivedTime >= heartBeatPeriod;
    }

    protected void checkCommunicationState() {
        final long now = System.currentTimeMillis();
        switch (currentState) {
            case OK:
                if (isCommunicationSuspectedToBeNonFunctional(now)) {
                    suspectTime = now;
                    currentState = CommunicationState.SUSPECT;
                    if (log.isDebugEnabled()) log.debug(
                        "[{}] No message have been received, communication suspected to be non functional, sending heartbeat...",
                        uiContext);
                    //uiContext.sendHeartBeat();
                }
                break;
            case SUSPECT:
                if (lastReceivedTime < suspectTime) {
                    if (now - suspectTime >= heartBeatPeriod) {
                        // No message have been received since we suspected the
                        // communication to be non functional
                        if (log.isInfoEnabled()) log.info(
                            "[{}] No message have been received since we suspected the communication to be non functional, context will be destroyed",
                            uiContext);
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
    }

    protected enum CommunicationState {
        OK,
        SUSPECT,
        KO
    }

    /**
     * Don't really desactivate the communication checker, only set a long period
     * ({@link #HEARTBEAT_PERIOD_BLOCKED}) for the heartbeat
     */
    public void enableCommunicationChecker(final boolean enabled) {
        if (enabled) {
            heartBeatPeriod = oldHeartBeatPeriod;
        } else {
            oldHeartBeatPeriod = heartBeatPeriod;
            heartBeatPeriod = HEARTBEAT_PERIOD_BLOCKED;
        }
    }

}
