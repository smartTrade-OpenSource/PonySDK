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

package com.ponysdk.core.ui.wa;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.component.FrameworkType;
import com.ponysdk.core.ui.component.PComponent;
import com.ponysdk.core.ui.component.PWebComponent;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.core.writer.ModelWriterCallback;
import com.ponysdk.test.ModelWriterForTest;
import net.jqwik.api.*;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.jupiter.api.Tag;
import org.mockito.Mockito;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for invalid slot rejection.
 * <p>
 * <b>Feature: ui-library-wrapper, Property 7: Invalid Slot Rejection</b>
 * </p>
 * <p>
 * For any component and any slot name NOT in declared slots, addToSlot SHALL NOT
 * modify children and SHALL log a warning.
 * </p>
 * <p>
 * <b>Validates: Requirements 7.5</b>
 * </p>
 */
@Tag("Feature: ui-library-wrapper, Property 7: Invalid Slot Rejection")
public class SlotRejectionPropertyTest {

    /**
     * Sets up UIContext required by PObject constructor.
     * Must be called before creating any PComponent instances.
     */
    private void initUIContext() {
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

    private void cleanupUIContext() {
        Txn.get().commit();
    }

    // ========== Property Test ==========

    /**
     * Property 7: For any set of declared slots and any slot name NOT in that set,
     * addToSlot SHALL NOT send a slot operation (no saveUpdate call).
     * <p>
     * The warning is logged internally by PWebComponent via SLF4J.
     * We verify the behavioral contract: no state modification occurs for invalid slot names.
     * </p>
     * <p>
     * <b>Validates: Requirements 7.5</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 7: Invalid slot name is rejected without modifying children")
    void invalidSlotName_isRejectedWithoutStateChange(
            @ForAll("declaredSlotSets") Set<String> declaredSlots,
            @ForAll("invalidSlotName") String invalidSlotName
    ) {
        Assume.that(!declaredSlots.contains(invalidSlotName));

        initUIContext();
        try {
            final AtomicBoolean saveUpdateCalled = new AtomicBoolean(false);
            final TestWebComponent component = new TestWebComponent(declaredSlots, saveUpdateCalled);
            final TestChildComponent child = new TestChildComponent();

            component.addToSlot(invalidSlotName, child);

            assertFalse(saveUpdateCalled.get(),
                    "addToSlot with invalid slot '" + invalidSlotName
                            + "' should NOT trigger saveUpdate. Declared slots: " + declaredSlots);
        } finally {
            cleanupUIContext();
        }
    }

    /**
     * Property 7 (complement): Valid slot names DO trigger saveUpdate,
     * confirming the rejection logic is specific to invalid slots.
     * <p>
     * <b>Validates: Requirements 7.5</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 7: Valid slot name triggers saveUpdate (control test)")
    void validSlotName_triggersSaveUpdate(
            @ForAll("nonEmptyDeclaredSlotSets") Set<String> declaredSlots
    ) {
        final String validSlot = declaredSlots.iterator().next();

        initUIContext();
        try {
            final AtomicBoolean saveUpdateCalled = new AtomicBoolean(false);
            final TestWebComponent component = new TestWebComponent(declaredSlots, saveUpdateCalled);
            final TestChildComponent child = new TestChildComponent();

            component.addToSlot(validSlot, child);

            assertTrue(saveUpdateCalled.get(),
                    "addToSlot with valid slot '" + validSlot
                            + "' should trigger saveUpdate. Declared slots: " + declaredSlots);
        } finally {
            cleanupUIContext();
        }
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<Set<String>> declaredSlotSets() {
        return Arbitraries.of("label", "prefix", "suffix", "help-text", "header", "footer", "icon", "nav")
                .set().ofMinSize(0).ofMaxSize(5);
    }

    @Provide
    Arbitrary<Set<String>> nonEmptyDeclaredSlotSets() {
        return Arbitraries.of("label", "prefix", "suffix", "help-text", "header", "footer", "icon", "nav")
                .set().ofMinSize(1).ofMaxSize(5);
    }

    @Provide
    Arbitrary<String> invalidSlotName() {
        return Arbitraries.of(
                "nonexistent", "invalid-slot", "content", "body", "sidebar",
                "actions", "toolbar", "menu", "panel", "extra"
        );
    }

    // ========== Test Helpers ==========

    /** Simple props record for testing. */
    record TestProps(String tag) {
        static TestProps defaults() {
            return new TestProps("wa-test");
        }
    }

    /**
     * Test-friendly PWebComponent subclass that tracks saveUpdate calls.
     */
    static class TestWebComponent extends PWebComponent<TestProps> {

        private final AtomicBoolean saveUpdateCalled;

        TestWebComponent(final Set<String> declaredSlots, final AtomicBoolean saveUpdateCalled) {
            super(TestProps.defaults(), declaredSlots);
            this.saveUpdateCalled = saveUpdateCalled;
        }

        @Override
        protected Class<TestProps> getPropsClass() {
            return TestProps.class;
        }

        @Override
        protected String getComponentSignature() {
            return "wa-test";
        }

        @Override
        protected void saveUpdate(final ModelWriterCallback callback) {
            saveUpdateCalled.set(true);
        }
    }

    /**
     * Minimal PComponent subclass to serve as a child in slot operations.
     */
    static class TestChildComponent extends PComponent<TestProps> {

        TestChildComponent() {
            super(TestProps.defaults(), FrameworkType.WEB_COMPONENT);
        }

        @Override
        protected Class<TestProps> getPropsClass() {
            return TestProps.class;
        }

        @Override
        protected String getComponentSignature() {
            return "wa-test-child";
        }
    }
}
