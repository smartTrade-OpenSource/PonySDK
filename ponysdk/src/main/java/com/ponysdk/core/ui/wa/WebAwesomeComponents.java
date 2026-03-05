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

package com.ponysdk.core.ui.wa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central initialization point for all Web Awesome component wrappers.
 * <p>
 * This class registers all 52 generated Web Awesome components in the
 * {@link ComponentRegistrar}, making them discoverable by their {@code wa-*}
 * tag names. It uses the generated {@link ComponentIndex} as the source of truth.
 * </p>
 * <p>
 * Usage:
 * <pre>{@code
 * // Initialize once at application startup
 * WebAwesomeComponents.initialize();
 * 
 * // Verify registration
 * ComponentRegistrar registrar = WebAwesomeComponents.getRegistrar();
 * System.out.println("Registered: " + registrar.size() + " components");
 * }</pre>
 * </p>
 * <p>
 * <strong>Requirements:</strong> 12.3, 13.8, 14.1
 * </p>
 *
 * @see ComponentIndex
 * @see ComponentRegistrar
 */
public final class WebAwesomeComponents {

    private static final Logger log = LoggerFactory.getLogger(WebAwesomeComponents.class);

    private static final ComponentRegistrar REGISTRAR = new ComponentRegistrar();
    private static volatile boolean initialized = false;

    // Private constructor - utility class
    private WebAwesomeComponents() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Initializes the Web Awesome component registry by registering all 57
     * generated components from the generated ComponentIndex.
     * <p>
     * This method is idempotent - calling it multiple times has no effect
     * after the first successful initialization.
     * </p>
     *
     * @throws IllegalStateException if component registration fails
     */
    public static synchronized void initialize() {
        if (initialized) {
            log.debug("WebAwesomeComponents already initialized, skipping");
            return;
        }

        log.info("Initializing Web Awesome component registry...");

        try {
            // Use reflection to load ComponentIndex from generated sources
            // This avoids a compile-time dependency on generated code
            final Class<?> indexClass = Class.forName("com.ponysdk.core.ui.wa.ComponentIndex");
            final var allComponentsField = indexClass.getDeclaredField("ALL_COMPONENTS");
            @SuppressWarnings("unchecked")
            final java.util.List<Object> indexEntries = (java.util.List<Object>) allComponentsField.get(null);

            // Convert to ComponentRegistrar entries using reflection
            final var entries = new java.util.ArrayList<ComponentRegistrar.ComponentEntry>();
            for (final Object entry : indexEntries) {
                final Class<?> entryClass = entry.getClass();
                final String tagName = (String) entryClass.getMethod("tagName").invoke(entry);
                final String javaClass = (String) entryClass.getMethod("javaClass").invoke(entry);
                final String propsClass = (String) entryClass.getMethod("propsClass").invoke(entry);
                final String status = (String) entryClass.getMethod("status").invoke(entry);
                
                entries.add(new ComponentRegistrar.ComponentEntry(tagName, javaClass, propsClass, status));
            }

            REGISTRAR.registerAll(entries);

            initialized = true;
            log.info("Successfully registered {} Web Awesome components", REGISTRAR.size());

            // Log component breakdown by status
            logComponentStats();

        } catch (final ClassNotFoundException e) {
            log.error("ComponentIndex not found. Run './gradlew :ponysdk:generateWebAwesomeWrappers' first.", e);
            throw new IllegalStateException("ComponentIndex not generated", e);
        } catch (final Exception e) {
            log.error("Failed to initialize Web Awesome components", e);
            throw new IllegalStateException("Component registration failed", e);
        }
    }

    /**
     * Returns the component registrar instance.
     * <p>
     * The registrar can be used to look up components by tag name or
     * inspect all registered components.
     * </p>
     *
     * @return the component registrar
     * @throws IllegalStateException if {@link #initialize()} has not been called
     */
    public static ComponentRegistrar getRegistrar() {
        if (!initialized) {
            throw new IllegalStateException(
                "WebAwesomeComponents not initialized. Call initialize() first."
            );
        }
        return REGISTRAR;
    }

    /**
     * Checks whether the component registry has been initialized.
     *
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Verifies that a specific component is registered.
     *
     * @param tagName the wa-* tag name to check
     * @return true if the component is registered
     */
    public static boolean isRegistered(final String tagName) {
        return initialized && REGISTRAR.isRegistered(tagName);
    }

    /**
     * Resets the initialization state (for testing purposes only).
     * <p>
     * <strong>WARNING:</strong> This method should only be used in test code.
     * </p>
     */
    static synchronized void resetForTesting() {
        initialized = false;
        REGISTRAR.getAll().clear();
        log.warn("WebAwesomeComponents reset for testing");
    }

    // ========================================================================
    // Private helpers
    // ========================================================================

    private static void logComponentStats() {
        final var all = REGISTRAR.getAll().values();
        final long stable = all.stream().filter(e -> "stable".equals(e.status())).count();
        final long experimental = all.stream().filter(e -> "experimental".equals(e.status())).count();
        final long deprecated = all.stream().filter(e -> "deprecated".equals(e.status())).count();

        log.info("Component status breakdown: stable={}, experimental={}, deprecated={}",
            stable, experimental, deprecated);
    }
}
