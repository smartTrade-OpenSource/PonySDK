/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.driver;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;

import com.ponysdk.core.model.WidgetType;

public class PonyWebElementTest {

    @Test
    public void searchesAndPrintsItsSubtree() throws Exception {
        final PonyWebElement panel = new PonyWebElement(null, 1, WidgetType.FLOW_PANEL);
        final PonyWebElement label = new PonyWebElement(null, 2, WidgetType.LABEL);
        label.styles.add("title");
        label.text = "hi";
        panel.children.add(label);
        label.parent = panel;

        assertSame(label, panel.findElement(By.className("title")));
        assertEquals(List.of(label), panel.findElementsAsPony(By.tagName("LABEL")));
        assertEquals(List.of(label), panel.findElements(By.cssSelector(".title")));

        final StringWriter writer = new StringWriter();
        panel.printTree(writer);
        assertEquals("<FLOW_PANEL id='1' >\n\t<LABEL id='2' class='[title]' >hi\n\t</LABEL>\n</FLOW_PANEL>\n", writer.toString());
    }

    @Test
    public void webElementState() {
        final PonyWebElement element = new PonyWebElement(null, 7, WidgetType.CHECKBOX);
        assertEquals("CHECKBOX", element.getTagName());
        assertNull(new PonyWebElement(null, 8, null).getTagName());
        assertEquals(7, element.hashCode());
        assertFalse(element.isSelected());
        assertTrue(element.isDisplayed());
        element.setDisplayed(false);
        assertFalse(element.isDisplayed());
        assertThrows(UnsupportedOperationException.class, element::submit);
        // Geometry, CSS and screenshots have no meaning without a browser
        assertNull(element.getLocation());
        assertNull(element.getSize());
        assertNull(element.getRect());
        assertNull(element.getCssValue("color"));
        assertNull(element.getScreenshotAs(OutputType.BASE64));
    }
}
