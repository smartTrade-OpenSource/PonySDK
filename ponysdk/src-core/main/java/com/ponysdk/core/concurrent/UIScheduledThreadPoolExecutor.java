
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
import com.ponysdk.ui.server.basic.PWindow;

public class UIScheduledThreadPoolExecutor implements UIScheduledExecutorService, UIContextListener {

    private static Logger log = LoggerFactory.getLogger(UIScheduledThreadPoolExecutor.class);

    private static UIScheduledThreadPoolExecutor INSTANCE;

    protected final ScheduledThreadPoolExecutor executor;
    protected Map<UIContext, Set<UIRunnable>> runnablesByUIContexts = new ConcurrentHashMap<UIContext, Set<UIRunnable>>();

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
    public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
        checkUIState();
        final UIRunnable runnable = new UIRunnable(command);
        final ScheduledFuture<?> future = executor.schedule(runnable, delay, unit);
        runnable.setFuture(future);
        registerTask(runnable);

        return future;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period, final TimeUnit unit) {
        checkUIState();
        final UIRunnable runnable = new UIRunnable(command);
        final ScheduledFuture<?> future = executor.scheduleAtFixedRate(runnable, initialDelay, period, unit);
        runnable.setFuture(future);
        registerTask(runnable);

        return future;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
        checkUIState();

        final UIRunnable runnable = new UIRunnable(command);
        final ScheduledFuture<?> future = executor.scheduleWithFixedDelay(runnable, initialDelay, delay, unit);
        runnable.setFuture(future);
        registerTask(runnable);

        return future;
    }

    static class WindowUIRunnable implements Runnable {

        private final Runnable runnable;
        private final PWindow window;

        public WindowUIRunnable(final PWindow window, final Runnable runnable) {
            this.runnable = runnable;
            this.window = window;
        }

        @Override
        public void run() {
            window.acquire();
            try {
                runnable.run();
                window.flush();
            } catch (final Exception e) {
                log.error("Cannot run UIRunnable", e);
            } finally {
                window.release();
            }
        }
    }

    protected class UIRunnable implements Runnable {

        private final Runnable runnable;
        private final UIContext uiContext;

        private boolean cancelled;
        private ScheduledFuture<?> future;
        private final PWindow window;

        public UIRunnable(final Runnable runnable) {
            this.uiContext = UIContext.get();

            this.window = UIContext.getCurrentWindow();
            if (window != null) {
                this.runnable = new WindowUIRunnable(window, runnable);
            } else {
                this.runnable = runnable;
            }
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
            }
        }

        public void cancel() {
            this.cancelled = true;
            this.future.cancel(true);
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
            runnables = new CopyOnWriteArraySet<UIRunnable>();
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
