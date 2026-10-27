/*
 * Copyright (c) 2026 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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
package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PHTMLTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void capture() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restore() {
        writer.uninstall();
    }

    @Test
    public void htmlAndWordWrapTravelInTheCreate() {
        final PHTML html = Element.newPHTML("<b>x</b>", true);
        assertEquals(WidgetType.HTML, html.getWidgetType());
        assertTrue(html.isWordWrap());

        PWindow.getMain().add(html);

        assertEquals(List.of("<b>x</b>"), writer.valuesOf(ServerToClientModel.HTML));
        assertEquals(List.of(true), writer.valuesOf(ServerToClientModel.WORD_WRAP));
    }

    @Test
    public void defaultsSendNothingExtra() {
        final PHTML html = Element.newPHTML();
        assertNull(html.getHTML());
        assertFalse(html.isWordWrap());
        PWindow.getMain().add(html);
        assertEquals(0, writer.count(ServerToClientModel.HTML));
        assertEquals(0, writer.count(ServerToClientModel.WORD_WRAP));
        assertFalse(Element.newPHTML("y").isWordWrap());
    }

    @Test
    public void htmlAndTextReplaceEachOther() {
        final PHTML html = Element.newPHTML("<i>a</i>");
        html.setText("plain");
        assertEquals("plain", html.getText());
        assertNull(html.getHTML());

        html.setHTML("<i>b</i>");
        assertEquals("<i>b</i>", html.getHTML());
        assertNull(html.getText());
        assertTrue(html.toString().endsWith("html=<i>b</i>"));
    }

    @Test
    public void changesAfterAttachAreSentOnce() {
        final PHTML html = Element.newPHTML();
        PWindow.getMain().add(html);
        writer.clear();

        html.setHTML("<p/>");
        html.setHTML("<p/>");
        html.setWordWrap(true);
        html.setWordWrap(true);
        html.setWordWrap(false);

        assertEquals(List.of("<p/>"), writer.valuesOf(ServerToClientModel.HTML));
        assertEquals(List.of(true, false), writer.valuesOf(ServerToClientModel.WORD_WRAP));
    }

    @Test
    public void changesBeforeAttachAreNotStacked() {
        final PHTML html = Element.newPHTML();
        html.setHTML("<p/>");
        html.setWordWrap(true);
        assertTrue(writer.isEmpty());
        PWindow.getMain().add(html);
        assertEquals(List.of("<p/>"), writer.valuesOf(ServerToClientModel.HTML));
        assertEquals(List.of(true), writer.valuesOf(ServerToClientModel.WORD_WRAP));
    }

}
