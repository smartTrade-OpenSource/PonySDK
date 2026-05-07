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
import java.util.List;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.component.PWebComponent;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.test.ModelWriterForTest;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.mockito.Mockito;

/**
 * Property-based tests for WidgetEditorDialog.
 * <p>
 * <b>Validates: Requirements 1.1, 1.4, 3.2, 4.1</b>
 * </p>
 */
public class WidgetEditorDialogPropertyTest {

    private ComponentRegistry registry;

    @BeforeProperty
    public void setUp() {
        initUIContext();
        registry = buildInsertableWidgetRegistry();
    }

    @AfterProperty
    public void tearDown() {
        Txn.get().commit();
    }

    // ========================================================================
    // Property 4: Dialog header contains slot display name
    // ========================================================================

    /**
     * Property 4: Dialog header contains slot display name.
     * <p>
     * For any slot name string (including empty string for default slot),
     * when a WidgetEditorDialog is created for that slot, the dialog caption
     * should contain the slot's display name.
     * </p>
     * <p><b>Validates: Requirements 1.1, 1.4, 3.2</b></p>
     */
    @Property(tries = 100)
    @Label("Property 4: Dialog header contains slot display name")
    @Tag("Feature: widget-slots-editor, Property 4: Dialog header contains slot display name")
    void dialogHeaderContainsSlotDisplayName(
            @ForAll("slotDisplayName") String slotDisplayName
    ) {
        final MethodIntrospector introspector = clazz -> Collections.emptyList();
        final WidgetEditorDialog.InsertCallback noOpCallback = widget -> {};

        final WidgetEditorDialog dialog = new WidgetEditorDialog(
                slotDisplayName,
                InsertableWidgetRegistry.WIDGET_NAMES,
                registry,
                introspector,
                noOpCallback
        );

        assertThat(dialog.getCaption())
                .as("Dialog caption should contain the slot display name '%s'", slotDisplayName)
                .contains(slotDisplayName);
    }

    // ========================================================================
    // Property 5: All insertable widgets are instantiable via reflection
    // ========================================================================

    /**
     * Property 5: All insertable widgets are instantiable via reflection.
     * <p>
     * For any widget name in InsertableWidgetRegistry.WIDGET_NAMES, resolving it
     * through ComponentRegistry and calling getDeclaredConstructor().newInstance()
     * should succeed and produce a non-null instance of PWebComponent.
     * </p>
     * <p><b>Validates: Requirements 4.1</b></p>
     */
    @Property(tries = 100)
    @Label("Property 5: All insertable widgets are instantiable via reflection")
    @Tag("Feature: widget-slots-editor, Property 5: All insertable widgets are instantiable via reflection")
    void allInsertableWidgetsAreInstantiableViaReflection(
            @ForAll("insertableWidgetName") String widgetName
    ) throws Exception {
        final Class<? extends PWebComponent<?>> widgetClass = registry.get(widgetName);

        assertThat(widgetClass)
                .as("Registry should contain class for widget '%s'", widgetName)
                .isNotNull();

        final PWebComponent<?> instance = widgetClass.getDeclaredConstructor().newInstance();

        assertThat(instance)
                .as("Instantiation of '%s' via reflection should produce a non-null PWebComponent", widgetName)
                .isNotNull();

        assertThat(instance)
                .as("Instance of '%s' should be a PWebComponent", widgetName)
                .isInstanceOf(PWebComponent.class);
    }

    // ========================================================================
    // Arbitraries / Providers
    // ========================================================================

    @Provide
    Arbitrary<String> slotDisplayName() {
        return Arbitraries.oneOf(
                Arbitraries.just(""),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30),
                Arbitraries.of("Default", "header", "footer", "prefix", "suffix", "icon", "label", "nav")
        );
    }

    @Provide
    Arbitrary<String> insertableWidgetName() {
        return Arbitraries.of(InsertableWidgetRegistry.WIDGET_NAMES);
    }

    // ========================================================================
    // Shared UIContext Setup (same pattern as SlotControlWidgetTest)
    // ========================================================================

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

    // ========================================================================
    // Registry Setup
    // ========================================================================

    /**
     * Builds a ComponentRegistry populated with all insertable widget classes,
     * keyed by their component names (matching InsertableWidgetRegistry.WIDGET_NAMES).
     */
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
}
