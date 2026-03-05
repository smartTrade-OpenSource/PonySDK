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

import com.ponysdk.core.ui.codegen.model.ComponentDefinition;

import java.nio.file.Path;
import java.util.List;

/**
 * Parser for Custom Elements Manifest (CEM) JSON files.
 * <p>
 * Parses CEM files following the standard schema and extracts component definitions
 * in a library-agnostic way. Supports CEM schema version 1.0 and later.
 * </p>
 */
public interface CEMParser {
    
    /**
     * Parses a CEM file and returns component definitions.
     * <p>
     * Extracts all components from the manifest, including their properties, events,
     * slots, methods, and CSS properties. Handles optional fields with sensible defaults
     * and ignores unknown extensions gracefully.
     * </p>
     *
     * @param cemPath path to the CEM JSON file
     * @return list of parsed component definitions
     * @throws CEMParseException if the file is malformed or invalid
     */
    List<ComponentDefinition> parse(Path cemPath) throws CEMParseException;
    
    /**
     * Validates a CEM file against the schema.
     * <p>
     * Checks for required fields and schema version compatibility without
     * performing full parsing.
     * </p>
     *
     * @param cemPath path to the CEM JSON file
     * @return validation result with errors if any
     */
    ValidationResult validate(Path cemPath);
}
