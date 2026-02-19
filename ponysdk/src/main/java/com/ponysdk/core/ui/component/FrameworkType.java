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
 * Defines the target UI framework for PComponent rendering.
 * <p>
 * Each framework type corresponds to a specific client-side adapter that handles
 * component mounting, props updates, and lifecycle management.
 * </p>
 *
 * @see com.ponysdk.core.ui.component.PComponent
 */
public enum FrameworkType {

    /**
     * React framework adapter.
     * Uses React 18 createRoot for mounting and props-based updates.
     */
    REACT,

    /**
     * Vue framework adapter.
     * Uses Vue 3 createApp for mounting and reactive props updates.
     */
    VUE,

    /**
     * Svelte framework adapter.
     * Uses Svelte component mounting with store-based updates.
     */
    SVELTE,

    /**
     * Web Components adapter.
     * Uses custom elements with property setter updates.
     */
    WEB_COMPONENT;

    private static final FrameworkType[] VALUES = FrameworkType.values();

    FrameworkType() {
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
     * Converts a raw byte value back to a FrameworkType.
     *
     * @param rawValue the byte value from protocol deserialization
     * @return the corresponding FrameworkType
     * @throws ArrayIndexOutOfBoundsException if rawValue is out of range
     */
    public static FrameworkType fromRawValue(final int rawValue) {
        return VALUES[rawValue];
    }

}
