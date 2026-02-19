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

package com.ponysdk.core.ui.component;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonPatch;
import javax.json.JsonValue;

import java.lang.reflect.RecordComponent;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Computes differences between props objects for efficient network transmission.
 * <p>
 * PropsDiffer supports two diff modes:
 * <ul>
 *   <li><b>JSON Patch</b> (RFC 6902): For normal updates, generates a minimal set of
 *       operations (add, remove, replace) to transform previous props into current props.</li>
 *   <li><b>Binary</b>: For high-frequency updates, uses a compact binary encoding that
 *       minimizes serialization overhead.</li>
 * </ul>
 * </p>
 * <p>
 * The differ works with Java Record types, using reflection to serialize record
 * components to JSON. This ensures type safety while maintaining flexibility.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * PropsDiffer<MyProps> differ = new PropsDiffer<>();
 *
 * // Compute JSON Patch diff
 * Optional<JsonArray> patch = differ.computeDiff(previousProps, currentProps);
 * if (patch.isPresent()) {
 *     // Send patch to client
 *     sendPatch(patch.get());
 * }
 *
 * // Compute binary diff for high-frequency updates
 * byte[] binaryDiff = differ.computeBinaryDiff(previousProps, currentProps);
 * }</pre>
 *
 * @param <TProps> the props type, must be a Java Record
 * @see com.ponysdk.core.ui.component.PComponent
 */
public class PropsDiffer<TProps extends Record> {

    /**
     * Binary field type markers for the binary diff format.
     */
    private static final byte TYPE_INT32 = 0x01;
    private static final byte TYPE_FLOAT64 = 0x02;
    private static final byte TYPE_STRING = 0x03;
    private static final byte TYPE_BOOLEAN = 0x04;
    private static final byte TYPE_NULL = 0x05;
    private static final byte TYPE_ARRAY_START = 0x06;
    private static final byte TYPE_ARRAY_END = 0x07;
    private static final byte TYPE_OBJECT_START = 0x08;
    private static final byte TYPE_OBJECT_END = 0x09;

    /**
     * Creates a new PropsDiffer instance.
     */
    public PropsDiffer() {
    }

    /**
     * Computes the JSON Patch diff between previous and current props.
     * <p>
     * If previous is null, returns empty (indicating full JSON is needed for creation).
     * If the props are equal, returns empty (no update needed).
     * Otherwise, returns the JSON Patch array containing only the operations needed
     * to transform previous into current.
     * </p>
     *
     * @param previous the previous props state, may be null for initial creation
     * @param current  the current props state, must not be null
     * @return an Optional containing the JSON Patch array if there are changes,
     *         or empty if no update is needed or full JSON is required
     * @throws NullPointerException if current is null
     */
    public Optional<JsonArray> computeDiff(final TProps previous, final TProps current) {
        Objects.requireNonNull(current, "Current props must not be null");

        if (previous == null) {
            // Full JSON needed for creation
            return Optional.empty();
        }

        if (previous.equals(current)) {
            // No changes, no update needed
            return Optional.empty();
        }

        final JsonObject prevJson = toJson(previous);
        final JsonObject currJson = toJson(current);

        // Use Jakarta JSON-P to compute the diff
        final JsonPatch patch = Json.createDiff(prevJson, currJson);
        final JsonArray patchArray = patch.toJsonArray();

        if (patchArray.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(patchArray);
    }

    /**
     * Computes a binary diff for high-frequency updates.
     * <p>
     * The binary format is optimized for minimal serialization overhead and
     * compact wire representation. It encodes only the changed fields using
     * a simple field-id/type/length/value format.
     * </p>
     * <p>
     * Binary format structure:
     * <pre>
     * +--------+--------+--------+--------+
     * | FieldID| Type   | Length | Value  |
     * +--------+--------+--------+--------+
     * | 1 byte | 1 byte | 2 bytes| N bytes|
     * +--------+--------+--------+--------+
     * </pre>
     * </p>
     *
     * @param previous the previous props state, may be null
     * @param current  the current props state, must not be null
     * @return the binary encoded diff, or empty array if no changes
     * @throws NullPointerException if current is null
     */
    public byte[] computeBinaryDiff(final TProps previous, final TProps current) {
        Objects.requireNonNull(current, "Current props must not be null");

        if (previous == null) {
            // Full binary encoding for creation
            return encodeToBinary(current);
        }

        if (previous.equals(current)) {
            // No changes
            return new byte[0];
        }

        // Encode only changed fields
        return encodeChangedFieldsToBinary(previous, current);
    }

    /**
     * Converts a props Record to a JsonObject.
     * <p>
     * Uses reflection to iterate over record components and serialize each
     * to the appropriate JSON type.
     * </p>
     *
     * @param props the props record to convert
     * @return the JSON representation of the props
     */
    public JsonObject toJson(final TProps props) {
        if (props == null) {
            return Json.createObjectBuilder().build();
        }
        return recordToJson(props);
    }

    /**
     * Converts a JsonObject back to a props Record.
     * <p>
     * This method is used for round-trip validation and applying patches.
     * </p>
     *
     * @param json       the JSON object to convert
     * @param propsClass the target record class
     * @return the reconstructed props record
     * @throws IllegalArgumentException if the JSON cannot be converted to the props type
     */
    @SuppressWarnings("unchecked")
    public TProps fromJson(final JsonObject json, final Class<TProps> propsClass) {
        Objects.requireNonNull(json, "JSON must not be null");
        Objects.requireNonNull(propsClass, "Props class must not be null");

        if (!propsClass.isRecord()) {
            throw new IllegalArgumentException("Props class must be a Record: " + propsClass.getName());
        }

        try {
            final RecordComponent[] components = propsClass.getRecordComponents();
            final Class<?>[] paramTypes = new Class<?>[components.length];
            final Object[] args = new Object[components.length];

            for (int i = 0; i < components.length; i++) {
                final RecordComponent component = components[i];
                paramTypes[i] = component.getType();
                args[i] = jsonValueToObject(json.get(component.getName()), component.getType(), component.getGenericType());
            }

            return propsClass.getDeclaredConstructor(paramTypes).newInstance(args);
        } catch (final Exception e) {
            throw new IllegalArgumentException("Failed to convert JSON to props: " + e.getMessage(), e);
        }
    }

    /**
     * Applies a JSON Patch to a props object and returns the result.
     *
     * @param props      the original props
     * @param patch      the JSON Patch to apply
     * @param propsClass the props class for reconstruction
     * @return the patched props
     */
    public TProps applyPatch(final TProps props, final JsonArray patch, final Class<TProps> propsClass) {
        Objects.requireNonNull(props, "Props must not be null");
        Objects.requireNonNull(patch, "Patch must not be null");
        Objects.requireNonNull(propsClass, "Props class must not be null");

        final JsonObject propsJson = toJson(props);
        final JsonPatch jsonPatch = Json.createPatch(patch);
        final JsonObject patchedJson = jsonPatch.apply(propsJson);

        return fromJson(patchedJson, propsClass);
    }

    // ========== Private Helper Methods ==========

    /**
     * Converts a Record to JsonObject using reflection.
     */
    private JsonObject recordToJson(final Record record) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        final RecordComponent[] components = record.getClass().getRecordComponents();

        for (final RecordComponent component : components) {
            try {
                final Object value = component.getAccessor().invoke(record);
                addValueToBuilder(builder, component.getName(), value);
            } catch (final Exception e) {
                throw new RuntimeException("Failed to access record component: " + component.getName(), e);
            }
        }

        return builder.build();
    }

    /**
     * Adds a value to a JsonObjectBuilder with appropriate type conversion.
     */
    private void addValueToBuilder(final JsonObjectBuilder builder, final String name, final Object value) {
        if (value == null) {
            builder.addNull(name);
        } else if (value instanceof String) {
            builder.add(name, (String) value);
        } else if (value instanceof Integer) {
            builder.add(name, (Integer) value);
        } else if (value instanceof Long) {
            builder.add(name, (Long) value);
        } else if (value instanceof Double) {
            builder.add(name, (Double) value);
        } else if (value instanceof Float) {
            builder.add(name, ((Float) value).doubleValue());
        } else if (value instanceof Boolean) {
            builder.add(name, (Boolean) value);
        } else if (value instanceof Record) {
            builder.add(name, recordToJson((Record) value));
        } else if (value instanceof Optional<?>) {
            final Optional<?> opt = (Optional<?>) value;
            if (opt.isPresent()) {
                addValueToBuilder(builder, name, opt.get());
            } else {
                builder.addNull(name);
            }
        } else if (value instanceof Collection<?>) {
            builder.add(name, collectionToJsonArray((Collection<?>) value));
        } else if (value instanceof Map<?, ?>) {
            builder.add(name, mapToJsonObject((Map<?, ?>) value));
        } else if (value.getClass().isArray()) {
            builder.add(name, arrayToJsonArray(value));
        } else {
            // Fallback to string representation
            builder.add(name, value.toString());
        }
    }

    /**
     * Converts a Collection to JsonArray.
     */
    private JsonArray collectionToJsonArray(final Collection<?> collection) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        for (final Object item : collection) {
            addValueToArrayBuilder(builder, item);
        }
        return builder.build();
    }

    /**
     * Converts a Map to JsonObject.
     */
    private JsonObject mapToJsonObject(final Map<?, ?> map) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            addValueToBuilder(builder, String.valueOf(entry.getKey()), entry.getValue());
        }
        return builder.build();
    }

    /**
     * Converts an array to JsonArray.
     */
    private JsonArray arrayToJsonArray(final Object array) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        if (array instanceof int[]) {
            for (final int i : (int[]) array) builder.add(i);
        } else if (array instanceof long[]) {
            for (final long l : (long[]) array) builder.add(l);
        } else if (array instanceof double[]) {
            for (final double d : (double[]) array) builder.add(d);
        } else if (array instanceof float[]) {
            for (final float f : (float[]) array) builder.add(f);
        } else if (array instanceof boolean[]) {
            for (final boolean b : (boolean[]) array) builder.add(b);
        } else if (array instanceof String[]) {
            for (final String s : (String[]) array) builder.add(s);
        } else if (array instanceof Object[]) {
            for (final Object o : (Object[]) array) addValueToArrayBuilder(builder, o);
        }
        return builder.build();
    }

    /**
     * Adds a value to a JsonArrayBuilder with appropriate type conversion.
     */
    private void addValueToArrayBuilder(final JsonArrayBuilder builder, final Object value) {
        if (value == null) {
            builder.addNull();
        } else if (value instanceof String) {
            builder.add((String) value);
        } else if (value instanceof Integer) {
            builder.add((Integer) value);
        } else if (value instanceof Long) {
            builder.add((Long) value);
        } else if (value instanceof Double) {
            builder.add((Double) value);
        } else if (value instanceof Float) {
            builder.add(((Float) value).doubleValue());
        } else if (value instanceof Boolean) {
            builder.add((Boolean) value);
        } else if (value instanceof Record) {
            builder.add(recordToJson((Record) value));
        } else if (value instanceof Collection<?>) {
            builder.add(collectionToJsonArray((Collection<?>) value));
        } else if (value instanceof Map<?, ?>) {
            builder.add(mapToJsonObject((Map<?, ?>) value));
        } else {
            builder.add(value.toString());
        }
    }

    /**
     * Converts a JsonValue to the appropriate Java object type.
     */
    @SuppressWarnings("unchecked")
    private Object jsonValueToObject(final JsonValue jsonValue, final Class<?> targetType, final java.lang.reflect.Type genericType) {
        if (jsonValue == null || jsonValue.getValueType() == JsonValue.ValueType.NULL) {
            if (targetType == Optional.class) {
                return Optional.empty();
            }
            return null;
        }

        // Handle Optional wrapper type
        if (targetType == Optional.class) {
            if (genericType instanceof java.lang.reflect.ParameterizedType) {
                final java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) genericType;
                final java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length > 0) {
                    final Class<?> innerType = typeArgs[0] instanceof Class<?> ? (Class<?>) typeArgs[0] : Object.class;
                    final Object innerValue = jsonValueToObject(jsonValue, innerType, typeArgs[0]);
                    return Optional.ofNullable(innerValue);
                }
            }
            // Fallback: wrap as-is
            return Optional.of(jsonValueToObject(jsonValue, Object.class, Object.class));
        }

        // Handle List type
        if (targetType == java.util.List.class || java.util.List.class.isAssignableFrom(targetType)) {
            if (jsonValue.getValueType() == JsonValue.ValueType.ARRAY) {
                final JsonArray jsonArray = (JsonArray) jsonValue;
                final java.util.List<Object> result = new java.util.ArrayList<>();
                
                // Determine element type from generic type
                Class<?> elementType = String.class; // default
                if (genericType instanceof java.lang.reflect.ParameterizedType) {
                    final java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) genericType;
                    final java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
                    if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?>) {
                        elementType = (Class<?>) typeArgs[0];
                    }
                }
                
                for (final JsonValue element : jsonArray) {
                    result.add(jsonValueToObject(element, elementType, elementType));
                }
                return result;
            }
        }

        switch (jsonValue.getValueType()) {
            case STRING: {
                final String str = ((javax.json.JsonString) jsonValue).getString();
                if (targetType == String.class) {
                    return str;
                } else if (targetType.isEnum()) {
                    return Enum.valueOf((Class<Enum>) targetType, str);
                }
                return str;
            }
            case NUMBER: {
                final javax.json.JsonNumber num = (javax.json.JsonNumber) jsonValue;
                if (targetType == Integer.class || targetType == int.class) {
                    return num.intValue();
                } else if (targetType == Long.class || targetType == long.class) {
                    return num.longValue();
                } else if (targetType == Double.class || targetType == double.class) {
                    return num.doubleValue();
                } else if (targetType == Float.class || targetType == float.class) {
                    return (float) num.doubleValue();
                }
                return num.doubleValue();
            }
            case TRUE:
                return Boolean.TRUE;
            case FALSE:
                return Boolean.FALSE;
            case OBJECT: {
                if (targetType.isRecord()) {
                    return fromJsonToRecord((JsonObject) jsonValue, (Class<? extends Record>) targetType);
                }
                return jsonValue;
            }
            case ARRAY:
                return jsonValue;
            default:
                return null;
        }
    }

    /**
     * Converts a JsonObject to a Record type.
     */
    @SuppressWarnings("unchecked")
    private <R extends Record> R fromJsonToRecord(final JsonObject json, final Class<R> recordClass) {
        try {
            final RecordComponent[] components = recordClass.getRecordComponents();
            final Class<?>[] paramTypes = new Class<?>[components.length];
            final Object[] args = new Object[components.length];

            for (int i = 0; i < components.length; i++) {
                final RecordComponent component = components[i];
                paramTypes[i] = component.getType();
                args[i] = jsonValueToObject(json.get(component.getName()), component.getType(), component.getGenericType());
            }

            return recordClass.getDeclaredConstructor(paramTypes).newInstance(args);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to convert JSON to record: " + recordClass.getName(), e);
        }
    }

    // ========== Binary Encoding Methods ==========

    /**
     * Encodes a full props object to binary format.
     */
    private byte[] encodeToBinary(final TProps props) {
        final ByteBuffer buffer = ByteBuffer.allocate(8192); // Initial capacity
        encodeRecordToBinary(buffer, props);
        final byte[] result = new byte[buffer.position()];
        buffer.flip();
        buffer.get(result);
        return result;
    }

    /**
     * Encodes only the changed fields between previous and current props.
     */
    private byte[] encodeChangedFieldsToBinary(final TProps previous, final TProps current) {
        final ByteBuffer buffer = ByteBuffer.allocate(8192);
        final RecordComponent[] components = current.getClass().getRecordComponents();

        for (byte fieldId = 0; fieldId < components.length; fieldId++) {
            final RecordComponent component = components[fieldId];
            try {
                final Object prevValue = component.getAccessor().invoke(previous);
                final Object currValue = component.getAccessor().invoke(current);

                if (!Objects.equals(prevValue, currValue)) {
                    buffer.put(fieldId);
                    encodeValueToBinary(buffer, currValue);
                }
            } catch (final Exception e) {
                throw new RuntimeException("Failed to encode field: " + component.getName(), e);
            }
        }

        final byte[] result = new byte[buffer.position()];
        buffer.flip();
        buffer.get(result);
        return result;
    }

    /**
     * Encodes a Record to binary format.
     */
    private void encodeRecordToBinary(final ByteBuffer buffer, final Record record) {
        buffer.put(TYPE_OBJECT_START);
        final RecordComponent[] components = record.getClass().getRecordComponents();

        for (byte fieldId = 0; fieldId < components.length; fieldId++) {
            final RecordComponent component = components[fieldId];
            try {
                final Object value = component.getAccessor().invoke(record);
                buffer.put(fieldId);
                encodeValueToBinary(buffer, value);
            } catch (final Exception e) {
                throw new RuntimeException("Failed to encode field: " + component.getName(), e);
            }
        }

        buffer.put(TYPE_OBJECT_END);
    }

    /**
     * Encodes a single value to binary format.
     */
    private void encodeValueToBinary(final ByteBuffer buffer, final Object value) {
        if (value == null) {
            buffer.put(TYPE_NULL);
        } else if (value instanceof Integer) {
            buffer.put(TYPE_INT32);
            buffer.putInt((Integer) value);
        } else if (value instanceof Long) {
            buffer.put(TYPE_INT32);
            buffer.putInt(((Long) value).intValue()); // Truncate to int for binary format
        } else if (value instanceof Double) {
            buffer.put(TYPE_FLOAT64);
            buffer.putDouble((Double) value);
        } else if (value instanceof Float) {
            buffer.put(TYPE_FLOAT64);
            buffer.putDouble(((Float) value).doubleValue());
        } else if (value instanceof Boolean) {
            buffer.put(TYPE_BOOLEAN);
            buffer.put((byte) ((Boolean) value ? 1 : 0));
        } else if (value instanceof String) {
            buffer.put(TYPE_STRING);
            final byte[] bytes = ((String) value).getBytes(StandardCharsets.UTF_8);
            buffer.putShort((short) bytes.length);
            buffer.put(bytes);
        } else if (value instanceof Record) {
            encodeRecordToBinary(buffer, (Record) value);
        } else if (value instanceof Optional<?>) {
            final Optional<?> opt = (Optional<?>) value;
            if (opt.isPresent()) {
                encodeValueToBinary(buffer, opt.get());
            } else {
                buffer.put(TYPE_NULL);
            }
        } else if (value instanceof Collection<?>) {
            final Collection<?> coll = (Collection<?>) value;
            buffer.put(TYPE_ARRAY_START);
            buffer.putShort((short) coll.size());
            for (final Object item : coll) {
                encodeValueToBinary(buffer, item);
            }
            buffer.put(TYPE_ARRAY_END);
        } else {
            // Fallback to string
            buffer.put(TYPE_STRING);
            final byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
            buffer.putShort((short) bytes.length);
            buffer.put(bytes);
        }
    }

}
