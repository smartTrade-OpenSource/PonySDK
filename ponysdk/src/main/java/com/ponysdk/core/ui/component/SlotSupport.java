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

import java.util.Set;

/**
 * Interface for Web Components that support named slot composition.
 * <p>
 * Slots allow inserting child components into named regions of a Web Component,
 * following the Web Components slot specification. Each component declares its
 * available slots based on the Custom Elements Manifest definition.
 * </p>
 *
 * @see PWebComponent
 */
public interface SlotSupport {

    /**
     * Adds a child component into the specified named slot.
     * <p>
     * If the slot name is not in the component's declared slots, a warning is logged
     * and the operation is ignored (Requirement 7.5).
     * </p>
     *
     * @param slotName the name of the slot to add the child to
     * @param child    the child component to add
     */
    void addToSlot(String slotName, PComponent<?> child);

    /**
     * Adds a child component into the default (unnamed) slot.
     *
     * @param child the child component to add
     */
    void addToDefaultSlot(PComponent<?> child);

    /**
     * Removes a child component from the specified named slot.
     * <p>
     * If the slot name is not in the component's declared slots, a warning is logged
     * and the operation is ignored.
     * </p>
     *
     * @param slotName the name of the slot to remove the child from
     * @param child    the child component to remove
     */
    void removeFromSlot(String slotName, PComponent<?> child);

    /**
     * Returns the set of slot names declared by this component.
     * <p>
     * The set includes all named slots from the component's Custom Elements Manifest
     * definition. The default slot (empty string) is included if the component supports it.
     * </p>
     *
     * @return an unmodifiable set of declared slot names
     */
    Set<String> getDeclaredSlots();
}
