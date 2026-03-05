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

/**
 * Warning about a type mapping that fell back to a generic type.
 *
 * @param componentName the component containing the property
 * @param propertyName  the property with the unmappable type
 * @param cemType       the original CEM type string
 * @param fallbackType  the fallback type used (e.g., "Object", "unknown")
 * @param reason        explanation of why the type couldn't be mapped
 */
public record TypeMappingWarning(
    String componentName,
    String propertyName,
    String cemType,
    String fallbackType,
    String reason
) {}
