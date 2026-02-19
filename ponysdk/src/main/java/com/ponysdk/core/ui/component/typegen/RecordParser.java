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

package com.ponysdk.core.ui.component.typegen;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Parses Java Record definitions using reflection to extract field names, types, and annotations.
 * <p>
 * The RecordParser is used by the type generation system to analyze Java Record classes
 * and produce structured information that can be used to generate TypeScript interfaces.
 * </p>
 * <p>
 * Supported types:
 * <ul>
 *   <li>Primitives: int, long, double, float, boolean, byte, short, char</li>
 *   <li>Boxed primitives: Integer, Long, Double, Float, Boolean, Byte, Short, Character</li>
 *   <li>String</li>
 *   <li>Optional fields</li>
 *   <li>List/Collection types</li>
 *   <li>Map types</li>
 *   <li>Nested Records (recursive)</li>
 *   <li>Enums</li>
 * </ul>
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * RecordParser parser = new RecordParser();
 * RecordInfo info = parser.parse(MyProps.class);
 * 
 * // Access parsed information
 * System.out.println("Record: " + info.name());
 * for (FieldInfo field : info.fields()) {
 *     System.out.println("  Field: " + field.name() + " : " + field.typeInfo().typeName());
 * }
 * }</pre>
 *
 * @see RecordInfo
 * @see FieldInfo
 * @see TypeInfo
 */
public class RecordParser {

    /**
     * Cache of already parsed records to handle recursive types and improve performance.
     */
    private final Map<Class<?>, RecordInfo> cache = new HashMap<>();

    /**
     * Set of records currently being parsed to detect circular references.
     */
    private final Set<Class<?>> inProgress = new HashSet<>();

    /**
     * Creates a new RecordParser instance.
     */
    public RecordParser() {
    }

    /**
     * Parses a Java Record class and extracts its structure.
     *
     * @param recordClass the Record class to parse
     * @return the parsed RecordInfo containing field information
     * @throws IllegalArgumentException if the class is not a Record
     * @throws NullPointerException if recordClass is null
     */
    public RecordInfo parse(final Class<? extends Record> recordClass) {
        Objects.requireNonNull(recordClass, "Record class must not be null");

        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException("Class must be a Record: " + recordClass.getName());
        }

        // Check cache first
        if (cache.containsKey(recordClass)) {
            return cache.get(recordClass);
        }

        // Detect circular references
        if (inProgress.contains(recordClass)) {
            // Return a placeholder for circular reference
            return new RecordInfo(
                    recordClass.getSimpleName(),
                    recordClass.getName(),
                    List.of(),
                    getClassAnnotations(recordClass),
                    true // isCircularReference
            );
        }

        inProgress.add(recordClass);

        try {
            final List<FieldInfo> fields = parseFields(recordClass);
            final List<AnnotationInfo> annotations = getClassAnnotations(recordClass);

            final RecordInfo info = new RecordInfo(
                    recordClass.getSimpleName(),
                    recordClass.getName(),
                    fields,
                    annotations,
                    false
            );

            cache.put(recordClass, info);
            return info;
        } finally {
            inProgress.remove(recordClass);
        }
    }

    /**
     * Parses all fields (record components) of a Record class.
     *
     * @param recordClass the Record class to parse
     * @return list of FieldInfo for each record component
     */
    private List<FieldInfo> parseFields(final Class<? extends Record> recordClass) {
        final RecordComponent[] components = recordClass.getRecordComponents();
        final List<FieldInfo> fields = new ArrayList<>(components.length);

        for (final RecordComponent component : components) {
            fields.add(parseField(component));
        }

        return fields;
    }

    /**
     * Parses a single record component into FieldInfo.
     *
     * @param component the record component to parse
     * @return the parsed FieldInfo
     */
    private FieldInfo parseField(final RecordComponent component) {
        final String name = component.getName();
        final Class<?> type = component.getType();
        final Type genericType = component.getGenericType();
        final List<AnnotationInfo> annotations = getFieldAnnotations(component);

        final TypeInfo typeInfo = parseType(type, genericType);

        return new FieldInfo(name, typeInfo, annotations);
    }

    /**
     * Parses a type into TypeInfo, handling generics and nested types.
     *
     * @param rawType the raw class type
     * @param genericType the generic type (may contain type parameters)
     * @return the parsed TypeInfo
     */
    private TypeInfo parseType(final Class<?> rawType, final Type genericType) {
        // Handle Optional
        if (rawType == Optional.class) {
            final TypeInfo innerType = extractGenericTypeArg(genericType, 0);
            return new TypeInfo(
                    TypeKind.OPTIONAL,
                    rawType.getSimpleName(),
                    rawType.getName(),
                    innerType,
                    null,
                    null,
                    true
            );
        }

        // Handle List/Collection
        if (List.class.isAssignableFrom(rawType) || Collection.class.isAssignableFrom(rawType)) {
            final TypeInfo elementType = extractGenericTypeArg(genericType, 0);
            return new TypeInfo(
                    TypeKind.LIST,
                    rawType.getSimpleName(),
                    rawType.getName(),
                    elementType,
                    null,
                    null,
                    false
            );
        }

        // Handle Map
        if (Map.class.isAssignableFrom(rawType)) {
            final TypeInfo keyType = extractGenericTypeArg(genericType, 0);
            final TypeInfo valueType = extractGenericTypeArg(genericType, 1);
            return new TypeInfo(
                    TypeKind.MAP,
                    rawType.getSimpleName(),
                    rawType.getName(),
                    null,
                    keyType,
                    valueType,
                    false
            );
        }

        // Handle primitives and boxed primitives
        if (isPrimitiveOrBoxed(rawType)) {
            return new TypeInfo(
                    TypeKind.PRIMITIVE,
                    rawType.getSimpleName(),
                    rawType.getName(),
                    null,
                    null,
                    null,
                    false
            );
        }

        // Handle String
        if (rawType == String.class) {
            return new TypeInfo(
                    TypeKind.STRING,
                    "String",
                    "java.lang.String",
                    null,
                    null,
                    null,
                    false
            );
        }

        // Handle Enum
        if (rawType.isEnum()) {
            return new TypeInfo(
                    TypeKind.ENUM,
                    rawType.getSimpleName(),
                    rawType.getName(),
                    null,
                    null,
                    null,
                    false
            );
        }

        // Handle nested Record
        if (rawType.isRecord()) {
            @SuppressWarnings("unchecked")
            final Class<? extends Record> recordType = (Class<? extends Record>) rawType;
            final RecordInfo nestedInfo = parse(recordType);
            return new TypeInfo(
                    TypeKind.RECORD,
                    rawType.getSimpleName(),
                    rawType.getName(),
                    null,
                    null,
                    null,
                    false,
                    nestedInfo
            );
        }

        // Handle arrays
        if (rawType.isArray()) {
            final Class<?> componentType = rawType.getComponentType();
            final TypeInfo elementType = parseType(componentType, componentType);
            return new TypeInfo(
                    TypeKind.ARRAY,
                    rawType.getSimpleName(),
                    rawType.getName(),
                    elementType,
                    null,
                    null,
                    false
            );
        }

        // Unknown type - treat as object
        return new TypeInfo(
                TypeKind.OBJECT,
                rawType.getSimpleName(),
                rawType.getName(),
                null,
                null,
                null,
                false
        );
    }

    /**
     * Extracts a generic type argument at the specified index.
     *
     * @param genericType the generic type
     * @param index the index of the type argument
     * @return the TypeInfo for the type argument, or OBJECT if not available
     */
    private TypeInfo extractGenericTypeArg(final Type genericType, final int index) {
        if (genericType instanceof ParameterizedType) {
            final ParameterizedType paramType = (ParameterizedType) genericType;
            final Type[] typeArgs = paramType.getActualTypeArguments();

            if (index < typeArgs.length) {
                final Type typeArg = typeArgs[index];

                if (typeArg instanceof Class<?>) {
                    return parseType((Class<?>) typeArg, typeArg);
                } else if (typeArg instanceof ParameterizedType) {
                    final ParameterizedType paramTypeArg = (ParameterizedType) typeArg;
                    final Class<?> rawType = (Class<?>) paramTypeArg.getRawType();
                    return parseType(rawType, paramTypeArg);
                }
            }
        }

        // Default to Object if type argument cannot be determined
        return new TypeInfo(
                TypeKind.OBJECT,
                "Object",
                "java.lang.Object",
                null,
                null,
                null,
                false
        );
    }

    /**
     * Checks if a type is a primitive or boxed primitive.
     *
     * @param type the type to check
     * @return true if the type is a primitive or boxed primitive
     */
    private boolean isPrimitiveOrBoxed(final Class<?> type) {
        return type.isPrimitive()
                || type == Integer.class
                || type == Long.class
                || type == Double.class
                || type == Float.class
                || type == Boolean.class
                || type == Byte.class
                || type == Short.class
                || type == Character.class;
    }

    /**
     * Extracts annotations from a Record class.
     *
     * @param recordClass the Record class
     * @return list of AnnotationInfo
     */
    private List<AnnotationInfo> getClassAnnotations(final Class<?> recordClass) {
        final Annotation[] annotations = recordClass.getAnnotations();
        return convertAnnotations(annotations);
    }

    /**
     * Extracts annotations from a record component (field).
     *
     * @param component the record component
     * @return list of AnnotationInfo
     */
    private List<AnnotationInfo> getFieldAnnotations(final RecordComponent component) {
        final Annotation[] annotations = component.getAnnotations();
        return convertAnnotations(annotations);
    }

    /**
     * Converts an array of Annotation objects to AnnotationInfo list.
     *
     * @param annotations the annotations to convert
     * @return list of AnnotationInfo
     */
    private List<AnnotationInfo> convertAnnotations(final Annotation[] annotations) {
        final List<AnnotationInfo> result = new ArrayList<>(annotations.length);

        for (final Annotation annotation : annotations) {
            final Map<String, Object> attributes = extractAnnotationAttributes(annotation);
            result.add(new AnnotationInfo(
                    annotation.annotationType().getSimpleName(),
                    annotation.annotationType().getName(),
                    attributes
            ));
        }

        return result;
    }

    /**
     * Extracts attribute values from an annotation.
     *
     * @param annotation the annotation
     * @return map of attribute names to values
     */
    private Map<String, Object> extractAnnotationAttributes(final Annotation annotation) {
        final Map<String, Object> attributes = new HashMap<>();

        try {
            for (final java.lang.reflect.Method method : annotation.annotationType().getDeclaredMethods()) {
                if (method.getParameterCount() == 0 && method.getReturnType() != void.class) {
                    final Object value = method.invoke(annotation);
                    attributes.put(method.getName(), value);
                }
            }
        } catch (final Exception e) {
            // Ignore annotation attribute extraction errors
        }

        return attributes;
    }

    /**
     * Clears the parser cache. Useful for testing or when re-parsing is needed.
     */
    public void clearCache() {
        cache.clear();
        inProgress.clear();
    }

    // ========== Data Classes ==========

    /**
     * Represents parsed information about a Java Record.
     *
     * @param name the simple name of the Record class
     * @param fullName the fully qualified name of the Record class
     * @param fields the list of fields in the Record
     * @param annotations the list of annotations on the Record class
     * @param isCircularReference true if this is a placeholder for a circular reference
     */
    public record RecordInfo(
            String name,
            String fullName,
            List<FieldInfo> fields,
            List<AnnotationInfo> annotations,
            boolean isCircularReference
    ) {
        /**
         * Creates a RecordInfo with validation.
         */
        public RecordInfo {
            Objects.requireNonNull(name, "Name must not be null");
            Objects.requireNonNull(fullName, "Full name must not be null");
            fields = fields != null ? List.copyOf(fields) : List.of();
            annotations = annotations != null ? List.copyOf(annotations) : List.of();
        }

        /**
         * Gets a field by name.
         *
         * @param fieldName the name of the field
         * @return Optional containing the FieldInfo if found
         */
        public Optional<FieldInfo> getField(final String fieldName) {
            return fields.stream()
                    .filter(f -> f.name().equals(fieldName))
                    .findFirst();
        }

        /**
         * Checks if the Record has a field with the given name.
         *
         * @param fieldName the name of the field
         * @return true if the field exists
         */
        public boolean hasField(final String fieldName) {
            return fields.stream().anyMatch(f -> f.name().equals(fieldName));
        }
    }

    /**
     * Represents parsed information about a field in a Record.
     *
     * @param name the name of the field
     * @param typeInfo the type information for the field
     * @param annotations the list of annotations on the field
     */
    public record FieldInfo(
            String name,
            TypeInfo typeInfo,
            List<AnnotationInfo> annotations
    ) {
        /**
         * Creates a FieldInfo with validation.
         */
        public FieldInfo {
            Objects.requireNonNull(name, "Name must not be null");
            Objects.requireNonNull(typeInfo, "TypeInfo must not be null");
            annotations = annotations != null ? List.copyOf(annotations) : List.of();
        }

        /**
         * Checks if the field is optional (wrapped in Optional).
         *
         * @return true if the field type is Optional
         */
        public boolean isOptional() {
            return typeInfo.isOptional();
        }

        /**
         * Checks if the field is a collection type (List, Collection).
         *
         * @return true if the field type is a collection
         */
        public boolean isCollection() {
            return typeInfo.kind() == TypeKind.LIST;
        }

        /**
         * Checks if the field is a nested Record.
         *
         * @return true if the field type is a Record
         */
        public boolean isNestedRecord() {
            return typeInfo.kind() == TypeKind.RECORD;
        }
    }

    /**
     * Represents type information for a field.
     *
     * @param kind the kind of type (PRIMITIVE, STRING, RECORD, etc.)
     * @param typeName the simple name of the type
     * @param fullTypeName the fully qualified name of the type
     * @param elementType for Optional/List/Array, the element type
     * @param keyType for Map, the key type
     * @param valueType for Map, the value type
     * @param isOptional true if this type is Optional
     * @param nestedRecordInfo for RECORD kind, the parsed RecordInfo
     */
    public record TypeInfo(
            TypeKind kind,
            String typeName,
            String fullTypeName,
            TypeInfo elementType,
            TypeInfo keyType,
            TypeInfo valueType,
            boolean isOptional,
            RecordInfo nestedRecordInfo
    ) {
        /**
         * Creates a TypeInfo without nested record info.
         */
        public TypeInfo(
                final TypeKind kind,
                final String typeName,
                final String fullTypeName,
                final TypeInfo elementType,
                final TypeInfo keyType,
                final TypeInfo valueType,
                final boolean isOptional
        ) {
            this(kind, typeName, fullTypeName, elementType, keyType, valueType, isOptional, null);
        }

        /**
         * Creates a TypeInfo with validation.
         */
        public TypeInfo {
            Objects.requireNonNull(kind, "Kind must not be null");
            Objects.requireNonNull(typeName, "Type name must not be null");
            Objects.requireNonNull(fullTypeName, "Full type name must not be null");
        }

        /**
         * Checks if this type represents a primitive or boxed primitive.
         *
         * @return true if this is a primitive type
         */
        public boolean isPrimitive() {
            return kind == TypeKind.PRIMITIVE;
        }

        /**
         * Checks if this type represents a String.
         *
         * @return true if this is a String type
         */
        public boolean isString() {
            return kind == TypeKind.STRING;
        }

        /**
         * Checks if this type represents a nested Record.
         *
         * @return true if this is a Record type
         */
        public boolean isRecord() {
            return kind == TypeKind.RECORD;
        }

        /**
         * Checks if this type represents a List or Collection.
         *
         * @return true if this is a List/Collection type
         */
        public boolean isList() {
            return kind == TypeKind.LIST;
        }

        /**
         * Checks if this type represents a Map.
         *
         * @return true if this is a Map type
         */
        public boolean isMap() {
            return kind == TypeKind.MAP;
        }

        /**
         * Checks if this type represents an Enum.
         *
         * @return true if this is an Enum type
         */
        public boolean isEnum() {
            return kind == TypeKind.ENUM;
        }
    }

    /**
     * Enumeration of type kinds supported by the parser.
     */
    public enum TypeKind {
        /** Primitive types (int, long, double, etc.) and their boxed equivalents */
        PRIMITIVE,
        /** String type */
        STRING,
        /** Nested Record type */
        RECORD,
        /** Optional wrapper type */
        OPTIONAL,
        /** List or Collection type */
        LIST,
        /** Map type */
        MAP,
        /** Array type */
        ARRAY,
        /** Enum type */
        ENUM,
        /** Unknown or generic Object type */
        OBJECT
    }

    /**
     * Represents information about an annotation.
     *
     * @param name the simple name of the annotation
     * @param fullName the fully qualified name of the annotation
     * @param attributes the annotation attributes as a map
     */
    public record AnnotationInfo(
            String name,
            String fullName,
            Map<String, Object> attributes
    ) {
        /**
         * Creates an AnnotationInfo with validation.
         */
        public AnnotationInfo {
            Objects.requireNonNull(name, "Name must not be null");
            Objects.requireNonNull(fullName, "Full name must not be null");
            attributes = attributes != null ? Map.copyOf(attributes) : Map.of();
        }

        /**
         * Gets an attribute value by name.
         *
         * @param attributeName the name of the attribute
         * @return Optional containing the attribute value if present
         */
        public Optional<Object> getAttribute(final String attributeName) {
            return Optional.ofNullable(attributes.get(attributeName));
        }

        /**
         * Gets an attribute value as a specific type.
         *
         * @param attributeName the name of the attribute
         * @param type the expected type of the attribute
         * @param <T> the type parameter
         * @return Optional containing the typed attribute value if present and of correct type
         */
        @SuppressWarnings("unchecked")
        public <T> Optional<T> getAttribute(final String attributeName, final Class<T> type) {
            final Object value = attributes.get(attributeName);
            if (value != null && type.isInstance(value)) {
                return Optional.of((T) value);
            }
            return Optional.empty();
        }
    }
}
