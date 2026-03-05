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
 * Unit tests for PComponent visibility control methods.
 * <p>
 * Tests that PComponent correctly inherits visibility control from PWidget
 * and that visibility changes don't affect props state.
 * </p>
 * <p>
 * **Validates: Requirements 2.1, 3.4**
 * </p>
 */
public class PComponentVisibilityTest {

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
     * Test props record for visibility tests.
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
     * Test that setVisible() and isVisible() work correctly.
     * <p>
     * **Validates: Requirement 2.1**
     * </p>
     */
    @Test
    public void testVisibilityControl() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Component should be visible by default
        assertTrue(component.isVisible(), "Component should be visible by default");

        // Hide the component
        component.setVisible(false);
        assertFalse(component.isVisible(), "Component should be hidden after setVisible(false)");

        // Show the component again
        component.setVisible(true);
        assertTrue(component.isVisible(), "Component should be visible after setVisible(true)");
    }

    /**
     * Test that visibility changes don't affect props state.
     * <p>
     * **Validates: Requirement 3.4**
     * </p>
     */
    @Test
    public void testVisibilityDoesNotAffectProps() {
        final TestProps initialProps = new TestProps("Initial", 42);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Verify initial props
        assertEquals(initialProps, component.getCurrentProps(), "Initial props should be set");
        assertNull(component.getPreviousProps(), "Previous props should be null initially");

        // Change visibility
        component.setVisible(false);

        // Verify props are unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Current props should remain unchanged after visibility change");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null after visibility change");

        // Change visibility again
        component.setVisible(true);

        // Verify props are still unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Current props should remain unchanged after second visibility change");
        assertNull(component.getPreviousProps(),
                "Previous props should still be null after second visibility change");
    }

    /**
     * Test that props updates work correctly regardless of visibility state.
     * <p>
     * **Validates: Requirements 3.1, 3.4**
     * </p>
     */
    @Test
    public void testPropsUpdateWhileHidden() {
        final TestProps initialProps = new TestProps("Initial", 0);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Hide the component
        component.setVisible(false);
        assertFalse(component.isVisible(), "Component should be hidden");

        // Update props while hidden
        final TestProps newProps = new TestProps("Updated", 1);
        component.setProps(newProps);

        // Verify props were updated
        assertEquals(newProps, component.getCurrentProps(),
                "Current props should be updated even when hidden");
        assertEquals(initialProps, component.getPreviousProps(),
                "Previous props should be stored");

        // Verify visibility state is unchanged
        assertFalse(component.isVisible(),
                "Component should still be hidden after props update");
    }

    /**
     * Test that props updates work correctly when component is visible.
     * <p>
     * **Validates: Requirements 3.1, 3.4**
     * </p>
     */
    @Test
    public void testPropsUpdateWhileVisible() {
        final TestProps initialProps = new TestProps("Initial", 0);
        final TestComponent component = new TestComponent(initialProps);
        PWindow.getMain().add(component);

        // Ensure component is visible
        assertTrue(component.isVisible(), "Component should be visible");

        // Update props while visible
        final TestProps newProps = new TestProps("Updated", 1);
        component.setProps(newProps);

        // Verify props were updated
        assertEquals(newProps, component.getCurrentProps(),
                "Current props should be updated when visible");
        assertEquals(initialProps, component.getPreviousProps(),
                "Previous props should be stored");

        // Verify visibility state is unchanged
        assertTrue(component.isVisible(),
                "Component should still be visible after props update");
    }

    /**
     * Test multiple visibility toggles don't corrupt props state.
     * <p>
     * **Validates: Requirements 2.1, 3.4**
     * </p>
     */
    @Test
    public void testMultipleVisibilityToggles() {
        final TestProps props = new TestProps("Test", 123);
        final TestComponent component = new TestComponent(props);
        PWindow.getMain().add(component);

        // Toggle visibility multiple times
        for (int i = 0; i < 5; i++) {
            component.setVisible(false);
            assertFalse(component.isVisible(), "Component should be hidden");
            assertEquals(props, component.getCurrentProps(), "Props should remain unchanged");

            component.setVisible(true);
            assertTrue(component.isVisible(), "Component should be visible");
            assertEquals(props, component.getCurrentProps(), "Props should remain unchanged");
        }
    }

    /**
     * Test that visibility state is independent of props updates.
     * <p>
     * **Validates: Requirement 3.5**
     * </p>
     */
    @Test
    public void testVisibilityIndependentOfPropsUpdates() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        PWindow.getMain().add(component);

        // Set initial visibility state
        component.setVisible(false);
        assertFalse(component.isVisible(), "Component should be hidden");

        // Update props multiple times
        component.setProps(new TestProps("Update1", 1));
        assertFalse(component.isVisible(),
                "Component should remain hidden after first props update");

        component.setProps(new TestProps("Update2", 2));
        assertFalse(component.isVisible(),
                "Component should remain hidden after second props update");

        component.setProps(new TestProps("Update3", 3));
        assertFalse(component.isVisible(),
                "Component should remain hidden after third props update");

        // Change visibility
        component.setVisible(true);
        assertTrue(component.isVisible(), "Component should be visible");

        // Update props again
        component.setProps(new TestProps("Update4", 4));
        assertTrue(component.isVisible(),
                "Component should remain visible after props update");
    }
}
