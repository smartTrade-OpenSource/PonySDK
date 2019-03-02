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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import static org.junit.Assert.*;

public class PElementTest extends PSuite {

    @Test
    public void testInit() {
        final PElement w = new PElement("a");
        assertEquals(WidgetType.ELEMENT, w.getWidgetType());
        assertEquals("a", w.getTagName());
        assertNotNull(w.toString());
    }

    @Test
    public void testAttach() {
        final PElement w = new PElement("a");
        PWindow.getMain().add(w);
        assertEquals(PWindow.getMain().getPRootPanel().getWidgetCount(), 1);
        w.removeFromParent();
        assertEquals(PWindow.getMain().getPRootPanel().getWidgetCount(), 0);
    }

    @Test
    public void testSetText() {
        final PElement w = new PElement("span");
        assertNull(w.getInnerText());
        assertNull(w.getInnerHTML());
        w.setInnerText("Text");
        assertEquals("Text", w.getInnerText());
        assertNull(w.getInnerHTML());
    }

    @Test
    public void testSetHtml() {
        final PElement w = new PElement("span");
        assertNull(w.getInnerText());
        assertNull(w.getInnerHTML());
        w.setInnerHTML("<span>test</span>");
        assertEquals("<span>test</span>", w.getInnerHTML());
        assertNull(w.getInnerText());
    }

    @Test
    public void testDumpDOM() {
        final PElement w = new PElement("span");
        w.setStyleName("t");
        w.setInnerText("test");
        PWindow.getMain().add(w);
        Document document = Jsoup.parse(w.dumpDOM());
        assertEquals("test", document.getElementsByClass("t").get(0).text());
    }

}
