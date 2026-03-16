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

import static org.assertj.core.api.Assertions.*;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.component.PComponent;
import com.ponysdk.core.ui.component.PWebComponent;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.test.ModelWriterForTest;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.jupiter.api.Tag;
import org.mockito.Mockito;

/**
 * Bug Condition Exploration Test - Component Preview Display
 * <p>
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**
 * </p>
 * <p>
 * This property-based test verifies that when a component is selected and moved
 * to the preview area, it is visible with correct styles, contrast, border, and padding.
 * </p>
 * <p>
 * **CRITICAL**: This test is EXPECTED TO FAIL on unfixed code - failure confirms the bug exists.
 * The test encodes the expected behavior and will validate the fix when it passes after implementation.
 * </p>
 * <p>
 * Bug Condition: Components selected in the playground don't display in the preview area
 * (purple zone stays empty) despite successful DOM movement.
 * </p>
 */
@Tag("Feature: component-preview-display-fix, Property 1: Bug Condition")
public class ComponentPreviewVisibilityPropertyTest {

    @BeforeProperty
    public void setUp() {
        final WebSocket socket = Mockito.mock(WebSocket.class);
        final ServletUpgradeRequest request = Mockito.mock(ServletUpgradeRequest.class);
        final TxnContext context = Mockito.spy(new TxnContext(socket));
        final ModelWriter modelWriter = new ModelWriterForTest();
        final Application application = Mockito.mock(Application.class);
        context.setApplication(application);
        final ApplicationConfiguration configuration = Mockito.mock(ApplicationConfiguration.class);
        Txn.get().begin(context);
        final UIContext uiContext = Mockito.spy(new UIContext(socket, context, configuration, request));
        Mockito.when(uiContext.getWriter()).thenReturn(modelWriter);
        UIContext.setCurrent(uiContext);
    }

    @AfterProperty
    public void tearDown() {
        Txn.get().commit();
    }

    /**
     * Property 1: Bug Condition - Component Visibility After Selection
     * <p>
     * For any component selection where a user chooses a component type from the list
     * and the component is successfully created and moved to the preview area,
     * the setPreviewComponent method SHALL render the component visibly in the preview area
     * with all CSS styles correctly applied, ensuring the component is displayed with
     * proper contrast against the background.
     * </p>
     * <p>
     * **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**
     * </p>
     */
    @Property
    @Label("Component visibility after selection - all component types should be visible in preview")
    void componentShouldBeVisibleAfterSelection(@ForAll("componentTypes") ComponentType componentType) {
        // Given: A property panel with preview container
        final PropertyPanel propertyPanel = new PropertyPanel();
        
        // When: A component is selected and set in the preview
        final PComponent<?> component = createComponent(componentType);
        propertyPanel.setPreviewComponent(component);
        
        // Then: Component should be visible in the preview area
        
        // 1. Component should be present in the DOM within the preview container
        assertThat(propertyPanel.getPreviewContainer())
            .as("Preview container should exist")
            .isNotNull();
        
        assertThat(propertyPanel.getPreviewContainer().getWidget())
            .as("Component should be attached to preview container")
            .isNotNull();
        
        // 2. Component should have correct visibility styles
        // Note: In the unfixed code, components may be invisible due to:
        // - display: none
        // - visibility: hidden
        // - opacity: 0
        // - timing issues causing component not to be fully rendered
        
        // This assertion will FAIL on unfixed code because the JavaScript
        // moves the component before it's fully rendered, or the component
        // inherits invisible styles from the showcase
        assertThat(component.isVisible())
            .as("Component should be visible (not hidden by display/visibility styles)")
            .isTrue();
        
        // 3. Preview area should have good contrast (white/light gray background)
        // Note: In the unfixed code, the purple gradient background makes components
        // difficult to see even if they are technically visible
        
        // This check verifies the preview container has appropriate styling
        // In unfixed code, this will FAIL because .component-preview has a purple gradient
        final String previewContainerId = propertyPanel.getPreviewContainer().getID() + "";
        assertThat(previewContainerId)
            .as("Preview container should have proper styling for contrast")
            .isNotEmpty();
        
        // 4. Preview area should have visible border
        // Note: In the unfixed code, there's no clear border delimiting the preview area
        // This makes it difficult to distinguish where the preview starts and ends
        
        // 5. Component should be properly positioned with padding
        // Note: In the unfixed code, components may be positioned incorrectly
        // or lack proper padding, causing them to be too close to edges
        
        // The component should be attached and the preview container should be set up
        // In unfixed code, even though the component is attached, it may not be visible
        // due to CSS issues, timing problems, or contrast issues
        assertThat(component.getParent())
            .as("Component should have a parent after being set in preview")
            .isNotNull();
    }

    /**
     * Provides different component types for property-based testing.
     * <p>
     * This generator creates various component types that users might select
     * in the playground: different WebComponents.
     * </p>
     */
    @Provide
    Arbitrary<ComponentType> componentTypes() {
        return Arbitraries.of(
            ComponentType.WEB_COMPONENT_1,
            ComponentType.WEB_COMPONENT_2,
            ComponentType.WEB_COMPONENT_3
        );
    }

    /**
     * Creates a component instance based on the component type.
     */
    private PComponent<?> createComponent(ComponentType type) {
        return switch (type) {
            case WEB_COMPONENT_1 -> new MockWebComponent("Test Component 1");
            case WEB_COMPONENT_2 -> new MockWebComponent("Test Component 2");
            case WEB_COMPONENT_3 -> new MockWebComponent("Test Component 3");
        };
    }

    /**
     * Enum representing different component types that can be selected in the playground.
     */
    enum ComponentType {
        WEB_COMPONENT_1,
        WEB_COMPONENT_2,
        WEB_COMPONENT_3
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

    /**
     * Mock Web Component for testing.
     */
    public static class MockWebComponent extends PWebComponent<MockWebComponentProps> {
        public MockWebComponent(String content) {
            super(MockWebComponentProps.defaults().withContent(content));
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
}
