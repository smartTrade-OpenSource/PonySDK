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

package com.ponysdk.core.ui.wa.form;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.ui.component.PComponent;
import com.ponysdk.core.ui.component.PWebComponent;

/**
 * Server-side form component that manages a list of input field children
 * with grouped validation support.
 * <p>
 * PForm encapsulates input components and provides:
 * <ul>
 *   <li>Grouped validation of all child fields</li>
 *   <li>Submit handling when all fields are valid</li>
 *   <li>Server-side validation error mapping to individual fields</li>
 *   <li>Both client-side (synchronous) and server-side (asynchronous) validation</li>
 * </ul>
 * </p>
 *
 * <p>Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.5</p>
 */
public class PForm extends PWebComponent<FormProps> {

    private static final Logger log = LoggerFactory.getLogger(PForm.class);

    private final List<PComponent<?>> fields = new ArrayList<>();
    private Consumer<Map<String, Object>> submitHandler;

    public PForm() {
        super(FormProps.defaults());
    }

    public PForm(final FormProps initialProps) {
        super(initialProps);
    }

    @Override
    protected Class<FormProps> getPropsClass() {
        return FormProps.class;
    }

    @Override
    protected String getComponentSignature() {
        return "wa-form";
    }

    /**
     * Adds an input component to the form's managed field list.
     *
     * @param inputComponent the input component to add
     * @throws NullPointerException if inputComponent is null
     */
    public void addField(final PComponent<?> inputComponent) {
        Objects.requireNonNull(inputComponent, "Input component must not be null");
        fields.add(inputComponent);
    }

    /**
     * Removes an input component from the form's managed field list.
     *
     * @param inputComponent the input component to remove
     */
    public void removeField(final PComponent<?> inputComponent) {
        fields.remove(inputComponent);
    }

    /**
     * Returns an unmodifiable view of the managed fields.
     *
     * @return the list of managed fields
     */
    public List<PComponent<?>> getFields() {
        return Collections.unmodifiableList(fields);
    }

    /**
     * Validates all child fields and returns a map of field name to error messages.
     * <p>
     * Each field's props record is inspected via reflection for validation constraints:
     * required, minlength, maxlength, min, max, pattern. The field's current value
     * is checked against these constraints.
     * </p>
     *
     * @return a map where keys are field names and values are lists of error messages;
     *         empty map if all fields are valid
     */
    public Map<String, List<String>> validate() {
        final Map<String, List<String>> errors = new LinkedHashMap<>();

        for (final PComponent<?> field : fields) {
            final Record props = field.getCurrentProps();
            if (props == null) continue;

            final String fieldName = extractFieldName(props);
            final List<String> fieldErrors = validateField(props);

            if (!fieldErrors.isEmpty()) {
                errors.put(fieldName, fieldErrors);
            }
        }

        return errors;
    }

    /**
     * Registers a handler that receives all field values when the form is submitted
     * and all fields are valid.
     * <p>
     * The handler receives a map of field name to field value. If validation fails,
     * the handler is not invoked and the validation errors are returned instead.
     * </p>
     *
     * @param handler the submit handler
     * @throws NullPointerException if handler is null
     */
    public void onSubmit(final Consumer<Map<String, Object>> handler) {
        Objects.requireNonNull(handler, "Submit handler must not be null");
        this.submitHandler = handler;
    }

    /**
     * Submits the form: validates all fields and invokes the submit handler if valid.
     *
     * @return validation errors if any fields are invalid; empty map if submission succeeded
     */
    public Map<String, List<String>> submit() {
        final Map<String, List<String>> errors = validate();

        if (errors.isEmpty() && submitHandler != null) {
            final Map<String, Object> values = collectFieldValues();
            submitHandler.accept(values);
        }

        return errors;
    }

    /**
     * Applies server-side validation error messages to corresponding input components.
     * <p>
     * For each entry in the error map, the corresponding field's customValidity
     * prop is set to the first error message, causing the client to display the error.
     * Only fields present in the error map are affected.
     * </p>
     *
     * @param errors a map of field name to error messages
     * @throws NullPointerException if errors is null
     */
    public void setValidationErrors(final Map<String, List<String>> errors) {
        Objects.requireNonNull(errors, "Errors map must not be null");

        for (final PComponent<?> field : fields) {
            final Record props = field.getCurrentProps();
            if (props == null) continue;

            final String fieldName = extractFieldName(props);
            final List<String> fieldErrors = errors.get(fieldName);

            if (fieldErrors != null && !fieldErrors.isEmpty()) {
                setCustomValidity(field, fieldErrors.get(0));
            } else if (errors.containsKey(fieldName)) {
                // Key present but empty list — clear the error
                setCustomValidity(field, "");
            }
        }
    }

    /**
     * Collects all field values into a map of field name to value.
     *
     * @return the field values map
     */
    Map<String, Object> collectFieldValues() {
        final Map<String, Object> values = new LinkedHashMap<>();

        for (final PComponent<?> field : fields) {
            final Record props = field.getCurrentProps();
            if (props == null) continue;

            final String fieldName = extractFieldName(props);
            final Object value = extractFieldValue(props, "value");
            values.put(fieldName, value);
        }

        return values;
    }

    // ========== Validation Logic ==========

    /**
     * Validates a single field's props against its constraints.
     *
     * @param props the field's current props record
     * @return list of error messages (empty if valid)
     */
    static List<String> validateField(final Record props) {
        final List<String> errors = new ArrayList<>();

        final Object value = extractFieldValue(props, "value");
        final String strValue = value != null ? value.toString() : "";

        // Check required
        if (getBooleanProp(props, "required") && strValue.isEmpty()) {
            errors.add("This field is required");
            return errors; // No point checking other constraints if empty and required
        }

        // Skip further validation if value is empty and not required
        if (strValue.isEmpty()) {
            return errors;
        }

        // Check minlength (0 means no constraint)
        final Optional<Number> minlength = getPositiveNumericProp(props, "minlength");
        if (minlength.isPresent() && strValue.length() < minlength.get().intValue()) {
            errors.add("Minimum length is " + minlength.get().intValue());
        }

        // Check maxlength (0 means no constraint)
        final Optional<Number> maxlength = getPositiveNumericProp(props, "maxlength");
        if (maxlength.isPresent() && strValue.length() > maxlength.get().intValue()) {
            errors.add("Maximum length is " + maxlength.get().intValue());
        }

        // Check min (numeric, 0 means no constraint)
        final Optional<Number> min = getPositiveNumericProp(props, "min");
        if (min.isPresent()) {
            try {
                final double numValue = Double.parseDouble(strValue);
                if (numValue < min.get().doubleValue()) {
                    errors.add("Minimum value is " + min.get());
                }
            } catch (final NumberFormatException ignored) {
                // Not a number — skip numeric validation
            }
        }

        // Check max (numeric, 0 means no constraint)
        final Optional<Number> max = getPositiveNumericProp(props, "max");
        if (max.isPresent()) {
            try {
                final double numValue = Double.parseDouble(strValue);
                if (numValue > max.get().doubleValue()) {
                    errors.add("Maximum value is " + max.get());
                }
            } catch (final NumberFormatException ignored) {
                // Not a number — skip numeric validation
            }
        }

        // Check pattern
        final Optional<String> pattern = getStringProp(props, "pattern");
        if (pattern.isPresent() && !pattern.get().isEmpty()) {
            try {
                if (!strValue.matches(pattern.get())) {
                    errors.add("Value does not match the required pattern");
                }
            } catch (final java.util.regex.PatternSyntaxException e) {
                log.warn("Invalid regex pattern '{}' on field: {}", pattern.get(), e.getMessage());
            }
        }

        return errors;
    }

    // ========== Reflection Helpers ==========

    /**
     * Extracts the field name from a props record. Uses the "name" field if present,
     * falls back to "label", then to the record class simple name.
     */
    static String extractFieldName(final Record props) {
        final Optional<String> name = getStringProp(props, "name");
        if (name.isPresent() && !name.get().isEmpty()) {
            return name.get();
        }
        final Optional<String> label = getStringProp(props, "label");
        if (label.isPresent() && !label.get().isEmpty()) {
            return label.get();
        }
        return props.getClass().getSimpleName();
    }

    /**
     * Extracts a field value from a props record by component name.
     */
    static Object extractFieldValue(final Record props, final String fieldName) {
        try {
            for (final RecordComponent rc : props.getClass().getRecordComponents()) {
                if (rc.getName().equals(fieldName)) {
                    final Object val = rc.getAccessor().invoke(props);
                    if (val instanceof Optional<?> opt) {
                        return opt.orElse(null);
                    }
                    return val;
                }
            }
        } catch (final Exception e) {
            log.debug("Could not extract field '{}' from {}: {}", fieldName, props.getClass().getSimpleName(), e.getMessage());
        }
        return null;
    }

    /**
     * Gets a boolean property from a props record.
     */
    static boolean getBooleanProp(final Record props, final String propName) {
        final Object val = extractFieldValue(props, propName);
        if (val instanceof Boolean b) {
            return b;
        }
        return false;
    }

    /**
     * Gets a numeric property from a props record, unwrapping Optional if needed.
     */
    static Optional<Number> getNumericProp(final Record props, final String propName) {
        final Object val = extractFieldValue(props, propName);
        if (val instanceof Number n) {
            return Optional.of(n);
        }
        return Optional.empty();
    }

    /**
     * Gets a numeric property, returning empty if the value is zero or negative.
     * Zero is treated as "no constraint" for validation properties.
     */
    static Optional<Number> getPositiveNumericProp(final Record props, final String propName) {
        final Optional<Number> val = getNumericProp(props, propName);
        if (val.isPresent() && val.get().doubleValue() > 0) {
            return val;
        }
        return Optional.empty();
    }

    /**
     * Gets a string property from a props record, unwrapping Optional if needed.
     */
    static Optional<String> getStringProp(final Record props, final String propName) {
        final Object val = extractFieldValue(props, propName);
        if (val instanceof String s) {
            return Optional.of(s);
        }
        return Optional.empty();
    }

    /**
     * Sets the customValidity on a field component by creating new props with the updated value.
     * Uses reflection to reconstruct the record with the new customValidity value.
     */
    @SuppressWarnings("unchecked")
    private void setCustomValidity(final PComponent<?> field, final String message) {
        final Record currentProps = field.getCurrentProps();
        if (currentProps == null) return;

        try {
            final RecordComponent[] components = currentProps.getClass().getRecordComponents();
            final Object[] args = new Object[components.length];
            final Class<?>[] types = new Class<?>[components.length];
            boolean hasCustomValidity = false;

            for (int i = 0; i < components.length; i++) {
                types[i] = components[i].getType();
                if ("customValidity".equals(components[i].getName())) {
                    args[i] = message;
                    hasCustomValidity = true;
                } else {
                    args[i] = components[i].getAccessor().invoke(currentProps);
                }
            }

            if (!hasCustomValidity) {
                log.warn("Field {} does not have a customValidity property", extractFieldName(currentProps));
                return;
            }

            final var constructor = currentProps.getClass().getDeclaredConstructor(types);
            final Record newProps = (Record) constructor.newInstance(args);
            updatePropsDirectly(field, newProps);
        } catch (final Exception e) {
            log.warn("Cannot set customValidity on field {}: {}", extractFieldName(currentProps), e.getMessage());
        }
    }

    /**
     * Updates a component's currentProps directly via reflection.
     * Used as fallback when the component is not attached to a window.
     */
    @SuppressWarnings("unchecked")
    private void updatePropsDirectly(final PComponent<?> field, final Record newProps) {
        try {
            final var currentField = PComponent.class.getDeclaredField("currentProps");
            currentField.setAccessible(true);
            final var previousField = PComponent.class.getDeclaredField("previousProps");
            previousField.setAccessible(true);
            previousField.set(field, currentField.get(field));
            currentField.set(field, newProps);
        } catch (final Exception e) {
            log.warn("Cannot update props directly on field: {}", e.getMessage());
        }
    }
}
