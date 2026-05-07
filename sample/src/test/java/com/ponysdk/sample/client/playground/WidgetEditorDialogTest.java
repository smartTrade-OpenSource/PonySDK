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

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.component.PWebComponent;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.test.ModelWriterForTest;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

/**
 * Unit tests for WidgetEditorDialog.
 * <p>
 * <b>Validates: Requirements 1.4, 1.5, 3.3, 3.4, 4.5, 4.6</b>
 * </p>
 */
class WidgetEditorDialogTest {

    private ComponentRegistry registry;
    private MethodIntrospector introspector;

    @BeforeEach
    void setUp() {
        initUIContext();
        registry = buildInsertableWidgetRegistry();
        introspector = clazz -> Collections.emptyList();
    }

    @AfterEach
    void tearDown() {
        Txn.get().commit();
    }

    // ========================================================================
    // Test: initial state — no selection, Insert disabled (Req 1.4, 1.5)
    // ========================================================================

    @Test
    void showOpensWithNoSelectionAndInsertDisabled() {
        final WidgetEditorDialog dialog = createDialog(widget -> {});

        dialog.show();

        assertThat(dialog.getInsertButton().isEnabled())
            .as("Insert button should be disabled when no widget is selected")
            .isFalse();
        assertThat(dialog.getSelectedWidgetClass())
            .as("No widget class should be selected initially")
            .isNull();
        assertThat(dialog.isShowing())
            .as("Dialog should be visible after show()")
            .isTrue();
    }

    // ========================================================================
    // Test: Cancel closes without insertion (Req 3.4)
    // ========================================================================

    @Test
    void cancelClosesDialogWithoutInvokingCallback() {
        final AtomicReference<PWebComponent<?>> inserted = new AtomicReference<>();
        final WidgetEditorDialog dialog = createDialog(inserted::set);

        dialog.show();
        assertThat(dialog.isShowing()).isTrue();

        // Simulate Cancel — the cancel button handler calls hide()
        dialog.hide();

        assertThat(dialog.isShowing())
            .as("Dialog should be hidden after Cancel")
            .isFalse();
        assertThat(inserted.get())
            .as("Callback should NOT have been invoked on Cancel")
            .isNull();
    }

    // ========================================================================
    // Test: selection enables Insert button (Req 1.5)
    // ========================================================================

    @Test
    void selectingWidgetTypeEnablesInsertButton() {
        final WidgetEditorDialog dialog = createDialog(widget -> {});

        dialog.show();
        assertThat(dialog.getInsertButton().isEnabled()).isFalse();

        // Select a widget type via the internal method
        dialog.onWidgetTypeSelected("Icon");

        assertThat(dialog.getInsertButton().isEnabled())
            .as("Insert button should be enabled after selecting a widget type")
            .isTrue();
        assertThat(dialog.getSelectedWidgetClass())
            .as("Selected widget class should be resolved from registry")
            .isNotNull();
    }

    // ========================================================================
    // Test: Insert triggers callback and closes dialog (Req 4.5)
    // ========================================================================

    @Test
    void insertTriggersCallbackAndClosesDialog() {
        final AtomicReference<PWebComponent<?>> inserted = new AtomicReference<>();
        final WidgetEditorDialog dialog = createDialog(inserted::set);

        dialog.show();
        // Select the first real widget (index 1, since index 0 is the empty item)
        dialog.getWidgetSelector().setSelectedIndex(1);
        dialog.onWidgetTypeSelected("Icon");
        dialog.onInsertClicked();

        assertThat(inserted.get())
            .as("Callback should have been invoked with a widget instance")
            .isNotNull();
        assertThat(dialog.isShowing())
            .as("Dialog should close after successful insertion")
            .isFalse();
    }

    // ========================================================================
    // Test: creation error displays message and keeps dialog open (Req 4.6)
    // ========================================================================

    @Test
    @SuppressWarnings("unchecked")
    void creationErrorDisplaysMessageAndKeepsDialogOpen() {
        // Register a class that has no default constructor — reflection will fail
        final ComponentRegistry badRegistry = new ComponentRegistry();
        badRegistry.register("BadWidget", (Class<? extends PWebComponent<?>>) (Class<?>) NoDefaultCtorWidget.class);

        final AtomicReference<PWebComponent<?>> inserted = new AtomicReference<>();
        final WidgetEditorDialog dialog = new WidgetEditorDialog(
            "test-slot",
            java.util.List.of("BadWidget"),
            badRegistry,
            clazz -> Collections.emptyList(),
            inserted::set
        );

        dialog.show();
        // Select the widget in the listbox (index 1 since index 0 is the empty item)
        dialog.getWidgetSelector().setSelectedIndex(1);
        dialog.onWidgetTypeSelected("BadWidget");
        assertThat(dialog.getInsertButton().isEnabled()).isTrue();

        // Attempt insert — should fail on reflection
        dialog.onInsertClicked();

        assertThat(dialog.getErrorLabel().isVisible())
            .as("Error label should be visible when creation fails")
            .isTrue();
        assertThat(dialog.isShowing())
            .as("Dialog should remain open on creation error")
            .isTrue();
        assertThat(inserted.get())
            .as("Callback should NOT have been invoked on error")
            .isNull();
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private WidgetEditorDialog createDialog(final WidgetEditorDialog.InsertCallback callback) {
        return new WidgetEditorDialog(
            "test-slot",
            InsertableWidgetRegistry.WIDGET_NAMES,
            registry,
            introspector,
            callback
        );
    }

    private static void initUIContext() {
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

    @SuppressWarnings("unchecked")
    private static ComponentRegistry buildInsertableWidgetRegistry() {
        final ComponentRegistry reg = new ComponentRegistry();
        for (final String widgetName : InsertableWidgetRegistry.WIDGET_NAMES) {
            try {
                final String className = "com.ponysdk.core.ui.wa.WA" + widgetName;
                final Class<?> clazz = Class.forName(className);
                reg.register(widgetName, (Class<? extends PWebComponent<?>>) clazz);
            } catch (final ClassNotFoundException e) {
                throw new RuntimeException("Failed to load WA class for " + widgetName, e);
            }
        }
        return reg;
    }

    // ========================================================================
    // Test widget class with no default constructor (for error test)
    // ========================================================================

    @SuppressWarnings("unchecked")
    public static abstract class NoDefaultCtorWidget extends PWebComponent {
        public NoDefaultCtorWidget(String required) {
            super(null, java.util.Set.of());
        }
    }
}
