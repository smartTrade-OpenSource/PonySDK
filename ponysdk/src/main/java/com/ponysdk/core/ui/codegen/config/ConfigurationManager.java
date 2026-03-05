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

package com.ponysdk.core.ui.codegen.config;

import com.ponysdk.core.ui.codegen.model.GeneratorConfiguration;
import com.ponysdk.core.ui.codegen.parser.ValidationResult;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Manages parsing, validation, and serialization of generator configuration files.
 */
public interface ConfigurationManager {
    
    /**
     * Parses a configuration file.
     * 
     * @param configPath path to configuration file
     * @return parsed configuration
     * @throws ConfigurationException if configuration is invalid
     */
    GeneratorConfiguration parse(Path configPath) throws ConfigurationException;
    
    /**
     * Serializes a configuration to a file.
     * 
     * @param config the configuration to serialize
     * @param outputPath path to write configuration
     * @throws IOException if writing fails
     */
    void serialize(GeneratorConfiguration config, Path outputPath) throws IOException;
    
    /**
     * Validates a configuration.
     * 
     * @param config the configuration to validate
     * @return validation result
     */
    ValidationResult validate(GeneratorConfiguration config);
}
