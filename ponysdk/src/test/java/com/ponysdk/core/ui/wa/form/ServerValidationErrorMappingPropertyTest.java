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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for server validation error mapping.
 * <p>
 * <b>Property 11: Server Validation Error Mapping</b>
 * </p>
 * <p>
 * For any map of field names to error messages sent from the server, the PForm SHALL apply
 * each error message to the correct corresponding input component, and no error SHALL be
 * applied to a field not present in the error map.
 * </p>
 * <p>
 * <b>Validates: Requirements 9.3</b>
 * </p>
 */
@Tag("Feature: ui-library-wrapper, Property 11: Server Validation Error Mapping")
public class ServerValidationErrorMappingPropertyTest {

    // ========== Test Props Record ==========

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
     * Property 11a: For any map of field names to errors, setValidationErrors() sets
     * customValidity on the correct corresponding input component with the first error message.
     * Fields NOT in the error map retain their original customValidity.
     * <p>
     * <b>Validates: Requirements 9.3</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 11: setValidationErrors applies errors to correct fields only")
    void setValidationErrorsAppliesErrorsToCorrectFields(
            @ForAll("formWithErrors") FormWithErrors formWithErrors
    ) {
        initUIContext();
        try {
            final PForm form = new PForm();
            final List<TestFieldComponent> components = new ArrayList<>();

            for (final TestFieldProps props : formWithErrors.fieldPropsList) {
                final TestFieldComponent comp = new TestFieldComponent(props);
                components.add(comp);
                form.addField(comp);
            }

            // Record original customValidity for each field before applying errors
            final Map<String, String> originalCustomValidity = new LinkedHashMap<>();
            for (final TestFieldComponent comp : components) {
                final TestFieldProps p = comp.getCurrentProps();
                originalCustomValidity.put(p.name(), p.customValidity());
            }

            form.setValidationErrors(formWithErrors.errors);

            // Verify each field
            for (final TestFieldComponent comp : components) {
                final TestFieldProps updatedProps = comp.getCurrentProps();
                final String fieldName = updatedProps.name();
                final List<String> fieldErrors = formWithErrors.errors.get(fieldName);

                if (fieldErrors != null && !fieldErrors.isEmpty()) {
                    // Field IS in error map with errors — customValidity should be the first error
                    assertEquals(fieldErrors.get(0), updatedProps.customValidity(),
                            "Field '" + fieldName + "' should have customValidity set to first error message");
                } else if (formWithErrors.errors.containsKey(fieldName)) {
                    // Field IS in error map but with empty list — customValidity should be cleared
                    assertEquals("", updatedProps.customValidity(),
                            "Field '" + fieldName + "' with empty error list should have customValidity cleared");
                } else {
                    // Field NOT in error map — customValidity should be unchanged
                    assertEquals(originalCustomValidity.get(fieldName), updatedProps.customValidity(),
                            "Field '" + fieldName + "' not in error map should retain original customValidity");
                }
            }
        } finally {
            cleanupUIContext();
        }
    }

    /**
     * Property 11b: The error mapping is complete — every field name in the error map
     * that corresponds to a form field gets its error applied.
     * <p>
     * <b>Validates: Requirements 9.3</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 11: every field in error map gets its error applied")
    void everyFieldInErrorMapGetsErrorApplied(
            @ForAll("formWithAllFieldsInErrorMap") FormWithErrors formWithErrors
    ) {
        initUIContext();
        try {
            final PForm form = new PForm();
            final List<TestFieldComponent> components = new ArrayList<>();

            for (final TestFieldProps props : formWithErrors.fieldPropsList) {
                final TestFieldComponent comp = new TestFieldComponent(props);
                components.add(comp);
                form.addField(comp);
            }

            form.setValidationErrors(formWithErrors.errors);

            // Every field should have been touched since all are in the error map
            for (final TestFieldComponent comp : components) {
                final TestFieldProps updatedProps = comp.getCurrentProps();
                final String fieldName = updatedProps.name();
                final List<String> fieldErrors = formWithErrors.errors.get(fieldName);

                assertNotNull(fieldErrors,
                        "Field '" + fieldName + "' should be in the error map");

                if (!fieldErrors.isEmpty()) {
                    assertEquals(fieldErrors.get(0), updatedProps.customValidity(),
                            "Field '" + fieldName + "' must have first error applied");
                } else {
                    assertEquals("", updatedProps.customValidity(),
                            "Field '" + fieldName + "' with empty errors must have customValidity cleared");
                }
            }
        } finally {
            cleanupUIContext();
        }
    }

    // ========== Data Holder ==========

    record FormWithErrors(List<TestFieldProps> fieldPropsList, Map<String, List<String>> errors) {}

    // ========== Arbitraries ==========

    /**
     * Generates a form with fields and a random error map where some fields have errors
     * and some don't. The error map may also contain keys not matching any field.
     */
    @Provide
    Arbitrary<FormWithErrors> formWithErrors() {
        return fieldPropsList().flatMap(fields -> {
            final List<String> fieldNames = fields.stream().map(TestFieldProps::name).toList();

            // Generate error entries for a random subset of field names
            final Arbitrary<Map<String, List<String>>> errorsArb = Arbitraries.subsetOf(fieldNames)
                    .flatMap(subset -> {
                        if (subset.isEmpty()) {
                            return Arbitraries.just(Map.<String, List<String>>of());
                        }
                        // For each selected field, generate 1-3 error messages
                        Arbitrary<Map<String, List<String>>> mapArb = Arbitraries.just(new LinkedHashMap<>());
                        for (final String name : subset) {
                            mapArb = mapArb.flatMap(map ->
                                    errorMessages().map(msgs -> {
                                        final var copy = new LinkedHashMap<>(map);
                                        copy.put(name, msgs);
                                        return copy;
                                    })
                            );
                        }
                        return mapArb;
                    });

            return errorsArb.map(errors -> new FormWithErrors(fields, errors));
        });
    }

    /**
     * Generates a form where ALL fields appear in the error map (some with errors, some with empty list).
     */
    @Provide
    Arbitrary<FormWithErrors> formWithAllFieldsInErrorMap() {
        return fieldPropsList().flatMap(fields -> {
            Arbitrary<Map<String, List<String>>> mapArb = Arbitraries.just(new LinkedHashMap<>());
            for (final TestFieldProps field : fields) {
                mapArb = mapArb.flatMap(map ->
                        Arbitraries.oneOf(errorMessages(), Arbitraries.just(List.<String>of()))
                                .map(msgs -> {
                                    final var copy = new LinkedHashMap<>(map);
                                    copy.put(field.name(), msgs);
                                    return copy;
                                })
                );
            }
            return mapArb.map(errors -> new FormWithErrors(fields, errors));
        });
    }

    /**
     * Generates a list of 1-6 field props with unique names.
     */
    private Arbitrary<List<TestFieldProps>> fieldPropsList() {
        return fieldProps().list().ofMinSize(1).ofMaxSize(6)
                .map(ServerValidationErrorMappingPropertyTest::ensureUniqueNames);
    }

    /**
     * Generates a single field props with no initial customValidity.
     */
    private Arbitrary<TestFieldProps> fieldProps() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(15),  // name
                Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(20),  // value
                Arbitraries.of(true, false)                                     // required
        ).as((name, value, required) ->
                new TestFieldProps(name, value, required, 0, 0, 0, 0, "", "", false, false)
        );
    }

    /**
     * Generates a list of 1-3 non-empty error messages.
     */
    private Arbitrary<List<String>> errorMessages() {
        return Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50)
                .list().ofMinSize(1).ofMaxSize(3);
    }

    // ========== Helpers ==========

    private static List<TestFieldProps> ensureUniqueNames(final List<TestFieldProps> list) {
        final var result = new ArrayList<TestFieldProps>(list.size());
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
