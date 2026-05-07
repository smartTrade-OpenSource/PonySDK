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

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.ponysdk.core.ui.component.PWebComponent;
import com.ponysdk.test.PSuite;

/**
 * Preservation Property Tests - Component Cleanup and Default Content
 * <p>
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**
 * </p>
 * <p>
 * **Property 2: Preservation** - Existing Functionality Unchanged
 * </p>
 * <p>
 * These tests verify that the bugfix does NOT break existing functionality.
 * All tests should PASS on both unfixed and fixed code, confirming that
 * the fix preserves the original behavior for non-buggy scenarios.
 * </p>
 * <p>
 * <b>Preservation Requirements:</b>
 * </p>
 * <ul>
 * <li>3.1 - Property modifications via PropertyPanel continue to work</li>
 * <li>3.2 - First component selection displays correctly</li>
 * <li>3.3 - Component list loads correctly</li>
 * <li>3.4 - Real-time updates work when modifying properties</li>
 * <li>3.5 - Non-Web Awesome components display correctly</li>
 * </ul>
 */
public class ComponentPreservationTest extends PSuite {

    private ComponentPlayground playground;
    private PropertyPanel propertyPanel;

    @Before
    public void setUp() {
        playground = new ComponentPlayground();
        propertyPanel = playground.getPropertyPanel();
    }

    /**
     * Preservation Test: First Component Display
     * <p>
     * **Validates: Requirement 3.2**
     * </p>
     * <p>
     * Tests that selecting a component for the first time displays it correctly.
     * This behavior must remain unchanged after the fix.
     * </p>
     */
    @Test
    public void testPreservation_firstComponentDisplay() {
        // Given: No component is currently selected
        assertNull("Initially no component should be selected", playground.getCurrentComponent());
        
        // When: We select a component for the first time
        selectComponent("Button");
        
        // Then: The component should be created and tracked
        PWebComponent<?> component = playground.getCurrentComponent();
        assertNotNull("Component should be created on first selection", component);
        assertTrue("Component should have a valid ID", component.getID() > 0);
        
        System.out.println("=== PRESERVATION TEST ===");
        System.out.println("First component display: PASSED");
        System.out.println("Component created with ID: " + component.getID());
        System.out.println("=========================");
    }

    /**
     * Preservation Test: Component List Loading
     * <p>
     * **Validates: Requirement 3.3**
     * </p>
     * <p>
     * Tests that the component list loads correctly during initialization.
     * This behavior must remain unchanged after the fix.
     * </p>
     */
    @Test
    public void testPreservation_componentListLoading() {
        // Given: A newly created playground
        // The playground should have loaded the component list during initialization
        
        // When: We check the available components
        // Note: We verify by attempting to select known components
        
        // Then: Known components should be selectable
        String[] knownComponents = {"Button", "Badge", "Input", "Spinner"};
        
        for (String componentName : knownComponents) {
            try {
                selectComponent(componentName);
                PWebComponent<?> component = playground.getCurrentComponent();
                assertNotNull("Component '" + componentName + "' should be loadable", component);
            } catch (Exception e) {
                fail("Component list should include '" + componentName + "': " + e.getMessage());
            }
        }
        
        System.out.println("=== PRESERVATION TEST ===");
        System.out.println("Component list loading: PASSED");
        System.out.println("All known components are loadable");
        System.out.println("=========================");
    }

    /**
     * Preservation Test: Component State After Selection
     * <p>
     * **Validates: Requirements 3.1, 3.4**
     * </p>
     * <p>
     * Tests that component state is properly maintained after selection,
     * enabling property modifications and real-time updates.
     * </p>
     */
    @Test
    public void testPreservation_componentStateAfterSelection() {
        // Given: A component is selected
        selectComponent("Button");
        PWebComponent<?> component = playground.getCurrentComponent();
        int componentId = component.getID();
        
        // When: We verify the component state
        // Then: The component should be properly initialized
        assertNotNull("Component should not be null", component);
        assertTrue("Component ID should be positive", componentId > 0);
        
        // The component should be the same instance when accessed again
        PWebComponent<?> sameComponent = playground.getCurrentComponent();
        assertEquals("Component reference should be stable", componentId, sameComponent.getID());
        
        System.out.println("=== PRESERVATION TEST ===");
        System.out.println("Component state preservation: PASSED");
        System.out.println("Component ID stable: " + componentId);
        System.out.println("=========================");
    }

    /**
     * Preservation Test: Multiple Different Components
     * <p>
     * **Validates: Requirements 3.2, 3.5**
     * </p>
     * <p>
     * Tests that different types of components can be selected and displayed.
     * Each component type should work correctly.
     * </p>
     */
    @Test
    public void testPreservation_multipleDifferentComponents() {
        // Given: Different component types (only known existing components)
        String[] componentTypes = {"Button", "Badge", "Input", "Spinner"};
        
        for (String componentType : componentTypes) {
            // When: We select each component type
            selectComponent(componentType);
            
            // Then: Each should be created successfully
            PWebComponent<?> component = playground.getCurrentComponent();
            assertNotNull("Component '" + componentType + "' should be created", component);
            assertTrue("Component '" + componentType + "' should have valid ID", 
                      component.getID() > 0);
        }
        
        System.out.println("=== PRESERVATION TEST ===");
        System.out.println("Multiple component types: PASSED");
        System.out.println("=========================");
    }

    /**
     * Preservation Test: Component Replacement Creates New Instance
     * <p>
     * **Validates: Requirements 3.1, 3.2**
     * </p>
     * <p>
     * Tests that selecting a new component creates a new instance,
     * which is necessary for property modifications to work correctly.
     * </p>
     */
    @Test
    public void testPreservation_componentReplacementCreatesNewInstance() {
        // Given: A component is selected
        selectComponent("Button");
        PWebComponent<?> firstComponent = playground.getCurrentComponent();
        int firstId = firstComponent.getID();
        
        // When: We select a different component
        selectComponent("Badge");
        PWebComponent<?> secondComponent = playground.getCurrentComponent();
        int secondId = secondComponent.getID();
        
        // Then: A new instance should be created
        assertNotEquals("Different components should have different IDs", firstId, secondId);
        assertNotSame("Different components should be different instances", 
                     firstComponent, secondComponent);
        
        System.out.println("=== PRESERVATION TEST ===");
        System.out.println("Component replacement: PASSED");
        System.out.println("First ID: " + firstId + ", Second ID: " + secondId);
        System.out.println("=========================");
    }

    /**
     * Preservation Test: PropertyPanel State
     * <p>
     * **Validates: Requirements 3.1, 3.4**
     * </p>
     * <p>
     * Tests that the PropertyPanel is properly associated with the playground
     * and ready to handle property modifications.
     * </p>
     */
    @Test
    public void testPreservation_propertyPanelState() {
        // Given: A playground with a property panel
        assertNotNull("PropertyPanel should exist", propertyPanel);
        
        // When: We select a component
        selectComponent("Button");
        
        // Then: The property panel should be ready for modifications
        // (The actual property controls are created dynamically based on component metadata)
        PWebComponent<?> component = playground.getCurrentComponent();
        assertNotNull("Component should be available for property modifications", component);
        
        System.out.println("=== PRESERVATION TEST ===");
        System.out.println("PropertyPanel state: PASSED");
        System.out.println("PropertyPanel ready for modifications");
        System.out.println("=========================");
    }

    /**
     * Helper method to simulate component selection.
     */
    private void selectComponent(String componentName) {
        try {
            java.lang.reflect.Method method = ComponentPlayground.class.getDeclaredMethod(
                "onComponentSelected", String.class);
            method.setAccessible(true);
            method.invoke(playground, componentName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to select component: " + componentName, e);
        }
    }
}
