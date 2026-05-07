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

/**
 * Resolves human-readable type hint strings from parameter types.
 * <p>
 * Stateless utility class. Thread-safe.
 * </p>
 */
public final class TypeHintResolver {

    private TypeHintResolver() {
        // Utility class
    }

    /**
     * Returns a human-readable type hint for the given parameter.
     *
     * @param parameterInfo the parameter info, must not be null
     * @return the type hint string (e.g. "String", "boolean", "number", "Variant")
     * @throws IllegalArgumentException if parameterInfo is null
     */
    public static String resolve(final ParameterInfo parameterInfo) {
        if (parameterInfo == null) {
            throw new IllegalArgumentException("parameterInfo must not be null");
        }

        // For Optional parameters, resolve based on the wrapped type
        if (parameterInfo.isOptional()) {
            final Class<?> wrappedType = parameterInfo.optionalWrappedType();
            if (wrappedType != null && wrappedType != Object.class) {
                final ParameterInfo unwrapped = new ParameterInfo(
                    parameterInfo.name(), wrappedType, false, null
                );
                return resolve(unwrapped) + "?";
            }
            return "optional";
        }

        final Class<?> type = parameterInfo.type();

        if (type == String.class) return "string";
        if (type == boolean.class || type == Boolean.class) return "boolean";
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class) return "number";
        if (type == double.class || type == Double.class) return "number";
        if (type == float.class || type == Float.class) return "number";
        if (type.isEnum()) return type.getSimpleName();

        return "unknown";
    }
}
