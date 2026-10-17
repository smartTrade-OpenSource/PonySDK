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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.junit.Test;
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
    }
}
