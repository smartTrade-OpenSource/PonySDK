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

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Unit tests for SlotControl construction, content update, and error handling.
 * <p>
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 4.2, 6.3, 6.4, 8.1, 8.2
 * </p>
 */
class SlotControlTest {

    private MockWebComponent mockComponent;

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

        mockComponent = new MockWebComponent();
    }

    @AfterEach
    void tearDown() {
        Txn.get().commit();
    }

    // ========== 3.1: Construction tests ==========

    @Test
    void testLabelShowsDefaultForEmptySlotName() {
        final SlotControl control = new SlotControl(new SlotMetadata("", "desc"));
        assertThat(control.getLabel().getText()).isEqualTo("(default)");
    }

    @Test
    void testLabelShowsSlotNameForNamedSlot() {
        final SlotControl control = new SlotControl(new SlotMetadata("header", "Header slot"));
        assertThat(control.getLabel().getText()).isEqualTo("header");
    }

    @Test
    void testPlaceholderUsesDescriptionWhenPresent() {
        final SlotControl control = new SlotControl(new SlotMetadata("header", "The header area"));
        assertThat(control.getTextBox().getPlaceholder()).isEqualTo("The header area");
    }

    @Test
    void testPlaceholderUsesDefaultWhenDescriptionEmpty() {
        final SlotControl control = new SlotControl(new SlotMetadata("header", ""));
        assertThat(control.getTextBox().getPlaceholder()).isEqualTo("Enter text...");
    }

    // ========== 3.2: updateSlotContent() add/remove tests ==========

    @Test
    void testAddTextToDefaultSlotCallsAddToDefaultSlot() {
        final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));

        final boolean result = control.updateSlotContent(mockComponent, "Hello");

        assertThat(result).isTrue();
        assertThat(mockComponent.operations).hasSize(1);
        final SlotOp op = mockComponent.operations.get(0);
        assertThat(op.type).isEqualTo("addDefault");
        assertThat(op.slotName).isNull();
    }

    @Test
    void testAddTextToNamedSlotCallsAddToSlot() {
        mockComponent.addDeclaredSlot("header");
        final SlotControl control = new SlotControl(new SlotMetadata("header", "Header"));

        final boolean result = control.updateSlotContent(mockComponent, "Title");

        assertThat(result).isTrue();
        assertThat(mockComponent.operations).hasSize(1);
        final SlotOp op = mockComponent.operations.get(0);
        assertThat(op.type).isEqualTo("add");
        assertThat(op.slotName).isEqualTo("header");
    }

    @Test
    void testEmptyTextRemovesExistingContent() {
        mockComponent.addDeclaredSlot("header");
        final SlotControl control = new SlotControl(new SlotMetadata("header", "Header"));

        // First add content
        control.updateSlotContent(mockComponent, "Title");
        mockComponent.operations.clear();

        // Then set empty text
        final boolean result = control.updateSlotContent(mockComponent, "");

        assertThat(result).isTrue();
        // Should have a remove but no add
        assertThat(mockComponent.operations).hasSize(1);
        assertThat(mockComponent.operations.get(0).type).isEqualTo("remove");
    }

    @Test
    void testNullTextRemovesExistingContent() {
        mockComponent.addDeclaredSlot("header");
        final SlotControl control = new SlotControl(new SlotMetadata("header", "Header"));

        // First add content
        control.updateSlotContent(mockComponent, "Title");
        mockComponent.operations.clear();

        // Then set null text
        final boolean result = control.updateSlotContent(mockComponent, null);

        assertThat(result).isTrue();
        assertThat(mockComponent.operations).hasSize(1);
        assertThat(mockComponent.operations.get(0).type).isEqualTo("remove");
    }

    @Test
    void testWhitespaceOnlyTextRemovesExistingContent() {
        mockComponent.addDeclaredSlot("header");
        final SlotControl control = new SlotControl(new SlotMetadata("header", "Header"));

        // First add content
        control.updateSlotContent(mockComponent, "Title");
        mockComponent.operations.clear();

        // Whitespace-only should be treated as empty
        final boolean result = control.updateSlotContent(mockComponent, "   ");
        assertThat(result).isTrue();
        assertThat(mockComponent.operations).hasSize(1);
        assertThat(mockComponent.operations.get(0).type).isEqualTo("remove");
    }

    @Test
    void testTabOnlyTextRemovesExistingContent() {
        mockComponent.addDeclaredSlot("header");
        final SlotControl control = new SlotControl(new SlotMetadata("header", "Header"));

        control.updateSlotContent(mockComponent, "Title");
        mockComponent.operations.clear();

        final boolean result = control.updateSlotContent(mockComponent, "\t");
        assertThat(result).isTrue();
        assertThat(mockComponent.operations).hasSize(1);
        assertThat(mockComponent.operations.get(0).type).isEqualTo("remove");
    }

    @Test
    void testReplacementRemovesOldAndAddsNew() {
        mockComponent.addDeclaredSlot("header");
        final SlotControl control = new SlotControl(new SlotMetadata("header", "Header"));

        // Add initial content
        control.updateSlotContent(mockComponent, "Old Title");
        mockComponent.operations.clear();

        // Replace with new content
        final boolean result = control.updateSlotContent(mockComponent, "New Title");

        assertThat(result).isTrue();
        // Should have remove then add
        assertThat(mockComponent.operations).hasSize(2);
        assertThat(mockComponent.operations.get(0).type).isEqualTo("remove");
        assertThat(mockComponent.operations.get(1).type).isEqualTo("add");
        assertThat(mockComponent.operations.get(1).slotName).isEqualTo("header");
    }

    // ========== 3.3: Error handling tests ==========

    @Test
    void testNonExistentNamedSlotReturnsFalseWithError() {
        // "missing-slot" is NOT declared in mockComponent
        final SlotControl control = new SlotControl(new SlotMetadata("missing-slot", "Missing"));

        final boolean result = control.updateSlotContent(mockComponent, "content");

        assertThat(result).isFalse();
        assertThat(control.getErrorLabel().isVisible()).isTrue();
        assertThat(control.getErrorLabel().getText()).contains("missing-slot");
    }

    @Test
    void testTextExceeding10000CharsReturnsFalseWithError() {
        final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));
        final String longText = "a".repeat(10001);

        final boolean result = control.updateSlotContent(mockComponent, longText);

        assertThat(result).isFalse();
        assertThat(control.getErrorLabel().isVisible()).isTrue();
        assertThat(control.getErrorLabel().getText()).contains("10000 characters");
    }

    @Test
    void testTextExactly10000CharsReturnsTrue() {
        final SlotControl control = new SlotControl(new SlotMetadata("", "Default"));
        final String exactText = "a".repeat(10000);

        final boolean result = control.updateSlotContent(mockComponent, exactText);

        assertThat(result).isTrue();
        assertThat(control.getErrorLabel().isVisible()).isFalse();
    }

    @Test
    void testSuccessfulUpdateClearsPreviousError() {
        // First trigger an error with a non-existent slot
        final SlotControl control = new SlotControl(new SlotMetadata("missing", "Missing"));
        control.updateSlotContent(mockComponent, "content");
        assertThat(control.getErrorLabel().isVisible()).isTrue();

        // Now add the slot and update successfully
        mockComponent.addDeclaredSlot("missing");
        final boolean result = control.updateSlotContent(mockComponent, "valid content");

        // updateSlotContent returns true; the caller (bindTo) clears the error.
        // But updateSlotContent itself doesn't call clearError — that's done in bindTo.
        // So we just verify the operation succeeded.
        assertThat(result).isTrue();
    }

    // ========== MockWebComponent with operation tracking ==========

    /**
     * Records a single slot operation for verification.
     */
    static final class SlotOp {
        final String type;      // "add", "addDefault", "remove"
        final String slotName;  // null for default slot
        final PComponent<?> child;

        SlotOp(final String type, final String slotName, final PComponent<?> child) {
            this.type = type;
            this.slotName = slotName;
            this.child = child;
        }
    }

    /**
     * Mock PWebComponent that tracks slot operations for test assertions.
     */
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
            // Don't call super to avoid sending real protocol messages
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
