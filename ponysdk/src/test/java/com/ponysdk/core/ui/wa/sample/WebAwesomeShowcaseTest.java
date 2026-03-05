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

package com.ponysdk.core.ui.wa.sample;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.wa.Size;
import com.ponysdk.core.ui.wa.Variant;
import com.ponysdk.core.ui.wa.WADivider;
import com.ponysdk.core.ui.wa.WASplitPanel;
import com.ponysdk.core.ui.wa.datatable.ColumnDef;
import com.ponysdk.core.ui.wa.datatable.PDataTable;
import com.ponysdk.core.ui.wa.form.PForm;
import com.ponysdk.core.ui.wa.layout.PContainer;
import com.ponysdk.core.ui.wa.layout.PResponsiveGrid;
import com.ponysdk.core.ui.wa.layout.PStack;
import com.ponysdk.core.ui.wa.theme.ThemeEngine;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.test.ModelWriterForTest;

/**
 * Runnable showcase that instantiates all Web Awesome wrapper components
 * inside a mocked UIContext. Run with:
 * <pre>
 *   ./gradlew :ponysdk:test --tests "*WebAwesomeShowcaseTest"
 * </pre>
 */
@DisplayName("Web Awesome Showcase — all component families")
class WebAwesomeShowcaseTest {

    @BeforeEach
    void initUIContext() {
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
    void cleanupUIContext() {
        Txn.get().commit();
    }

    // ── Theme ──────────────────────────────────────────────

    @Test
    @DisplayName("ThemeEngine: light, dark, custom theme, token override, toggle")
    void themeEngine() {
        final var injected = new java.util.ArrayList<String>();
        final ThemeEngine theme = new ThemeEngine(injected::add);

        // Default light theme
        assertEquals("light", theme.getActiveThemeId());

        // Apply light
        theme.applyTheme("light");
        assertFalse(injected.isEmpty(), "CSS should be injected");

        // Override a token
        theme.setToken("primary-color", "#1e40af");
        assertEquals("#1e40af", theme.getTheme("light").tokens().get("primary-color"));

        // Custom theme extending light
        theme.createCustomTheme("brand", "light", Map.of(
            "primary-color", "#7c3aed",
            "success-color", "#059669"
        ));
        assertTrue(theme.getThemeIds().contains("brand"));
        theme.applyTheme("brand");
        assertEquals("brand", theme.getActiveThemeId());

        // Toggle dark mode
        theme.toggleDarkMode();
        assertEquals("dark", theme.getActiveThemeId());

        // Toggle back
        theme.toggleDarkMode();
        assertEquals("light", theme.getActiveThemeId());
    }

    // ── Layout ─────────────────────────────────────────────

    @Test
    @DisplayName("PResponsiveGrid: columns, gap, hideOnMobile")
    void responsiveGrid() {
        final PResponsiveGrid grid = new PResponsiveGrid();
        grid.setColumns(3);
        grid.setGap("1rem");
        grid.setHideOnMobile(false);

        assertEquals(3, grid.getCurrentProps().columns());
        assertEquals("1rem", grid.getCurrentProps().gap());
        assertFalse(grid.getCurrentProps().hideOnMobile());
    }

    @Test
    @DisplayName("PStack: vertical, horizontal, wrap, alignment, justification")
    void stack() {
        // Vertical stack
        final PStack vStack = new PStack();
        vStack.setOrientation("vertical");
        vStack.setGap("0.5rem");
        vStack.setAlignment("stretch");
        vStack.setJustification("flex-start");

        assertEquals("vertical", vStack.getCurrentProps().orientation());
        assertEquals("0.5rem", vStack.getCurrentProps().gap());

        // Horizontal stack with wrap
        final PStack hStack = new PStack();
        hStack.setOrientation("horizontal");
        hStack.setGap("1rem");
        hStack.setWrap(true);

        assertEquals("horizontal", hStack.getCurrentProps().orientation());
        assertTrue(hStack.getCurrentProps().wrap());
    }

    // ── Container ────────────────────────────────────────

    @Test
    @DisplayName("PContainer: maxWidth, padding, centered")
    void container() {
        final PContainer container = new PContainer();
        container.setMaxWidth("1200px");
        container.setPadding("1rem");
        container.setCentered(true);

        assertEquals("1200px", container.getCurrentProps().maxWidth());
        assertEquals("1rem", container.getCurrentProps().padding());
        assertTrue(container.getCurrentProps().centered());
    }

    // ── Divider ───────────────────────────────────────────

    @Test
    @DisplayName("WADivider: horizontal and vertical orientation")
    void divider() {
        final WADivider hDivider = new WADivider();
        assertEquals("", hDivider.getCurrentProps().orientation());

        final WADivider vDivider = new WADivider();
        vDivider.setOrientation("vertical");
        assertEquals("vertical", vDivider.getCurrentProps().orientation());
    }

    // ── SplitPanel ────────────────────────────────────────

    @Test
    @DisplayName("WASplitPanel: position, orientation, disabled")
    void splitPanel() {
        final WASplitPanel splitPanel = new WASplitPanel();
        splitPanel.setPosition(30);
        splitPanel.setOrientation("horizontal");
        splitPanel.setDisabled(false);

        assertEquals(30, splitPanel.getCurrentProps().position());
        assertEquals("horizontal", splitPanel.getCurrentProps().orientation());
        assertFalse(splitPanel.getCurrentProps().disabled());
    }

    // ── Shared Enums ──────────────────────────────────────

    @Test
    @DisplayName("Variant and Size enums: values and serialization")
    void sharedEnums() {
        assertEquals("primary", Variant.PRIMARY.getValue());
        assertEquals("danger", Variant.DANGER.toString());
        assertEquals(5, Variant.values().length);

        assertEquals("small", Size.SMALL.getValue());
        assertEquals("large", Size.LARGE.toString());
        assertEquals(3, Size.values().length);
    }

    // ── Form ───────────────────────────────────────────────

    @Test
    @DisplayName("PForm: create, validate, submit, validation errors")
    void form() {
        final PForm form = new PForm();

        // Register submit handler
        final AtomicReference<Map<String, Object>> received = new AtomicReference<>();
        form.onSubmit(received::set);

        // Empty form — validate should return no errors
        final var errors = form.validate();
        assertTrue(errors.isEmpty(), "Empty form should have no validation errors");

        // Submit empty form — handler should fire
        form.submit();
        assertNotNull(received.get(), "Submit handler should be invoked on valid form");

        // Apply server-side validation errors
        form.setValidationErrors(Map.of(
            "email", List.of("Invalid email format"),
            "age", List.of("Must be at least 18")
        ));
    }

    // ── DataTable ──────────────────────────────────────────

    @Test
    @DisplayName("PDataTable: columns, data, pagination, selection, virtual scroll")
    void dataTable() {
        final PDataTable table = new PDataTable();

        // Define columns
        table.setColumns(List.of(
            ColumnDef.sortable("name", "Name"),
            ColumnDef.sortable("email", "Email"),
            ColumnDef.of("role", "Role"),
            new ColumnDef("age", "Age", "number", true, false, Optional.of(80))
        ));
        assertEquals(4, table.getCurrentProps().columns().size());

        // Set data
        table.setData(List.of(
            Map.of("name", "Alice", "email", "alice@example.com", "role", "Admin", "age", 32),
            Map.of("name", "Bob", "email", "bob@example.com", "role", "User", "age", 28),
            Map.of("name", "Charlie", "email", "charlie@example.com", "role", "User", "age", 45)
        ));
        assertEquals(3, table.getCurrentProps().data().size());

        // Pagination
        table.setPage(0);
        table.setPageSize(25);
        assertEquals(0, table.getCurrentProps().page());
        assertEquals(25, table.getCurrentProps().pageSize());

        // Virtual scroll
        table.setVirtualScroll(false);
        assertFalse(table.getCurrentProps().virtualScroll());

        // Event handlers (just wire them — no client to fire events)
        table.onSort(sortEvent ->
            System.out.println("Sort: " + sortEvent.field() + " " + sortEvent.direction()));
        table.onPageChange(page ->
            System.out.println("Page: " + page));
        table.onSelectionChange(selected ->
            System.out.println("Selected: " + selected));

        // Programmatic selection
        table.selectRow("row-1");
        assertTrue(table.getCurrentProps().selectedRows().contains("row-1"));
        table.deselectRow("row-1");
        assertFalse(table.getCurrentProps().selectedRows().contains("row-1"));
        table.clearSelection();
        assertTrue(table.getCurrentProps().selectedRows().isEmpty());
    }

    // ── Full Showcase (integration) ────────────────────────

    // TODO: Re-enable when WebAwesomeShowcase class is created
    // @Test
    // @DisplayName("Full showcase: instantiate all components in sequence")
    // void fullShowcase() {
    //     // This delegates to the existing WebAwesomeShowcase class
    //     final WebAwesomeShowcase showcase = new WebAwesomeShowcase();
    //     assertDoesNotThrow(showcase::buildShowcase);
    // }
}
