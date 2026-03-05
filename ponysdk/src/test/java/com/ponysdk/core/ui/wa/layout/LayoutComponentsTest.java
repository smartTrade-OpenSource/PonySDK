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

package com.ponysdk.core.ui.wa.layout;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.component.PropsDiffer;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.test.ModelWriterForTest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.json.JsonObject;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for layout components: Breakpoint, PResponsiveGrid, PStack,
 * PContainer and their props records.
 *
 * <p>Validates: Requirements 4.1, 4.2, 4.4, 4.5, 4.6</p>
 * <p>Note: PDivider and PSplitPanel have been migrated to generated WADivider and WASplitPanel</p>
 */
class LayoutComponentsTest {

    private final PropsDiffer<ResponsiveGridProps> gridDiffer = new PropsDiffer<>();
    private final PropsDiffer<StackProps> stackDiffer = new PropsDiffer<>();
    private final PropsDiffer<ContainerProps> containerDiffer = new PropsDiffer<>();

    @BeforeEach
    void setUp() {
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

    @AfterEach
    void tearDown() {
        Txn.get().commit();
    }

    // ========== Breakpoint enum ==========

    @Test
    void breakpoint_mobileRange() {
        assertEquals(0, Breakpoint.MOBILE.getMinWidth());
        assertEquals(599, Breakpoint.MOBILE.getMaxWidth());
    }

    @Test
    void breakpoint_tabletRange() {
        assertEquals(600, Breakpoint.TABLET.getMinWidth());
        assertEquals(1023, Breakpoint.TABLET.getMaxWidth());
    }

    @Test
    void breakpoint_desktopRange() {
        assertEquals(1024, Breakpoint.DESKTOP.getMinWidth());
        assertEquals(Integer.MAX_VALUE, Breakpoint.DESKTOP.getMaxWidth());
    }

    @Test
    void breakpoint_forWidth_mobile() {
        assertEquals(Breakpoint.MOBILE, Breakpoint.forWidth(0));
        assertEquals(Breakpoint.MOBILE, Breakpoint.forWidth(320));
        assertEquals(Breakpoint.MOBILE, Breakpoint.forWidth(599));
    }

    @Test
    void breakpoint_forWidth_tablet() {
        assertEquals(Breakpoint.TABLET, Breakpoint.forWidth(600));
        assertEquals(Breakpoint.TABLET, Breakpoint.forWidth(768));
        assertEquals(Breakpoint.TABLET, Breakpoint.forWidth(1023));
    }

    @Test
    void breakpoint_forWidth_desktop() {
        assertEquals(Breakpoint.DESKTOP, Breakpoint.forWidth(1024));
        assertEquals(Breakpoint.DESKTOP, Breakpoint.forWidth(1920));
    }

    // ========== ResponsiveGridProps ==========

    @Test
    void responsiveGridProps_defaults() {
        final ResponsiveGridProps props = ResponsiveGridProps.defaults();
        assertEquals(12, props.columns());
        assertEquals("1rem", props.gap());
        assertNotNull(props.breakpoints());
        assertEquals(3, props.breakpoints().size());
        assertFalse(props.hideOnMobile());
        assertFalse(props.hideOnTablet());
        assertFalse(props.hideOnDesktop());
    }

    @Test
    void responsiveGridProps_serialization_roundTrip() {
        final ResponsiveGridProps original = ResponsiveGridProps.defaults();
        final JsonObject json = gridDiffer.toJson(original);
        final ResponsiveGridProps restored = gridDiffer.fromJson(json, ResponsiveGridProps.class);
        assertEquals(original.columns(), restored.columns());
        assertEquals(original.gap(), restored.gap());
        assertEquals(original.hideOnMobile(), restored.hideOnMobile());
    }

    @Test
    void responsiveGridProps_customBreakpoints() {
        final ResponsiveGridProps props = new ResponsiveGridProps(
            6, "2rem",
            Map.of("mobile", new BreakpointConfig(0, 1, "0.5rem")),
            true, false, false
        );
        assertEquals(6, props.columns());
        assertEquals("2rem", props.gap());
        assertTrue(props.hideOnMobile());
        assertEquals(1, props.breakpoints().size());
    }

    // ========== StackProps ==========

    @Test
    void stackProps_defaults() {
        final StackProps props = StackProps.defaults();
        assertEquals("vertical", props.orientation());
        assertEquals("1rem", props.gap());
        assertEquals("stretch", props.alignment());
        assertEquals("start", props.justification());
        assertFalse(props.wrap());
        assertFalse(props.hideOnMobile());
    }

    @Test
    void stackProps_serialization_roundTrip() {
        final StackProps original = new StackProps(
            "horizontal", "2rem", "center", "space-between", true,
            false, true, false
        );
        final JsonObject json = stackDiffer.toJson(original);
        final StackProps restored = stackDiffer.fromJson(json, StackProps.class);
        assertEquals(original.orientation(), restored.orientation());
        assertEquals(original.gap(), restored.gap());
        assertEquals(original.alignment(), restored.alignment());
        assertEquals(original.justification(), restored.justification());
        assertEquals(original.wrap(), restored.wrap());
        assertEquals(original.hideOnTablet(), restored.hideOnTablet());
    }

    @Test
    void stackProps_horizontalOrientation() {
        final StackProps props = new StackProps(
            "horizontal", "0.5rem", "end", "space-around", false,
            false, false, false
        );
        assertEquals("horizontal", props.orientation());
        assertEquals("end", props.alignment());
        assertEquals("space-around", props.justification());
    }

    // ========== ContainerProps ==========

    @Test
    void containerProps_defaults() {
        final ContainerProps props = ContainerProps.defaults();
        assertEquals("1200px", props.maxWidth());
        assertEquals("1rem", props.padding());
        assertTrue(props.centered());
        assertFalse(props.hideOnMobile());
    }

    @Test
    void containerProps_serialization_roundTrip() {
        final ContainerProps original = new ContainerProps("80rem", "2rem 3rem", false, true, false, true);
        final JsonObject json = containerDiffer.toJson(original);
        final ContainerProps restored = containerDiffer.fromJson(json, ContainerProps.class);
        assertEquals(original.maxWidth(), restored.maxWidth());
        assertEquals(original.padding(), restored.padding());
        assertEquals(original.centered(), restored.centered());
        assertEquals(original.hideOnMobile(), restored.hideOnMobile());
        assertEquals(original.hideOnDesktop(), restored.hideOnDesktop());
    }

    // ========== BreakpointConfig ==========

    @Test
    void breakpointConfig_values() {
        final BreakpointConfig config = new BreakpointConfig(600, 6, "0.75rem");
        assertEquals(600, config.minWidth());
        assertEquals(6, config.columns());
        assertEquals("0.75rem", config.gap());
    }

    @Test
    void breakpointConfig_serialization_roundTrip() {
        final PropsDiffer<BreakpointConfig> differ = new PropsDiffer<>();
        final BreakpointConfig original = new BreakpointConfig(1024, 12, "1rem");
        final JsonObject json = differ.toJson(original);
        final BreakpointConfig restored = differ.fromJson(json, BreakpointConfig.class);
        assertEquals(original.minWidth(), restored.minWidth());
        assertEquals(original.columns(), restored.columns());
        assertEquals(original.gap(), restored.gap());
    }

    // ========== Component instantiation with default props ==========

    @Test
    void pResponsiveGrid_defaultProps() {
        final PResponsiveGrid grid = new PResponsiveGrid();
        final ResponsiveGridProps props = grid.getCurrentProps();
        assertEquals(12, props.columns());
        assertEquals("1rem", props.gap());
        assertFalse(props.hideOnMobile());
    }

    @Test
    void pStack_defaultProps() {
        final PStack stack = new PStack();
        final StackProps props = stack.getCurrentProps();
        assertEquals("vertical", props.orientation());
        assertEquals("1rem", props.gap());
        assertEquals("stretch", props.alignment());
        assertEquals("start", props.justification());
        assertFalse(props.wrap());
    }

    // ========== Component instantiation with custom props ==========

    @Test
    void pResponsiveGrid_customProps() {
        final PContainer container = new PContainer();
        final ContainerProps props = container.getCurrentProps();
        assertEquals("1200px", props.maxWidth());
        assertEquals("1rem", props.padding());
        assertTrue(props.centered());
    }

    // ========== Component instantiation with custom props ==========

    @Test
    void pStack_customProps() {
        final StackProps custom = new StackProps("horizontal", "0.5rem", "center", "space-between", true, false, false, false);
        final PStack stack = new PStack(custom);
        assertEquals(custom, stack.getCurrentProps());
    }

    @Test
    void pContainer_customProps() {
        final ContainerProps custom = new ContainerProps("80rem", "2rem", false, false, true, false);
        final PContainer container = new PContainer(custom);
        assertEquals(custom, container.getCurrentProps());
    }

    // ========== Conditional display props ==========

    @Test
    void allLayoutProps_supportConditionalDisplay() {
        final ResponsiveGridProps grid = new ResponsiveGridProps(12, "1rem", Map.of(), true, true, true);
        assertTrue(grid.hideOnMobile());
        assertTrue(grid.hideOnTablet());
        assertTrue(grid.hideOnDesktop());

        final StackProps stack = new StackProps("vertical", "1rem", "start", "start", false, true, true, true);
        assertTrue(stack.hideOnMobile());
        assertTrue(stack.hideOnTablet());
        assertTrue(stack.hideOnDesktop());

        final ContainerProps container = new ContainerProps("1200px", "1rem", true, true, true, true);
        assertTrue(container.hideOnMobile());
        assertTrue(container.hideOnTablet());
        assertTrue(container.hideOnDesktop());
    }
}
