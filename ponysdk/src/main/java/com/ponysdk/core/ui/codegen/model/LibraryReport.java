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

package com.ponysdk.core.ui.codegen.model;

import java.util.List;

/**
 * Report for a single library's code generation results.
 *
 * @param libraryName           the library identifier
 * @param componentCount        number of components generated
 * @param propertyCount         number of properties processed
 * @param eventCount            number of events processed
 * @param methodCount           number of methods processed
 * @param generatedFiles        list of generated file paths
 * @param skippedComponents     list of component names that were skipped
 * @param typeMappingWarnings   list of type mapping warnings
 */
public record LibraryReport(
    String libraryName,
    int componentCount,
    int propertyCount,
    int eventCount,
    int methodCount,
    List<String> generatedFiles,
    List<String> skippedComponents,
    List<TypeMappingWarning> typeMappingWarnings
) {}
