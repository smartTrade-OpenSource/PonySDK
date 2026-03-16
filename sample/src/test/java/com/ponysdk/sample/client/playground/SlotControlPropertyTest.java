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

import static org.assertj.core.api.Assertions.*;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.component.PComponent;
import com.ponysdk.core.ui.component.PTextComponent;
import com.ponysdk.core.ui.component.PWebComponent;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.test.ModelWriterForTest;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.jupiter.api.Tag;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Property-based tests for SlotControl.
 * <p>
 * **Validates: Requirements 1.2, 1.4, 2.1, 2.2, 2.5, 3.1, 4.3, 4.4, 6.1, 6.2, 6.3, 6.4**
 * </p>
 */
public class SlotControlPropertyTest {

    private MockWebComponent mockComponent;

    @BeforeProperty
    public void setUp() {
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

        mockComponent = new MockWebComponent();
    }

    @AfterProperty
    public void tearDown() {
        Txn.get().commit();
    }

    /**
     * Property 1: Text content round-trip preservation.
     * <p>
     * For any valid-length text, after updateSlotContent the PTextComponent
     * created in the slot contains exactly that text.
     * </p>
     * <p>
     * **Validates: Requirements 1.2**
     * </p>
     */
    @Property(tries = 100)
    @Tag("Feature: functional-slot-system, Property 1: Text content round-trip preservation")
    public void textContentRoundTrip(@ForAll @StringLength(max = 10000) String text) {
        final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));

        control.updateSlotContent(mockComponent, text);

        if (text != null && !text.trim().isEmpty()) {
            // Should have an addDefault operation with a PTextComponent containing the text
            final SlotOp lastAdd = findLastOp(mockComponent.operations, "addDefault");
            assertThat(lastAdd).as("Expected an addDefault operation for non-empty text '%s'", text).isNotNull();
            assertThat(lastAdd.child).isInstanceOf(PTextComponent.class);
            assertThat(((PTextComponent) lastAdd.child).getText()).isEqualTo(text);
        } else {
            // Empty/whitespace text should not produce an add operation
            final boolean hasAdd = mockComponent.operations.stream()
                .anyMatch(op -> "addDefault".equals(op.type) || "add".equals(op.type));
            assertThat(hasAdd).as("Empty/whitespace text should not produce an add operation").isFalse();
        }
    }

    /**
     * Property 2: Slot content replacement idempotence.
     * <p>
     * After two successive updates, only the second text is active and a remove
     * operation occurred between the two adds.
     * </p>
     * <p>
     * **Validates: Requirements 2.1, 2.5**
     * </p>
     */
    @Property(tries = 100)
    @Tag("Feature: functional-slot-system, Property 2: Slot content replacement idempotence")
    public void slotContentReplacement(
            @ForAll @StringLength(min = 1, max = 100) String text1,
            @ForAll @StringLength(min = 1, max = 100) String text2) {

        Assume.that(!text1.trim().isEmpty());
        Assume.that(!text2.trim().isEmpty());

        final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));

        control.updateSlotContent(mockComponent, text1);
        mockComponent.operations.clear();

        control.updateSlotContent(mockComponent, text2);

        // After second call, the last add operation's child should have text2
        final SlotOp lastAdd = findLastOp(mockComponent.operations, "addDefault");
        assertThat(lastAdd).as("Expected an addDefault operation for text2").isNotNull();
        assertThat(((PTextComponent) lastAdd.child).getText()).isEqualTo(text2);

        // There should be a remove operation before the add
        final boolean hasRemove = mockComponent.operations.stream()
            .anyMatch(op -> "remove".equals(op.type));
        assertThat(hasRemove).as("Expected a remove operation between the two adds").isTrue();
    }

    /**
     * Property 3: Empty text removes slot content.
     * <p>
     * After adding content, setting empty or null text removes the content
     * without adding new content.
     * </p>
     * <p>
     * **Validates: Requirements 2.2**
     * </p>
     */
    @Property(tries = 100)
    @Tag("Feature: functional-slot-system, Property 3: Empty text removes slot content")
    public void emptyTextRemovesContent(@ForAll @StringLength(min = 1, max = 100) String initialText) {
        Assume.that(!initialText.trim().isEmpty());

        final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));

        // Add initial content
        control.updateSlotContent(mockComponent, initialText);
        mockComponent.operations.clear();

        // Set empty text — should remove but not add
        control.updateSlotContent(mockComponent, "");

        final boolean hasRemove = mockComponent.operations.stream()
            .anyMatch(op -> "remove".equals(op.type));
        final boolean hasAdd = mockComponent.operations.stream()
            .anyMatch(op -> "addDefault".equals(op.type) || "add".equals(op.type));
        assertThat(hasRemove).as("Empty text should trigger a remove").isTrue();
        assertThat(hasAdd).as("Empty text should not trigger an add").isFalse();

        // Re-add content, clear, then set null
        control.updateSlotContent(mockComponent, initialText);
        mockComponent.operations.clear();

        control.updateSlotContent(mockComponent, null);

        final boolean hasRemoveNull = mockComponent.operations.stream()
            .anyMatch(op -> "remove".equals(op.type));
        final boolean hasAddNull = mockComponent.operations.stream()
            .anyMatch(op -> "addDefault".equals(op.type) || "add".equals(op.type));
        assertThat(hasRemoveNull).as("Null text should trigger a remove").isTrue();
        assertThat(hasAddNull).as("Null text should not trigger an add").isFalse();
    }

    /**
     * Property 4: Slot control independence.
     * <p>
     * Two SlotControls for different named slots operate independently.
     * </p>
     * <p>
     * **Validates: Requirements 4.3, 4.4**
     * </p>
     */
    @Property(tries = 100)
    @Tag("Feature: functional-slot-system, Property 4: Slot control independence")
    public void slotIndependence(
            @ForAll @StringLength(min = 1, max = 100) String text1,
            @ForAll @StringLength(min = 1, max = 100) String text2) {

        Assume.that(!text1.trim().isEmpty());
        Assume.that(!text2.trim().isEmpty());

        mockComponent.addDeclaredSlot("slotA");
        mockComponent.addDeclaredSlot("slotB");

        final SlotControl controlA = new SlotControl(new SlotMetadata("slotA", "Slot A"));
        final SlotControl controlB = new SlotControl(new SlotMetadata("slotB", "Slot B"));

        // Update slotA
        controlA.updateSlotContent(mockComponent, text1);
        final List<SlotOp> opsAfterA = new ArrayList<>(mockComponent.operations);

        // Update slotB
        controlB.updateSlotContent(mockComponent, text2);
        // Ops added after slotA update are for slotB
        final List<SlotOp> opsForB = new ArrayList<>(mockComponent.operations.subList(opsAfterA.size(), mockComponent.operations.size()));

        // Verify slotA operations only reference slotA
        for (final SlotOp op : opsAfterA) {
            if ("add".equals(op.type)) {
                assertThat(op.slotName).isEqualTo("slotA");
            }
        }

        // Verify slotB operations only reference slotB
        for (final SlotOp op : opsForB) {
            if ("add".equals(op.type)) {
                assertThat(op.slotName).isEqualTo("slotB");
            }
        }
    }

    /**
     * Property 5: Text length validation boundary.
     * <p>
     * Text exceeding 10000 characters is rejected with an error.
     * </p>
     * <p>
     * **Validates: Requirements 6.3, 6.4**
     * </p>
     */
    @Property(tries = 100)
    @Tag("Feature: functional-slot-system, Property 5: Text length validation boundary")
    public void textLengthValidation(@ForAll @IntRange(min = 10001, max = 20000) int length) {
        final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));
        final String longText = "a".repeat(length);

        final boolean result = control.updateSlotContent(mockComponent, longText);

        assertThat(result).as("Text of length %d should be rejected", length).isFalse();
        assertThat(control.getErrorLabel().isVisible()).isTrue();
    }

    /**
     * Property 6: Named slot existence validation.
     * <p>
     * Updating a named slot that is not declared on the component fails with an error
     * containing the slot name.
     * </p>
     * <p>
     * **Validates: Requirements 3.1**
     * </p>
     */
    @Property(tries = 100)
    @Tag("Feature: functional-slot-system, Property 6: Named slot existence validation")
    public void namedSlotExistenceValidation(@ForAll("nonEmptySlotName") String slotName) {
        // slotName is NOT in mockComponent's declared slots
        final SlotControl control = new SlotControl(new SlotMetadata(slotName, "Test"));

        final boolean result = control.updateSlotContent(mockComponent, "content");

        assertThat(result).as("Non-existent slot '%s' should be rejected", slotName).isFalse();
        assertThat(control.getErrorLabel().isVisible()).isTrue();
        assertThat(control.getErrorLabel().getText()).contains(slotName);
    }

    @Provide
    Arbitrary<String> nonEmptySlotName() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);
    }

    // ========== Helper ==========

    private static SlotOp findLastOp(final List<SlotOp> operations, final String type) {
        for (int i = operations.size() - 1; i >= 0; i--) {
            if (operations.get(i).type.equals(type)) {
                return operations.get(i);
            }
        }
        return null;
    }

    // ========== MockWebComponent with operation tracking ==========

    static final class SlotOp {
        final String type;
        final String slotName;
        final PComponent<?> child;

        SlotOp(final String type, final String slotName, final PComponent<?> child) {
            this.type = type;
            this.slotName = slotName;
            this.child = child;
        }
    }

    static class MockWebComponent extends PWebComponent<MockWebComponent.MockProps> {

        public record MockProps() {
            public static MockProps defaults() {
                return new MockProps();
            }
        }

        private final Set<String> mutableSlots;
        final List<SlotOp> operations = new ArrayList<>();

        MockWebComponent() {
            this(new HashSet<>());
        }

        MockWebComponent(final Set<String> declaredSlots) {
            super(MockProps.defaults(), declaredSlots);
            this.mutableSlots = declaredSlots;
        }

        void addDeclaredSlot(final String slotName) {
            mutableSlots.add(slotName);
        }

        @Override
        public void addToSlot(final String slotName, final PComponent<?> child) {
            operations.add(new SlotOp("add", slotName, child));
        }

        @Override
        public void addToDefaultSlot(final PComponent<?> child) {
            operations.add(new SlotOp("addDefault", null, child));
        }

        @Override
        public void removeFromSlot(final String slotName, final PComponent<?> child) {
            operations.add(new SlotOp("remove", slotName, child));
        }

        @Override
        protected Class<MockProps> getPropsClass() {
            return MockProps.class;
        }

        @Override
        protected String getComponentSignature() {
            return "MockWebComponent";
        }
    }
}
