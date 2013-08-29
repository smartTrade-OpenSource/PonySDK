
package com.ponysdk.core.concurrent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.socket.ConnectionListener;
import com.ponysdk.ui.server.basic.PCommand;

public class UIScheduledThreadPoolExecutor implements UIScheduledExecutorService, ConnectionListener {

    private static Logger log = LoggerFactory.getLogger(UIScheduledThreadPoolExecutor.class);

    private static UIScheduledThreadPoolExecutor INSTANCE;

    protected final ScheduledExecutorService executor;
    protected Map<UIContext, Set<UIRunnable>> runnablesBySession = new ConcurrentHashMap<UIContext, Set<UIRunnable>>();

    private UIScheduledThreadPoolExecutor(final ScheduledExecutorService executor) {
        log.info("Initializing UIScheduledThreadPoolExecutor");
        this.executor = executor;
    }

    public static UIScheduledThreadPoolExecutor initDefault() {
        if (INSTANCE != null) throw new IllegalAccessError("Already initialized");
        INSTANCE = new UIScheduledThreadPoolExecutor(Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors()));
        return INSTANCE;
    }

    public static UIScheduledThreadPoolExecutor init(final ScheduledExecutorService executor) {
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
        final ScheduledFuture<?> future = executor.scheduleAtFixedRate(new UIRunnable(command), initialDelay, period, unit);
        runnable.setFuture(future);

        registerTask(runnable);

        return future;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
        checkUIState();

        final UIRunnable runnable = new UIRunnable(command);
        final ScheduledFuture<?> future = executor.scheduleWithFixedDelay(new UIRunnable(command), initialDelay, delay, unit);
        runnable.setFuture(future);

        registerTask(runnable);

        return future;
    }

    static protected class UIRunnable implements Runnable {

        private final Runnable runnable;
        private final UIContext uiContext;

        private boolean cancelled;
        private ScheduledFuture<?> future;

        public UIRunnable(final Runnable runnable) {
            this.uiContext = UIContext.get();
            this.runnable = runnable;
        }

        @Override
        public void run() {

            if (cancelled) return;

            uiContext.getPusher().execute(new PCommand() {

                @Override
                public void execute() {
                    runnable.run();
                }
            });
        }

        public void setCancelled(final boolean cancelled) {
            this.cancelled = cancelled;
            this.future.cancel(false);
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
        uiContext.getPusher().addConnectionListener(this);
        final String sessionID = uiContext.getSession().getId();
        Set<UIRunnable> runnables = runnablesBySession.get(sessionID);
        if (runnables == null) {
            runnables = new HashSet<UIScheduledThreadPoolExecutor.UIRunnable>();
            runnablesBySession.put(uiContext, runnables);
        }
        runnables.add(runnable);
    }

    @Override
    public void onOpen() {}

    @Override
    public void onClose() {
        final Set<UIRunnable> runnables = runnablesBySession.remove(UIContext.get());
        if (runnables != null) {
            for (final UIRunnable runnable : runnables) {
                runnable.setCancelled(true);
            }
        }
    }

}
