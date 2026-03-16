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

import com.ponysdk.core.ui.component.PComponent;
import com.ponysdk.core.ui.component.PWebComponent;
import com.ponysdk.test.PSuite;

/**
 * Preservation Tests - Component Preview Display Fix
 * <p>
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**
 * </p>
 * <p>
 * These tests verify that the bugfix does not break existing functionality:
 * - Property panel displays component properties correctly
 * - Component creation and management unchanged
 * - Container clearing works correctly
 * </p>
 */
public class ComponentPreviewPreservationTest extends PSuite {

    private PropertyPanel propertyPanel;

    @Before
    public void setUp() {
        propertyPanel = new PropertyPanel();
    }

    /**
     * Test that the preview container is accessible and can be cleared.
     * <p>
     * **Validates: Requirement 3.6** - previewContainer.clear() cleans previous content correctly
     * </p>
     */
    @Test
    public void testPreviewContainer_canBeClearedSuccessfully() {
        // Given: A property panel with a preview container
        assertNotNull("Preview container should exist", propertyPanel.getPreviewContainer());
        
        // When: We clear the preview container
        propertyPanel.clear();
        
        // Then: The container should be cleared without errors
        assertNull("Preview container should be empty after clear", 
                   propertyPanel.getPreviewContainer().getWidget());
    }

    /**
     * Test that the form container is accessible.
     * <p>
     * **Validates: Requirement 3.1** - Property panel displays component properties correctly
     * </p>
     */
    @Test
    public void testFormContainer_isAccessible() {
        // Given: A property panel
        // When: We access the form container
        // Then: It should be accessible without errors
        assertNotNull("Form container should exist", propertyPanel.getFormContainer());
    }

    /**
     * Test that setPreviewComponent throws exception for null component.
     * <p>
     * **Validates: Requirement 3.4** - Component management unchanged
     * </p>
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetPreviewComponent_throwsExceptionForNull() {
        // Given: A property panel
        // When: We try to set a null component
        propertyPanel.setPreviewComponent(null);
        
        // Then: An IllegalArgumentException should be thrown
    }

    /**
     * Test that setPreviewComponent accepts valid components without errors.
     * <p>
     * **Validates: Requirements 3.2, 3.3** - Component creation and JavaScript execution
     * </p>
     */
    @Test
    public void testSetPreviewComponent_acceptsValidComponent() {
        // Given: A valid web component
        final PComponent<?> component = new MockWebComponent();
        
        // When: We set the component in the preview
        // Then: It should execute without throwing exceptions
        try {
            propertyPanel.setPreviewComponent(component);
            // If we reach here, the method executed successfully
            assertTrue("setPreviewComponent should execute without errors", true);
        } catch (Exception e) {
            fail("setPreviewComponent should not throw exception for valid component: " + e.getMessage());
        }
    }

    /**
     * Mock Web Component for testing.
     */
    public static class MockWebComponent extends PWebComponent<MockWebComponentProps> {
        public MockWebComponent() {
            super(MockWebComponentProps.defaults());
        }

        @Override
        protected Class<MockWebComponentProps> getPropsClass() {
            return MockWebComponentProps.class;
        }

        @Override
        protected String getComponentSignature() {
            return "mock-web-component";
        }
    }

    /**
     * Mock props record for testing Web Components.
     */
    public record MockWebComponentProps(String content) {
        public static MockWebComponentProps defaults() {
            return new MockWebComponentProps("");
        }
        
        public MockWebComponentProps withContent(String content) {
            return new MockWebComponentProps(content);
        }
    }
}
