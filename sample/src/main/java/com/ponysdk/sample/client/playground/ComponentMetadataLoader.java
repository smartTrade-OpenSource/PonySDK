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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Loads component metadata from custom-elements.json.
 * <p>
 * This class parses the Custom Elements Manifest to extract slot definitions
 * and other metadata for Web Components, making them available to the playground
 * for dynamic UI generation.
 * </p>
 */
public class ComponentMetadataLoader {

    private static final Logger LOGGER = Logger.getLogger(ComponentMetadataLoader.class.getName());
    private static final String CUSTOM_ELEMENTS_JSON = "/data/custom-elements.json";

    private final Map<String, ComponentMetadata> metadataCache = new HashMap<>();
    private boolean loaded = false;

    /**
     * Loads all component metadata from custom-elements.json.
     * <p>
     * This method is called once during initialization. Subsequent calls
     * return cached data.
     * </p>
     */
    public void load() {
        if (loaded) {
            return;
        }

        try (InputStream is = getClass().getResourceAsStream(CUSTOM_ELEMENTS_JSON)) {
            if (is == null) {
                LOGGER.log(Level.WARNING, "custom-elements.json not found at: " + CUSTOM_ELEMENTS_JSON);
                loaded = true;
                return;
            }

            final Gson gson = new Gson();
            final JsonObject root = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
            
            parseManifest(root);
            loaded = true;
            
            LOGGER.log(Level.INFO, "Loaded metadata for {0} components", metadataCache.size());
            
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load custom-elements.json", e);
            loaded = true;
        }
    }

    /**
     * Gets metadata for a specific component by tag name.
     *
     * @param tagName the component tag name (e.g., "wa-button")
     * @return the component metadata, or empty if not found
     */
    public Optional<ComponentMetadata> getMetadata(final String tagName) {
        if (!loaded) {
            load();
        }
        return Optional.ofNullable(metadataCache.get(tagName));
    }

    /**
     * Gets all loaded component metadata.
     *
     * @return unmodifiable map of tag name to metadata
     */
    public Map<String, ComponentMetadata> getAllMetadata() {
        if (!loaded) {
            load();
        }
        return Collections.unmodifiableMap(metadataCache);
    }

    /**
     * Parses the custom-elements.json manifest structure.
     */
    private void parseManifest(final JsonObject root) {
        if (!root.has("modules")) {
            return;
        }

        final JsonArray modules = root.getAsJsonArray("modules");
        for (final JsonElement moduleElement : modules) {
            if (!moduleElement.isJsonObject()) {
                continue;
            }

            final JsonObject module = moduleElement.getAsJsonObject();
            if (!module.has("declarations")) {
                continue;
            }

            final JsonArray declarations = module.getAsJsonArray("declarations");
            for (final JsonElement declElement : declarations) {
                if (!declElement.isJsonObject()) {
                    continue;
                }

                final JsonObject declaration = declElement.getAsJsonObject();
                parseDeclaration(declaration);
            }
        }
    }

    /**
     * Parses a single component declaration.
     */
    private void parseDeclaration(final JsonObject declaration) {
        if (!declaration.has("tagName") || !declaration.has("customElement")) {
            return;
        }

        final String tagName = declaration.get("tagName").getAsString();
        final String className = declaration.has("name") ? declaration.get("name").getAsString() : tagName;
        final String summary = declaration.has("summary") ? declaration.get("summary").getAsString() : "";
        
        final List<SlotMetadata> slots = parseSlots(declaration);
        
        final ComponentMetadata metadata = new ComponentMetadata(tagName, className, summary, slots);
        metadataCache.put(tagName, metadata);
    }

    /**
     * Parses slot definitions from a component declaration.
     */
    private List<SlotMetadata> parseSlots(final JsonObject declaration) {
        if (!declaration.has("slots")) {
            return Collections.emptyList();
        }

        final JsonArray slotsArray = declaration.getAsJsonArray("slots");
        final List<SlotMetadata> slots = new ArrayList<>();

        for (final JsonElement slotElement : slotsArray) {
            if (!slotElement.isJsonObject()) {
                continue;
            }

            final JsonObject slotObj = slotElement.getAsJsonObject();
            final String name = slotObj.has("name") ? slotObj.get("name").getAsString() : "";
            final String description = slotObj.has("description") ? slotObj.get("description").getAsString() : "";
            
            slots.add(new SlotMetadata(name, description));
        }

        return slots;
    }
}
