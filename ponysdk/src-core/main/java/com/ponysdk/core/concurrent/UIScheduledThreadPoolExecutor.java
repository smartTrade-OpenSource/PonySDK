
package com.ponysdk.core.concurrent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.UIContextListener;
import com.ponysdk.ui.server.basic.PPusher.PusherState;

public class UIScheduledThreadPoolExecutor implements UIScheduledExecutorService, UIContextListener {

    private static Logger log = LoggerFactory.getLogger(UIScheduledThreadPoolExecutor.class);

    private static UIScheduledThreadPoolExecutor INSTANCE;

    protected final ScheduledThreadPoolExecutor executor;
    protected Map<UIContext, Set<UIRunnable>> runnablesByUIContexts = new ConcurrentHashMap<>();

    private UIScheduledThreadPoolExecutor(final ScheduledThreadPoolExecutor executor) {
        log.info("Initializing UIScheduledThreadPoolExecutor");
        this.executor = executor;
    }

    public static UIScheduledThreadPoolExecutor initDefault() {
        if (INSTANCE != null) throw new IllegalAccessError("Already initialized");
        INSTANCE = new UIScheduledThreadPoolExecutor(new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors()));
        return INSTANCE;
    }

    public static UIScheduledThreadPoolExecutor init(final ScheduledThreadPoolExecutor executor) {
        if (INSTANCE != null) throw new IllegalAccessError("Already initialized");
        INSTANCE = new UIScheduledThreadPoolExecutor(executor);
        return INSTANCE;
    }

    public static UIScheduledThreadPoolExecutor get() {
        return INSTANCE;
    }

    private void checkUIState() {
        if (UIContext.get() == null) throw new IllegalAccessError("UIScheduledThreadPoolExecutor must be called from UI client code");
        if (UIContext.get().getPusher() == null) throw new IllegalAccessError("PPusher not initialized");
    }

    @Override
    public UIRunnable schedule(final Runnable runnable, final long delay, final TimeUnit unit) {
        checkUIState();
        final UIRunnable uiRunnable = new UIRunnable(runnable, false);
        final ScheduledFuture<?> future = executor.schedule(uiRunnable, delay, unit);
        uiRunnable.setFuture(future);

        registerTask(uiRunnable);

        return uiRunnable;
    }

    @Override
    public UIRunnable scheduleAtFixedRate(final Runnable runnable, final long initialDelay, final long period, final TimeUnit unit) {
        checkUIState();
        final UIRunnable uiRunnable = new UIRunnable(runnable, true);
        final ScheduledFuture<?> future = executor.scheduleAtFixedRate(uiRunnable, initialDelay, period, unit);
        uiRunnable.setFuture(future);
        registerTask(uiRunnable);

        return uiRunnable;
    }

    @Override
    public UIRunnable scheduleWithFixedDelay(final Runnable runnable, final long initialDelay, final long delay, final TimeUnit unit) {
        checkUIState();

        final UIRunnable uiRunnable = new UIRunnable(runnable, true);
        final ScheduledFuture<?> future = executor.scheduleWithFixedDelay(uiRunnable, initialDelay, delay, unit);
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
                if (cancelled) return;
                if (uiContext.getPusher() == null) return;
                if (uiContext.getPusher().getPusherState() != PusherState.STOPPED) {
                    if (!uiContext.getPusher().execute(runnable)) cancel();
                }
            } catch (final Throwable throwable) {
                log.error("Error occurred", throwable);
                cancel();
            } finally {
                if (!repeated) {
                    purge();
                }
            }
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
