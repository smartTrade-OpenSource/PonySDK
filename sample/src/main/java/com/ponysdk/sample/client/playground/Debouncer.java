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

package com.ponysdk.sample.client.playground;

import com.ponysdk.core.server.concurrent.PScheduler;

import java.time.Duration;

/**
 * Utility class for debouncing rapid events.
 * <p>
 * Delays execution of actions until input stabilizes, canceling pending
 * actions when new input arrives.
 * </p>
 * <p>
 * Requirements: 2.2, 8.3, 8.4
 * </p>
 */
public class Debouncer {

    private final long delayMs;
    private PScheduler.UIRunnable pendingAction;

    /**
     * Creates a new Debouncer with the specified delay.
     *
     * @param delayMs the delay in milliseconds before executing actions
     * @throws IllegalArgumentException if delayMs is negative
     */
    public Debouncer(final long delayMs) {
        if (delayMs < 0) {
            throw new IllegalArgumentException("delayMs must not be negative");
        }
        this.delayMs = delayMs;
    }

    /**
     * Debounces an action by delaying its execution.
     * <p>
     * If this method is called again before the delay expires, the previous
     * pending action is canceled and the new action is scheduled.
     * </p>
     *
     * @param action the action to execute after the delay, must not be null
     * @throws IllegalArgumentException if action is null
     */
    public void debounce(final Runnable action) {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }

        // Cancel any pending action
        cancel();

        // Schedule the new action
        pendingAction = PScheduler.schedule(action, Duration.ofMillis(delayMs));
    }

    /**
     * Cancels any pending action.
     * <p>
     * If no action is pending, this method has no effect.
     * </p>
     */
    public void cancel() {
        if (pendingAction != null) {
            pendingAction.cancel();
            pendingAction = null;
        }
    }
}
