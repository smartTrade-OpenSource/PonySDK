
package com.ponysdk.core.servlet;

import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;

public class CommunicationSanityChecker {

    protected static final Logger log = LoggerFactory.getLogger(CommunicationSanityChecker.class);

    private static final int CHECK_PERIOD = 1000;
    public static final String THREAD_COUNT_SYSTEM_PROPERTY = "communication.sanity.checker.thread.count";
    private static final int MAX_THREAD_CHECKER = Integer.parseInt(System.getProperty(THREAD_COUNT_SYSTEM_PROPERTY, "" + Runtime.getRuntime().availableProcessors()));

    protected static final ScheduledThreadPoolExecutor sanityCheckerTimer = new ScheduledThreadPoolExecutor(MAX_THREAD_CHECKER, new ThreadFactory() {

        private int i = 0;

        @Override
        public Thread newThread(final Runnable r) {
            final Thread t = new Thread(r);
            t.setName(CommunicationSanityChecker.class.getName() + "-" + i++);
            t.setDaemon(true);
            return t;
        }
    });

    protected final UIContext uiContext;
    protected long heartBeatPeriod; // In MilliSeconds
    protected long lastReceivedTime;
    protected final AtomicBoolean started = new AtomicBoolean(false);
    private RunnableScheduledFuture<?> sanityChecker;

    protected enum CommunicationState {
        OK, SUSPECT, KO
    }

    protected CommunicationState currentState;
    protected long suspectTime = -1;

    public CommunicationSanityChecker(final UIContext uiContext) {
        this.uiContext = uiContext;
        setHeartBeatPeriod(uiContext.getApplication().getOptions().getHeartBeatPeriod());
    }

    private boolean isStarted() {
        return started.get();
    }

    private boolean isSanityCheckEnabled() {
        return heartBeatPeriod > 0;
    }

    protected void logInfo(final String message) {
        log.info("[" + uiContext + "] " + message);
    }

    protected void logError(final String message, final Throwable t) {
        log.error("[" + uiContext + "] " + message, t);
    }

    public void start() {
        final long now = System.currentTimeMillis();
        lastReceivedTime = now;
        if (isSanityCheckEnabled() && !isStarted()) {
            currentState = CommunicationState.OK;
            sanityChecker = (RunnableScheduledFuture<?>) sanityCheckerTimer.scheduleAtFixedRate(new SanityChecker(), 0, CHECK_PERIOD, TimeUnit.MILLISECONDS);
            started.set(true);
            logInfo("Started. HeartbeatPeriod: " + heartBeatPeriod + " ms.");
        }
    }

    public void stop() {
        if (isSanityCheckEnabled() && isStarted()) {
            if (sanityChecker != null) {
                sanityChecker.cancel(true);
                sanityCheckerTimer.remove(sanityChecker);
                sanityChecker = null;
            }
            started.set(false);
            logInfo("Stopped.");
        }
    }

    public void onMessageReceived() {
        lastReceivedTime = System.currentTimeMillis();
    }

    public void setHeartBeatPeriod(final long hearBeatInt) {
        heartBeatPeriod = TimeUnit.SECONDS.toMillis(hearBeatInt);
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
                    // sanityCheckerTimer.schedule(new SendHeartBeatTask(), 0, TimeUnit.MILLISECONDS);
                }
                break;
            case SUSPECT:
                if (lastReceivedTime < suspectTime) {
                    if ((now - suspectTime) >= heartBeatPeriod) {
                        // No message have been received since we suspected the communication to be non
                        // functional
                        currentState = CommunicationState.KO;
                        stop();
                        uiContext.destroy();
                    }
                } else {
                    currentState = CommunicationState.OK;
                    suspectTime = -1;
                }
                break;
            default:
                break;
        }
    }

    public class SanityChecker implements Runnable {

        @Override
        public void run() {
            try {
                checkCommunicationState();
            } catch (final Throwable e) {
                logError("Error while checking communication state", e);
            }
        }
    }

}