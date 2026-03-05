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

package com.ponysdk.core.ui.component;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.test.ModelWriterForTest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PComponent CSS class management methods.
 * <p>
 * Tests that PComponent correctly inherits CSS class management from PWidget
 * and that style changes don't affect props state.
 * </p>
 * <p>
 * **Validates: Requirements 2.2, 3.4**
 * </p>
 */
public class PComponentCssClassTest {

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

    @AfterAll
    public static void afterClass() {
        Txn.get().commit();
    }

    /**
     * Test props record for CSS class tests.
     */
    record TestProps(String title, int count) {
        static TestProps defaults() {
            return new TestProps("Test", 0);
        }
    }

    /**
     * Test component implementation.
     */
    static class TestComponent extends PComponent<TestProps> {

        TestComponent(TestProps props) {
            super(props, FrameworkType.REACT);
        }

        @Override
        protected Class<TestProps> getPropsClass() {
            return TestProps.class;
        }

        @Override
        protected String getComponentSignature() {
            return "test-component";
        }
    }

    /**
     * Test that addStyleName() adds CSS classes correctly.
     * <p>
     * **Validates: Requirement 2.2**
     * </p>
     */
    @Test
    public void testAddStyleName() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Add a style name
        component.addStyleName("test-class");
        
        // Verify the style name was added
        assertTrue(component.hasStyleName("test-class"), 
                "Component should have style 'test-class'");
    }

    /**
     * Test that multiple style names can be added.
     * <p>
     * **Validates: Requirement 2.2**
     * </p>
     */
    @Test
    public void testAddMultipleStyleNames() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Add multiple style names
        component.addStyleName("class1");
        component.addStyleName("class2");
        component.addStyleName("class3");
        
        // Verify all style names were added
        assertTrue(component.hasStyleName("class1"), "Should have 'class1'");
        assertTrue(component.hasStyleName("class2"), "Should have 'class2'");
        assertTrue(component.hasStyleName("class3"), "Should have 'class3'");
    }

    /**
     * Test that removeStyleName() removes CSS classes correctly.
     * <p>
     * **Validates: Requirement 2.2**
     * </p>
     */
    @Test
    public void testRemoveStyleName() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Add style names
        component.addStyleName("class1");
        component.addStyleName("class2");
        
        // Remove one style name
        component.removeStyleName("class1");
        
        // Verify class1 was removed but class2 remains
        assertFalse(component.hasStyleName("class1"), "Should not have 'class1'");
        assertTrue(component.hasStyleName("class2"), "Should still have 'class2'");
    }

    /**
     * Test that setStyleName() replaces all CSS classes.
     * <p>
     * **Validates: Requirement 2.2**
     * </p>
     */
    @Test
    public void testSetStyleName() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Add some initial style names
        component.addStyleName("class1");
        component.addStyleName("class2");
        
        // Set a new style name (should replace all previous)
        component.setStyleName("new-class");
        
        // Verify only the new style name is present
        final String styleName = component.getStyleName();
        assertEquals("new-class", styleName, 
                "Style name should be exactly 'new-class'");
    }

    /**
     * Test that style changes don't affect props state.
     * <p>
     * **Validates: Requirement 3.4**
     * </p>
     */
    @Test
    public void testStyleChangesDoNotAffectProps() {
        final TestProps initialProps = new TestProps("Initial", 42);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Verify initial props
        assertEquals(initialProps, component.getCurrentProps(), 
                "Initial props should be set");
        assertNull(component.getPreviousProps(), 
                "Previous props should be null initially");

        // Add style names
        component.addStyleName("class1");
        component.addStyleName("class2");

        // Verify props are unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Current props should remain unchanged after adding styles");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null after adding styles");

        // Remove a style name
        component.removeStyleName("class1");

        // Verify props are still unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Current props should remain unchanged after removing style");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null after removing style");

        // Set a new style name
        component.setStyleName("new-class");

        // Verify props are still unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Current props should remain unchanged after setting style");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null after setting style");
    }

    /**
     * Test that props updates work correctly regardless of style state.
     * <p>
     * **Validates: Requirements 3.1, 3.4**
     * </p>
     */
    @Test
    public void testPropsUpdateWithStyles() {
        final TestProps initialProps = new TestProps("Initial", 0);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Add some styles
        component.addStyleName("styled-component");
        component.addStyleName("active");

        // Update props
        final TestProps newProps = new TestProps("Updated", 1);
        component.setProps(newProps);

        // Verify props were updated
        assertEquals(newProps, component.getCurrentProps(),
                "Current props should be updated");
        assertEquals(initialProps, component.getPreviousProps(),
                "Previous props should be stored");

        // Verify styles are unchanged
        assertTrue(component.hasStyleName("styled-component"), 
                "Should still have 'styled-component'");
        assertTrue(component.hasStyleName("active"), 
                "Should still have 'active'");
    }

    /**
     * Test that style state is independent of props updates.
     * <p>
     * **Validates: Requirement 3.5**
     * </p>
     */
    @Test
    public void testStylesIndependentOfPropsUpdates() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Set initial styles
        component.addStyleName("class1");
        component.addStyleName("class2");

        // Update props multiple times
        component.setProps(new TestProps("Update1", 1));
        assertTrue(component.hasStyleName("class1"), 
                "Should still have 'class1' after first props update");
        assertTrue(component.hasStyleName("class2"), 
                "Should still have 'class2' after first props update");

        component.setProps(new TestProps("Update2", 2));
        assertTrue(component.hasStyleName("class1"), 
                "Should still have 'class1' after second props update");
        assertTrue(component.hasStyleName("class2"), 
                "Should still have 'class2' after second props update");

        component.setProps(new TestProps("Update3", 3));
        assertTrue(component.hasStyleName("class1"), 
                "Should still have 'class1' after third props update");
        assertTrue(component.hasStyleName("class2"), 
                "Should still have 'class2' after third props update");
    }

    /**
     * Test multiple style operations don't corrupt props state.
     * <p>
     * **Validates: Requirements 2.2, 3.4**
     * </p>
     */
    @Test
    public void testMultipleStyleOperations() {
        final TestProps props = new TestProps("Test", 123);
        final TestComponent component = new TestComponent(props);
        PWindow.getMain().add(component);

        // Perform multiple style operations
        component.addStyleName("class1");
        assertEquals(props, component.getCurrentProps(), 
                "Props should remain unchanged");

        component.addStyleName("class2");
        assertEquals(props, component.getCurrentProps(), 
                "Props should remain unchanged");

        component.removeStyleName("class1");
        assertEquals(props, component.getCurrentProps(), 
                "Props should remain unchanged");

        component.setStyleName("new-class");
        assertEquals(props, component.getCurrentProps(), 
                "Props should remain unchanged");

        component.addStyleName("another-class");
        assertEquals(props, component.getCurrentProps(), 
                "Props should remain unchanged");
    }

    /**
     * Test that empty or null style names are handled correctly.
     * <p>
     * **Validates: Requirement 2.2**
     * </p>
     */
    @Test
    public void testEmptyStyleNames() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Add a valid style name first
        component.addStyleName("valid-class");
        
        // Try to add empty string (should be handled gracefully)
        component.addStyleName("");
        
        // Verify the valid class is still there
        assertTrue(component.hasStyleName("valid-class"), 
                "Should still have 'valid-class'");
    }

    /**
     * Test that setStyleName with null clears all styles.
     * <p>
     * **Validates: Requirement 2.2**
     * </p>
     */
    @Test
    public void testSetStyleNameNull() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Add some styles
        component.addStyleName("class1");
        component.addStyleName("class2");
        
        // Set style name to null (should clear all)
        component.setStyleName(null);
        
        // Verify styles are cleared
        final String styleName = component.getStyleName();
        assertTrue(styleName == null || styleName.isEmpty(), 
                "Style name should be null or empty");
    }
}
