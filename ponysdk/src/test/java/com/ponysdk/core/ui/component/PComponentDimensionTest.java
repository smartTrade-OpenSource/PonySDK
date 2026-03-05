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
 * Unit tests for PComponent dimension control methods.
 * <p>
 * Tests that PComponent correctly inherits dimension control from PWidget
 * and that dimension changes don't affect props state.
 * </p>
 * <p>
 * **Validates: Requirements 2.3, 3.4**
 * </p>
 */
public class PComponentDimensionTest {

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
     * Test props record for dimension tests.
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
     * Test that setWidth() and getWidth() work correctly.
     * <p>
     * **Validates: Requirement 2.3**
     * </p>
     */
    @Test
    public void testWidthControl() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Width should be null by default
        assertNull(component.getWidth(), "Width should be null by default");

        // Set width
        component.setWidth("100px");
        assertEquals("100px", component.getWidth(), "Width should be '100px'");

        // Change width
        component.setWidth("200px");
        assertEquals("200px", component.getWidth(), "Width should be '200px'");

        // Set width to percentage
        component.setWidth("50%");
        assertEquals("50%", component.getWidth(), "Width should be '50%'");
    }

    /**
     * Test that setHeight() and getHeight() work correctly.
     * <p>
     * **Validates: Requirement 2.3**
     * </p>
     */
    @Test
    public void testHeightControl() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Height should be null by default
        assertNull(component.getHeight(), "Height should be null by default");

        // Set height
        component.setHeight("100px");
        assertEquals("100px", component.getHeight(), "Height should be '100px'");

        // Change height
        component.setHeight("200px");
        assertEquals("200px", component.getHeight(), "Height should be '200px'");

        // Set height to percentage
        component.setHeight("75%");
        assertEquals("75%", component.getHeight(), "Height should be '75%'");
    }

    /**
     * Test that both width and height can be set independently.
     * <p>
     * **Validates: Requirement 2.3**
     * </p>
     */
    @Test
    public void testWidthAndHeightIndependent() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Set width
        component.setWidth("100px");
        assertEquals("100px", component.getWidth(), "Width should be '100px'");
        assertNull(component.getHeight(), "Height should still be null");

        // Set height
        component.setHeight("200px");
        assertEquals("100px", component.getWidth(), "Width should still be '100px'");
        assertEquals("200px", component.getHeight(), "Height should be '200px'");

        // Change width
        component.setWidth("150px");
        assertEquals("150px", component.getWidth(), "Width should be '150px'");
        assertEquals("200px", component.getHeight(), "Height should still be '200px'");

        // Change height
        component.setHeight("250px");
        assertEquals("150px", component.getWidth(), "Width should still be '150px'");
        assertEquals("250px", component.getHeight(), "Height should be '250px'");
    }

    /**
     * Test that dimension changes don't affect props state.
     * <p>
     * **Validates: Requirement 3.4**
     * </p>
     */
    @Test
    public void testDimensionChangesDoNotAffectProps() {
        final TestProps initialProps = new TestProps("Initial", 42);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Verify initial props
        assertEquals(initialProps, component.getCurrentProps(), 
                "Initial props should be set");
        assertNull(component.getPreviousProps(), 
                "Previous props should be null initially");

        // Set width
        component.setWidth("100px");

        // Verify props are unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Current props should remain unchanged after setting width");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null after setting width");

        // Set height
        component.setHeight("200px");

        // Verify props are still unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Current props should remain unchanged after setting height");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null after setting height");

        // Change both dimensions
        component.setWidth("150px");
        component.setHeight("250px");

        // Verify props are still unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Current props should remain unchanged after changing dimensions");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null after changing dimensions");
    }

    /**
     * Test that props updates work correctly regardless of dimension state.
     * <p>
     * **Validates: Requirements 3.1, 3.4**
     * </p>
     */
    @Test
    public void testPropsUpdateWithDimensions() {
        final TestProps initialProps = new TestProps("Initial", 0);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Set dimensions
        component.setWidth("100px");
        component.setHeight("200px");

        // Update props
        final TestProps newProps = new TestProps("Updated", 1);
        component.setProps(newProps);

        // Verify props were updated
        assertEquals(newProps, component.getCurrentProps(),
                "Current props should be updated");
        assertEquals(initialProps, component.getPreviousProps(),
                "Previous props should be stored");

        // Verify dimensions are unchanged
        assertEquals("100px", component.getWidth(), 
                "Width should still be '100px'");
        assertEquals("200px", component.getHeight(), 
                "Height should still be '200px'");
    }

    /**
     * Test that dimension state is independent of props updates.
     * <p>
     * **Validates: Requirement 3.5**
     * </p>
     */
    @Test
    public void testDimensionsIndependentOfPropsUpdates() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Set initial dimensions
        component.setWidth("100px");
        component.setHeight("200px");

        // Update props multiple times
        component.setProps(new TestProps("Update1", 1));
        assertEquals("100px", component.getWidth(), 
                "Width should still be '100px' after first props update");
        assertEquals("200px", component.getHeight(), 
                "Height should still be '200px' after first props update");

        component.setProps(new TestProps("Update2", 2));
        assertEquals("100px", component.getWidth(), 
                "Width should still be '100px' after second props update");
        assertEquals("200px", component.getHeight(), 
                "Height should still be '200px' after second props update");

        component.setProps(new TestProps("Update3", 3));
        assertEquals("100px", component.getWidth(), 
                "Width should still be '100px' after third props update");
        assertEquals("200px", component.getHeight(), 
                "Height should still be '200px' after third props update");
    }

    /**
     * Test multiple dimension operations don't corrupt props state.
     * <p>
     * **Validates: Requirements 2.3, 3.4**
     * </p>
     */
    @Test
    public void testMultipleDimensionOperations() {
        final TestProps props = new TestProps("Test", 123);
        final TestComponent component = new TestComponent(props);
        PWindow.getMain().add(component);

        // Perform multiple dimension operations
        component.setWidth("100px");
        assertEquals(props, component.getCurrentProps(), 
                "Props should remain unchanged");

        component.setHeight("200px");
        assertEquals(props, component.getCurrentProps(), 
                "Props should remain unchanged");

        component.setWidth("150px");
        assertEquals(props, component.getCurrentProps(), 
                "Props should remain unchanged");

        component.setHeight("250px");
        assertEquals(props, component.getCurrentProps(), 
                "Props should remain unchanged");

        component.setWidth("50%");
        assertEquals(props, component.getCurrentProps(), 
                "Props should remain unchanged");

        component.setHeight("75%");
        assertEquals(props, component.getCurrentProps(), 
                "Props should remain unchanged");
    }

    /**
     * Test that null dimensions are handled correctly.
     * <p>
     * **Validates: Requirement 2.3**
     * </p>
     */
    @Test
    public void testNullDimensions() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Set dimensions
        component.setWidth("100px");
        component.setHeight("200px");
        
        // Clear width
        component.setWidth(null);
        assertNull(component.getWidth(), "Width should be null");
        assertEquals("200px", component.getHeight(), "Height should still be '200px'");
        
        // Clear height
        component.setHeight(null);
        assertNull(component.getWidth(), "Width should still be null");
        assertNull(component.getHeight(), "Height should be null");
    }

    /**
     * Test that empty string dimensions are handled correctly.
     * <p>
     * **Validates: Requirement 2.3**
     * </p>
     */
    @Test
    public void testEmptyDimensions() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Set dimensions to empty strings
        component.setWidth("");
        component.setHeight("");
        
        // Empty strings should be stored as-is
        assertEquals("", component.getWidth(), "Width should be empty string");
        assertEquals("", component.getHeight(), "Height should be empty string");
    }

    /**
     * Test various dimension formats.
     * <p>
     * **Validates: Requirement 2.3**
     * </p>
     */
    @Test
    public void testVariousDimensionFormats() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Test pixels
        component.setWidth("100px");
        assertEquals("100px", component.getWidth());

        // Test percentage
        component.setWidth("50%");
        assertEquals("50%", component.getWidth());

        // Test em units
        component.setWidth("10em");
        assertEquals("10em", component.getWidth());

        // Test rem units
        component.setWidth("5rem");
        assertEquals("5rem", component.getWidth());

        // Test viewport units
        component.setWidth("100vw");
        assertEquals("100vw", component.getWidth());

        // Test auto
        component.setWidth("auto");
        assertEquals("auto", component.getWidth());

        // Test calc
        component.setWidth("calc(100% - 20px)");
        assertEquals("calc(100% - 20px)", component.getWidth());
    }

    /**
     * Test that setting the same dimension value doesn't cause issues.
     * <p>
     * **Validates: Requirement 2.3**
     * </p>
     */
    @Test
    public void testSetSameDimensionValue() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Set width
        component.setWidth("100px");
        assertEquals("100px", component.getWidth());

        // Set the same width again
        component.setWidth("100px");
        assertEquals("100px", component.getWidth());

        // Set height
        component.setHeight("200px");
        assertEquals("200px", component.getHeight());

        // Set the same height again
        component.setHeight("200px");
        assertEquals("200px", component.getHeight());
    }

    /**
     * Test dimension changes with visibility and style changes.
     * <p>
     * **Validates: Requirements 2.3, 3.4**
     * </p>
     */
    @Test
    public void testDimensionsWithOtherWidgetState() {
        final TestProps props = new TestProps("Test", 123);
        final TestComponent component = new TestComponent(props);
        PWindow.getMain().add(component);

        // Set dimensions
        component.setWidth("100px");
        component.setHeight("200px");

        // Change visibility
        component.setVisible(false);
        assertEquals("100px", component.getWidth(), "Width should remain unchanged");
        assertEquals("200px", component.getHeight(), "Height should remain unchanged");
        assertEquals(props, component.getCurrentProps(), "Props should remain unchanged");

        // Add styles
        component.addStyleName("test-class");
        assertEquals("100px", component.getWidth(), "Width should remain unchanged");
        assertEquals("200px", component.getHeight(), "Height should remain unchanged");
        assertEquals(props, component.getCurrentProps(), "Props should remain unchanged");

        // Change dimensions
        component.setWidth("150px");
        component.setHeight("250px");
        assertFalse(component.isVisible(), "Visibility should remain unchanged");
        assertTrue(component.hasStyleName("test-class"), "Style should remain unchanged");
        assertEquals(props, component.getCurrentProps(), "Props should remain unchanged");
    }
}
