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

import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import org.junit.Test;

import static org.junit.Assert.*;

public class PButtonTest extends PSuite {

    @Test
    public void testInit() {
        final PButton widget = Element.newPButton();
        assertEquals(WidgetType.BUTTON, widget.getWidgetType());
        assertNotNull(widget.toString());
    }

    @Test
    public void testAttach() {
        final PButton button = Element.newPButton();
        PWindow.getMain().add(button);
        assertEquals(PWindow.getMain().getPRootPanel().getWidgetCount(), 1);
        button.removeFromParent();
        assertEquals(PWindow.getMain().getPRootPanel().getWidgetCount(), 0);
    }

    @Test
    public void testSetText() {
        final PButton widget = Element.newPButton("", "HTML");
        assertEquals("", widget.getText());
        assertEquals("HTML", widget.getHTML());
        widget.setText("Text");
        assertEquals("Text", widget.getText());
        assertNull(widget.getHTML());
    }

    @Test
    public void testSetHtml() {
        final PButton widget = Element.newPButton("Text");
        assertEquals("Text", widget.getText());
        assertNull(widget.getHTML());
        widget.setHTML("HTML");
        assertEquals("HTML", widget.getHTML());
        assertNull(widget.getText());
    }

    @Test
    public void testSetEnabled() {
        final PButton widget = Element.newPButton();
        assertTrue(widget.isEnabled());
        widget.setEnabled(false);
        assertFalse(widget.isEnabled());
        widget.setEnabled(true);
        assertTrue(widget.isEnabled());
    }

    @Test
    public void testSetEnabledOnRequest() {
        final PButton widget = Element.newPButton();
        assertFalse(widget.isEnabledOnRequest());
        widget.setEnabledOnRequest(false);
        assertFalse(widget.isEnabledOnRequest());
        widget.setEnabledOnRequest(true);
        assertTrue(widget.isEnabledOnRequest());
    }

    @Test
    public void testShowLoadingOnRequest() {
        final PButton widget = Element.newPButton();
        assertFalse(widget.isShowLoadingOnRequest());
        widget.showLoadingOnRequest(false);
        assertFalse(widget.isShowLoadingOnRequest());
        widget.showLoadingOnRequest(true);
        assertTrue(widget.isShowLoadingOnRequest());
    }

    @Test
    public void testDumDOM() {
        final PButton button = Element.newPButton();
        String ID = String.valueOf(button.getID());

        assertEquals("<button pid=\"" + ID + "\" class=\"\"></button>", button.dumpDOM());

        button.setHTML("<a>html</a>");
        assertEquals("<button pid=\"" + ID + "\" class=\"\"><a>html</a></button>", button.dumpDOM());

        button.setHTML("text");
        assertEquals("<button pid=\"" + ID + "\" class=\"\">text</button>", button.dumpDOM());

        button.addStyleName("style");
        assertEquals("<button pid=\"" + ID + "\" class=\"style\">text</button>", button.dumpDOM());
    }

}
