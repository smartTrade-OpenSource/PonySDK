
package com.ponysdk.core.concurrent;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.UIContextListener;
import com.ponysdk.core.stm.Txn;

public class PScheduler implements UIContextListener {

    private static Logger log = LoggerFactory.getLogger(PScheduler.class);

    private static PScheduler INSTANCE;

    static {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {

            private int i = 0;

            @Override
            public Thread newThread(final Runnable r) {
                final Thread t = new Thread(r);
                t.setName(PScheduler.class.getName() + "-" + i++);
                t.setDaemon(true);
                return t;
            }
        });
        INSTANCE = new PScheduler(executor);
    }

    protected final ScheduledThreadPoolExecutor executor;
    protected Map<UIContext, Set<UIRunnable>> runnablesByUIContexts = new ConcurrentHashMap<>();

    private PScheduler(final ScheduledThreadPoolExecutor executor) {
        log.info("Initializing PScheduler");
        this.executor = executor;
    }

    public static UIRunnable schedule(final Runnable runnable, final Duration duration) {
        return INSTANCE.schedule0(runnable, duration);
    }

    public UIRunnable schedule0(final Runnable runnable, final Duration duration) {
        final UIRunnable uiRunnable = new UIRunnable(runnable, false);
        uiRunnable.setFuture(executor.schedule(uiRunnable, duration.toMillis(), TimeUnit.MILLISECONDS));
        registerTask(uiRunnable);
        return uiRunnable;
    }

    public static UIRunnable scheduleAtFixedRate(final Runnable runnable, final Duration period) {
        return scheduleAtFixedRate(runnable, Duration.ZERO, period);
    }

    public static UIRunnable scheduleAtFixedRate(final Runnable runnable, final Duration delay, final Duration period) {
        return INSTANCE.scheduleAtFixedRate0(runnable, delay, period);
    }

    public UIRunnable scheduleAtFixedRate0(final Runnable runnable, final Duration delay, final Duration period) {
        final UIRunnable uiRunnable = new UIRunnable(runnable, true);
        uiRunnable.setFuture(executor.scheduleAtFixedRate(uiRunnable, delay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS));
        registerTask(uiRunnable);
        return uiRunnable;
    }

    public static UIRunnable scheduleWithFixedDelay(final Runnable runnable, final Duration delay, final Duration period) {
        return INSTANCE.scheduleWithFixedDelay0(runnable, delay.toMillis(), delay.toMillis());
    }

    public UIRunnable scheduleWithFixedDelay0(final Runnable runnable, final long delayMillis, final long periodMillis) {
        final UIRunnable uiRunnable = new UIRunnable(runnable, true);
        final ScheduledFuture<?> future = executor.scheduleWithFixedDelay(uiRunnable, delayMillis, periodMillis, TimeUnit.MILLISECONDS);
        uiRunnable.setFuture(future);
        registerTask(uiRunnable);

        return uiRunnable;
    }

    public class UIRunnable implements Runnable {

        private final Runnable runnable;
        private final UIContext uiContext;

        private boolean cancelled;
        private ScheduledFuture<?> future;
        private final boolean repeated;

        public UIRunnable(final Runnable runnable, final boolean repeated) {
            this.uiContext = UIContext.get();
            this.runnable = runnable;
            this.repeated = repeated;
        }

        @Override
        public void run() {
            try {
                if (cancelled)
                    return;
                if (!execute())
                    cancel();
            } catch (final Throwable throwable) {
                log.error("Error occurred", throwable);
                cancel();
            } finally {
                if (!repeated) {
                    purge();
                }
            }
        }

        public void begin() {
            uiContext.begin();
        }

        public void end() {
            uiContext.end();
        }

        public boolean execute() {
            try {
                begin();
                try {
                    final Txn txn = Txn.get();
                    txn.begin(uiContext.getContext());
                    try {
                        runnable.run();
                        txn.commit();
                    } catch (final Throwable e) {
                        log.error("Cannot process commmand", e);
                        txn.rollback();
                        return false;
                    }
                } finally {
                    end();
                }
            } catch (final Throwable e) {
                log.error("Cannot execute command : " + runnable, e);
                return false;
            }
            return true;
        }

        public void cancel() {
            this.cancelled = true;
            this.future.cancel(false);
            purge();
        }

        public void purge() {
            executor.purge();
            runnablesByUIContexts.get(uiContext).remove(this);
        }

        public void setFuture(final ScheduledFuture<?> future) {
            this.future = future;
        }

        public UIContext getUiContext() {
            return uiContext;
        }

    }

    protected void registerTask(final UIRunnable runnable) {
        final UIContext uiContext = runnable.getUiContext();
        uiContext.addUIContextListener(this);
        Set<UIRunnable> runnables = runnablesByUIContexts.get(uiContext);
        if (runnables == null) {
            runnables = new CopyOnWriteArraySet<>();
            runnablesByUIContexts.put(uiContext, runnables);
        }
        runnables.add(runnable);
    }

    @Override
    public void onUIContextDestroyed(final UIContext uiContext) {
        final Set<UIRunnable> runnables = runnablesByUIContexts.get(uiContext);
        if (runnables != null) {
            for (final UIRunnable runnable : runnables) {
                runnable.cancel();
            }
            runnablesByUIContexts.remove(uiContext);
        }
    }

}
