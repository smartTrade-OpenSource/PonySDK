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
 * Unit tests for PComponent props updates with different widget states.
 * <p>
 * Tests that props updates work correctly regardless of the component's widget state
 * (visibility, styles, etc.), ensuring that PWidget state and PComponent props state
 * are truly independent.
 * </p>
 * <p>
 * **Validates: Requirements 3.1, 3.2, 3.3**
 * </p>
 */
public class PComponentPropsWidgetStateTest {

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
     * Test that setProps() works on a visible component.
     * <p>
     * **Validates: Requirement 3.1**
     * </p>
     */
    @Test
    public void testSetPropsOnVisibleComponent() {
        final TestProps initialProps = new TestProps("Initial", 0, false);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Ensure component is visible
        assertTrue(component.isVisible(), "Component should be visible by default");

        // Update props
        final TestProps newProps = new TestProps("Updated", 1, true);
        component.setProps(newProps);

        // Verify props were updated
        assertEquals(newProps, component.getCurrentProps(),
                "Current props should be updated on visible component");
        assertEquals(initialProps, component.getPreviousProps(),
                "Previous props should be stored");

        // Verify visibility state is unchanged
        assertTrue(component.isVisible(),
                "Component should remain visible after props update");
    }

    /**
     * Test that setProps() works on a hidden component.
     * <p>
     * **Validates: Requirements 3.1, 3.2**
     * </p>
     */
    @Test
    public void testSetPropsOnHiddenComponent() {
        final TestProps initialProps = new TestProps("Initial", 0, false);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Hide the component
        component.setVisible(false);
        assertFalse(component.isVisible(), "Component should be hidden");

        // Update props while hidden
        final TestProps newProps = new TestProps("Updated", 1, true);
        component.setProps(newProps);

        // Verify props were updated
        assertEquals(newProps, component.getCurrentProps(),
                "Current props should be updated on hidden component");
        assertEquals(initialProps, component.getPreviousProps(),
                "Previous props should be stored");

        // Verify visibility state is unchanged
        assertFalse(component.isVisible(),
                "Component should remain hidden after props update");
    }

    /**
     * Test that setProps() works on a component with custom styles.
     * <p>
     * **Validates: Requirements 3.1, 3.3**
     * </p>
     */
    @Test
    public void testSetPropsOnStyledComponent() {
        final TestProps initialProps = new TestProps("Initial", 0, false);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Add custom styles
        component.addStyleName("custom-class");
        component.addStyleName("active-state");
        assertTrue(component.hasStyleName("custom-class"), "Should have 'custom-class'");
        assertTrue(component.hasStyleName("active-state"), "Should have 'active-state'");

        // Update props
        final TestProps newProps = new TestProps("Updated", 1, true);
        component.setProps(newProps);

        // Verify props were updated
        assertEquals(newProps, component.getCurrentProps(),
                "Current props should be updated on styled component");
        assertEquals(initialProps, component.getPreviousProps(),
                "Previous props should be stored");

        // Verify styles are unchanged
        assertTrue(component.hasStyleName("custom-class"),
                "Should still have 'custom-class' after props update");
        assertTrue(component.hasStyleName("active-state"),
                "Should still have 'active-state' after props update");
    }

    /**
     * Test that setProps() works on a component without custom styles.
     * <p>
     * **Validates: Requirement 3.1**
     * </p>
     */
    @Test
    public void testSetPropsOnUnstyledComponent() {
        final TestProps initialProps = new TestProps("Initial", 0, false);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Verify no custom styles
        final String styleName = component.getStyleName();
        assertTrue(styleName == null || styleName.isEmpty(),
                "Component should have no custom styles");

        // Update props
        final TestProps newProps = new TestProps("Updated", 1, true);
        component.setProps(newProps);

        // Verify props were updated
        assertEquals(newProps, component.getCurrentProps(),
                "Current props should be updated on unstyled component");
        assertEquals(initialProps, component.getPreviousProps(),
                "Previous props should be stored");
    }

    /**
     * Test that setProps() works on a hidden component with custom styles.
     * <p>
     * **Validates: Requirements 3.1, 3.2, 3.3**
     * </p>
     */
    @Test
    public void testSetPropsOnHiddenStyledComponent() {
        final TestProps initialProps = new TestProps("Initial", 0, false);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Hide the component and add styles
        component.setVisible(false);
        component.addStyleName("hidden-styled");
        component.addStyleName("special");

        assertFalse(component.isVisible(), "Component should be hidden");
        assertTrue(component.hasStyleName("hidden-styled"), "Should have 'hidden-styled'");
        assertTrue(component.hasStyleName("special"), "Should have 'special'");

        // Update props
        final TestProps newProps = new TestProps("Updated", 1, true);
        component.setProps(newProps);

        // Verify props were updated
        assertEquals(newProps, component.getCurrentProps(),
                "Current props should be updated on hidden styled component");
        assertEquals(initialProps, component.getPreviousProps(),
                "Previous props should be stored");

        // Verify widget state is unchanged
        assertFalse(component.isVisible(),
                "Component should remain hidden after props update");
        assertTrue(component.hasStyleName("hidden-styled"),
                "Should still have 'hidden-styled' after props update");
        assertTrue(component.hasStyleName("special"),
                "Should still have 'special' after props update");
    }

    /**
     * Test multiple props updates on a component with changing widget states.
     * <p>
     * **Validates: Requirements 3.1, 3.2, 3.3**
     * </p>
     */
    @Test
    public void testMultiplePropsUpdatesWithChangingWidgetStates() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Update 1: Visible, no styles
        final TestProps props1 = new TestProps("Update1", 1, false);
        component.setProps(props1);
        assertEquals(props1, component.getCurrentProps(),
                "Props should be updated (visible, no styles)");

        // Change to hidden
        component.setVisible(false);

        // Update 2: Hidden, no styles
        final TestProps props2 = new TestProps("Update2", 2, true);
        component.setProps(props2);
        assertEquals(props2, component.getCurrentProps(),
                "Props should be updated (hidden, no styles)");
        assertEquals(props1, component.getPreviousProps(),
                "Previous props should be props1");

        // Add styles
        component.addStyleName("styled");

        // Update 3: Hidden, with styles
        final TestProps props3 = new TestProps("Update3", 3, false);
        component.setProps(props3);
        assertEquals(props3, component.getCurrentProps(),
                "Props should be updated (hidden, with styles)");
        assertEquals(props2, component.getPreviousProps(),
                "Previous props should be props2");

        // Change to visible
        component.setVisible(true);

        // Update 4: Visible, with styles
        final TestProps props4 = new TestProps("Update4", 4, true);
        component.setProps(props4);
        assertEquals(props4, component.getCurrentProps(),
                "Props should be updated (visible, with styles)");
        assertEquals(props3, component.getPreviousProps(),
                "Previous props should be props3");

        // Verify final widget state
        assertTrue(component.isVisible(), "Component should be visible");
        assertTrue(component.hasStyleName("styled"), "Should have 'styled'");
    }

    /**
     * Test that props updates always propagate regardless of widget state.
     * <p>
     * **Validates: Requirements 3.1, 3.2, 3.3**
     * </p>
     */
    @Test
    public void testPropsUpdatesAlwaysPropagate() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Test various widget state combinations
        final boolean[] visibilityStates = {true, false, true, false};
        final String[][] styleStates = {
                {},
                {"class1"},
                {"class1", "class2"},
                {"class3"}
        };

        TestProps previousProps = TestProps.defaults();

        for (int i = 0; i < visibilityStates.length; i++) {
            // Set widget state
            component.setVisible(visibilityStates[i]);
            component.setStyleName(null); // Clear styles
            for (String style : styleStates[i]) {
                component.addStyleName(style);
            }

            // Update props
            final TestProps newProps = new TestProps("Update" + i, i, i % 2 == 0);
            component.setProps(newProps);

            // Verify props were updated
            assertEquals(newProps, component.getCurrentProps(),
                    "Props should be updated in iteration " + i);
            if (i > 0) {
                assertEquals(previousProps, component.getPreviousProps(),
                        "Previous props should be correct in iteration " + i);
            }

            // Verify widget state matches what we set
            assertEquals(visibilityStates[i], component.isVisible(),
                    "Visibility should match in iteration " + i);
            for (String style : styleStates[i]) {
                assertTrue(component.hasStyleName(style),
                        "Should have style '" + style + "' in iteration " + i);
            }

            previousProps = newProps;
        }
    }

    /**
     * Test that props updates work with dimension changes.
     * <p>
     * **Validates: Requirements 3.1, 3.3**
     * </p>
     */
    @Test
    public void testSetPropsWithDimensionChanges() {
        final TestProps initialProps = new TestProps("Initial", 0, false);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Set dimensions
        component.setWidth("200px");
        component.setHeight("100px");

        // Update props
        final TestProps newProps = new TestProps("Updated", 1, true);
        component.setProps(newProps);

        // Verify props were updated
        assertEquals(newProps, component.getCurrentProps(),
                "Current props should be updated");
        assertEquals(initialProps, component.getPreviousProps(),
                "Previous props should be stored");

        // Note: PWidget doesn't provide getters for width/height in the current implementation,
        // but the dimensions should be preserved internally
    }

    /**
     * Test rapid props updates with different widget states.
     * <p>
     * **Validates: Requirements 3.1, 3.2, 3.3**
     * </p>
     */
    @Test
    public void testRapidPropsUpdatesWithWidgetStateChanges() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        TestProps lastProps = TestProps.defaults();

        // Perform rapid updates with interleaved widget state changes
        for (int i = 1; i <= 10; i++) {
            // Change widget state
            if (i % 3 == 0) {
                component.setVisible(!component.isVisible());
            }
            if (i % 2 == 0) {
                component.addStyleName("class" + i);
            }

            // Update props
            final TestProps newProps = new TestProps("Rapid" + i, i, i % 2 == 0);
            component.setProps(newProps);

            // Verify props were updated
            assertEquals(newProps, component.getCurrentProps(),
                    "Props should be updated in rapid iteration " + i);
            assertEquals(lastProps, component.getPreviousProps(),
                    "Previous props should be correct in rapid iteration " + i);

            lastProps = newProps;
        }
    }

    /**
     * Test that props updates work when component is re-shown after being hidden.
     * <p>
     * **Validates: Requirements 3.1, 3.2**
     * </p>
     */
    @Test
    public void testPropsUpdatesAfterVisibilityToggle() {
        final TestProps initialProps = new TestProps("Initial", 0, false);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Hide component
        component.setVisible(false);

        // Update props while hidden
        final TestProps hiddenProps = new TestProps("Hidden", 1, true);
        component.setProps(hiddenProps);
        assertEquals(hiddenProps, component.getCurrentProps(),
                "Props should be updated while hidden");

        // Show component
        component.setVisible(true);

        // Update props while visible
        final TestProps visibleProps = new TestProps("Visible", 2, false);
        component.setProps(visibleProps);
        assertEquals(visibleProps, component.getCurrentProps(),
                "Props should be updated after being shown");
        assertEquals(hiddenProps, component.getPreviousProps(),
                "Previous props should be the hidden props");

        // Verify visibility
        assertTrue(component.isVisible(), "Component should be visible");
    }

    /**
     * Test that props updates work when styles are changed multiple times.
     * <p>
     * **Validates: Requirements 3.1, 3.3**
     * </p>
     */
    @Test
    public void testPropsUpdatesWithMultipleStyleChanges() {
        final TestProps initialProps = new TestProps("Initial", 0, false);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Add initial styles
        component.addStyleName("style1");

        // Update props
        final TestProps props1 = new TestProps("Update1", 1, true);
        component.setProps(props1);
        assertEquals(props1, component.getCurrentProps(),
                "Props should be updated with style1");

        // Change styles
        component.removeStyleName("style1");
        component.addStyleName("style2");

        // Update props again
        final TestProps props2 = new TestProps("Update2", 2, false);
        component.setProps(props2);
        assertEquals(props2, component.getCurrentProps(),
                "Props should be updated with style2");
        assertEquals(props1, component.getPreviousProps(),
                "Previous props should be props1");

        // Replace all styles
        component.setStyleName("style3");

        // Update props again
        final TestProps props3 = new TestProps("Update3", 3, true);
        component.setProps(props3);
        assertEquals(props3, component.getCurrentProps(),
                "Props should be updated with style3");
        assertEquals(props2, component.getPreviousProps(),
                "Previous props should be props2");

        // Verify final style state
        assertEquals("style3", component.getStyleName(),
                "Final style should be style3");
    }
}
