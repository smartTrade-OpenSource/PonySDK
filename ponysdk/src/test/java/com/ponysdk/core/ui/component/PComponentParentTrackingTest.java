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
import com.ponysdk.core.ui.basic.IsPWidget;
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
 * Unit tests for PComponent parent tracking functionality.
 * <p>
 * Tests that PComponent correctly inherits parent-child relationship management
 * from PWidget and that parent tracking works correctly when components are
 * added to containers.
 * </p>
 * <p>
 * **Validates: Requirements 2.5, 8.1, 8.5**
 * </p>
 */
public class PComponentParentTrackingTest {

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
     * Test props record for parent tracking tests.
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
     * Test that getParent() returns null initially.
     * <p>
     * **Validates: Requirements 2.5, 8.5**
     * </p>
     */
    @Test
    public void testGetParentInitiallyNull() {
        final TestComponent component = new TestComponent(TestProps.defaults());

        // Initially, component should have no parent
        assertNull(component.getParent(), "Component should have no parent initially");
    }

    /**
     * Test that adding component to window sets parent correctly.
     * <p>
     * **Validates: Requirements 8.1, 8.5**
     * </p>
     */
    @Test
    public void testAddingToWindowSetsParent() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        final PWindow window = PWindow.getMain();

        // Initially, component should have no parent
        assertNull(component.getParent(), "Component should have no parent initially");

        // Add component to window
        window.add(component);

        // Verify parent is set automatically (parent will be the root panel, not the window)
        assertNotNull(component.getParent(),
                "Component parent should be set automatically when added to window");
    }

    /**
     * Test that parent tracking works with multiple components in same window.
     * <p>
     * **Validates: Requirements 8.1, 8.5**
     * </p>
     */
    @Test
    public void testMultipleComponentsInSameWindow() {
        final TestComponent component1 = new TestComponent(new TestProps("Component1", 1));
        final TestComponent component2 = new TestComponent(new TestProps("Component2", 2));
        final TestComponent component3 = new TestComponent(new TestProps("Component3", 3));
        final PWindow window = PWindow.getMain();

        // Add all components to window
        window.add(component1);
        window.add(component2);
        window.add(component3);

        // Verify all components have a parent set
        assertNotNull(component1.getParent(),
                "Component1 should have a parent");
        assertNotNull(component2.getParent(),
                "Component2 should have a parent");
        assertNotNull(component3.getParent(),
                "Component3 should have a parent");
        
        // Verify all components have the same parent
        assertEquals(component1.getParent(), component2.getParent(),
                "All components should have the same parent");
        assertEquals(component1.getParent(), component3.getParent(),
                "All components should have the same parent");
    }

    /**
     * Test that parent reference is maintained after props updates.
     * <p>
     * **Validates: Requirements 2.5, 8.1**
     * </p>
     */
    @Test
    public void testParentReferenceAfterPropsUpdate() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        final PWindow window = PWindow.getMain();

        // Add component to window
        window.add(component);
        final IsPWidget initialParent = component.getParent();
        assertNotNull(initialParent, "Component parent should be set");

        // Update props
        component.setProps(new TestProps("Updated", 42));

        // Verify parent reference is maintained
        assertEquals(initialParent, component.getParent(),
                "Component parent should remain unchanged after props update");
    }

    /**
     * Test that parent reference is maintained after visibility changes.
     * <p>
     * **Validates: Requirements 2.5, 8.1**
     * </p>
     */
    @Test
    public void testParentReferenceAfterVisibilityChange() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        final PWindow window = PWindow.getMain();

        // Add component to window
        window.add(component);
        final IsPWidget initialParent = component.getParent();
        assertNotNull(initialParent, "Component parent should be set");

        // Change visibility
        component.setVisible(false);
        assertEquals(initialParent, component.getParent(),
                "Component parent should remain unchanged after hiding");

        component.setVisible(true);
        assertEquals(initialParent, component.getParent(),
                "Component parent should remain unchanged after showing");
    }

    /**
     * Test that parent reference is maintained after style changes.
     * <p>
     * **Validates: Requirements 2.5, 8.1**
     * </p>
     */
    @Test
    public void testParentReferenceAfterStyleChange() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        final PWindow window = PWindow.getMain();

        // Add component to window
        window.add(component);
        final IsPWidget initialParent = component.getParent();
        assertNotNull(initialParent, "Component parent should be set");

        // Change styles
        component.addStyleName("test-style");
        assertEquals(initialParent, component.getParent(),
                "Component parent should remain unchanged after adding style");

        component.setStyleName("another-style");
        assertEquals(initialParent, component.getParent(),
                "Component parent should remain unchanged after setting style");
    }

    /**
     * Test that getParent() returns correct type (IsPWidget).
     * <p>
     * **Validates: Requirements 2.5, 8.5**
     * </p>
     */
    @Test
    public void testGetParentReturnsIsPWidget() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        final PWindow window = PWindow.getMain();

        window.add(component);

        final IsPWidget parent = component.getParent();
        assertNotNull(parent, "Parent should not be null");
        assertInstanceOf(IsPWidget.class, parent,
                "Parent should be an instance of IsPWidget");
    }

    /**
     * Test that parent reference persists across multiple operations.
     * <p>
     * **Validates: Requirements 2.5, 8.1**
     * </p>
     */
    @Test
    public void testParentReferencePersistence() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        final PWindow window = PWindow.getMain();

        // Add component to window
        window.add(component);
        final IsPWidget initialParent = component.getParent();
        assertNotNull(initialParent, "Component should have a parent");

        // Perform various operations
        component.setProps(new TestProps("Updated", 1));
        component.setVisible(false);
        component.addStyleName("test-style");
        component.setWidth("100px");
        component.setHeight("50px");
        component.setVisible(true);
        component.setProps(new TestProps("Updated Again", 2));

        // Verify parent reference is still the same
        assertEquals(initialParent, component.getParent(),
                "Component parent should remain unchanged after multiple operations");
    }
}
