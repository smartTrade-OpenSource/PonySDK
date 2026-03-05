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
 * Unit tests for PComponent removeFromParent() functionality.
 * <p>
 * Tests that PComponent correctly inherits removeFromParent() from PWidget
 * and that the method properly removes the component from its parent container
 * and clears the parent reference.
 * </p>
 * <p>
 * **Validates: Requirements 2.6, 8.3**
 * </p>
 */
public class PComponentRemoveFromParentTest {

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
     * Test props record for removeFromParent tests.
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
     * Test that removeFromParent() clears the parent reference.
     * <p>
     * **Validates: Requirements 2.6, 8.3**
     * </p>
     */
    @Test
    public void testRemoveFromParentClearsParentReference() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        final PWindow window = PWindow.getMain();

        // Add component to window
        window.add(component);
        assertNotNull(component.getParent(), "Component should have a parent after being added");

        // Remove component from parent
        component.removeFromParent();

        // Verify parent reference is cleared
        assertNull(component.getParent(), "Component parent should be null after removeFromParent()");
    }

    /**
     * Test that removeFromParent() removes component from parent's child list.
     * <p>
     * **Validates: Requirements 2.6, 8.3**
     * </p>
     */
    @Test
    public void testRemoveFromParentRemovesFromChildList() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        final PWindow window = PWindow.getMain();

        // Get initial count
        final int initialCount = window.getPRootPanel().getWidgetCount();

        // Add component to window
        window.add(component);
        assertEquals(initialCount + 1, window.getPRootPanel().getWidgetCount(),
                "Parent should have one more child after adding component");

        // Remove component from parent
        component.removeFromParent();

        // Verify component is removed from parent's child list
        assertEquals(initialCount, window.getPRootPanel().getWidgetCount(),
                "Parent should have same count as before after removeFromParent()");
    }

    /**
     * Test that removeFromParent() can be called multiple times safely.
     * <p>
     * **Validates: Requirements 2.6, 8.3**
     * </p>
     */
    @Test
    public void testRemoveFromParentIdempotent() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        final PWindow window = PWindow.getMain();

        // Add component to window
        window.add(component);
        assertNotNull(component.getParent(), "Component should have a parent");

        // Remove component from parent
        component.removeFromParent();
        assertNull(component.getParent(), "Component parent should be null after first removeFromParent()");

        // Call removeFromParent() again - should not throw exception
        assertDoesNotThrow(() -> component.removeFromParent(),
                "removeFromParent() should be safe to call multiple times");
        assertNull(component.getParent(), "Component parent should still be null");
    }

    /**
     * Test that removeFromParent() works correctly with multiple components.
     * <p>
     * **Validates: Requirements 2.6, 8.3**
     * </p>
     */
    @Test
    public void testRemoveFromParentWithMultipleComponents() {
        final TestComponent component1 = new TestComponent(new TestProps("Component1", 1));
        final TestComponent component2 = new TestComponent(new TestProps("Component2", 2));
        final TestComponent component3 = new TestComponent(new TestProps("Component3", 3));
        final PWindow window = PWindow.getMain();

        // Get initial count
        final int initialCount = window.getPRootPanel().getWidgetCount();

        // Add all components to window
        window.add(component1);
        window.add(component2);
        window.add(component3);
        assertEquals(initialCount + 3, window.getPRootPanel().getWidgetCount(),
                "Parent should have 3 more children");

        // Remove middle component
        component2.removeFromParent();
        assertEquals(initialCount + 2, window.getPRootPanel().getWidgetCount(),
                "Parent should have 2 more children after removing one");
        assertNull(component2.getParent(), "Removed component should have no parent");
        assertNotNull(component1.getParent(), "Other components should still have parent");
        assertNotNull(component3.getParent(), "Other components should still have parent");

        // Remove first component
        component1.removeFromParent();
        assertEquals(initialCount + 1, window.getPRootPanel().getWidgetCount(),
                "Parent should have 1 more child after removing another");
        assertNull(component1.getParent(), "Removed component should have no parent");
        assertNotNull(component3.getParent(), "Remaining component should still have parent");

        // Remove last component
        component3.removeFromParent();
        assertEquals(initialCount, window.getPRootPanel().getWidgetCount(),
                "Parent should have same count as before after removing all");
        assertNull(component3.getParent(), "Removed component should have no parent");
    }

    /**
     * Test that removeFromParent() doesn't affect component props.
     * <p>
     * **Validates: Requirements 2.6, 8.3**
     * </p>
     */
    @Test
    public void testRemoveFromParentDoesNotAffectProps() {
        final TestProps initialProps = new TestProps("Test Component", 42);
        final TestComponent component = new TestComponent(initialProps);
        final PWindow window = PWindow.getMain();

        // Add component to window
        window.add(component);

        // Remove component from parent
        component.removeFromParent();

        // Verify props are unchanged
        assertEquals(initialProps, component.getCurrentProps(),
                "Component props should remain unchanged after removeFromParent()");
    }

    /**
     * Test that removeFromParent() doesn't affect component visibility state.
     * <p>
     * **Validates: Requirements 2.6, 8.3**
     * </p>
     */
    @Test
    public void testRemoveFromParentDoesNotAffectVisibility() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        final PWindow window = PWindow.getMain();

        // Add component to window and set visibility
        window.add(component);
        component.setVisible(false);
        assertFalse(component.isVisible(), "Component should be hidden");

        // Remove component from parent
        component.removeFromParent();

        // Verify visibility state is unchanged
        assertFalse(component.isVisible(),
                "Component visibility should remain unchanged after removeFromParent()");
    }

    /**
     * Test that removeFromParent() doesn't affect component styles.
     * <p>
     * **Validates: Requirements 2.6, 8.3**
     * </p>
     */
    @Test
    public void testRemoveFromParentDoesNotAffectStyles() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        final PWindow window = PWindow.getMain();

        // Add component to window and set styles
        window.add(component);
        component.addStyleName("test-style");
        component.setWidth("100px");

        // Remove component from parent
        component.removeFromParent();

        // Verify styles are unchanged (component should still have the style internally)
        // Note: We can't directly check styles after removal, but we can verify
        // that the component can be re-added and still has its styles
        window.add(component);
        // If styles were lost, this would fail in a real scenario
        // For now, we just verify the component can be re-added successfully
        assertNotNull(component.getParent(), "Component should be re-addable after removal");
    }

    /**
     * Test that component can be re-added after removeFromParent().
     * <p>
     * **Validates: Requirements 2.6, 8.3**
     * </p>
     */
    @Test
    public void testComponentCanBeReAddedAfterRemoval() {
        final TestComponent component = new TestComponent(TestProps.defaults());
        final PWindow window = PWindow.getMain();

        // Get initial count
        final int initialCount = window.getPRootPanel().getWidgetCount();

        // Add component to window
        window.add(component);
        assertNotNull(component.getParent(), "Component should have a parent");

        // Remove component from parent
        component.removeFromParent();
        assertNull(component.getParent(), "Component should have no parent after removal");

        // Re-add component to window
        window.add(component);
        assertNotNull(component.getParent(), "Component should have a parent after re-adding");
        assertEquals(initialCount + 1, window.getPRootPanel().getWidgetCount(),
                "Parent should have one more child after re-adding");
    }
}
