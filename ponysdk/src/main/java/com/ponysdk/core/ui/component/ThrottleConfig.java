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

/**
 * Configuration for throttling component updates.
 * <p>
 * Throttling controls the minimum interval between updates sent to the client.
 * When updates occur faster than the throttle interval, only the latest state
 * is sent, reducing network traffic and improving performance.
 * </p>
 * <p>
 * Each {@link PComponent} instance has its own ThrottleConfig, allowing
 * fine-grained control over update frequency based on component needs.
 * </p>
 * <p>
 * By default, throttling is disabled (interval = 0), meaning updates are sent
 * immediately. Set a positive interval to enable throttling, or explicitly
 * disable it for real-time components that require immediate updates.
 * </p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Enable throttling with 100ms interval
 * ThrottleConfig config = new ThrottleConfig();
 * config.setInterval(100);
 *
 * // Disable throttling for real-time updates
 * config.setEnabled(false);
 *
 * // Check if throttling is active
 * if (config.isEnabled()) {
 *     long interval = config.getInterval();
 * }
 * }</pre>
 *
 * @see com.ponysdk.core.ui.component.PComponent
 * @see com.ponysdk.core.ui.component.ThrottleController
 */
public class ThrottleConfig {

    /**
     * The minimum interval between updates in milliseconds.
     * A value of 0 means throttling is disabled.
     */
    private long intervalMs = 0;

    /**
     * Whether throttling is currently enabled.
     * Automatically set to true when a positive interval is configured.
     */
    private boolean enabled = false;

    /**
     * Creates a new ThrottleConfig with throttling disabled.
     */
    public ThrottleConfig() {
    }

    /**
     * Creates a new ThrottleConfig with the specified interval.
     *
     * @param intervalMs the minimum interval between updates in milliseconds;
     *                   a value of 0 or less disables throttling
     */
    public ThrottleConfig(final long intervalMs) {
        setInterval(intervalMs);
    }

    /**
     * Sets the minimum interval between updates.
     * <p>
     * Setting a positive interval automatically enables throttling.
     * Setting 0 or a negative value disables throttling.
     * </p>
     *
     * @param intervalMs the minimum interval between updates in milliseconds
     */
    public void setInterval(final long intervalMs) {
        this.intervalMs = Math.max(0, intervalMs);
        this.enabled = this.intervalMs > 0;
    }

    /**
     * Returns the minimum interval between updates in milliseconds.
     *
     * @return the throttle interval, or 0 if throttling is disabled
     */
    public long getInterval() {
        return intervalMs;
    }

    /**
     * Returns whether throttling is currently enabled.
     * <p>
     * Throttling is enabled when a positive interval is set and
     * {@link #setEnabled(boolean)} has not been called with {@code false}.
     * </p>
     *
     * @return {@code true} if throttling is enabled, {@code false} otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Explicitly enables or disables throttling.
     * <p>
     * This method allows disabling throttling even when a positive interval
     * is configured, which is useful for real-time components that temporarily
     * need immediate updates.
     * </p>
     * <p>
     * Note: Enabling throttling when the interval is 0 has no effect since
     * there is no delay to apply.
     * </p>
     *
     * @param enabled {@code true} to enable throttling, {@code false} to disable
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled && this.intervalMs > 0;
    }

    @Override
    public String toString() {
        return "ThrottleConfig{" +
                "intervalMs=" + intervalMs +
                ", enabled=" + enabled +
                '}';
    }

}
