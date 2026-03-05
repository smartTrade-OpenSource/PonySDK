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

import java.util.Optional;

/**
 * Analyzes method parameter types and determines appropriate form control types.
 * <p>
 * This analyzer identifies parameter types (String, boolean, int, long, enum, Optional)
 * and determines what type of UI control should be generated for each parameter.
 * </p>
 */
public class ParameterAnalyzer {

    /**
     * Enum representing the different types of controls that can be generated.
     */
    public enum ControlType {
        /** Text box for String parameters */
        TEXT_BOX,
        /** Checkbox for boolean parameters */
        CHECK_BOX,
        /** Numeric text box for int/long parameters */
        NUMERIC_TEXT_BOX,
        /** List box for enum parameters */
        LIST_BOX,
        /** Checkbox + control for Optional parameters */
        OPTIONAL_CONTROL,
        /** Label for unsupported types */
        UNSUPPORTED
    }

    /**
     * Determines the appropriate control type for a given parameter.
     *
     * @param parameterInfo the parameter information, must not be null
     * @return the control type to use for this parameter, never null
     * @throws IllegalArgumentException if parameterInfo is null
     */
    public ControlType determineControlType(final ParameterInfo parameterInfo) {
        if (parameterInfo == null) {
            throw new IllegalArgumentException("parameterInfo must not be null");
        }

        // Handle Optional parameters
        if (parameterInfo.isOptional()) {
            return ControlType.OPTIONAL_CONTROL;
        }

        final Class<?> type = parameterInfo.type();

        // String parameters
        if (String.class.equals(type)) {
            return ControlType.TEXT_BOX;
        }

        // Boolean parameters (both primitive and wrapper)
        if (boolean.class.equals(type) || Boolean.class.equals(type)) {
            return ControlType.CHECK_BOX;
        }

        // Numeric parameters (int and long, both primitive and wrapper)
        if (isNumericType(type)) {
            return ControlType.NUMERIC_TEXT_BOX;
        }

        // Enum parameters
        if (type.isEnum()) {
            return ControlType.LIST_BOX;
        }

        // Unsupported type
        return ControlType.UNSUPPORTED;
    }

    /**
     * Checks if a parameter type is String.
     *
     * @param parameterInfo the parameter information, must not be null
     * @return true if the parameter is a String
     * @throws IllegalArgumentException if parameterInfo is null
     */
    public boolean isStringType(final ParameterInfo parameterInfo) {
        if (parameterInfo == null) {
            throw new IllegalArgumentException("parameterInfo must not be null");
        }
        return !parameterInfo.isOptional() && String.class.equals(parameterInfo.type());
    }

    /**
     * Checks if a parameter type is boolean.
     *
     * @param parameterInfo the parameter information, must not be null
     * @return true if the parameter is a boolean (primitive or wrapper)
     * @throws IllegalArgumentException if parameterInfo is null
     */
    public boolean isBooleanType(final ParameterInfo parameterInfo) {
        if (parameterInfo == null) {
            throw new IllegalArgumentException("parameterInfo must not be null");
        }
        final Class<?> type = parameterInfo.type();
        return !parameterInfo.isOptional() && 
               (boolean.class.equals(type) || Boolean.class.equals(type));
    }

    /**
     * Checks if a parameter type is numeric (int or long).
     *
     * @param parameterInfo the parameter information, must not be null
     * @return true if the parameter is int or long (primitive or wrapper)
     * @throws IllegalArgumentException if parameterInfo is null
     */
    public boolean isNumericType(final ParameterInfo parameterInfo) {
        if (parameterInfo == null) {
            throw new IllegalArgumentException("parameterInfo must not be null");
        }
        return !parameterInfo.isOptional() && isNumericType(parameterInfo.type());
    }

    /**
     * Checks if a class type is numeric (int or long).
     *
     * @param type the class type to check
     * @return true if the type is int or long (primitive or wrapper)
     */
    private boolean isNumericType(final Class<?> type) {
        return int.class.equals(type) || Integer.class.equals(type) ||
               long.class.equals(type) || Long.class.equals(type);
    }

    /**
     * Checks if a parameter type is an enum.
     *
     * @param parameterInfo the parameter information, must not be null
     * @return true if the parameter is an enum type
     * @throws IllegalArgumentException if parameterInfo is null
     */
    public boolean isEnumType(final ParameterInfo parameterInfo) {
        if (parameterInfo == null) {
            throw new IllegalArgumentException("parameterInfo must not be null");
        }
        return !parameterInfo.isOptional() && parameterInfo.type().isEnum();
    }

    /**
     * Checks if a parameter type is Optional.
     *
     * @param parameterInfo the parameter information, must not be null
     * @return true if the parameter is an Optional type
     * @throws IllegalArgumentException if parameterInfo is null
     */
    public boolean isOptionalType(final ParameterInfo parameterInfo) {
        if (parameterInfo == null) {
            throw new IllegalArgumentException("parameterInfo must not be null");
        }
        return parameterInfo.isOptional();
    }

    /**
     * Gets the wrapped type for an Optional parameter.
     *
     * @param parameterInfo the parameter information, must not be null and must be Optional
     * @return the wrapped type class, never null
     * @throws IllegalArgumentException if parameterInfo is null or not Optional
     */
    public Class<?> getOptionalWrappedType(final ParameterInfo parameterInfo) {
        if (parameterInfo == null) {
            throw new IllegalArgumentException("parameterInfo must not be null");
        }
        if (!parameterInfo.isOptional()) {
            throw new IllegalArgumentException("parameterInfo must be Optional type");
        }
        return parameterInfo.optionalWrappedType();
    }

    /**
     * Checks if a parameter type is supported by the form generator.
     *
     * @param parameterInfo the parameter information, must not be null
     * @return true if the parameter type is supported
     * @throws IllegalArgumentException if parameterInfo is null
     */
    public boolean isSupportedType(final ParameterInfo parameterInfo) {
        if (parameterInfo == null) {
            throw new IllegalArgumentException("parameterInfo must not be null");
        }
        return determineControlType(parameterInfo) != ControlType.UNSUPPORTED;
    }
}
