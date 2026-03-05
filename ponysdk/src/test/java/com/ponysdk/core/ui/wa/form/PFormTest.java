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
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PForm}.
 *
 * <p>Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.5</p>
 */
class PFormTest {

    // ========== Test Props Record ==========

    public record TestInputProps(
        String name,
        String label,
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
    ) {
        static TestInputProps of(final String name, final String value) {
            return new TestInputProps(name, "", value, false, 0, 0, 0, 0, "", "", false, false);
        }

        static TestInputProps required(final String name, final String value) {
            return new TestInputProps(name, "", value, true, 0, 0, 0, 0, "", "", false, false);
        }

        static TestInputProps withMinLength(final String name, final String value, final int minLen) {
            return new TestInputProps(name, "", value, false, minLen, 0, 0, 0, "", "", false, false);
        }

        static TestInputProps withMaxLength(final String name, final String value, final int maxLen) {
            return new TestInputProps(name, "", value, false, 0, maxLen, 0, 0, "", "", false, false);
        }

        static TestInputProps withMinMax(final String name, final String value, final double min, final double max) {
            return new TestInputProps(name, "", value, false, 0, 0, min, max, "", "", false, false);
        }

        static TestInputProps withPattern(final String name, final String value, final String pattern) {
            return new TestInputProps(name, "", value, false, 0, 0, 0, 0, pattern, "", false, false);
        }
    }

    /** Test component wrapping TestInputProps. */
    static class TestInputComponent extends PWebComponent<TestInputProps> {
        TestInputComponent(final TestInputProps props) {
            super(props);
        }

        @Override
        protected Class<TestInputProps> getPropsClass() {
            return TestInputProps.class;
        }

        @Override
        protected String getComponentSignature() {
            return "wa-input";
        }
    }

    private PForm form;

    @BeforeEach
    void setUp() {
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

        form = new PForm();
    }

    @AfterEach
    void tearDown() {
        Txn.get().commit();
    }

    // ========== addField / removeField / getFields ==========

    @Test
    void addField_addsComponentToManagedFields() {
        final var field = new TestInputComponent(TestInputProps.of("email", ""));
        form.addField(field);
        assertEquals(1, form.getFields().size());
        assertSame(field, form.getFields().get(0));
    }

    @Test
    void addField_throwsOnNull() {
        assertThrows(NullPointerException.class, () -> form.addField(null));
    }

    @Test
    void removeField_removesComponentFromManagedFields() {
        final var field = new TestInputComponent(TestInputProps.of("email", ""));
        form.addField(field);
        form.removeField(field);
        assertTrue(form.getFields().isEmpty());
    }

    @Test
    void getFields_returnsUnmodifiableList() {
        form.addField(new TestInputComponent(TestInputProps.of("email", "")));
        assertThrows(UnsupportedOperationException.class, () -> form.getFields().add(null));
    }

    // ========== validate() ==========

    @Test
    void validate_returnsEmptyMapWhenAllFieldsValid() {
        form.addField(new TestInputComponent(TestInputProps.of("name", "John")));
        form.addField(new TestInputComponent(TestInputProps.of("email", "john@example.com")));
        assertTrue(form.validate().isEmpty());
    }

    @Test
    void validate_detectsRequiredEmptyField() {
        form.addField(new TestInputComponent(TestInputProps.required("name", "")));
        final Map<String, List<String>> errors = form.validate();
        assertTrue(errors.containsKey("name"));
        assertTrue(errors.get("name").get(0).contains("required"));
    }

    @Test
    void validate_skipsNonRequiredEmptyField() {
        form.addField(new TestInputComponent(TestInputProps.of("optional", "")));
        assertTrue(form.validate().isEmpty());
    }

    @Test
    void validate_detectsMinLengthViolation() {
        form.addField(new TestInputComponent(TestInputProps.withMinLength("name", "ab", 3)));
        final Map<String, List<String>> errors = form.validate();
        assertTrue(errors.containsKey("name"));
        assertTrue(errors.get("name").get(0).contains("Minimum length"));
    }

    @Test
    void validate_detectsMaxLengthViolation() {
        form.addField(new TestInputComponent(TestInputProps.withMaxLength("name", "toolong", 3)));
        final Map<String, List<String>> errors = form.validate();
        assertTrue(errors.containsKey("name"));
        assertTrue(errors.get("name").get(0).contains("Maximum length"));
    }

    @Test
    void validate_detectsMinValueViolation() {
        form.addField(new TestInputComponent(TestInputProps.withMinMax("age", "5", 10, 100)));
        final Map<String, List<String>> errors = form.validate();
        assertTrue(errors.containsKey("age"));
        assertTrue(errors.get("age").get(0).contains("Minimum value"));
    }

    @Test
    void validate_detectsMaxValueViolation() {
        form.addField(new TestInputComponent(TestInputProps.withMinMax("age", "150", 10, 100)));
        final Map<String, List<String>> errors = form.validate();
        assertTrue(errors.containsKey("age"));
        assertTrue(errors.get("age").get(0).contains("Maximum value"));
    }

    @Test
    void validate_detectsPatternViolation() {
        form.addField(new TestInputComponent(TestInputProps.withPattern("code", "abc", "\\d+")));
        final Map<String, List<String>> errors = form.validate();
        assertTrue(errors.containsKey("code"));
        assertTrue(errors.get("code").get(0).contains("pattern"));
    }

    @Test
    void validate_passesPatternMatch() {
        form.addField(new TestInputComponent(TestInputProps.withPattern("code", "123", "\\d+")));
        assertTrue(form.validate().isEmpty());
    }

    @Test
    void validate_multipleFieldsMultipleErrors() {
        form.addField(new TestInputComponent(TestInputProps.required("name", "")));
        form.addField(new TestInputComponent(TestInputProps.withMinLength("email", "a", 5)));
        form.addField(new TestInputComponent(TestInputProps.of("optional", "valid")));
        final Map<String, List<String>> errors = form.validate();
        assertEquals(2, errors.size());
        assertTrue(errors.containsKey("name"));
        assertTrue(errors.containsKey("email"));
        assertFalse(errors.containsKey("optional"));
    }

    @Test
    void validate_skipsNumericCheckForNonNumericValues() {
        form.addField(new TestInputComponent(TestInputProps.withMinMax("field", "abc", 10, 100)));
        assertTrue(form.validate().isEmpty());
    }

    @Test
    void validate_zeroMinLengthDoesNotTriggerError() {
        form.addField(new TestInputComponent(TestInputProps.withMinLength("name", "a", 0)));
        assertTrue(form.validate().isEmpty());
    }

    // ========== onSubmit() / submit() ==========

    @Test
    void onSubmit_throwsOnNullHandler() {
        assertThrows(NullPointerException.class, () -> form.onSubmit(null));
    }

    @Test
    void submit_invokesHandlerWhenAllFieldsValid() {
        form.addField(new TestInputComponent(TestInputProps.of("name", "John")));
        form.addField(new TestInputComponent(TestInputProps.of("email", "john@test.com")));
        final AtomicReference<Map<String, Object>> received = new AtomicReference<>();
        form.onSubmit(received::set);
        final Map<String, List<String>> errors = form.submit();
        assertTrue(errors.isEmpty());
        assertNotNull(received.get());
        assertEquals("John", received.get().get("name"));
        assertEquals("john@test.com", received.get().get("email"));
    }

    @Test
    void submit_doesNotInvokeHandlerWhenValidationFails() {
        form.addField(new TestInputComponent(TestInputProps.required("name", "")));
        final AtomicReference<Map<String, Object>> received = new AtomicReference<>();
        form.onSubmit(received::set);
        final Map<String, List<String>> errors = form.submit();
        assertFalse(errors.isEmpty());
        assertNull(received.get());
    }

    @Test
    void submit_returnsErrorsForInvalidFields() {
        form.addField(new TestInputComponent(TestInputProps.required("name", "")));
        form.addField(new TestInputComponent(TestInputProps.of("email", "valid@test.com")));
        final Map<String, List<String>> errors = form.submit();
        assertEquals(1, errors.size());
        assertTrue(errors.containsKey("name"));
    }

    @Test
    void submit_withNoHandler_stillValidates() {
        form.addField(new TestInputComponent(TestInputProps.of("name", "John")));
        assertTrue(form.submit().isEmpty());
    }

    // ========== setValidationErrors() ==========

    @Test
    void setValidationErrors_throwsOnNull() {
        assertThrows(NullPointerException.class, () -> form.setValidationErrors(null));
    }

    @Test
    void setValidationErrors_appliesErrorToMatchingField() {
        final var field = new TestInputComponent(TestInputProps.of("email", "bad"));
        form.addField(field);
        form.setValidationErrors(Map.of("email", List.of("Invalid email format")));
        assertEquals("Invalid email format", field.getCurrentProps().customValidity());
    }

    @Test
    void setValidationErrors_doesNotAffectUnmatchedFields() {
        final var nameField = new TestInputComponent(TestInputProps.of("name", "John"));
        final var emailField = new TestInputComponent(TestInputProps.of("email", "bad"));
        form.addField(nameField);
        form.addField(emailField);
        form.setValidationErrors(Map.of("email", List.of("Invalid email")));
        assertEquals("", nameField.getCurrentProps().customValidity());
        assertEquals("Invalid email", emailField.getCurrentProps().customValidity());
    }

    @Test
    void setValidationErrors_clearsErrorWhenEmptyList() {
        final var field = new TestInputComponent(
            new TestInputProps("email", "", "bad", false, 0, 0, 0, 0, "", "Previous error", false, false)
        );
        form.addField(field);
        form.setValidationErrors(Map.of("email", List.of()));
        assertEquals("", field.getCurrentProps().customValidity());
    }

    // ========== collectFieldValues() ==========

    @Test
    void collectFieldValues_returnsAllFieldValues() {
        form.addField(new TestInputComponent(TestInputProps.of("name", "Alice")));
        form.addField(new TestInputComponent(TestInputProps.of("age", "30")));
        final Map<String, Object> values = form.collectFieldValues();
        assertEquals(2, values.size());
        assertEquals("Alice", values.get("name"));
        assertEquals("30", values.get("age"));
    }

    // ========== extractFieldName ==========

    @Test
    void extractFieldName_usesNameField() {
        assertEquals("myField", PForm.extractFieldName(TestInputProps.of("myField", "val")));
    }

    @Test
    void extractFieldName_fallsBackToLabel() {
        final var props = new TestInputProps("", "My Label", "val", false, 0, 0, 0, 0, "", "", false, false);
        assertEquals("My Label", PForm.extractFieldName(props));
    }

    @Test
    void extractFieldName_fallsBackToClassName() {
        record NoNameProps(String value) {}
        assertEquals("NoNameProps", PForm.extractFieldName(new NoNameProps("val")));
    }

    // ========== FormProps ==========

    @Test
    void formProps_defaults() {
        final FormProps defaults = FormProps.defaults();
        assertFalse(defaults.novalidate());
        assertEquals("", defaults.name());
        assertEquals("", defaults.label());
    }

    @Test
    void pForm_componentSignature() {
        assertEquals("wa-form", form.getComponentSignature());
    }

    @Test
    void pForm_propsClass() {
        assertEquals(FormProps.class, form.getPropsClass());
    }
}
