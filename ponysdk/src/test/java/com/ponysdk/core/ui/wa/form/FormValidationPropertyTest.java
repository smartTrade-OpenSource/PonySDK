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

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.component.PWebComponent;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.test.ModelWriterForTest;
import net.jqwik.api.*;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.jupiter.api.Tag;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for form validation completeness.
 * <p>
 * <b>Property 10: Form Validation Completeness</b>
 * </p>
 * <p>
 * For any PForm containing N input fields with validation constraints, submitting the form
 * SHALL validate all N fields and produce a validation result map with an entry for each
 * invalid field. When all fields are valid, a single submission event SHALL be dispatched
 * containing all N field values.
 * </p>
 * <p>
 * <b>Validates: Requirements 9.2, 9.5</b>
 * </p>
 */
@Tag("Feature: ui-library-wrapper, Property 10: Form Validation Completeness")
public class FormValidationPropertyTest {

    // ========== Test Props Record ==========

    /**
     * Props record with all validation-relevant fields for property testing.
     */
    public record TestFieldProps(
        String name,
        String value,
        boolean required,
        double minlength,
        double maxlength,
        double min,
        double max,
        String pattern,
        String customValidity,
        boolean disabled,
        boolean readonly
    ) {}

    /** Test component wrapping TestFieldProps. */
    static class TestFieldComponent extends PWebComponent<TestFieldProps> {
        TestFieldComponent(final TestFieldProps props) {
            super(props);
        }

        @Override
        protected Class<TestFieldProps> getPropsClass() {
            return TestFieldProps.class;
        }

        @Override
        protected String getComponentSignature() {
            return "wa-input";
        }
    }

    // ========== UIContext Setup ==========

    private void initUIContext() {
        final WebSocket socket = Mockito.mock(WebSocket.class);
        final ServletUpgradeRequest request = Mockito.mock(ServletUpgradeRequest.class);
        final TxnContext context = Mockito.spy(new TxnContext(socket));
        final ModelWriter modelWriter = new ModelWriterForTest();
        final Application application = Mockito.mock(Application.class);
        context.setApplication(application);
        final ApplicationConfiguration configuration = Mockito.mock(ApplicationConfiguration.class);
        Txn.get().begin(context);
        final UIContext uiContext = Mockito.spy(new UIContext(socket, context, configuration, request));
        Mockito.when(uiContext.getWriter()).thenReturn(modelWriter);
        UIContext.setCurrent(uiContext);
    }

    private void cleanupUIContext() {
        Txn.get().commit();
    }

    // ========== Property Tests ==========

    /**
     * Property 10a: For any PForm with N fields, validate() produces a result map
     * that contains an entry for every invalid field and no entries for valid fields.
     * The total number of entries in the error map plus the number of valid fields equals N.
     * <p>
     * <b>Validates: Requirements 9.2</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 10: validate() covers all N fields — errors only for invalid fields")
    void validateCoversAllFields(@ForAll("fieldPropsList") List<TestFieldProps> fieldPropsList) {
        initUIContext();
        try {
            final PForm form = new PForm();
            for (final TestFieldProps props : fieldPropsList) {
                form.addField(new TestFieldComponent(props));
            }

            final Map<String, List<String>> errors = form.validate();

            // Every field must be accounted for: either in the error map or valid
            for (final TestFieldProps props : fieldPropsList) {
                final List<String> fieldErrors = PForm.validateField(props);
                if (fieldErrors.isEmpty()) {
                    assertFalse(errors.containsKey(props.name()),
                            "Valid field '" + props.name() + "' should NOT appear in error map");
                } else {
                    assertTrue(errors.containsKey(props.name()),
                            "Invalid field '" + props.name() + "' MUST appear in error map");
                    assertEquals(fieldErrors, errors.get(props.name()),
                            "Error messages for '" + props.name() + "' must match validateField() output");
                }
            }

            // Error map must not contain any keys that aren't field names
            for (final String key : errors.keySet()) {
                assertTrue(fieldPropsList.stream().anyMatch(p -> p.name().equals(key)),
                        "Error map key '" + key + "' must correspond to a field in the form");
            }
        } finally {
            cleanupUIContext();
        }
    }

    /**
     * Property 10b: When all fields are valid, submit() invokes the handler with
     * a map containing all N field values, and returns an empty error map.
     * <p>
     * <b>Validates: Requirements 9.5</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 10: submit() with all valid fields dispatches single event with all N values")
    void submitWithAllValidFieldsDispatchesSingleEvent(
            @ForAll("validFieldPropsList") List<TestFieldProps> fieldPropsList
    ) {
        initUIContext();
        try {
            final PForm form = new PForm();
            for (final TestFieldProps props : fieldPropsList) {
                form.addField(new TestFieldComponent(props));
            }

            final AtomicReference<Map<String, Object>> received = new AtomicReference<>();
            form.onSubmit(received::set);

            final Map<String, List<String>> errors = form.submit();

            assertTrue(errors.isEmpty(), "All fields are valid — errors must be empty");
            assertNotNull(received.get(), "Submit handler must be invoked when all fields are valid");
            assertEquals(fieldPropsList.size(), received.get().size(),
                    "Handler must receive exactly N field values");

            for (final TestFieldProps props : fieldPropsList) {
                assertTrue(received.get().containsKey(props.name()),
                        "Handler values must contain field '" + props.name() + "'");
                assertEquals(props.value(), received.get().get(props.name()),
                        "Handler value for '" + props.name() + "' must match the field's value");
            }
        } finally {
            cleanupUIContext();
        }
    }

    /**
     * Property 10c: When at least one field is invalid, submit() does NOT invoke
     * the handler and returns a non-empty error map.
     * <p>
     * <b>Validates: Requirements 9.2</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 10: submit() with invalid fields does not invoke handler")
    void submitWithInvalidFieldsDoesNotInvokeHandler(
            @ForAll("fieldPropsListWithAtLeastOneInvalid") List<TestFieldProps> fieldPropsList
    ) {
        initUIContext();
        try {
            final PForm form = new PForm();
            for (final TestFieldProps props : fieldPropsList) {
                form.addField(new TestFieldComponent(props));
            }

            final AtomicReference<Map<String, Object>> received = new AtomicReference<>();
            form.onSubmit(received::set);

            final Map<String, List<String>> errors = form.submit();

            assertFalse(errors.isEmpty(), "At least one field is invalid — errors must not be empty");
            assertNull(received.get(), "Submit handler must NOT be invoked when validation fails");
        } finally {
            cleanupUIContext();
        }
    }

    // ========== Arbitraries ==========

    /**
     * Generates a list of 1-8 field props with unique names and random validation constraints.
     * Fields may be valid or invalid.
     */
    @Provide
    Arbitrary<List<TestFieldProps>> fieldPropsList() {
        return fieldProps().list().ofMinSize(1).ofMaxSize(8)
                .map(FormValidationPropertyTest::ensureUniqueNames);
    }

    /**
     * Generates a list of 1-8 field props where ALL fields are valid.
     */
    @Provide
    Arbitrary<List<TestFieldProps>> validFieldPropsList() {
        return validFieldProps().list().ofMinSize(1).ofMaxSize(8)
                .map(FormValidationPropertyTest::ensureUniqueNames);
    }

    /**
     * Generates a list of 1-8 field props where at least one field is invalid.
     */
    @Provide
    Arbitrary<List<TestFieldProps>> fieldPropsListWithAtLeastOneInvalid() {
        return Combinators.combine(
                invalidFieldProps(),
                fieldProps().list().ofMinSize(0).ofMaxSize(7)
        ).as((invalid, rest) -> {
            final var list = new java.util.ArrayList<>(rest);
            list.add(0, invalid);
            return ensureUniqueNames(list);
        });
    }

    /**
     * Generates a single field props with random constraints — may be valid or invalid.
     */
    private Arbitrary<TestFieldProps> fieldProps() {
        return Arbitraries.oneOf(validFieldProps(), invalidFieldProps());
    }

    /**
     * Generates a field props that is guaranteed to be valid.
     * - Non-empty value satisfies required
     * - Value length within minlength/maxlength bounds
     * - Numeric value within min/max bounds
     * - No pattern constraint (0-length pattern means no constraint)
     */
    private Arbitrary<TestFieldProps> validFieldProps() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(15),  // name
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),  // value (non-empty, satisfies minlength)
                Arbitraries.of(true, false)                                     // required
        ).as((name, value, required) ->
                new TestFieldProps(name, value, required, 0, 0, 0, 0, "", "", false, false)
        );
    }

    /**
     * Generates a field props that is guaranteed to be invalid.
     * Uses one of several strategies to ensure invalidity.
     */
    private Arbitrary<TestFieldProps> invalidFieldProps() {
        return Arbitraries.oneOf(
                requiredEmptyField(),
                minLengthViolation(),
                maxLengthViolation()
        );
    }

    /** Required field with empty value. */
    private Arbitrary<TestFieldProps> requiredEmptyField() {
        return Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(15)
                .map(name -> new TestFieldProps(name, "", true, 0, 0, 0, 0, "", "", false, false));
    }

    /** Non-empty value shorter than minlength. */
    private Arbitrary<TestFieldProps> minLengthViolation() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(15),  // name
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(2),   // short value
                Arbitraries.integers().between(5, 20)                           // minlength > value length
        ).as((name, value, minLen) ->
                new TestFieldProps(name, value, false, minLen, 0, 0, 0, "", "", false, false)
        );
    }

    /** Non-empty value longer than maxlength. */
    private Arbitrary<TestFieldProps> maxLengthViolation() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(15),   // name
                Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(20),  // long value
                Arbitraries.integers().between(1, 5)                             // maxlength < value length
        ).as((name, value, maxLen) ->
                new TestFieldProps(name, value, false, 0, maxLen, 0, 0, "", "", false, false)
        );
    }

    // ========== Helpers ==========

    /**
     * Ensures all field props in the list have unique names by appending an index suffix.
     */
    private static List<TestFieldProps> ensureUniqueNames(final List<TestFieldProps> list) {
        final var result = new java.util.ArrayList<TestFieldProps>(list.size());
        for (int i = 0; i < list.size(); i++) {
            final TestFieldProps p = list.get(i);
            result.add(new TestFieldProps(
                    p.name() + "_" + i, p.value(), p.required(),
                    p.minlength(), p.maxlength(), p.min(), p.max(),
                    p.pattern(), p.customValidity(), p.disabled(), p.readonly()
            ));
        }
        return result;
    }
}
