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
 * Unit tests for widget state changes not affecting PComponent props.
 * <p>
 * Tests that changing widget state (visibility, styles, dimensions) doesn't affect
 * the component's props state, ensuring complete independence between PWidget state
 * and PComponent props.
 * </p>
 * <p>
 * **Validates: Requirements 3.4, 3.5**
 * </p>
 */
public class PComponentWidgetStatePropsTest {

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
     * Test props record for widget state tests.
     */
    record TestProps(String title, int count, boolean active) {
        static TestProps defaults() {
            return new TestProps("Test", 0, false);
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
     * Test that changing visibility doesn't affect props.
     * <p>
     * **Validates: Requirement 3.4**
     * </p>
     */
    @Test
    public void testVisibilityChangeDoesNotAffectProps() {
        final TestProps initialProps = new TestProps("Initial", 42, true);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Verify initial props
        assertEquals(initialProps, component.getCurrentProps(),
                "Initial props should be set");

        // Change visibility to hidden
        component.setVisible(false);

        // Verify props are unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Props should remain unchanged after hiding");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null (no props update occurred)");

        // Change visibility back to visible
        component.setVisible(true);

        // Verify props are still unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Props should remain unchanged after showing");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null (no props update occurred)");
    }

    /**
     * Test that adding style names doesn't affect props.
     * <p>
     * **Validates: Requirement 3.4**
     * </p>
     */
    @Test
    public void testAddStyleNameDoesNotAffectProps() {
        final TestProps initialProps = new TestProps("Styled", 100, false);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Verify initial props
        assertEquals(initialProps, component.getCurrentProps(),
                "Initial props should be set");

        // Add style names
        component.addStyleName("custom-class");
        component.addStyleName("active-state");
        component.addStyleName("highlighted");

        // Verify props are unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Props should remain unchanged after adding styles");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null (no props update occurred)");

        // Verify styles were actually added
        assertTrue(component.hasStyleName("custom-class"),
                "Style 'custom-class' should be present");
        assertTrue(component.hasStyleName("active-state"),
                "Style 'active-state' should be present");
        assertTrue(component.hasStyleName("highlighted"),
                "Style 'highlighted' should be present");
    }

    /**
     * Test that removing style names doesn't affect props.
     * <p>
     * **Validates: Requirement 3.4**
     * </p>
     */
    @Test
    public void testRemoveStyleNameDoesNotAffectProps() {
        final TestProps initialProps = new TestProps("Unstyled", 200, true);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Add some styles first
        component.addStyleName("class1");
        component.addStyleName("class2");
        component.addStyleName("class3");

        // Verify initial props are unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Props should be unchanged after adding styles");

        // Remove style names
        component.removeStyleName("class1");
        component.removeStyleName("class3");

        // Verify props are still unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Props should remain unchanged after removing styles");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null (no props update occurred)");

        // Verify styles were actually removed
        assertFalse(component.hasStyleName("class1"),
                "Style 'class1' should be removed");
        assertTrue(component.hasStyleName("class2"),
                "Style 'class2' should still be present");
        assertFalse(component.hasStyleName("class3"),
                "Style 'class3' should be removed");
    }

    /**
     * Test that setting style name doesn't affect props.
     * <p>
     * **Validates: Requirement 3.4**
     * </p>
     */
    @Test
    public void testSetStyleNameDoesNotAffectProps() {
        final TestProps initialProps = new TestProps("Replaced", 300, false);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Add initial styles
        component.addStyleName("old-class1");
        component.addStyleName("old-class2");

        // Replace all styles with setStyleName
        component.setStyleName("new-class");

        // Verify props are unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Props should remain unchanged after setting style name");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null (no props update occurred)");

        // Verify style was actually set
        assertEquals("new-class", component.getStyleName(),
                "Style name should be 'new-class'");
    }

    /**
     * Test that setting dimensions doesn't affect props.
     * <p>
     * **Validates: Requirement 3.4**
     * </p>
     */
    @Test
    public void testSetDimensionsDoesNotAffectProps() {
        final TestProps initialProps = new TestProps("Sized", 400, true);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Verify initial props
        assertEquals(initialProps, component.getCurrentProps(),
                "Initial props should be set");

        // Set dimensions
        component.setWidth("300px");
        component.setHeight("200px");

        // Verify props are unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Props should remain unchanged after setting dimensions");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null (no props update occurred)");
    }

    /**
     * Test that multiple widget state changes don't affect props.
     * <p>
     * **Validates: Requirements 3.4, 3.5**
     * </p>
     */
    @Test
    public void testMultipleWidgetStateChangesDoNotAffectProps() {
        final TestProps initialProps = new TestProps("Complex", 500, false);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Verify initial props
        assertEquals(initialProps, component.getCurrentProps(),
                "Initial props should be set");

        // Perform multiple widget state changes
        component.setVisible(false);
        component.addStyleName("class1");
        component.setWidth("100px");
        component.setVisible(true);
        component.addStyleName("class2");
        component.setHeight("50px");
        component.removeStyleName("class1");
        component.setStyleName("final-class");
        component.setVisible(false);

        // Verify props are still unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Props should remain unchanged after multiple widget state changes");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null (no props update occurred)");

        // Verify widget state changes were applied
        assertFalse(component.isVisible(),
                "Component should be hidden");
        assertEquals("final-class", component.getStyleName(),
                "Style name should be 'final-class'");
    }

    /**
     * Test that widget state changes don't affect props even after props updates.
     * <p>
     * **Validates: Requirements 3.4, 3.5**
     * </p>
     */
    @Test
    public void testWidgetStateChangesDoNotAffectPropsAfterPropsUpdate() {
        final TestProps initialProps = new TestProps("Initial", 0, false);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Update props first
        final TestProps updatedProps = new TestProps("Updated", 1, true);
        component.setProps(updatedProps);

        // Verify props were updated
        assertEquals(updatedProps, component.getCurrentProps(),
                "Props should be updated");
        assertEquals(initialProps, component.getPreviousProps(),
                "Previous props should be initial props");

        // Now change widget state
        component.setVisible(false);
        component.addStyleName("test-class");
        component.setWidth("150px");

        // Verify props are unchanged by widget state changes
        assertEquals(updatedProps, component.getCurrentProps(),
                "Current props should remain unchanged after widget state changes");
        assertEquals(initialProps, component.getPreviousProps(),
                "Previous props should remain unchanged after widget state changes");
    }

    /**
     * Test that rapid widget state changes don't affect props.
     * <p>
     * **Validates: Requirements 3.4, 3.5**
     * </p>
     */
    @Test
    public void testRapidWidgetStateChangesDoNotAffectProps() {
        final TestProps initialProps = new TestProps("Rapid", 999, true);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Perform rapid widget state changes
        for (int i = 0; i < 20; i++) {
            component.setVisible(i % 2 == 0);
            component.addStyleName("class" + i);
            if (i % 3 == 0) {
                component.setWidth((100 + i * 10) + "px");
            }
            if (i % 5 == 0) {
                component.setHeight((50 + i * 5) + "px");
            }
        }

        // Verify props are unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Props should remain unchanged after rapid widget state changes");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null (no props update occurred)");
    }

    /**
     * Test that widget state changes between props updates don't corrupt props.
     * <p>
     * **Validates: Requirements 3.4, 3.5**
     * </p>
     */
    @Test
    public void testWidgetStateChangesBetweenPropsUpdates() {
        final TestProps props1 = new TestProps("Props1", 1, false);
        final TestComponent component = new TestComponent(props1);
        PWindow.getMain().add(component);

        // First props update
        final TestProps props2 = new TestProps("Props2", 2, true);
        component.setProps(props2);
        assertEquals(props2, component.getCurrentProps(),
                "Props should be updated to props2");
        assertEquals(props1, component.getPreviousProps(),
                "Previous props should be props1");

        // Widget state changes
        component.setVisible(false);
        component.addStyleName("between-updates");
        component.setWidth("200px");

        // Verify props are unchanged by widget state changes
        assertEquals(props2, component.getCurrentProps(),
                "Current props should still be props2");
        assertEquals(props1, component.getPreviousProps(),
                "Previous props should still be props1");

        // Second props update
        final TestProps props3 = new TestProps("Props3", 3, false);
        component.setProps(props3);
        assertEquals(props3, component.getCurrentProps(),
                "Props should be updated to props3");
        assertEquals(props2, component.getPreviousProps(),
                "Previous props should be props2");

        // More widget state changes
        component.setVisible(true);
        component.removeStyleName("between-updates");
        component.setHeight("100px");

        // Verify props are still unchanged by widget state changes
        assertEquals(props3, component.getCurrentProps(),
                "Current props should still be props3");
        assertEquals(props2, component.getPreviousProps(),
                "Previous props should still be props2");
    }

    /**
     * Test that setting title doesn't affect props.
     * <p>
     * **Validates: Requirement 3.4**
     * </p>
     */
    @Test
    public void testSetTitleDoesNotAffectProps() {
        final TestProps initialProps = new TestProps("Titled", 600, false);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Set title
        component.setTitle("Component Title");

        // Verify props are unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Props should remain unchanged after setting title");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null (no props update occurred)");
    }

    /**
     * Test that all PWidget methods don't affect props.
     * <p>
     * **Validates: Requirements 3.4, 3.5**
     * </p>
     */
    @Test
    public void testAllWidgetMethodsDoNotAffectProps() {
        final TestProps initialProps = new TestProps("Complete", 700, true);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Call various PWidget methods
        component.setVisible(false);
        component.setVisible(true);
        component.addStyleName("style1");
        component.addStyleName("style2");
        component.removeStyleName("style1");
        component.setStyleName("final-style");
        component.setWidth("250px");
        component.setHeight("150px");
        component.setTitle("Test Title");

        // Verify props are completely unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Props should remain unchanged after all widget method calls");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null (no props update occurred)");

        // Verify widget state was actually changed
        assertTrue(component.isVisible(), "Component should be visible");
        assertEquals("final-style", component.getStyleName(),
                "Style should be 'final-style'");
    }
}
