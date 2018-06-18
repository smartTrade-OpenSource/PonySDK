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

package com.ponysdk.core.server.concurrent;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;

public class PScheduler {

    private static final Logger log = LoggerFactory.getLogger(PScheduler.class);

    private static final PScheduler INSTANCE;

    static {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
            new ThreadFactory() {

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

    private final ScheduledThreadPoolExecutor executor;
    private final Map<UIContext, Set<UIRunnable>> runnablesByUIContexts = new ConcurrentHashMap<>();

    private PScheduler(final ScheduledThreadPoolExecutor executor) {
        this.executor = executor;
    }

    public static UIRunnable schedule(final Runnable runnable) {
        return schedule(UIContext.get(), runnable, Duration.ZERO);
    }

    public static UIRunnable schedule(final UIContext context, final Runnable runnable) {
        return schedule(context, runnable, Duration.ZERO);
    }

    public static UIRunnable schedule(final Runnable runnable, final Duration duration) {
        return INSTANCE.schedule0(UIContext.get(), runnable, duration);
    }

    public static UIRunnable schedule(final UIContext context, final Runnable runnable, final Duration duration) {
        return INSTANCE.schedule0(context, runnable, duration);
    }

    public static UIRunnable scheduleAtFixedRate(final Runnable runnable, final Duration period) {
        return scheduleAtFixedRate(UIContext.get(), runnable, period);
    }

    public static UIRunnable scheduleAtFixedRate(final UIContext context, final Runnable runnable, final Duration period) {
        return scheduleAtFixedRate(context, runnable, Duration.ZERO, period);
    }

    public static UIRunnable scheduleAtFixedRate(final Runnable runnable, final Duration delay, final Duration period) {
        return scheduleAtFixedRate(UIContext.get(), runnable, delay, period);
    }

    public static UIRunnable scheduleAtFixedRate(final UIContext context, final Runnable runnable, final Duration delay,
                                                 final Duration period) {
        return INSTANCE.scheduleAtFixedRate0(context, runnable, delay, period);
    }

    public static UIRunnable scheduleWithFixedDelay(final Runnable runnable, final Duration delay, final Duration period) {
        return INSTANCE.scheduleWithFixedDelay0(UIContext.get(), runnable, delay.toMillis(), period.toMillis());
    }

    public static UIRunnable scheduleWithFixedDelay(final UIContext context, final Runnable runnable, final Duration delay,
                                                    final Duration period) {
        return INSTANCE.scheduleWithFixedDelay0(context, runnable, delay.toMillis(), period.toMillis());
    }

    private UIRunnable schedule0(final UIContext context, final Runnable runnable, final Duration duration) {
        final UIRunnable uiRunnable = new UIRunnable(context, this, runnable, false);
        uiRunnable.setFuture(executor.schedule(uiRunnable, duration.toMillis(), TimeUnit.MILLISECONDS));
        registerTask(uiRunnable);
        return uiRunnable;
    }

    private UIRunnable scheduleAtFixedRate0(final UIContext context, final Runnable runnable, final Duration delay,
                                            final Duration period) {
        final UIRunnable uiRunnable = new UIRunnable(context, this, runnable, true);
        uiRunnable.setFuture(executor.scheduleAtFixedRate(uiRunnable, delay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS));
        registerTask(uiRunnable);
        return uiRunnable;
    }

    private UIRunnable scheduleWithFixedDelay0(final UIContext context, final Runnable runnable, final long delayMillis,
                                               final long periodMillis) {
        final UIRunnable uiRunnable = new UIRunnable(context, this, runnable, true);
        final ScheduledFuture<?> future = executor.scheduleWithFixedDelay(uiRunnable, delayMillis, periodMillis,
            TimeUnit.MILLISECONDS);
        uiRunnable.setFuture(future);
        registerTask(uiRunnable);

        return uiRunnable;
    }

    public static <R> Consumer<R> delegate(final Consumer<R> consumer) {
        return delegate(consumer, UIContext.get());
    }

    public static <R> Consumer<R> delegate(final Consumer<R> callable, final UIContext uiContext) {
        return INSTANCE.delegate0(callable, uiContext);
    }

    private <R> Consumer<R> delegate0(final Consumer<R> callback, final UIContext uiContext) {
        return new UIDelegator<>(callback, uiContext);
    }

    private void purge(final UIRunnable uiRunnable) {
        executor.purge();
        final Set<UIRunnable> set = runnablesByUIContexts.get(uiRunnable.getUiContext());
        if (set != null) {
            set.remove(uiRunnable);
        }
    }

    private void registerTask(final UIRunnable runnable) {
        final UIContext uiContext = runnable.getUiContext();
        Set<UIRunnable> runnables = runnablesByUIContexts.get(uiContext);
        if (runnables == null) {
            runnables = Collections.newSetFromMap(new ConcurrentHashMap<>());
            runnables.add(runnable);
            runnablesByUIContexts.put(uiContext, runnables);
            uiContext.addContextDestroyListener(context -> runnablesByUIContexts.remove(context).forEach(UIRunnable::cancel));
        } else {
            runnables.add(runnable);
        }
    }

    public static final class UIRunnable implements Runnable {

        private final Runnable runnable;
        private final UIContext uiContext;
        private final boolean repeated;
        private final PScheduler scheduler;
        private boolean cancelled;
        private ScheduledFuture<?> future;

        UIRunnable(final UIContext context, final PScheduler scheduler, final Runnable runnable, final boolean repeated) {
            this.uiContext = context;
            this.runnable = runnable;
            this.repeated = repeated;
            this.scheduler = scheduler;
        }

        @Override
        public void run() {
            try {
                if (cancelled) return;
                if (!execute()) cancel();
            } catch (final Throwable throwable) {
                log.error("Error occurred", throwable);
                cancel();
            } finally {
                if (!repeated) {
                    scheduler.purge(this);
                }
            }
        }

        public boolean execute() {
            try {
                uiContext.begin();
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
                    uiContext.end();
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
            scheduler.purge(this);
        }

        void setFuture(final ScheduledFuture<?> future) {
            this.future = future;
        }

        public UIContext getUiContext() {
            return uiContext;
        }
    }

}
