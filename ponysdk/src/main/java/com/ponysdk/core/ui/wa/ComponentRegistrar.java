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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side registrar that connects the generated {@code ComponentIndex}
 * to the system, making all Web Awesome component wrappers discoverable
 * by their {@code wa-*} tag name.
 *
 * <p>This class iterates over component entries (tag name, wrapper class,
 * props class, status) and maintains a lookup map keyed by tag name.
 * It serves as the server-side counterpart to the client-side
 * {@code ComponentRegistry} which maps signatures to factories.</p>
 *
 * <p>Validates: Requirements 12.3, 13.8</p>
 *
 * @see ComponentRegistrar.ComponentEntry
 */
public class ComponentRegistrar {

    private static final Logger log = LoggerFactory.getLogger(ComponentRegistrar.class);

    private final Map<String, ComponentEntry> registry = new LinkedHashMap<>();

    /**
     * A component entry describing a registered Web Awesome wrapper.
     */
    public record ComponentEntry(
        String tagName,
        String wrapperClassName,
        String propsClassName,
        String status
    ) {}

    /**
     * Registers all components from the given list of entries.
     *
     * @param entries the component entries to register (typically from ComponentIndex.ALL_COMPONENTS)
     */
    public void registerAll(final List<ComponentEntry> entries) {
        for (final ComponentEntry entry : entries) {
            register(entry);
        }
        log.info("Registered {} Web Awesome components", registry.size());
    }

    /**
     * Registers a single component entry.
     *
     * @param entry the component entry to register
     */
    public void register(final ComponentEntry entry) {
        if (entry.tagName() == null || entry.tagName().isBlank()) {
            log.warn("Skipping component entry with null or blank tag name: {}", entry);
            return;
        }
        if (registry.containsKey(entry.tagName())) {
            log.warn("Duplicate registration for tag '{}', overwriting previous entry", entry.tagName());
        }
        registry.put(entry.tagName(), entry);
    }

    /**
     * Looks up a component entry by its {@code wa-*} tag name.
     *
     * @param tagName the tag name to look up (e.g., "wa-button")
     * @return an Optional containing the entry if found
     */
    public Optional<ComponentEntry> lookup(final String tagName) {
        return Optional.ofNullable(registry.get(tagName));
    }

    /**
     * Returns an unmodifiable view of all registered entries.
     */
    public Map<String, ComponentEntry> getAll() {
        return Collections.unmodifiableMap(registry);
    }

    /**
     * Returns the number of registered components.
     */
    public int size() {
        return registry.size();
    }

    /**
     * Checks whether a component with the given tag name is registered.
     */
    public boolean isRegistered(final String tagName) {
        return registry.containsKey(tagName);
    }
}
