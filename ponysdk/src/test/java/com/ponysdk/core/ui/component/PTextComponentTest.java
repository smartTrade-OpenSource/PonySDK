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
 * Unit tests for PTextComponent.
 * <p>
 * Tests that PTextComponent correctly encapsulates text content for use in Web Component slots.
 * </p>
 * <p>
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 7.1**
 * </p>
 */
public class PTextComponentTest {

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
     * Test that PTextComponent extends PComponent (Requirement 1.1).
     */
    @Test
    void testPTextComponentExtendsPComponent() {
        final PTextComponent textComponent = new PTextComponent("test");
        assertTrue(textComponent instanceof PComponent, "PTextComponent should extend PComponent");
    }

    /**
     * Test that PTextComponent stores text content when instantiated (Requirement 1.2).
     */
    @Test
    void testPTextComponentStoresTextContent() {
        final String expectedText = "Hello World";
        final PTextComponent textComponent = new PTextComponent(expectedText);
        assertEquals(expectedText, textComponent.getText(), "PTextComponent should store the text content");
    }

    /**
     * Test that PTextComponent handles null text by converting to empty string (Requirement 1.2).
     */
    @Test
    void testPTextComponentHandlesNullText() {
        final PTextComponent textComponent = new PTextComponent(null);
        assertEquals("", textComponent.getText(), "PTextComponent should convert null to empty string");
    }

    /**
     * Test that PTextComponent supports updating text content after instantiation (Requirement 1.4).
     */
    @Test
    void testPTextComponentSupportsTextUpdate() {
        final PTextComponent textComponent = new PTextComponent("initial");
        assertEquals("initial", textComponent.getText());
        
        textComponent.setText("updated");
        assertEquals("updated", textComponent.getText(), "PTextComponent should support updating text content");
    }

    /**
     * Test that setText handles null by converting to empty string (Requirement 1.4).
     */
    @Test
    void testSetTextHandlesNull() {
        final PTextComponent textComponent = new PTextComponent("initial");
        textComponent.setText(null);
        assertEquals("", textComponent.getText(), "setText should convert null to empty string");
    }

    /**
     * Test that PTextComponent uses EmptyProps record (Requirement 7.1).
     */
    @Test
    void testPTextComponentUsesEmptyProps() {
        final PTextComponent textComponent = new PTextComponent("test");
        assertNotNull(textComponent.getCurrentProps(), "PTextComponent should have props");
        assertTrue(textComponent.getCurrentProps() instanceof PTextComponent.EmptyProps, 
                   "PTextComponent should use EmptyProps record");
    }

    /**
     * Test that PTextComponent returns correct component signature (Requirement 7.1).
     */
    @Test
    void testPTextComponentSignature() {
        final PTextComponent textComponent = new PTextComponent("test");
        assertEquals("PTextComponent", textComponent.getComponentSignature(), 
                     "PTextComponent should return correct component signature");
    }

    /**
     * Test that PTextComponent uses WEB_COMPONENT framework type (Requirement 7.1).
     */
    @Test
    void testPTextComponentFrameworkType() {
        final PTextComponent textComponent = new PTextComponent("test");
        assertEquals(FrameworkType.WEB_COMPONENT, textComponent.getFrameworkType(), 
                     "PTextComponent should use WEB_COMPONENT framework type");
    }

    /**
     * Test that PTextComponent preserves Unicode characters (Requirement 6.1).
     */
    @Test
    void testPTextComponentPreservesUnicode() {
        final String unicodeText = "Hello 世界 🌍 émojis";
        final PTextComponent textComponent = new PTextComponent(unicodeText);
        assertEquals(unicodeText, textComponent.getText(), 
                     "PTextComponent should preserve Unicode characters");
    }

    /**
     * Test that PTextComponent preserves whitespace (Requirement 6.2).
     */
    @Test
    void testPTextComponentPreservesWhitespace() {
        final String textWithWhitespace = "  Hello\n\tWorld  ";
        final PTextComponent textComponent = new PTextComponent(textWithWhitespace);
        assertEquals(textWithWhitespace, textComponent.getText(), 
                     "PTextComponent should preserve whitespace");
    }

    /**
     * Test that empty string is handled correctly.
     */
    @Test
    void testPTextComponentWithEmptyString() {
        final PTextComponent textComponent = new PTextComponent("");
        assertEquals("", textComponent.getText(), "PTextComponent should handle empty string");
    }

    /**
     * Test that text content can be updated multiple times.
     */
    @Test
    void testMultipleTextUpdates() {
        final PTextComponent textComponent = new PTextComponent("first");
        assertEquals("first", textComponent.getText());
        
        textComponent.setText("second");
        assertEquals("second", textComponent.getText());
        
        textComponent.setText("third");
        assertEquals("third", textComponent.getText());
    }
}
