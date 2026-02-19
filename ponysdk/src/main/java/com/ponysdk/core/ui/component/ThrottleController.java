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

package com.ponysdk.core.ui.component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for throttling component updates.
 * <p>
 * ThrottleController manages scheduled update batching for {@link PComponent} instances.
 * When updates occur faster than the configured throttle interval, only the latest
 * state is sent to the client, reducing network traffic and improving performance.
 * </p>
 * <p>
 * Key behaviors:
 * <ul>
 *   <li>If throttling is disabled for a component, updates are executed immediately</li>
 *   <li>If an update is already scheduled, subsequent updates replace the pending action
 *       with the latest one (preserving the most recent props state)</li>
 *   <li>After the throttle interval elapses, the latest update action is executed</li>
 *   <li>Each component is tracked independently by its object ID</li>
 * </ul>
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is thread-safe. Multiple threads can safely call {@link #scheduleUpdate}
 * concurrently. The internal state is managed using concurrent data structures and
 * atomic operations.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ThrottleController controller = new ThrottleController();
 *
 * // Schedule an update for a component
 * controller.scheduleUpdate(myComponent, () -> {
 *     // Send props update to client
 *     myComponent.sendPropsUpdate();
 * });
 *
 * // Shutdown when done
 * controller.shutdown();
 * }</pre>
 *
 * @see ThrottleConfig
 * @see PComponent
 */
public class ThrottleController {

    private static final Logger log = LoggerFactory.getLogger(ThrottleController.class);

    /**
     * Map of pending updates by component object ID.
     */
    private final Map<Integer, ScheduledUpdate> pendingUpdates = new ConcurrentHashMap<>();

    /**
     * Scheduler for executing delayed updates.
     */
    private final ScheduledExecutorService scheduler;

    /**
     * Flag indicating whether the controller has been shut down.
     */
    private volatile boolean shutdown = false;

    /**
     * Creates a new ThrottleController with a default single-threaded scheduler.
     */
    public ThrottleController() {
        this(createDefaultScheduler());
    }

    /**
     * Creates a new ThrottleController with the specified scheduler.
     * <p>
     * This constructor is primarily for testing purposes, allowing injection
     * of a custom scheduler.
     * </p>
     *
     * @param scheduler the scheduler to use for delayed execution
     * @throws NullPointerException if scheduler is null
     */
    public ThrottleController(final ScheduledExecutorService scheduler) {
        Objects.requireNonNull(scheduler, "Scheduler must not be null");
        this.scheduler = scheduler;
    }

    /**
     * Creates the default scheduler for throttle execution.
     *
     * @return a single-threaded scheduled executor
     */
    private static ScheduledExecutorService createDefaultScheduler() {
        final ThreadFactory threadFactory = runnable -> {
            final Thread thread = new Thread(runnable, "PComponent-ThrottleController");
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    /**
     * Schedules an update for the specified component.
     * <p>
     * If throttling is disabled for the component, the update action is executed
     * immediately. Otherwise, the update is scheduled according to the component's
     * throttle configuration.
     * </p>
     * <p>
     * If an update is already pending for the component, the new update action
     * replaces the existing one. This ensures that only the latest props state
     * is sent when the throttle interval elapses (Requirement 4.4).
     * </p>
     * <p>
     * When multiple updates occur within the throttle window, only one update
     * is sent containing the final state (Requirement 4.2).
     * </p>
     *
     * @param component    the component to update
     * @param updateAction the action to execute for the update
     * @throws NullPointerException if component or updateAction is null
     * @throws IllegalStateException if the controller has been shut down
     */
    public void scheduleUpdate(final PComponent<?> component, final Runnable updateAction) {
        Objects.requireNonNull(component, "Component must not be null");
        Objects.requireNonNull(updateAction, "Update action must not be null");

        if (shutdown) {
            throw new IllegalStateException("ThrottleController has been shut down");
        }

        final ThrottleConfig config = component.getThrottleConfig();

        // If throttling is disabled, execute immediately
        if (!config.isEnabled()) {
            log.debug("Throttling disabled for component {}, executing immediately", component.getID());
            executeUpdate(updateAction);
            return;
        }

        final int objectId = component.getID();
        final long intervalMs = config.getInterval();

        // Use compute to atomically handle the pending update
        pendingUpdates.compute(objectId, (id, existing) -> {
            if (existing != null && !existing.isExecuted()) {
                // Update already pending - replace with latest action (Requirement 4.4)
                log.debug("Replacing pending update for component {} with latest action", objectId);
                existing.setUpdateAction(updateAction);
                return existing;
            }

            // No pending update or previous one executed - schedule new update
            log.debug("Scheduling new update for component {} with {}ms delay", objectId, intervalMs);
            final ScheduledUpdate scheduled = new ScheduledUpdate(updateAction);

            // Schedule the execution
            final ScheduledFuture<?> future = scheduler.schedule(() -> {
                executeScheduledUpdate(objectId, scheduled);
            }, intervalMs, TimeUnit.MILLISECONDS);

            scheduled.setFuture(future);
            return scheduled;
        });
    }

    /**
     * Executes a scheduled update and removes it from the pending map.
     *
     * @param objectId  the component object ID
     * @param scheduled the scheduled update to execute
     */
    private void executeScheduledUpdate(final int objectId, final ScheduledUpdate scheduled) {
        try {
            scheduled.execute();
        } finally {
            // Remove from pending updates after execution
            pendingUpdates.remove(objectId, scheduled);
        }
    }

    /**
     * Executes an update action safely.
     *
     * @param updateAction the action to execute
     */
    private void executeUpdate(final Runnable updateAction) {
        try {
            updateAction.run();
        } catch (final Exception e) {
            log.error("Error executing update action", e);
        }
    }

    /**
     * Cancels any pending update for the specified component.
     * <p>
     * This method should be called when a component is destroyed to prevent
     * updates from being sent to a non-existent component.
     * </p>
     *
     * @param objectId the component object ID
     */
    public void cancelPendingUpdate(final int objectId) {
        final ScheduledUpdate removed = pendingUpdates.remove(objectId);
        if (removed != null) {
            removed.cancel();
            log.debug("Cancelled pending update for component {}", objectId);
        }
    }

    /**
     * Returns whether there is a pending update for the specified component.
     *
     * @param objectId the component object ID
     * @return {@code true} if an update is pending, {@code false} otherwise
     */
    public boolean hasPendingUpdate(final int objectId) {
        final ScheduledUpdate update = pendingUpdates.get(objectId);
        return update != null && !update.isExecuted();
    }

    /**
     * Returns the number of pending updates.
     * <p>
     * This method is primarily for testing and monitoring purposes.
     * </p>
     *
     * @return the number of pending updates
     */
    public int getPendingUpdateCount() {
        return (int) pendingUpdates.values().stream()
                .filter(update -> !update.isExecuted())
                .count();
    }

    /**
     * Shuts down the controller and cancels all pending updates.
     * <p>
     * After shutdown, no new updates can be scheduled. This method should be
     * called when the application is shutting down to release resources.
     * </p>
     */
    public void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;

        // Cancel all pending updates
        pendingUpdates.values().forEach(ScheduledUpdate::cancel);
        pendingUpdates.clear();

        // Shutdown the scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (final InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("ThrottleController shut down");
    }

    /**
     * Returns whether the controller has been shut down.
     *
     * @return {@code true} if shut down, {@code false} otherwise
     */
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * Represents a scheduled update with its associated action.
     * <p>
     * This class is thread-safe and allows the update action to be replaced
     * while the update is pending, ensuring that only the latest state is sent.
     * </p>
     */
    public static class ScheduledUpdate {

        /**
         * The update action to execute.
         */
        private final AtomicReference<Runnable> updateAction;

        /**
         * Flag indicating whether the update has been executed.
         */
        private final AtomicBoolean executed = new AtomicBoolean(false);

        /**
         * Flag indicating whether the update has been cancelled.
         */
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        /**
         * The scheduled future for cancellation.
         */
        private volatile ScheduledFuture<?> future;

        /**
         * Creates a new ScheduledUpdate with the specified action.
         *
         * @param updateAction the initial update action
         */
        public ScheduledUpdate(final Runnable updateAction) {
            this.updateAction = new AtomicReference<>(updateAction);
        }

        /**
         * Sets the update action, replacing any previous action.
         * <p>
         * This method is used to update the action when multiple updates
         * occur within the throttle window, ensuring only the latest
         * props state is sent.
         * </p>
         *
         * @param updateAction the new update action
         */
        public void setUpdateAction(final Runnable updateAction) {
            this.updateAction.set(updateAction);
        }

        /**
         * Returns the current update action.
         *
         * @return the update action
         */
        public Runnable getUpdateAction() {
            return updateAction.get();
        }

        /**
         * Sets the scheduled future for this update.
         *
         * @param future the scheduled future
         */
        void setFuture(final ScheduledFuture<?> future) {
            this.future = future;
        }

        /**
         * Executes the update action if not already executed or cancelled.
         * <p>
         * This method is idempotent - calling it multiple times has no effect
         * after the first execution.
         * </p>
         */
        public void execute() {
            if (cancelled.get()) {
                return;
            }
            if (executed.compareAndSet(false, true)) {
                final Runnable action = updateAction.get();
                if (action != null) {
                    try {
                        action.run();
                    } catch (final Exception e) {
                        LoggerFactory.getLogger(ScheduledUpdate.class)
                                .error("Error executing scheduled update", e);
                    }
                }
            }
        }

        /**
         * Cancels the scheduled update.
         * <p>
         * If the update has already been executed, this method has no effect.
         * </p>
         */
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                if (future != null) {
                    future.cancel(false);
                }
            }
        }

        /**
         * Returns whether the update has been executed.
         *
         * @return {@code true} if executed, {@code false} otherwise
         */
        public boolean isExecuted() {
            return executed.get();
        }

        /**
         * Returns whether the update has been cancelled.
         *
         * @return {@code true} if cancelled, {@code false} otherwise
         */
        public boolean isCancelled() {
            return cancelled.get();
        }
    }

}
