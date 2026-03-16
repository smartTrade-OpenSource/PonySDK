package com.ponysdk.sample.client.playground;

import com.ponysdk.core.ui.component.PWebComponent;
import com.ponysdk.test.PSuite;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * Tests for SlotControl validation functionality.
 * Validates Requirements 3.1, 3.2, 6.3, 6.4
 */
public class SlotControlValidationTest extends PSuite {

    private SlotControl slotControl;
    private MockWebComponent mockComponent;

    @Before
    public void setUp() {
        SlotMetadata metadata = new SlotMetadata("test-slot", "Test slot description");
        slotControl = new SlotControl(metadata);
        mockComponent = new MockWebComponent();
    }

    @Test
    public void testTextLengthValidation_WithinLimit() {
        // Text with exactly 10000 characters should be accepted
        String text = "a".repeat(10000);
        
        // Add the slot to the component so validation passes
        mockComponent.addDeclaredSlot("test-slot");
        
        slotControl.updateSlotContent(mockComponent, text);
        
        // Error label should not be visible
        assertFalse("Error should not be shown for text within limit",
            slotControl.getErrorLabel().isVisible());
    }

    @Test
    public void testTextLengthValidation_ExceedsLimit() {
        // Text with 10001 characters should trigger error
        String text = "a".repeat(10001);
        
        // Add the slot to the component so we test length validation, not slot existence
        mockComponent.addDeclaredSlot("test-slot");
        
        slotControl.updateSlotContent(mockComponent, text);
        
        // Error label should be visible with appropriate message
        assertTrue("Error should be shown for text exceeding limit",
            slotControl.getErrorLabel().isVisible());
        assertTrue("Error message should mention character limit",
            slotControl.getErrorLabel().getText().contains("10000 characters"));
    }

    @Test
    public void testSlotExistenceValidation_ValidSlot() {
        // Create slot control for a slot that exists
        mockComponent.addDeclaredSlot("test-slot");
        
        slotControl.updateSlotContent(mockComponent, "Test content");
        
        // Error label should not be visible
        assertFalse("Error should not be shown for existing slot",
            slotControl.getErrorLabel().isVisible());
    }

    @Test
    public void testSlotExistenceValidation_NonExistentSlot() {
        // Slot "test-slot" is not declared in mockComponent
        
        slotControl.updateSlotContent(mockComponent, "Test content");
        
        // Error label should be visible with appropriate message
        assertTrue("Error should be shown for non-existent slot",
            slotControl.getErrorLabel().isVisible());
        assertTrue("Error message should indicate slot doesn't exist",
            slotControl.getErrorLabel().getText().contains("does not exist"));
        assertTrue("Error message should mention the slot name",
            slotControl.getErrorLabel().getText().contains("test-slot"));
    }

    @Test
    public void testDefaultSlotDoesNotRequireValidation() {
        // Default slot (empty name) should not require existence validation
        SlotMetadata defaultMetadata = new SlotMetadata("", "Default slot");
        SlotControl defaultSlotControl = new SlotControl(defaultMetadata);
        
        defaultSlotControl.updateSlotContent(mockComponent, "Test content");
        
        // Error label should not be visible
        assertFalse("Error should not be shown for default slot",
            defaultSlotControl.getErrorLabel().isVisible());
    }

    /**
     * Mock PWebComponent for testing purposes
     */
    private static class MockWebComponent extends PWebComponent<MockWebComponent.MockProps> {
        
        public record MockProps() {
            public static MockProps defaults() {
                return new MockProps();
            }
        }
        
        private final Set<String> mutableSlots;
        
        public MockWebComponent() {
            this(new java.util.HashSet<>());
        }
        
        public MockWebComponent(Set<String> declaredSlots) {
            super(MockProps.defaults(), declaredSlots);
            this.mutableSlots = declaredSlots;
        }
        
        public void addDeclaredSlot(String slotName) {
            mutableSlots.add(slotName);
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
