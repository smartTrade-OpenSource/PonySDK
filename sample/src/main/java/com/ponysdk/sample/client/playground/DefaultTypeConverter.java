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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of {@link TypeConverter} that converts string values
 * to various Java types.
 * <p>
 * This converter supports:
 * <ul>
 *   <li>String to String (pass-through)</li>
 *   <li>String to boolean (parse "true"/"false")</li>
 *   <li>String to int/long (parse numeric with validation)</li>
 *   <li>String to enum (find enum constant by name)</li>
 *   <li>String to Optional (wrap in Optional.of() or Optional.empty())</li>
 * </ul>
 * </p>
 */
public class DefaultTypeConverter implements TypeConverter {

    private static final Logger LOGGER = Logger.getLogger(DefaultTypeConverter.class.getName());

    /**
     * Converts a string value to the target type.
     * <p>
     * Handles null and empty strings appropriately for each type.
     * For Optional types, null or empty strings result in Optional.empty().
     * </p>
     *
     * @param <T>        the target type
     * @param value      the string value to convert, may be null
     * @param targetType the target class, must not be null
     * @return the converted value, may be null
     * @throws ConversionException if conversion fails
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T convert(final String value, final Class<T> targetType) throws ConversionException {
        if (targetType == null) {
            throw new ConversionException("targetType must not be null");
        }

        try {
            // Handle Optional types
            if (targetType == Optional.class) {
                return (T) convertToOptional(value);
            }

            // Handle null or empty values for non-Optional types
            if (value == null || value.trim().isEmpty()) {
                if (targetType.isPrimitive()) {
                    throw new ConversionException("Cannot convert null or empty string to primitive type: " + targetType.getName());
                }
                return null;
            }

            // String pass-through
            if (targetType == String.class) {
                return (T) value;
            }

            // Boolean conversion
            if (targetType == Boolean.class || targetType == boolean.class) {
                return (T) convertToBoolean(value);
            }

            // Integer conversion
            if (targetType == Integer.class || targetType == int.class) {
                return (T) convertToInteger(value);
            }

            // Long conversion
            if (targetType == Long.class || targetType == long.class) {
                return (T) convertToLong(value);
            }

            // Enum conversion
            if (targetType.isEnum()) {
                return (T) convertToEnum(value, (Class<? extends Enum>) targetType);
            }

            throw new ConversionException("Unsupported target type: " + targetType.getName());

        } catch (final ConversionException e) {
            throw e;
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Unexpected error during conversion", e);
            throw new ConversionException("Failed to convert '" + value + "' to " + targetType.getName(), e);
        }
    }

    /**
     * Converts a string to Optional.
     * <p>
     * Returns Optional.empty() for null or empty strings,
     * otherwise returns Optional.of(value).
     * </p>
     */
    private Optional<String> convertToOptional(final String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    /**
     * Converts a string to Boolean.
     * <p>
     * Accepts "true" (case-insensitive) as true, "false" (case-insensitive) as false.
     * </p>
     */
    private Boolean convertToBoolean(final String value) throws ConversionException {
        final String trimmed = value.trim().toLowerCase();
        if ("true".equals(trimmed)) {
            return Boolean.TRUE;
        } else if ("false".equals(trimmed)) {
            return Boolean.FALSE;
        } else {
            throw new ConversionException("Invalid boolean value: expected 'true' or 'false', got '" + value + "'");
        }
    }

    /**
     * Converts a string to Integer.
     * <p>
     * Validates that the string represents a valid integer value.
     * </p>
     */
    private Integer convertToInteger(final String value) throws ConversionException {
        try {
            return Integer.parseInt(value.trim());
        } catch (final NumberFormatException e) {
            throw new ConversionException("Invalid integer value: expected numeric string, got '" + value + "'", e);
        }
    }

    /**
     * Converts a string to Long.
     * <p>
     * Validates that the string represents a valid long value.
     * </p>
     */
    private Long convertToLong(final String value) throws ConversionException {
        try {
            return Long.parseLong(value.trim());
        } catch (final NumberFormatException e) {
            throw new ConversionException("Invalid long value: expected numeric string, got '" + value + "'", e);
        }
    }

    /**
     * Converts a string to an enum constant.
     * <p>
     * Finds the enum constant by name (case-sensitive).
     * </p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <E extends Enum<E>> E convertToEnum(final String value, final Class<? extends Enum> enumType) throws ConversionException {
        try {
            return (E) Enum.valueOf(enumType, value.trim());
        } catch (final IllegalArgumentException e) {
            throw new ConversionException("Invalid enum value: '" + value + "' is not a valid constant of " + enumType.getSimpleName(), e);
        }
    }
}
