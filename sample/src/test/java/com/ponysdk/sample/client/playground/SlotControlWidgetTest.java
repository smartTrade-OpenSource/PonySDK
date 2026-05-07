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
import com.ponysdk.core.ui.component.PWebComponent;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.test.ModelWriterForTest;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Unit tests and property-based tests for SlotControl widget insertion features.
 * <p>
 * <b>Validates: Requirements 4.3, 4.4, 5.1, 5.2, 5.3, 5.4, 6.1, 6.3</b>
 * </p>
 */
public class SlotControlWidgetTest {

    // ========================================================================
    // Unit Tests (JUnit 5)
    // ========================================================================

    @Nested
    class UnitTests {

        private MockWebComponent mockComponent;

        @BeforeEach
        void setUp() {
            initUIContext();
            mockComponent = new MockWebComponent();
        }

        @AfterEach
        void tearDown() {
            Txn.get().commit();
        }

        @Test
        void insertWidgetIntoDefaultSlotSetsContentTypeToWidget() {
            final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));
            final MockWebComponent widget = new MockWebComponent();

            control.insertWidget(mockComponent, widget, "wa-icon");

            assertThat(control.getContentType()).isEqualTo(SlotContentType.WIDGET);
        }

        @Test
        void insertWidgetUpdatesWidgetIndicator() {
            final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));
            final MockWebComponent widget = new MockWebComponent();

            control.insertWidget(mockComponent, widget, "wa-badge");

            assertThat(control.getWidgetIndicator().getText()).isEqualTo("wa-badge");
        }

        @Test
        void insertWidgetReplacesExistingText() {
            final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));

            // First insert text
            control.updateSlotContent(mockComponent, "Hello");
            mockComponent.operations.clear();

            // Then insert widget
            final MockWebComponent widget = new MockWebComponent();
            control.insertWidget(mockComponent, widget, "wa-icon");

            // Should have removed old text, then added widget
            final boolean hasRemove = mockComponent.operations.stream()
                .anyMatch(op -> "remove".equals(op.type));
            assertThat(hasRemove).as("Old text content should be removed").isTrue();
            assertThat(control.getContentType()).isEqualTo(SlotContentType.WIDGET);
        }

        @Test
        void insertWidgetReplacesExistingWidget() {
            final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));

            // Insert first widget
            final MockWebComponent widget1 = new MockWebComponent();
            control.insertWidget(mockComponent, widget1, "wa-icon");
            mockComponent.operations.clear();

            // Insert second widget
            final MockWebComponent widget2 = new MockWebComponent();
            control.insertWidget(mockComponent, widget2, "wa-badge");

            // Should have removed old widget, then added new one
            final boolean hasRemove = mockComponent.operations.stream()
                .anyMatch(op -> "remove".equals(op.type));
            assertThat(hasRemove).as("Old widget should be removed").isTrue();
            assertThat(control.getContentType()).isEqualTo(SlotContentType.WIDGET);
            assertThat(control.getWidgetIndicator().getText()).isEqualTo("wa-badge");
        }

        @Test
        void clearContentResetsToNone() {
            final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));
            final MockWebComponent widget = new MockWebComponent();

            control.insertWidget(mockComponent, widget, "wa-icon");
            control.clearContent(mockComponent);

            assertThat(control.getContentType()).isEqualTo(SlotContentType.NONE);
            assertThat(control.getWidgetIndicator().getText()).isEmpty();
        }

        @Test
        void clearContentOnEmptySlotIsNoOp() {
            final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));

            control.clearContent(mockComponent);

            assertThat(control.getContentType()).isEqualTo(SlotContentType.NONE);
            assertThat(mockComponent.operations).isEmpty();
        }

        @Test
        void textInputAfterWidgetReplacesWidget() {
            final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));
            final MockWebComponent widget = new MockWebComponent();

            control.insertWidget(mockComponent, widget, "wa-icon");
            mockComponent.operations.clear();

            // Enter text — should replace widget
            control.updateSlotContent(mockComponent, "New text");

            assertThat(control.getContentType()).isEqualTo(SlotContentType.TEXT);
            assertThat(control.getWidgetIndicator().getText()).isEmpty();
        }

        @Test
        void insertWidgetIntoNamedSlotUsesAddToSlot() {
            mockComponent.addDeclaredSlot("header");
            final SlotControl control = new SlotControl(new SlotMetadata("header", "Header"));
            final MockWebComponent widget = new MockWebComponent();

            control.insertWidget(mockComponent, widget, "wa-spinner");

            final SlotOp lastAdd = findLastOp(mockComponent.operations, "add");
            assertThat(lastAdd).isNotNull();
            assertThat(lastAdd.slotName).isEqualTo("header");
            assertThat(control.getContentType()).isEqualTo(SlotContentType.WIDGET);
        }

        @Test
        void widgetIndicatorEmptyWhenContentTypeIsText() {
            final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));

            control.updateSlotContent(mockComponent, "Some text");

            assertThat(control.getContentType()).isEqualTo(SlotContentType.TEXT);
            assertThat(control.getWidgetIndicator().getText()).isNullOrEmpty();
        }

        @Test
        void widgetIndicatorEmptyWhenContentTypeIsNone() {
            final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));

            assertThat(control.getContentType()).isEqualTo(SlotContentType.NONE);
            assertThat(control.getWidgetIndicator().getText()).isNullOrEmpty();
        }
    }

    // ========================================================================
    // Property-Based Tests (jqwik)
    // ========================================================================

    private MockWebComponent mockComponent;

    @BeforeProperty
    public void setUp() {
        initUIContext();
        mockComponent = new MockWebComponent();
    }

    @AfterProperty
    public void tearDown() {
        Txn.get().commit();
    }

    /**
     * Property 6: Inserting content into a slot replaces previous content.
     * <p>
     * For any SlotControl that already contains content (either text or widget),
     * inserting new content (text or widget) should result in the slot containing
     * only the new content. The contentType should reflect the new content kind.
     * </p>
     * <p><b>Validates: Requirements 4.4, 5.5</b></p>
     */
    @Property(tries = 100)
    @Label("Property 6: Inserting content into a slot replaces previous content")
    @net.jqwik.api.Tag("Feature: widget-slots-editor, Property 6: Inserting content into a slot replaces previous content")
    void insertingContentReplacePreviousContent(
            @ForAll("contentTransition") ContentTransition transition
    ) {
        final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));

        // Step 1: Set up initial content
        switch (transition.initialKind()) {
            case TEXT -> control.updateSlotContent(mockComponent, transition.initialText());
            case WIDGET -> {
                final MockWebComponent widget = new MockWebComponent();
                control.insertWidget(mockComponent, widget, transition.initialWidgetType());
            }
        }

        // Verify initial state
        assertThat(control.getContentType())
            .as("Initial content type should match what was inserted")
            .isEqualTo(transition.initialKind() == ContentKind.TEXT ? SlotContentType.TEXT : SlotContentType.WIDGET);

        mockComponent.operations.clear();

        // Step 2: Insert new content
        switch (transition.newKind()) {
            case TEXT -> control.updateSlotContent(mockComponent, transition.newText());
            case WIDGET -> {
                final MockWebComponent newWidget = new MockWebComponent();
                control.insertWidget(mockComponent, newWidget, transition.newWidgetType());
            }
        }

        // Step 3: Verify replacement
        // A remove operation should have occurred for the old content
        final boolean hasRemove = mockComponent.operations.stream()
            .anyMatch(op -> "remove".equals(op.type));
        assertThat(hasRemove)
            .as("Old content should be removed when new content is inserted")
            .isTrue();

        // Content type should reflect the new content
        final SlotContentType expectedType = transition.newKind() == ContentKind.TEXT
            ? SlotContentType.TEXT : SlotContentType.WIDGET;
        assertThat(control.getContentType())
            .as("Content type should reflect the new content kind")
            .isEqualTo(expectedType);
    }

    /**
     * Property 7: Content type indicator reflects actual slot state.
     * <p>
     * For any SlotControl, the widgetIndicator label text should match widgetTypeName
     * when contentType is WIDGET, and be empty when contentType is TEXT or NONE.
     * </p>
     * <p><b>Validates: Requirements 5.2, 5.3</b></p>
     */
    @Property(tries = 100)
    @Label("Property 7: Content type indicator reflects actual slot state")
    @net.jqwik.api.Tag("Feature: widget-slots-editor, Property 7: Content type indicator reflects actual slot state")
    void contentTypeIndicatorReflectsSlotState(
            @ForAll("slotStateSequence") SlotStateSequence sequence
    ) {
        final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));

        for (final SlotAction action : sequence.actions()) {
            switch (action.kind()) {
                case TEXT -> control.updateSlotContent(mockComponent, action.text());
                case WIDGET -> {
                    final MockWebComponent widget = new MockWebComponent();
                    control.insertWidget(mockComponent, widget, action.widgetType());
                }
                case CLEAR -> control.clearContent(mockComponent);
            }
        }

        // Verify indicator consistency after all actions
        final SlotContentType type = control.getContentType();
        final String indicatorText = control.getWidgetIndicator().getText();

        switch (type) {
            case WIDGET -> assertThat(indicatorText)
                .as("When contentType is WIDGET, indicator should show widget type name")
                .isNotEmpty();
            case TEXT, NONE -> assertThat(indicatorText)
                .as("When contentType is %s, indicator should be empty", type)
                .isNullOrEmpty();
        }
    }

    /**
     * Property 8: Widget disposal on removal clears all references.
     * <p>
     * For any SlotControl containing a widget, after calling clearContent(),
     * the contentType should be NONE and widgetIndicator should be empty.
     * removeFromSlot should have been called on the parent component.
     * </p>
     * <p><b>Validates: Requirements 5.4, 6.1</b></p>
     */
    @Property(tries = 100)
    @Label("Property 8: Widget disposal on removal clears all references")
    @net.jqwik.api.Tag("Feature: widget-slots-editor, Property 8: Widget disposal on removal clears all references")
    void widgetDisposalClearsAllReferences(
            @ForAll("insertableWidgetType") String widgetType,
            @ForAll("slotName") String slotName
    ) {
        if (!slotName.isEmpty()) {
            mockComponent.addDeclaredSlot(slotName);
        }
        final SlotControl control = new SlotControl(new SlotMetadata(slotName, "Test slot"));

        // Insert a widget
        final MockWebComponent widget = new MockWebComponent();
        control.insertWidget(mockComponent, widget, widgetType);

        assertThat(control.getContentType()).isEqualTo(SlotContentType.WIDGET);
        mockComponent.operations.clear();

        // Clear content
        control.clearContent(mockComponent);

        // Verify all references are cleared
        assertThat(control.getContentType())
            .as("After clearContent, contentType should be NONE")
            .isEqualTo(SlotContentType.NONE);

        assertThat(control.getWidgetIndicator().getText())
            .as("After clearContent, widgetIndicator should be empty")
            .isEmpty();

        // Verify removeFromSlot was called
        final boolean hasRemove = mockComponent.operations.stream()
            .anyMatch(op -> "remove".equals(op.type));
        assertThat(hasRemove)
            .as("clearContent should call removeFromSlot on the parent component")
            .isTrue();
    }

    /**
     * Property 9: Component switch clears all slot widgets.
     * <p>
     * For any set of SlotControl instances with widgets inserted, after calling
     * clearContent(parentComponent) on each (simulating clearPreviousComponent()),
     * all slots should have contentType NONE and widgetIndicator empty.
     * </p>
     * <p><b>Validates: Requirements 6.2</b></p>
     */
    @Property(tries = 100)
    @Label("Property 9: Component switch clears all slot widgets")
    @net.jqwik.api.Tag("Feature: widget-slots-editor, Property 9: Component switch clears all slot widgets")
    void componentSwitchClearsAllSlotWidgets(
            @ForAll("slotControlSet") List<SlotSetup> slotSetups
    ) {
        final List<SlotControl> controls = new ArrayList<>();

        // Step 1: Create slot controls and insert a widget into each
        for (final SlotSetup setup : slotSetups) {
            if (!setup.slotName().isEmpty()) {
                mockComponent.addDeclaredSlot(setup.slotName());
            }
            final SlotControl control = new SlotControl(new SlotMetadata(setup.slotName(), "Slot " + setup.slotName()));
            final MockWebComponent widget = new MockWebComponent();
            control.insertWidget(mockComponent, widget, setup.widgetType());

            // Verify widget was inserted
            assertThat(control.getContentType())
                .as("Widget should be inserted before cleanup")
                .isEqualTo(SlotContentType.WIDGET);
            controls.add(control);
        }

        // Step 2: Simulate clearPreviousComponent() — call clearContent on each
        for (final SlotControl control : controls) {
            control.clearContent(mockComponent);
        }

        // Step 3: Verify all slots are cleared
        for (int i = 0; i < controls.size(); i++) {
            final SlotControl control = controls.get(i);
            assertThat(control.getContentType())
                .as("After clearPreviousComponent, slot %d contentType should be NONE", i)
                .isEqualTo(SlotContentType.NONE);
            assertThat(control.getWidgetIndicator().getText())
                .as("After clearPreviousComponent, slot %d widgetIndicator should be empty", i)
                .isEmpty();
        }
    }

    // ========================================================================
    // Arbitraries / Providers
    // ========================================================================

    @Provide
    Arbitrary<String> insertableWidgetType() {
        return Arbitraries.of("wa-icon", "wa-badge", "wa-spinner", "wa-button", "wa-tag",
            "wa-avatar", "wa-divider", "wa-progress-bar", "wa-progress-ring", "wa-skeleton");
    }

    @Provide
    Arbitrary<String> slotName() {
        return Arbitraries.of("", "header", "footer", "prefix", "suffix", "icon");
    }

    @Provide
    Arbitrary<ContentTransition> contentTransition() {
        final Arbitrary<ContentKind> kindArb = Arbitraries.of(ContentKind.TEXT, ContentKind.WIDGET);
        final Arbitrary<String> textArb = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50);
        final Arbitrary<String> widgetTypeArb = insertableWidgetType();

        return Combinators.combine(kindArb, textArb, widgetTypeArb, kindArb, textArb, widgetTypeArb)
            .as(ContentTransition::new);
    }

    @Provide
    Arbitrary<List<SlotSetup>> slotControlSet() {
        final Arbitrary<SlotSetup> setupArb = Combinators.combine(slotName(), insertableWidgetType())
            .as(SlotSetup::new);
        return setupArb.list().ofMinSize(1).ofMaxSize(5);
    }

    @Provide
    Arbitrary<SlotStateSequence> slotStateSequence() {
        final Arbitrary<SlotAction> actionArb = Arbitraries.oneOf(
            // TEXT action
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30)
                .map(text -> new SlotAction(ActionKind.TEXT, text, null)),
            // WIDGET action
            insertableWidgetType()
                .map(wt -> new SlotAction(ActionKind.WIDGET, null, wt)),
            // CLEAR action
            Arbitraries.just(new SlotAction(ActionKind.CLEAR, null, null))
        );

        return actionArb.list().ofMinSize(1).ofMaxSize(5).map(SlotStateSequence::new);
    }

    // ========================================================================
    // Test Data Types
    // ========================================================================

    enum ContentKind { TEXT, WIDGET }
    enum ActionKind { TEXT, WIDGET, CLEAR }

    record ContentTransition(
        ContentKind initialKind, String initialText, String initialWidgetType,
        ContentKind newKind, String newText, String newWidgetType
    ) {}

    record SlotAction(ActionKind kind, String text, String widgetType) {}
    record SlotStateSequence(List<SlotAction> actions) {}
    record SlotSetup(String slotName, String widgetType) {}

    // ========================================================================
    // Shared UIContext Setup
    // ========================================================================

    private static void initUIContext() {
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

    // ========================================================================
    // Helpers
    // ========================================================================

    private static SlotOp findLastOp(final List<SlotOp> operations, final String type) {
        for (int i = operations.size() - 1; i >= 0; i--) {
            if (operations.get(i).type.equals(type)) {
                return operations.get(i);
            }
        }
        return null;
    }

    // ========================================================================
    // MockWebComponent with operation tracking
    // ========================================================================

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
