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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;

public class CommunicationSanityChecker {

    private static final Logger log = LoggerFactory.getLogger(CommunicationSanityChecker.class);

    private static final int CHECK_PERIOD = 1000;
    /**
     * The roundtrip probe goes every 5 s, or every heartbeat period if shorter: it measures the latency, and keeps
     * the terminal's watchdog (no message for twice the period) quiet on an idle session.
     */
    private static final int ROUNDTRIP_PERIOD = 5000;
    private static final int MAX_THREAD_CHECKER = Integer.parseInt(
        System.getProperty("communication.sanity.checker.thread.count", String.valueOf(Runtime.getRuntime().availableProcessors())));
    protected static final ScheduledThreadPoolExecutor sanityCheckerTimer = new ScheduledThreadPoolExecutor(MAX_THREAD_CHECKER,
        Thread.ofVirtual().name("SanityChecker-", 0).factory());

    static {
        // a stopped checker leaves the queue at once: no purge() (a copy and scan of every session's checker)
        sanityCheckerTimer.setRemoveOnCancelPolicy(true);
    }

    protected final AtomicBoolean started = new AtomicBoolean(false);
    private final UIContext uiContext;
    private long heartBeatPeriod;
    private int roundTripChecks; // checks between two probes
    private int checksSinceRoundTrip;
    private ScheduledFuture<?> sanityChecker;
    private CommunicationState currentState = CommunicationState.OK;
    private volatile long suspectTime = -1;
    /** The time of the checks; a test sets its own. */
    LongSupplier clock = System::currentTimeMillis;

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
            sanityChecker = sanityCheckerTimer.scheduleWithFixedDelay(() -> {
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
        // Only one of concurrent stops (the destroy listener, the check finding the connection dead) cancels
        if (started.compareAndSet(true, false)) {
            sanityChecker.cancel(false);
            sanityChecker = null;
            log.info("Stop communication sanity checker on UIContext #{}", uiContext.getID());
        }
    }

    public void setHeartBeatPeriod(final long heartbeat, final TimeUnit timeUnit) {
        heartBeatPeriod = TimeUnit.MILLISECONDS.convert(heartbeat, timeUnit);
        roundTripChecks = (int) Math.max(1, Math.min(ROUNDTRIP_PERIOD, heartBeatPeriod) / CHECK_PERIOD);
        checksSinceRoundTrip = roundTripChecks - 1; // the first check sends one
    }

    /**
     * Only what comes from the client proves it is there: its messages, and the pongs its browser sends back to the
     * server's pings outside the page's JavaScript, so even while the page is busy (RFC 6455, 5.5.2). The server's own
     * sends prove nothing: they fill the socket's buffers whether the client reads them or not.
     * <p>
     * After half a heartbeat period of silence, a ping goes at each check. After a period, the connection is suspect;
     * after another one, it is dead: the socket is dropped, and the context suspended or destroyed as configured.
     */
    void checkCommunicationState() {
        // Skip sanity checks while UIContext is suspended (waiting for WS reconnection)
        if (uiContext.isSuspended()) return;

        final long now = clock.getAsLong();
        final long lastReceived = uiContext.getLastReceivedTime();
        if (currentState == CommunicationState.KO) return;
        if (currentState == CommunicationState.OK) {
            if (now - lastReceived >= heartBeatPeriod) {
                suspectTime = now;
                currentState = CommunicationState.SUSPECT;
                if (log.isDebugEnabled()) log.debug("Nothing received on UIContext #{} for {} ms, connection suspected",
                    uiContext.getID(), now - lastReceived);
            }
        } else if (lastReceived >= suspectTime) { // SUSPECT
            currentState = CommunicationState.OK;
            suspectTime = -1;
        } else if (now - suspectTime >= heartBeatPeriod) {
            log.info("Nothing received on UIContext #{} for {} ms, not even a pong: the connection is dead",
                uiContext.getID(), now - lastReceived);
            currentState = CommunicationState.KO;
            stop();
            uiContext.connectionDead();
            return;
        }

        if (now - lastReceived >= heartBeatPeriod / 2) uiContext.ping();
        if (++checksSinceRoundTrip >= roundTripChecks) {
            checksSinceRoundTrip = 0;
            uiContext.sendRoundTrip();
        }
    }

}
