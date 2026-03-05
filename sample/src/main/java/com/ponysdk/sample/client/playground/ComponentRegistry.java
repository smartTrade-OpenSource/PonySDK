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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.core.ui.component.PWebComponent;

/**
 * Registry of discovered components.
 * <p>
 * Maintains a mapping of component names to their classes. Component names are
 * extracted from class names (e.g., WAButton → Button).
 * </p>
 */
public class ComponentRegistry {

    private final Map<String, Class<? extends PWebComponent<?>>> components = new LinkedHashMap<>();

    /**
     * Registers a component with the given name.
     *
     * @param name  the component name, must not be null
     * @param clazz the component class, must not be null
     */
    public void register(String name, Class<? extends PWebComponent<?>> clazz) {
        if (name == null) throw new IllegalArgumentException("name must not be null");
        if (clazz == null) throw new IllegalArgumentException("clazz must not be null");
        components.put(name, clazz);
    }

    /**
     * Gets the component class for the given name.
     *
     * @param name the component name, must not be null
     * @return the component class, or null if not found
     */
    public Class<? extends PWebComponent<?>> get(String name) {
        if (name == null) throw new IllegalArgumentException("name must not be null");
        return components.get(name);
    }

    /**
     * Gets all registered component names in alphabetical order.
     *
     * @return an unmodifiable list of component names, never null
     */
    public List<String> getComponentNames() {
        final List<String> names = new ArrayList<>(components.keySet());
        Collections.sort(names);
        return Collections.unmodifiableList(names);
    }

    /**
     * Clears all registered components.
     */
    public void clear() {
        components.clear();
    }
}
