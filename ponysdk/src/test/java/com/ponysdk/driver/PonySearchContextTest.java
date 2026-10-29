/*
 * Copyright (c) 2018 PonySDK
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.ponysdk.core.model.WidgetType;

/**
 * Tests {@link PonySearchContext} lookups. Regression guard for the Selenium-4 infinite recursion:
 * {@code findElement(By)} used to delegate to {@code by.findElement(this)}, which bounces back into
 * this SearchContext and overflows the stack. Lookups must now resolve directly without recursion.
 */
public class PonySearchContextTest {

    private static PonyWebElement element(final int id, final WidgetType type, final String style, final String idAttr) {
        final PonyWebElement e = new PonyWebElement(null, id, type); // driver not needed for searching
        if (style != null) e.styles.add(style);
        if (idAttr != null) e.attributes.put("id", idAttr);
        return e;
    }

    private PonySearchContext context() {
        final PonyWebElement button = element(1, WidgetType.BUTTON, "primary", "submit");
        final PonyWebElement label = element(2, WidgetType.LABEL, "title", null);
        final PonyWebElement otherButton = element(3, WidgetType.BUTTON, "secondary", "cancel");
        return new PonySearchContext(List.of(button, label, otherButton), false);
    }

    @Test
    public void findByTagNameResolvesWithoutRecursion() {
        // This call previously triggered StackOverflowError under Selenium 4.
        final var found = context().findElement(By.tagName("LABEL"));
        assertEquals(WidgetType.LABEL, ((PonyWebElement) found).widgetType);
    }

    @Test
    public void findElementsByTagNameReturnsAllMatches() {
        assertEquals(2, context().findElements(By.tagName("BUTTON")).size());
        assertEquals(1, context().findElements(By.tagName("LABEL")).size());
    }

    @Test
    public void findByClassName() {
        final var found = (PonyWebElement) context().findElement(By.className("title"));
        assertEquals(WidgetType.LABEL, found.widgetType);
    }

    @Test
    public void findById() {
        final var found = (PonyWebElement) context().findElement(By.id("cancel"));
        assertEquals(3, found.objectID);
    }

    @Test
    public void findByCssSelectorTagAndClass() {
        final var found = (PonyWebElement) context().findElement(By.cssSelector("BUTTON.primary"));
        assertEquals(1, found.objectID);
    }

    @Test
    public void missingElementReturnsNull() {
        assertNull(context().findElement(By.tagName("CHECKBOX")));
        assertNull(context().findElement(By.className("nope")));
        assertNull(context().findElement(By.id("nope")));
    }

    private static void attach(final PonyWebElement parent, final PonyWebElement child) {
        parent.children.add(child);
        child.parent = parent;
    }

    private static int[] ids(final List<?> elements) {
        return elements.stream().mapToInt(e -> ((PonyWebElement) e).objectID).toArray();
    }

    /** panel(10) > [ inner panel(11, "box") > [ label(12, "title big"), button(13) ], label(14, "title") ] */
    private static PonySearchContext tree(final boolean global) {
        final PonyWebElement panel = element(10, WidgetType.FLOW_PANEL, null, null);
        final PonyWebElement inner = element(11, WidgetType.FLOW_PANEL, "box", null);
        final PonyWebElement nested = element(12, WidgetType.LABEL, "title", "deep");
        nested.styles.add("big");
        nested.attributes.put("name", "n");
        final PonyWebElement button = element(13, WidgetType.BUTTON, null, null);
        final PonyWebElement label = element(14, WidgetType.LABEL, "title", null);
        label.attributes.put("name", "n");
        attach(panel, inner);
        attach(inner, nested);
        attach(inner, button);
        attach(panel, label);
        // The driver's global context lists every element, flat; an element's context holds its children and dives
        return global ? new PonySearchContext(List.of(panel, inner, nested, button, label), false)
                : new PonySearchContext(List.of(panel), true);
    }

    @Test
    public void cssDescendantAndChildCombinators() {
        for (final boolean global : new boolean[] { true, false }) {
            final PonySearchContext context = tree(global);
            final String in = global ? "global" : "element";
            assertArrayEquals(new int[] { 12, 14 }, ids(context.findElements(By.cssSelector("FLOW_PANEL LABEL"))), in);
            assertArrayEquals(new int[] { 12, 14 }, ids(context.findElements(By.cssSelector("FLOW_PANEL>LABEL"))), in);
            assertArrayEquals(new int[] { 12 }, ids(context.findElements(By.cssSelector("FLOW_PANEL > LABEL.big"))), in);
            assertArrayEquals(new int[] { 12 }, ids(context.findElements(By.cssSelector(".box > .title.big"))), in);
            assertArrayEquals(new int[] { 12 }, ids(context.findElements(By.cssSelector(".box LABEL"))), in);
            assertArrayEquals(new int[] { 13 }, ids(context.findElements(By.cssSelector("FLOW_PANEL FLOW_PANEL BUTTON"))), in);
            assertEquals(12, ((PonyWebElement) context.findElement(By.cssSelector("FLOW_PANEL .big"))).objectID, in);
            assertNull(context.findElement(By.cssSelector("BUTTON LABEL")), in);
        }
    }

    @Test
    public void otherLookupsDiveIntoChildren() {
        final PonySearchContext context = tree(false);
        assertArrayEquals(new int[] { 12, 14 }, ids(context.findElements(By.tagName("LABEL"))));
        assertArrayEquals(new int[] { 12, 14 }, ids(context.findElements(By.className("title"))));
        assertArrayEquals(new int[] { 12, 14 }, ids(context.findElements(By.name("n"))));
        assertEquals(12, ((PonyWebElement) context.findElement(By.id("deep"))).objectID);
        assertEquals(13, ((PonyWebElement) context.findElement(By.tagName("BUTTON"))).objectID);
        assertNull(context.findElement(By.name("none")));
    }

    @Test
    public void namedLookupHelpers() {
        final PonySearchContext context = tree(false);
        assertEquals(12, ((PonyWebElement) context.findElementById("deep")).objectID);
        assertEquals(14, ((PonyWebElement) context.findElementByName("n")).objectID, "breadth first: the nearest match");
        assertEquals(11, ((PonyWebElement) context.findElementByClassName("box")).objectID);
        assertEquals(12, ((PonyWebElement) context.findElementByCssSelector(".box .big")).objectID);
        assertEquals(13, ((PonyWebElement) context.findElementByTagName("BUTTON")).objectID);
        assertArrayEquals(new int[] { 12, 14 }, ids(context.findElementsByName("n")));
        assertArrayEquals(new int[] { 12, 14 }, ids(context.findElementsByClassName("title")));
        assertArrayEquals(new int[] { 12 }, ids(context.findElementsByClassName("big title")));
        assertArrayEquals(new int[] { 13 }, ids(context.findElementsByCssSelector(".box BUTTON")));
        assertArrayEquals(new int[] { 10, 11 }, ids(context.findElementsByTagName("FLOW_PANEL")));
        assertNull(context.findElementByClassName("  "), "a blank lookup finds nothing");
        assertNull(context.findElementsByTagName(" "));
    }

    @Test
    public void invalidSelectorsAreRejected() {
        final PonySearchContext context = tree(true);
        assertThrows(IllegalArgumentException.class, () -> context.findElements(By.cssSelector("LABEL.")));
        assertThrows(IllegalArgumentException.class, () -> context.findElements(By.cssSelector("FLOW_PANEL >")));
        assertThrows(IllegalArgumentException.class, () -> context.findElements(By.cssSelector("FLOW_PANEL >> LABEL")));
        assertThrows(IllegalArgumentException.class, () -> context.findElements(By.cssSelector(">LABEL")));
        assertThrows(IllegalArgumentException.class, () -> context.findElements(By.cssSelector("NOT_A_WIDGET")));
        assertThrows(IllegalArgumentException.class, () -> context.findElement(By.xpath("//div")));
        assertThrows(UnsupportedOperationException.class, () -> context.findElementsById("deep"));
    }

    @Test
    public void aSingleLookupAnswersTheFirstMatch() {
        final PonyWebElement first = element(1, WidgetType.LABEL, "x", null);
        final PonySearchContext context = new PonySearchContext(List.of(first, element(2, WidgetType.LABEL, "x", null)), false);
        assertSame(first, context.findElement(By.className("x")));
        assertSame(first, context.findElement(By.cssSelector(".x")));
    }
}
