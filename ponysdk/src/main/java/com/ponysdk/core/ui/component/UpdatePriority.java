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
 * Defines the priority level for component updates.
 * <p>
 * When multiple updates are queued, the system processes HIGH priority updates
 * before NORMAL, and NORMAL before LOW. This allows critical UI updates to be
 * delivered with minimal latency while less important updates can be deferred.
 * </p>
 * <p>
 * The default priority for newly created components is {@link #NORMAL}.
 * Priority can be changed at runtime for any component.
 * </p>
 *
 * @see com.ponysdk.core.ui.component.PComponent
 */
public enum UpdatePriority {

    /**
     * High priority updates are processed first.
     * Use for critical UI updates that must be delivered immediately.
     */
    HIGH(0),

    /**
     * Normal priority updates are processed after HIGH.
     * This is the default priority for all components.
     */
    NORMAL(1),

    /**
     * Low priority updates are processed last.
     * Use for non-critical updates that can be deferred.
     */
    LOW(2);

    private static final UpdatePriority[] VALUES = UpdatePriority.values();

    private final int order;

    UpdatePriority(final int order) {
        this.order = order;
    }

    /**
     * Returns the ordering value for priority comparison.
     * <p>
     * Lower values indicate higher priority. HIGH=0, NORMAL=1, LOW=2.
     * </p>
     *
     * @return the order value for sorting
     */
    public int getOrder() {
        return order;
    }

    /**
     * Returns the byte value for protocol serialization.
     *
     * @return the ordinal value as a byte
     */
    public final byte getValue() {
        return (byte) ordinal();
    }

    /**
     * Converts a raw byte value back to an UpdatePriority.
     *
     * @param rawValue the byte value from protocol deserialization
     * @return the corresponding UpdatePriority
     * @throws ArrayIndexOutOfBoundsException if rawValue is out of range
     */
    public static UpdatePriority fromRawValue(final int rawValue) {
        return VALUES[rawValue];
    }

}
