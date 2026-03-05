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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ponysdk.core.ui.component.PWebComponent;
import com.ponysdk.core.ui.wa.ComponentIndex;

/**
 * Default implementation of {@link ComponentScanner} that uses the generated
 * {@link ComponentIndex} to discover Web Awesome components.
 * <p>
 * This scanner:
 * <ul>
 *   <li>Loads component classes from the ComponentIndex</li>
 *   <li>Verifies each class extends PWebComponent</li>
 *   <li>Sorts component classes alphabetically by simple name</li>
 *   <li>Handles reflection errors gracefully</li>
 * </ul>
 * </p>
 */
public class DefaultComponentScanner implements ComponentScanner {

    private static final Logger LOGGER = Logger.getLogger(DefaultComponentScanner.class.getName());

    /**
     * Scans for Web Awesome component classes using the ComponentIndex.
     * <p>
     * The method loads each class from the index, verifies it extends PWebComponent,
     * and returns them sorted alphabetically by simple name (e.g., WAButton, WAInput).
     * </p>
     *
     * @return a list of component classes extending PWebComponent, sorted alphabetically,
     *         never null (returns empty list if scanning fails)
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Class<? extends PWebComponent<?>>> scanComponents() {
        final List<Class<? extends PWebComponent<?>>> components = new ArrayList<>();

        try {
            // Iterate through all components in the index
            for (final ComponentIndex.ComponentEntry entry : ComponentIndex.ALL_COMPONENTS) {
                try {
                    // Load the class
                    final Class<?> clazz = Class.forName(entry.javaClass());

                    // Verify it extends PWebComponent
                    if (PWebComponent.class.isAssignableFrom(clazz)) {
                        components.add((Class<? extends PWebComponent<?>>) clazz);
                    } else {
                        LOGGER.log(Level.WARNING, "Class {0} does not extend PWebComponent", entry.javaClass());
                    }
                } catch (final ClassNotFoundException e) {
                    LOGGER.log(Level.WARNING, "Failed to load component class: " + entry.javaClass(), e);
                } catch (final ClassCastException e) {
                    LOGGER.log(Level.WARNING, "Failed to cast component class: " + entry.javaClass(), e);
                }
            }

            // Sort alphabetically by simple class name (WAButton, WAInput, etc.)
            Collections.sort(components, (c1, c2) -> c1.getSimpleName().compareTo(c2.getSimpleName()));

            LOGGER.log(Level.INFO, "Successfully scanned {0} Web Awesome components", components.size());

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to scan components", e);
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(components);
    }

    /**
     * Extracts the component name from a class name.
     * <p>
     * Converts class names like "WAButton" to "Button" by removing the "WA" prefix.
     * </p>
     *
     * @param componentClass the component class, must not be null
     * @return the component name without the "WA" prefix
     * @throws IllegalArgumentException if componentClass is null
     */
    public static String extractComponentName(final Class<? extends PWebComponent<?>> componentClass) {
        if (componentClass == null) {
            throw new IllegalArgumentException("componentClass must not be null");
        }

        final String simpleName = componentClass.getSimpleName();

        // Remove "WA" prefix if present
        if (simpleName.startsWith("WA") && simpleName.length() > 2) {
            return simpleName.substring(2);
        }

        return simpleName;
    }
}
