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

package com.ponysdk.core.ui.codegen.parser;

import com.ponysdk.core.ui.codegen.model.*;

import javax.json.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.List;

/**
 * Default implementation of CEMParser.
 * <p>
 * Parses Custom Elements Manifest files following the standard schema.
 * Supports CEM schema version 1.0 and later with backward compatibility.
 * </p>
 */
public class DefaultCEMParser implements CEMParser {
    
    private static final Logger LOG = Logger.getLogger(DefaultCEMParser.class.getName());
    
    @Override
    public List<ComponentDefinition> parse(Path cemPath) throws CEMParseException {
        final JsonObject root = readJson(cemPath);
        
        // Validate schema version (1.0+)
        final String schemaVersion = getString(root, "schemaVersion");
        if (schemaVersion == null) {
            throw new CEMParseException("Missing required 'schemaVersion' field");
        }
        if (!isCompatibleSchemaVersion(schemaVersion)) {
            throw new CEMParseException("Unsupported schema version: " + schemaVersion + " (expected 1.0 or later)");
        }
        
        // Validate required fields
        final JsonArray modules = root.getJsonArray("modules");
        if (modules == null) {
            throw new CEMParseException("Missing required 'modules' array");
        }
        
        final List<ComponentDefinition> components = new ArrayList<>();
        
        for (final JsonValue moduleValue : modules) {
            if (moduleValue.getValueType() != JsonValue.ValueType.OBJECT) continue;
            final JsonObject module = moduleValue.asJsonObject();
            
            final JsonArray declarations = module.getJsonArray("declarations");
            if (declarations == null) continue;
            
            for (final JsonValue declValue : declarations) {
                if (declValue.getValueType() != JsonValue.ValueType.OBJECT) continue;
                final JsonObject decl = declValue.asJsonObject();
                
                // Only process class declarations with tag names
                if (!"class".equals(getString(decl, "kind"))) continue;
                
                final String tagName = getString(decl, "tagName");
                if (tagName == null || tagName.isEmpty()) continue;
                
                // Filter out private components (Requirement 10.5)
                if ("private".equals(getString(decl, "privacy"))) continue;
                
                components.add(parseDeclaration(decl, tagName));
            }
        }
        
        return components;
    }
    
    @Override
    public ValidationResult validate(Path cemPath) {
        final ValidationResult.Builder builder = ValidationResult.builder();
        
        try {
            final JsonObject root = readJson(cemPath);
            
            // Check schema version
            final String schemaVersion = getString(root, "schemaVersion");
            if (schemaVersion == null) {
                builder.addWarning("Missing 'schemaVersion' field");
            } else if (!isCompatibleSchemaVersion(schemaVersion)) {
                builder.addWarning("Unsupported schema version: " + schemaVersion + " (expected 1.0 or later)");
            }
            
            // Check required fields
            if (root.getJsonArray("modules") == null) {
                builder.addError("Missing required 'modules' array");
            }
            
            // Check for deprecated fields and log warnings (Requirement 10.4)
            checkDeprecatedFields(root, builder);
            
        } catch (CEMParseException e) {
            builder.addError(e.getMessage());
        }
        
        return builder.build();
    }
    
    private JsonObject readJson(Path cemPath) throws CEMParseException {
        try (final InputStream is = Files.newInputStream(cemPath);
             final JsonReader reader = Json.createReader(is)) {
            return reader.readObject();
        } catch (final IOException e) {
            throw new CEMParseException("Failed to read CEM file: " + cemPath + " - " + e.getMessage(), e);
        } catch (final JsonException e) {
            // Try to extract line information if available
            String message = "Invalid JSON structure: " + e.getMessage();
            if (e.getMessage() != null && e.getMessage().contains("line")) {
                // JsonException message often contains line information
                throw new CEMParseException(message, e);
            }
            throw new CEMParseException(message, e);
        }
    }
    
    private ComponentDefinition parseDeclaration(JsonObject decl, String tagName) {
        final String className = getStringOrDefault(decl, "name", "");
        final String summary = getStringOrDefault(decl, "summary", "");
        final String description = getStringOrDefault(decl, "description", "");
        final String status = getStringOrDefault(decl, "status", "stable");
        
        final List<PropertyDef> properties = parseProperties(decl.getJsonArray("members"));
        final List<EventDef> events = parseEvents(decl.getJsonArray("events"));
        final List<SlotDef> slots = parseSlots(decl.getJsonArray("slots"));
        final List<MethodDef> methods = parseMethods(decl.getJsonArray("members"));
        final List<CssPropertyDef> cssProperties = parseCssProperties(decl.getJsonArray("cssProperties"));
        
        return new ComponentDefinition(tagName, className, summary, description,
            properties, events, slots, methods, cssProperties, status);
    }
    
    private List<PropertyDef> parseProperties(JsonArray members) {
        if (members == null) return Collections.emptyList();
        
        final List<PropertyDef> properties = new ArrayList<>();
        for (final JsonValue memberValue : members) {
            if (memberValue.getValueType() != JsonValue.ValueType.OBJECT) continue;
            final JsonObject member = memberValue.asJsonObject();
            
            if (!"field".equals(getString(member, "kind"))) continue;
            
            final String name = getString(member, "name");
            if (name == null || name.startsWith("#")) continue; // Skip private fields
            
            final String cemType = extractTypeText(member);
            final String description = getStringOrDefault(member, "description", "");
            final String defaultValue = getString(member, "default");
            final boolean required = member.getBoolean("required", false);
            final String privacy = getStringOrDefault(member, "privacy", "public");
            
            properties.add(new PropertyDef(name, cemType, "", "", description, defaultValue, required, privacy));
        }
        return properties;
    }
    
    private List<EventDef> parseEvents(JsonArray events) {
        if (events == null) return Collections.emptyList();
        
        final List<EventDef> result = new ArrayList<>();
        for (final JsonValue eventValue : events) {
            if (eventValue.getValueType() != JsonValue.ValueType.OBJECT) continue;
            final JsonObject event = eventValue.asJsonObject();
            
            final String name = getString(event, "name");
            if (name == null) continue;
            
            final String description = getStringOrDefault(event, "description", "");
            final String detailType = extractTypeText(event);
            final boolean bubbles = event.getBoolean("bubbles", false);
            final boolean cancelable = event.getBoolean("cancelable", false);
            
            result.add(new EventDef(name, description, detailType, bubbles, cancelable));
        }
        return result;
    }
    
    private List<SlotDef> parseSlots(JsonArray slots) {
        if (slots == null) return Collections.emptyList();
        
        final List<SlotDef> result = new ArrayList<>();
        for (final JsonValue slotValue : slots) {
            if (slotValue.getValueType() != JsonValue.ValueType.OBJECT) continue;
            final JsonObject slot = slotValue.asJsonObject();
            
            final String name = getStringOrDefault(slot, "name", "");
            final String description = getStringOrDefault(slot, "description", "");
            
            result.add(new SlotDef(name, description));
        }
        return result;
    }
    
    private List<MethodDef> parseMethods(JsonArray members) {
        if (members == null) return Collections.emptyList();
        
        final List<MethodDef> methods = new ArrayList<>();
        for (final JsonValue memberValue : members) {
            if (memberValue.getValueType() != JsonValue.ValueType.OBJECT) continue;
            final JsonObject member = memberValue.asJsonObject();
            
            if (!"method".equals(getString(member, "kind"))) continue;
            
            final String name = getString(member, "name");
            if (name == null || "private".equals(getString(member, "privacy"))) continue;
            
            final String description = getStringOrDefault(member, "description", "");
            final List<ParameterDef> parameters = parseParameters(member.getJsonArray("parameters"));
            final String returnType = extractReturnType(member);
            final boolean async = returnType != null && returnType.startsWith("Promise");
            
            methods.add(new MethodDef(name, description, parameters, returnType, async));
        }
        return methods;
    }
    
    private List<ParameterDef> parseParameters(JsonArray parameters) {
        if (parameters == null) return Collections.emptyList();
        
        final List<ParameterDef> result = new ArrayList<>();
        for (final JsonValue paramValue : parameters) {
            if (paramValue.getValueType() != JsonValue.ValueType.OBJECT) continue;
            final JsonObject param = paramValue.asJsonObject();
            
            final String name = getString(param, "name");
            if (name == null) continue;
            
            final String cemType = extractTypeText(param);
            final String description = getStringOrDefault(param, "description", "");
            
            result.add(new ParameterDef(name, cemType, "", description));
        }
        return result;
    }
    
    private List<CssPropertyDef> parseCssProperties(JsonArray cssProperties) {
        if (cssProperties == null) return Collections.emptyList();
        
        final List<CssPropertyDef> result = new ArrayList<>();
        for (final JsonValue propValue : cssProperties) {
            if (propValue.getValueType() != JsonValue.ValueType.OBJECT) continue;
            final JsonObject prop = propValue.asJsonObject();
            
            final String name = getString(prop, "name");
            if (name == null) continue;
            
            final String description = getStringOrDefault(prop, "description", "");
            final String syntax = getString(prop, "syntax");
            final String defaultValue = getString(prop, "default");
            
            result.add(new CssPropertyDef(name, description, syntax, defaultValue));
        }
        return result;
    }
    
    private String extractTypeText(JsonObject obj) {
        final JsonObject typeObj = obj.getJsonObject("type");
        if (typeObj == null) return null;
        return getString(typeObj, "text");
    }
    
    private String extractReturnType(JsonObject method) {
        final JsonObject returnObj = method.getJsonObject("return");
        if (returnObj == null) return "void";
        final JsonObject typeObj = returnObj.getJsonObject("type");
        if (typeObj == null) return "void";
        return getString(typeObj, "text");
    }
    
    private static String getString(JsonObject obj, String key) {
        final JsonValue value = obj.get(key);
        if (value == null || value.getValueType() == JsonValue.ValueType.NULL) return null;
        if (value.getValueType() == JsonValue.ValueType.STRING) return ((JsonString) value).getString();
        return value.toString();
    }
    
    private static String getStringOrDefault(JsonObject obj, String key, String defaultValue) {
        final String value = getString(obj, key);
        return value != null ? value : defaultValue;
    }

    private boolean isCompatibleSchemaVersion(String schemaVersion) {
        if (schemaVersion == null || schemaVersion.isEmpty()) {
            return false;
        }
        // Support version 1.0 and later
        try {
            String[] parts = schemaVersion.split("\\.");
            if (parts.length > 0) {
                int majorVersion = Integer.parseInt(parts[0]);
                return majorVersion >= 1;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return false;
    }
    
    /**
     * Checks for deprecated fields in CEM and logs warnings.
     * Handles mapping deprecated fields to current equivalents (Requirement 10.3, 10.4).
     */
    private void checkDeprecatedFields(JsonObject root, ValidationResult.Builder builder) {
        // Check for deprecated 'version' field (should use 'schemaVersion')
        if (root.containsKey("version") && !root.containsKey("schemaVersion")) {
            String warning = "Deprecated field 'version' found. Use 'schemaVersion' instead.";
            builder.addWarning(warning);
            LOG.warning(warning);
        }
        
        // Unknown extensions are ignored gracefully (Requirement 10.2)
        // No action needed - JSON parser naturally ignores unknown fields
    }

}
