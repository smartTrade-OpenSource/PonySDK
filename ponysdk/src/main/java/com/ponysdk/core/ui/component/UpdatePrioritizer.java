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

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prioritizes component updates based on their {@link UpdatePriority}.
 * <p>
 * UpdatePrioritizer manages a priority queue of pending updates, ensuring that
 * HIGH priority updates are processed before NORMAL, and NORMAL before LOW.
 * This allows critical UI updates to be delivered with minimal latency while
 * less important updates can be deferred.
 * </p>
 *
 * <h2>Priority Ordering</h2>
 * <p>
 * Updates are processed in the following order:
 * <ol>
 *   <li>{@link UpdatePriority#HIGH} - Critical updates processed first</li>
 *   <li>{@link UpdatePriority#NORMAL} - Default priority, processed second</li>
 *   <li>{@link UpdatePriority#LOW} - Non-critical updates processed last</li>
 * </ol>
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is thread-safe. Multiple threads can safely call {@link #enqueue}
 * concurrently. The {@link #flush} method processes all queued updates and
 * should typically be called from a single thread (e.g., the UI thread).
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * UpdatePrioritizer prioritizer = new UpdatePrioritizer();
 *
 * // Enqueue updates with different priorities
 * prioritizer.enqueue(lowPriorityComponent, () -> sendLowUpdate());
 * prioritizer.enqueue(highPriorityComponent, () -> sendHighUpdate());
 * prioritizer.enqueue(normalPriorityComponent, () -> sendNormalUpdate());
 *
 * // Flush processes: HIGH first, then NORMAL, then LOW
 * prioritizer.flush();
 * }</pre>
 *
 * @see UpdatePriority
 * @see PComponent
 */
public class UpdatePrioritizer {

    private static final Logger log = LoggerFactory.getLogger(UpdatePrioritizer.class);

    /**
     * Default initial capacity for the priority queue.
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 100;

    /**
     * Comparator that orders updates by priority (lower order value = higher priority).
     */
    private static final Comparator<PendingUpdate> PRIORITY_COMPARATOR =
            Comparator.comparingInt(u -> u.getPriority().getOrder());

    /**
     * Priority queue for pending updates.
     */
    private final PriorityBlockingQueue<PendingUpdate> queue;

    /**
     * Flag indicating whether a flush is currently in progress.
     */
    private final AtomicBoolean flushing = new AtomicBoolean(false);

    /**
     * Creates a new UpdatePrioritizer with the default initial capacity.
     */
    public UpdatePrioritizer() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Creates a new UpdatePrioritizer with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the priority queue
     * @throws IllegalArgumentException if initialCapacity is less than 1
     */
    public UpdatePrioritizer(final int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("Initial capacity must be at least 1");
        }
        this.queue = new PriorityBlockingQueue<>(initialCapacity, PRIORITY_COMPARATOR);
    }

    /**
     * Enqueues an update for the specified component.
     * <p>
     * The update will be processed according to the component's current
     * {@link UpdatePriority} when {@link #flush()} is called.
     * </p>
     *
     * @param component the component whose update is being queued
     * @param update    the update action to execute
     * @throws NullPointerException if component or update is null
     */
    public void enqueue(final PComponent<?> component, final Runnable update) {
        Objects.requireNonNull(component, "Component must not be null");
        Objects.requireNonNull(update, "Update action must not be null");

        final UpdatePriority priority = component.getPriority();
        final PendingUpdate pendingUpdate = new PendingUpdate(priority, update);

        queue.offer(pendingUpdate);

        log.debug("Enqueued {} priority update for component {}", priority, component.getID());
    }

    /**
     * Enqueues an update with the specified priority.
     * <p>
     * This method allows enqueuing updates without a component reference,
     * useful for system-level updates or testing.
     * </p>
     *
     * @param priority the priority for the update
     * @param update   the update action to execute
     * @throws NullPointerException if priority or update is null
     */
    public void enqueue(final UpdatePriority priority, final Runnable update) {
        Objects.requireNonNull(priority, "Priority must not be null");
        Objects.requireNonNull(update, "Update action must not be null");

        final PendingUpdate pendingUpdate = new PendingUpdate(priority, update);
        queue.offer(pendingUpdate);

        log.debug("Enqueued {} priority update", priority);
    }

    /**
     * Flushes all pending updates in priority order.
     * <p>
     * Updates are processed in order of priority: all HIGH priority updates
     * are executed before any NORMAL updates, and all NORMAL updates are
     * executed before any LOW updates (Requirement 5.2).
     * </p>
     * <p>
     * If an update throws an exception, it is logged and processing continues
     * with the next update.
     * </p>
     * <p>
     * This method is reentrant-safe: if called while a flush is already in
     * progress, it returns immediately without processing.
     * </p>
     *
     * @return the number of updates that were processed
     */
    public int flush() {
        // Prevent reentrant flushing
        if (!flushing.compareAndSet(false, true)) {
            log.debug("Flush already in progress, skipping");
            return 0;
        }

        int processedCount = 0;
        try {
            PendingUpdate update;
            while ((update = queue.poll()) != null) {
                try {
                    update.execute();
                    processedCount++;
                } catch (final Exception e) {
                    log.error("Error executing {} priority update", update.getPriority(), e);
                    // Continue processing remaining updates
                }
            }

            if (processedCount > 0) {
                log.debug("Flushed {} updates", processedCount);
            }
        } finally {
            flushing.set(false);
        }

        return processedCount;
    }

    /**
     * Returns the number of pending updates in the queue.
     *
     * @return the number of pending updates
     */
    public int size() {
        return queue.size();
    }

    /**
     * Returns whether the queue is empty.
     *
     * @return {@code true} if no updates are pending, {@code false} otherwise
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Clears all pending updates from the queue.
     * <p>
     * This method should be used with caution as it discards all pending
     * updates without executing them.
     * </p>
     */
    public void clear() {
        final int cleared = queue.size();
        queue.clear();
        if (cleared > 0) {
            log.debug("Cleared {} pending updates", cleared);
        }
    }

    /**
     * Returns whether a flush is currently in progress.
     *
     * @return {@code true} if flushing, {@code false} otherwise
     */
    public boolean isFlushing() {
        return flushing.get();
    }

    /**
     * Represents a pending update with its associated priority.
     * <p>
     * This class encapsulates an update action along with its priority level,
     * allowing the {@link UpdatePrioritizer} to order updates correctly.
     * </p>
     */
    public static class PendingUpdate {

        /**
         * The priority of this update.
         */
        private final UpdatePriority priority;

        /**
         * The update action to execute.
         */
        private final Runnable update;

        /**
         * Flag indicating whether this update has been executed.
         */
        private volatile boolean executed = false;

        /**
         * Creates a new PendingUpdate with the specified priority and action.
         *
         * @param priority the priority level for this update
         * @param update   the update action to execute
         * @throws NullPointerException if priority or update is null
         */
        public PendingUpdate(final UpdatePriority priority, final Runnable update) {
            this.priority = Objects.requireNonNull(priority, "Priority must not be null");
            this.update = Objects.requireNonNull(update, "Update action must not be null");
        }

        /**
         * Returns the priority of this update.
         *
         * @return the update priority
         */
        public UpdatePriority getPriority() {
            return priority;
        }

        /**
         * Returns the update action.
         *
         * @return the update action
         */
        public Runnable getUpdate() {
            return update;
        }

        /**
         * Executes the update action.
         * <p>
         * This method is idempotent - calling it multiple times has no effect
         * after the first execution.
         * </p>
         */
        public void execute() {
            if (!executed) {
                executed = true;
                update.run();
            }
        }

        /**
         * Returns whether this update has been executed.
         *
         * @return {@code true} if executed, {@code false} otherwise
         */
        public boolean isExecuted() {
            return executed;
        }

        @Override
        public String toString() {
            return "PendingUpdate[priority=" + priority + ", executed=" + executed + "]";
        }
    }

}
