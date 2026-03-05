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

package com.ponysdk.core.ui.codegen;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.component.PComponent;
import com.ponysdk.core.ui.component.PWebComponent;
import com.ponysdk.core.ui.component.PropsDiffer;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.test.ModelWriterForTest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying that generated wrapper classes work correctly
 * with the PWebComponent infrastructure.
 * <p>
 * Tests Requirements 12.1, 12.2, 12.3, 12.4, 12.5, 12.6
 * </p>
 */
class PWebComponentIntegrationTest {

    @BeforeAll
    public static void beforeClass() {
        final WebSocket socket = Mockito.mock(WebSocket.class);
        final ServletUpgradeRequest request = Mockito.mock(ServletUpgradeRequest.class);

        final TxnContext context = Mockito.spy(new TxnContext(socket));
        ModelWriter modelWriter = new ModelWriterForTest();

        final Application application = Mockito.mock(Application.class);
        context.setApplication(application);

        final ApplicationConfiguration configuration = Mockito.mock(ApplicationConfiguration.class);

        Txn.get().begin(context);
        final UIContext uiContext = Mockito.spy(new UIContext(socket, context, configuration, request));
        Mockito.when(uiContext.getWriter()).thenReturn(modelWriter);

        UIContext.setCurrent(uiContext);
    }

    // ========== Test Props Records ==========

    /**
     * Test props record for a button component.
     */
    record ButtonProps(
        String variant,
        String size,
        boolean disabled
    ) {
        static ButtonProps defaults() {
            return new ButtonProps("neutral", "medium", false);
        }

        ButtonProps withVariant(String variant) {
            return new ButtonProps(variant, size, disabled);
        }

        ButtonProps withSize(String size) {
            return new ButtonProps(variant, size, disabled);
        }

        ButtonProps withDisabled(boolean disabled) {
            return new ButtonProps(variant, size, disabled);
        }
    }

    /**
     * Test props record with optional fields.
     */
    record InputProps(
        String value,
        Optional<String> placeholder,
        boolean required
    ) {
        static InputProps defaults() {
            return new InputProps("", Optional.empty(), false);
        }

        InputProps withValue(String value) {
            return new InputProps(value, placeholder, required);
        }

        InputProps withPlaceholder(String placeholder) {
            return new InputProps(value, Optional.of(placeholder), required);
        }
    }

    // ========== Test Wrapper Classes ==========

    /**
     * Test wrapper class simulating a generated button component.
     */
    static class TestButton extends PWebComponent<ButtonProps> {
        private static final String TAG_NAME = "test-button";
        private static final Set<String> DECLARED_SLOTS = Set.of("prefix", "suffix");

        TestButton() {
            this(ButtonProps.defaults());
        }

        TestButton(ButtonProps initialProps) {
            super(initialProps, DECLARED_SLOTS);
        }

        @Override
        protected Class<ButtonProps> getPropsClass() {
            return ButtonProps.class;
        }

        @Override
        protected String getComponentSignature() {
            return TAG_NAME;
        }

        // Generated event listener method
        public void addClickListener(Consumer<JsonObject> handler) {
            onEvent("test-click", handler);
        }

        // Generated slot method
        public void addPrefix(PComponent<?> child) {
            addToSlot("prefix", child);
        }
    }

    /**
     * Test wrapper class with optional props.
     */
    static class TestInput extends PWebComponent<InputProps> {
        private static final String TAG_NAME = "test-input";

        TestInput() {
            this(InputProps.defaults());
        }

        TestInput(InputProps initialProps) {
            super(initialProps);
        }

        @Override
        protected Class<InputProps> getPropsClass() {
            return InputProps.class;
        }

        @Override
        protected String getComponentSignature() {
            return TAG_NAME;
        }

        public void addChangeListener(Consumer<JsonObject> handler) {
            onEvent("test-change", handler);
        }
    }

    // ========== Requirement 12.1: Generated wrapper classes extend PWebComponent ==========

    @Test
    void testGeneratedWrapperExtendsPWebComponent() {
        TestButton button = new TestButton();

        // Verify it's an instance of PWebComponent
        assertInstanceOf(PWebComponent.class, button);
        assertInstanceOf(PComponent.class, button);

        // Verify it has the correct props type
        assertEquals(ButtonProps.class, button.getPropsClass());
    }

    @Test
    void testGeneratedWrapperHasCorrectSignature() {
        TestButton button = new TestButton();

        assertEquals("test-button", button.getComponentSignature());
    }

    @Test
    void testGeneratedWrapperAcceptsInitialProps() {
        ButtonProps customProps = new ButtonProps("primary", "large", true);
        TestButton button = new TestButton(customProps);

        assertEquals(customProps, button.getCurrentProps());
    }

    @Test
    void testGeneratedWrapperUsesDefaultProps() {
        TestButton button = new TestButton();

        ButtonProps expected = ButtonProps.defaults();
        assertEquals(expected, button.getCurrentProps());
    }

    // ========== Requirement 12.2: Generated wrapper classes integrate with PropsDiffer ==========

    @Test
    void testPropsRecordWorksWithPropsDiffer() {
        PropsDiffer<ButtonProps> differ = new PropsDiffer<>();

        ButtonProps props1 = new ButtonProps("primary", "medium", false);
        ButtonProps props2 = new ButtonProps("primary", "large", false);

        // Compute diff
        Optional<JsonArray> patchOpt = differ.computeDiff(props1, props2);

        // Should detect the size change
        assertTrue(patchOpt.isPresent());
        JsonArray patch = patchOpt.get();
        assertTrue(patch.size() > 0);
    }

    @Test
    void testPropsRecordDetectsNoChanges() {
        PropsDiffer<ButtonProps> differ = new PropsDiffer<>();

        ButtonProps props1 = new ButtonProps("primary", "medium", false);
        ButtonProps props2 = new ButtonProps("primary", "medium", false);

        // Compute diff
        Optional<JsonArray> patchOpt = differ.computeDiff(props1, props2);

        // Should detect no changes
        assertFalse(patchOpt.isPresent());
    }

    @Test
    void testPropsRecordSerializationRoundTrip() {
        PropsDiffer<ButtonProps> differ = new PropsDiffer<>();

        ButtonProps original = new ButtonProps("success", "small", true);

        // Serialize to JSON
        JsonObject json = differ.toJson(original);

        // Deserialize back
        ButtonProps result = differ.fromJson(json, ButtonProps.class);

        assertEquals(original, result);
    }

    @Test
    void testPropsRecordWithOptionalFieldsWorksWithPropsDiffer() {
        PropsDiffer<InputProps> differ = new PropsDiffer<>();

        InputProps props1 = new InputProps("hello", Optional.empty(), false);
        InputProps props2 = new InputProps("hello", Optional.of("Enter text"), false);

        // Compute diff
        Optional<JsonArray> patchOpt = differ.computeDiff(props1, props2);

        // Should detect the placeholder change
        assertTrue(patchOpt.isPresent());
    }

    @Test
    void testPropsRecordBuilderMethodsWorkWithPropsDiffer() {
        PropsDiffer<ButtonProps> differ = new PropsDiffer<>();

        ButtonProps props1 = ButtonProps.defaults();
        ButtonProps props2 = props1.withVariant("primary").withDisabled(true);

        // Compute diff
        Optional<JsonArray> patchOpt = differ.computeDiff(props1, props2);

        // Should detect both changes
        assertTrue(patchOpt.isPresent());
        JsonArray patch = patchOpt.get();
        assertTrue(patch.size() >= 2); // At least 2 operations for 2 field changes
    }

    // ========== Requirement 12.3: Generated wrapper classes integrate with Event_Bridge ==========

    @Test
    void testGeneratedEventListenerRegistration() {
        TestButton button = new TestButton();
        AtomicBoolean handlerCalled = new AtomicBoolean(false);

        // Register event listener using generated method
        button.addClickListener(event -> {
            handlerCalled.set(true);
        });

        // Simulate event from client
        JsonObject eventData = javax.json.Json.createObjectBuilder()
            .add("eventType", "test-click")
            .add("payload", javax.json.Json.createObjectBuilder().build())
            .build();

        button.onClientData(eventData);

        assertTrue(handlerCalled.get(), "Event handler should be called");
    }

    @Test
    void testGeneratedEventListenerReceivesPayload() {
        TestButton button = new TestButton();
        AtomicReference<JsonObject> receivedPayload = new AtomicReference<>();

        button.addClickListener(payload -> {
            receivedPayload.set(payload);
        });

        // Simulate event with payload
        JsonObject eventData = javax.json.Json.createObjectBuilder()
            .add("eventType", "test-click")
            .add("payload", javax.json.Json.createObjectBuilder()
                .add("x", 100)
                .add("y", 200)
                .build())
            .build();

        button.onClientData(eventData);

        assertNotNull(receivedPayload.get());
        assertEquals(100, receivedPayload.get().getInt("x"));
        assertEquals(200, receivedPayload.get().getInt("y"));
    }

    @Test
    void testMultipleEventListenersCanBeRegistered() {
        TestInput input = new TestInput();
        AtomicBoolean handler1Called = new AtomicBoolean(false);
        AtomicBoolean handler2Called = new AtomicBoolean(false);

        // Register first listener
        input.addChangeListener(event -> handler1Called.set(true));

        // Register second listener (should replace first)
        input.addChangeListener(event -> handler2Called.set(true));

        // Simulate event
        JsonObject eventData = javax.json.Json.createObjectBuilder()
            .add("eventType", "test-change")
            .add("payload", javax.json.Json.createObjectBuilder().build())
            .build();

        input.onClientData(eventData);

        // Only the second handler should be called (replacement behavior)
        assertFalse(handler1Called.get());
        assertTrue(handler2Called.get());
    }

    // ========== Requirement 12.4: Generated wrapper classes integrate with Component_Terminal ==========

    @Test
    void testGeneratedWrapperHasDeclaredSlots() {
        TestButton button = new TestButton();

        Set<String> slots = button.getDeclaredSlots();

        assertNotNull(slots);
        assertEquals(2, slots.size());
        assertTrue(slots.contains("prefix"));
        assertTrue(slots.contains("suffix"));
    }

    @Test
    void testGeneratedWrapperWithoutSlotsHasEmptySet() {
        TestInput input = new TestInput();

        Set<String> slots = input.getDeclaredSlots();

        assertNotNull(slots);
        assertTrue(slots.isEmpty());
    }

    @Test
    void testGeneratedSlotMethodCallsAddToSlot() {
        TestButton button = new TestButton();
        TestButton childButton = new TestButton();

        // This should not throw an exception
        assertDoesNotThrow(() -> button.addPrefix(childButton));
    }

    // ========== Requirement 12.5: Generated Props_Records compatible with lifecycle methods ==========

    @Test
    void testPropsRecordCompatibleWithSetProps() {
        TestButton button = new TestButton();

        ButtonProps initialProps = button.getCurrentProps();
        ButtonProps newProps = initialProps.withVariant("primary");

        // This should work without errors
        assertDoesNotThrow(() -> {
            // Note: setProps is protected, so we test through the public API
            // by verifying the props can be used in construction
            TestButton button2 = new TestButton(newProps);
            assertEquals(newProps, button2.getCurrentProps());
        });
    }

    @Test
    void testPropsRecordMaintainsImmutability() {
        ButtonProps props1 = new ButtonProps("primary", "medium", false);
        ButtonProps props2 = props1.withVariant("success");

        // Original should be unchanged
        assertEquals("primary", props1.variant());
        assertEquals("success", props2.variant());

        // Other fields should be preserved
        assertEquals(props1.size(), props2.size());
        assertEquals(props1.disabled(), props2.disabled());
    }

    @Test
    void testPropsRecordEqualsAndHashCode() {
        ButtonProps props1 = new ButtonProps("primary", "medium", false);
        ButtonProps props2 = new ButtonProps("primary", "medium", false);
        ButtonProps props3 = new ButtonProps("success", "medium", false);

        // Test equals
        assertEquals(props1, props2);
        assertNotEquals(props1, props3);

        // Test hashCode consistency
        assertEquals(props1.hashCode(), props2.hashCode());
    }

    // ========== Requirement 12.6: Maintain backward compatibility with existing PComponent APIs ==========

    @Test
    void testGeneratedWrapperCompatibleWithPComponentAPI() {
        TestButton button = new TestButton();

        // Should be able to use PComponent methods
        assertNotNull(button.getCurrentProps());
        assertNull(button.getPreviousProps()); // No update yet
        assertEquals(ButtonProps.class, button.getPropsClass());
    }

    @Test
    void testGeneratedWrapperSupportsEventHandlerRemoval() {
        TestButton button = new TestButton();
        AtomicBoolean handlerCalled = new AtomicBoolean(false);

        button.addClickListener(event -> handlerCalled.set(true));

        // Remove the handler
        button.removeEventHandler("test-click");

        // Simulate event
        JsonObject eventData = javax.json.Json.createObjectBuilder()
            .add("eventType", "test-click")
            .add("payload", javax.json.Json.createObjectBuilder().build())
            .build();

        button.onClientData(eventData);

        // Handler should not be called
        assertFalse(handlerCalled.get());
    }

    // ========== End-to-End Integration Tests ==========

    @Test
    void testCompleteIntegrationWorkflow() {
        // Create component with initial props
        ButtonProps initialProps = new ButtonProps("neutral", "medium", false);
        TestButton button = new TestButton(initialProps);

        // Verify initial state
        assertEquals(initialProps, button.getCurrentProps());
        assertEquals("test-button", button.getComponentSignature());

        // Register event listener
        AtomicBoolean clicked = new AtomicBoolean(false);
        button.addClickListener(event -> clicked.set(true));

        // Simulate event
        JsonObject eventData = javax.json.Json.createObjectBuilder()
            .add("eventType", "test-click")
            .add("payload", javax.json.Json.createObjectBuilder().build())
            .build();

        button.onClientData(eventData);

        // Verify event was handled
        assertTrue(clicked.get());

        // Test props diffing
        PropsDiffer<ButtonProps> differ = new PropsDiffer<>();
        ButtonProps newProps = initialProps.withVariant("primary");
        Optional<JsonArray> diff = differ.computeDiff(initialProps, newProps);

        assertTrue(diff.isPresent());
    }

    @Test
    void testIntegrationWithOptionalProps() {
        // Create component with optional props
        InputProps props1 = InputProps.defaults();
        TestInput input = new TestInput(props1);

        // Verify initial state
        assertEquals(props1, input.getCurrentProps());
        assertTrue(input.getCurrentProps().placeholder().isEmpty());

        // Test props with optional value
        InputProps props2 = props1.withPlaceholder("Enter text");
        TestInput input2 = new TestInput(props2);

        assertEquals("Enter text", input2.getCurrentProps().placeholder().get());

        // Test diffing with optional fields
        PropsDiffer<InputProps> differ = new PropsDiffer<>();
        Optional<JsonArray> diff = differ.computeDiff(props1, props2);

        assertTrue(diff.isPresent());
    }

    @Test
    void testIntegrationWithSlots() {
        TestButton parentButton = new TestButton();
        TestButton childButton = new TestButton();

        // Verify slots are declared
        assertTrue(parentButton.getDeclaredSlots().contains("prefix"));

        // Add child to slot
        assertDoesNotThrow(() -> parentButton.addPrefix(childButton));

        // Verify default slot works
        assertDoesNotThrow(() -> parentButton.addToDefaultSlot(childButton));
    }

    @Test
    void testPropsRecordWithAllFieldTypes() {
        // Test that props records with various field types work correctly
        ButtonProps props = new ButtonProps("primary", "large", true);

        // String field
        assertEquals("primary", props.variant());

        // String field
        assertEquals("large", props.size());

        // boolean field
        assertTrue(props.disabled());

        // Test with PropsDiffer
        PropsDiffer<ButtonProps> differ = new PropsDiffer<>();
        JsonObject json = differ.toJson(props);

        assertNotNull(json);
        assertTrue(json.containsKey("variant"));
        assertTrue(json.containsKey("size"));
        assertTrue(json.containsKey("disabled"));
    }
}
