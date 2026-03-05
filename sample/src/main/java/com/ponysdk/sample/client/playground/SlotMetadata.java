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

/**
 * Metadata for a Web Component slot.
 * <p>
 * Slots allow content composition in Web Components. An empty name indicates
 * the default slot (unnamed slot).
 * </p>
 *
 * @param name        the slot name (empty string for default slot)
 * @param description human-readable description of the slot's purpose
 */
public record SlotMetadata(
    String name,
    String description
) {
    /**
     * Returns true if this is the default (unnamed) slot.
     */
    public boolean isDefaultSlot() {
        return name == null || name.isEmpty();
    }
    
    /**
     * Returns a display name for the slot.
     */
    public String getDisplayName() {
        return isDefaultSlot() ? "(default)" : name;
    }
}
