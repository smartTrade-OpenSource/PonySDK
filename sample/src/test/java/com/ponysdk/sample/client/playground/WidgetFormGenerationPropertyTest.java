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

import java.util.List;
import java.util.Set;

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
 * Property-based tests for widget form generation.
 * <p>
 * <b>Validates: Requirements 1.3, 2.1, 2.3</b>
 * </p>
 */
public class WidgetFormGenerationPropertyTest {

    private ComponentRegistry registry;
    private MethodIntrospector introspector;
    private FormGenerator formGenerator;

    /**
     * Supported parameter types that FormGenerator will create controls for.
     * Must match the logic in FormGenerator.isUnsupportedType().
     */
    private static boolean isSupportedType(final Class<?> type) {
        return type == String.class
            || type == boolean.class || type == Boolean.class
            || type == int.class || type == Integer.class
            || type == long.class || type == Long.class
            || type.isEnum();
    }

    /**
     * Hidden property names that FormGenerator excludes.
     * Mirrors FormGenerator.HIDDEN_PROPERTIES.
     */
    private static final Set<String> HIDDEN_PROPERTIES = Set.of(
        "shadowRootOptions",
        "hasSlotController",
        "slotController",
        "elementInternals",
        "formControlController",
        "hasFormControlController",
        "validationTarget",
        "validationMessage",
        "validity",
        "willValidate",
        "formAssociated",
        "shadowRoot",
        "internals",
        "renderRoot",
        "updateComplete",
        "isUpdatePending",
        "hasUpdated",
        "localizeController",
        "localize"
    );

    @BeforeProperty
    public void setUp() {
        initUIContext();
        registry = buildInsertableWidgetRegistry();
        introspector = new DefaultMethodIntrospector();
        formGenerator = new FormGenerator();
    }

    @AfterProperty
    public void tearDown() {
        Txn.get().commit();
    }

    // ========================================================================
    // Property 2: Form generation matches widget setters
    // ========================================================================

    /**
     * Property 2: Form generation matches widget setters.
     * <p>
     * For any widget class from the insertable widgets list, generating a props
     * form should produce exactly one PropertyControl for each public setter method
     * that is not in the hidden properties set and has a supported parameter type.
     * The number of generated controls should equal the number of eligible setter methods.
     * </p>
     * <p><b>Validates: Requirements 1.3, 2.1</b></p>
     */
    @Property(tries = 100)
    @Label("Property 2: Form generation matches widget setters")
    @Tag("Feature: widget-slots-editor, Property 2: Form generation matches widget setters")
    void formGenerationMatchesWidgetSetters(
            @ForAll("insertableWidgetName") String widgetName
    ) {
        final Class<? extends PWebComponent<?>> widgetClass = registry.get(widgetName);
        assertThat(widgetClass).as("Registry should contain class for '%s'", widgetName).isNotNull();

        // Discover all setters via the real introspector
        final List<MethodSignature> allSetters = introspector.discoverSetters(widgetClass);

        // Compute the expected eligible setters: supported type AND not hidden
        final long expectedEligibleCount = allSetters.stream()
                .filter(ms -> !isHiddenProperty(ms))
                .filter(ms -> hasSupportedFirstParam(ms))
                .count();

        // Generate controls via the real FormGenerator
        final List<PropertyControl> controls = formGenerator.generateControls(allSetters);

        assertThat(controls.size())
                .as("Number of generated controls for '%s' should match eligible setters", widgetName)
                .isEqualTo((int) expectedEligibleCount);
    }

    // ========================================================================
    // Property 3: Hidden properties are excluded from generated forms
    // ========================================================================

    /**
     * Property 3: Hidden properties are excluded from generated forms.
     * <p>
     * For any widget class from the insertable widgets list, none of the generated
     * PropertyControl instances should correspond to a method whose property name
     * is in the HIDDEN_PROPERTIES set. Conversely, every non-hidden method with a
     * supported type should be present.
     * </p>
     * <p><b>Validates: Requirements 2.3</b></p>
     */
    @Property(tries = 100)
    @Label("Property 3: Hidden properties are excluded from generated forms")
    @Tag("Feature: widget-slots-editor, Property 3: Hidden properties are excluded from generated forms")
    void hiddenPropertiesAreExcludedFromGeneratedForms(
            @ForAll("insertableWidgetName") String widgetName
    ) {
        final Class<? extends PWebComponent<?>> widgetClass = registry.get(widgetName);
        assertThat(widgetClass).as("Registry should contain class for '%s'", widgetName).isNotNull();

        final List<MethodSignature> allSetters = introspector.discoverSetters(widgetClass);
        final List<PropertyControl> controls = formGenerator.generateControls(allSetters);

        // Verify no generated control corresponds to a hidden property
        for (final PropertyControl pc : controls) {
            final String propertyName = extractPropertyName(pc.method().methodName());
            assertThat(HIDDEN_PROPERTIES)
                    .as("Generated control '%s' for widget '%s' should not be a hidden property",
                            pc.method().methodName(), widgetName)
                    .doesNotContain(propertyName);
        }

        // Verify every non-hidden setter with a supported type IS present
        final List<String> generatedMethodNames = controls.stream()
                .map(pc -> pc.method().methodName())
                .toList();

        for (final MethodSignature ms : allSetters) {
            if (!isHiddenProperty(ms) && hasSupportedFirstParam(ms)) {
                assertThat(generatedMethodNames)
                        .as("Non-hidden eligible setter '%s' for widget '%s' should be in generated controls",
                                ms.methodName(), widgetName)
                        .contains(ms.methodName());
            }
        }
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    /**
     * Extracts the property name from a setter method name.
     * e.g. "setShadowRoot" → "shadowRoot"
     */
    private static String extractPropertyName(final String methodName) {
        if (methodName.startsWith("set") && methodName.length() > 3) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        }
        return methodName;
    }

    /**
     * Checks if a method signature corresponds to a hidden property.
     */
    private static boolean isHiddenProperty(final MethodSignature ms) {
        return HIDDEN_PROPERTIES.contains(extractPropertyName(ms.methodName()));
    }

    /**
     * Checks if the first parameter of a method signature has a supported type.
     */
    private static boolean hasSupportedFirstParam(final MethodSignature ms) {
        final List<ParameterInfo> params = ms.parameters();
        if (params.isEmpty()) {
            return true; // no-param setters are not filtered as unsupported
        }
        return isSupportedType(params.get(0).type());
    }

    // ========================================================================
    // Arbitraries / Providers
    // ========================================================================

    @Provide
    Arbitrary<String> insertableWidgetName() {
        return Arbitraries.of(InsertableWidgetRegistry.WIDGET_NAMES);
    }

    // ========================================================================
    // Shared UIContext Setup (same pattern as WidgetEditorDialogPropertyTest)
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
