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

package com.ponysdk.core.ui.wa.codegen;

import java.util.List;

/**
 * Represents a generated enum definition.
 *
 * @param enumName        the enum name in PascalCase (e.g., "ButtonVariant")
 * @param packageName     the package name (e.g., "com.ponysdk.core.ui.wa.enums")
 * @param constants       the list of enum constants
 * @param sourceComponent the component name this enum was generated from (e.g., "wa-button")
 * @param sourceProperty  the property name this enum was generated from (e.g., "variant")
 */
public record EnumDefinition(
    String enumName,
    String packageName,
    List<EnumConstant> constants,
    String sourceComponent,
    String sourceProperty
) {
    /**
     * Returns the fully qualified name of this enum.
     *
     * @return the package name followed by the enum name (e.g., "com.ponysdk.core.ui.wa.enums.ButtonVariant")
     */
    public String getFullyQualifiedName() {
        return packageName + "." + enumName;
    }
}
