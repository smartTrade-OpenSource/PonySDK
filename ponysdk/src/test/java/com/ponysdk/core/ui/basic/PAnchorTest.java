/*
 * Copyright (c) 2018 PonySDK
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

import java.util.List;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.testutil.CapturingModelWriter;
import com.ponysdk.test.PSuite;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class PAnchorTest extends PSuite {

    @Test
    public void testInit() {
        final PAnchor anchor = Element.newPAnchor();
        assertEquals(WidgetType.ANCHOR, anchor.getWidgetType());
        assertNotNull(anchor.toString());
    }

    @Test
    public void testAttach() {
        final PAnchor anchor = Element.newPAnchor();
        PWindow.getMain().add(anchor);
        assertEquals(PWindow.getMain().getPRootPanel().getWidgetCount(), 1);
        anchor.removeFromParent();
        assertEquals(PWindow.getMain().getPRootPanel().getWidgetCount(), 0);
    }

    @Test
    public void testSetText() {
        final PAnchor anchor = Element.newPAnchor("");
        assertEquals("", anchor.getText());
        assertNull(anchor.getHTML());
        anchor.setText("Text");
        assertEquals("Text", anchor.getText());
        assertNull(anchor.getHTML());
    }

    @Test
    public void testSetHtml() {
        final PAnchor anchor = Element.newPAnchor();
        assertNull(anchor.getHTML());
        assertNull(anchor.getText());
        anchor.setHTML("html");
        assertEquals("html", anchor.getHTML());
        assertNull(anchor.getText());
    }

    @Test
    public void testSetHref() {
        final PAnchor anchor = Element.newPAnchor("Text", "#test");
        assertEquals("Text", anchor.getText());
        assertEquals("#test", anchor.getHref());
        assertNull(anchor.getHTML());

        final PAnchor anchor1 = Element.newPAnchor("Text");
        assertEquals("Text", anchor1.getText());
        assertNull(anchor1.getHref());
        anchor1.setHref("#test");
        assertEquals("#test", anchor1.getHref());
    }

    @Test
    public void testSetEnabled() {
        final PAnchor anchor = Element.newPAnchor();
        assertTrue(anchor.isEnabled());
        anchor.setEnabled(false);
        assertFalse(anchor.isEnabled());
        anchor.setEnabled(true);
        assertTrue(anchor.isEnabled());
    }

    @Test
    public void testSetEnabledOnRequest() {
        final PAnchor anchor = Element.newPAnchor();
        assertFalse(anchor.isEnabledOnRequest());
        anchor.setEnabledOnRequest(false);
        assertFalse(anchor.isEnabledOnRequest());
        anchor.setEnabledOnRequest(true);
        assertTrue(anchor.isEnabledOnRequest());
    }

    @Test
    public void testShowLoadingOnRequest() {
        final PAnchor anchor = Element.newPAnchor();
        assertFalse(anchor.isShowLoadingOnRequest());
        anchor.showLoadingOnRequest(false);
        assertFalse(anchor.isShowLoadingOnRequest());
        anchor.showLoadingOnRequest(true);
        assertTrue(anchor.isShowLoadingOnRequest());
    }

    @Test
    public void testDumDOM() {
        final PAnchor a = Element.newPAnchor();
        String ID = String.valueOf(a.getID());

        assertEquals("<a pid=\"" + ID + "\" class=\"\"></a>", a.dumpDOM());

        a.setHTML("<a>html</a>");
        assertEquals("<a pid=\"" + ID + "\" class=\"\"><a>html</a></a>", a.dumpDOM());

        a.setHTML("text");
        assertEquals("<a pid=\"" + ID + "\" class=\"\">text</a>", a.dumpDOM());

        a.addStyleName("style");
        assertEquals("<a pid=\"" + ID + "\" class=\"style\">text</a>", a.dumpDOM());

        a.setHref("href");
        assertEquals("<a pid=\"" + ID + "\" class=\"style\" href=\"href\">text</a>", a.dumpDOM());
    }

    @Test
    public void testCreationAndUpdatesAreWritten() {
        final CapturingModelWriter writer = CapturingModelWriter.install();
        try {
            final PAnchor anchor = Element.newPAnchor("Text", "#a");
            PWindow.getMain().add(anchor);
            assertEquals(List.of("Text"), writer.valuesOf(ServerToClientModel.TEXT));
            assertEquals(List.of("#a"), writer.valuesOf(ServerToClientModel.HREF));
            assertEquals(0, writer.count(ServerToClientModel.HTML));

            writer.clear();
            anchor.setText("Text");
            anchor.setHref("#a");
            assertTrue(writer.isEmpty());

            anchor.setHref("#b");
            anchor.setHTML("<b>x</b>");
            anchor.setHTML("<b>x</b>");
            anchor.setText("t");
            assertEquals(List.of("#b"), writer.valuesOf(ServerToClientModel.HREF));
            assertEquals(List.of("<b>x</b>"), writer.valuesOf(ServerToClientModel.HTML));
            assertEquals(List.of("t"), writer.valuesOf(ServerToClientModel.TEXT));
            assertNull(anchor.getHTML());

            writer.clear();
            final PAnchor html = Element.newPAnchor();
            html.setHTML("<i>h</i>");
            PWindow.getMain().add(html);
            assertEquals(List.of("<i>h</i>"), writer.valuesOf(ServerToClientModel.HTML));
            assertEquals(0, writer.count(ServerToClientModel.TEXT));
            assertEquals(0, writer.count(ServerToClientModel.HREF));
            anchor.removeFromParent();
            html.removeFromParent();
        } finally {
            writer.uninstall();
        }
    }

}
