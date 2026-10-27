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

package com.ponysdk.core.ui.list.paging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;

public class PagerTest extends PSuite {

    private static List<String> anchorTexts(final DefaultPagerView view) {
        final List<String> texts = new ArrayList<>();
        for (int i = 0; i < view.getWidgetCount(); i++) {
            texts.add(((PAnchor) view.getWidget(i)).getText());
        }
        return texts;
    }

    @Test
    public void testDefaults() {
        final DefaultPagerView view = new DefaultPagerView();
        final Pager<String> pager = new Pager<>(view);
        assertEquals(20, pager.getPageSize());
        assertEquals(0, pager.getCurrentPage());
        assertSame(view, pager.getView());
        assertSame(view, pager.asWidget());
        assertFalse(view.isVisible());
        assertEquals(List.of("<<", "<", ">", ">>"), anchorTexts(view));

        assertEquals(10, new Pager<String>(view, 10).getPageSize());
    }

    @Test
    public void testSinglePageHidesThePager() {
        final DefaultPagerView view = new DefaultPagerView();
        final Pager<String> pager = new Pager<>(view, 10, 3);
        pager.process(25);
        assertTrue(view.isVisible());
        pager.process(10);
        assertFalse(view.isVisible());
        pager.process(0);
        assertFalse(view.isVisible());
    }

    @Test
    public void testProcessBuildsPagingWindow() {
        final DefaultPagerView view = new DefaultPagerView();
        final Pager<String> pager = new Pager<>(view, 10, 3);

        pager.process(95);
        assertTrue(view.isVisible());
        assertEquals(List.of("<<", "<", "1", "2", "3", ">", ">>"), anchorTexts(view));
        final PAnchor first = (PAnchor) view.getWidget(2);
        assertFalse(first.isEnabled());
        assertTrue(first.hasStyleName("pony-Page-Navigation-selectedItem"));
        assertFalse(((PAnchor) view.getWidget(0)).isEnabled());
        assertFalse(((PAnchor) view.getWidget(1)).isEnabled());
        assertTrue(((PAnchor) view.getWidget(5)).isEnabled());
        assertTrue(((PAnchor) view.getWidget(6)).isEnabled());

        pager.setCurrentPage(5);
        pager.process(95);
        assertEquals(List.of("<<", "<", "5", "6", "7", ">", ">>"), anchorTexts(view));
        assertFalse(((PAnchor) view.getWidget(3)).isEnabled());

        // The window sticks to the last pages
        pager.setCurrentPage(9);
        pager.process(95);
        assertEquals(List.of("<<", "<", "8", "9", "10", ">", ">>"), anchorTexts(view));
        assertFalse(((PAnchor) view.getWidget(6)).isEnabled());

        // The current page is clamped to the page count
        pager.process(30);
        assertEquals(2, pager.getCurrentPage());
        assertEquals(List.of("<<", "<", "1", "2", "3", ">", ">>"), anchorTexts(view));
    }

    @Test
    public void testNavigationNotifiesListeners() {
        final DefaultPagerView view = new DefaultPagerView();
        final Pager<String> pager = new Pager<>(view, 10, 5);
        final List<Integer> pages = new ArrayList<>();
        pager.addListener(pages::add);
        pager.setCurrentPage(2);
        pager.process(100);

        PEmulator.click(view.getWidget(0)); // start
        PEmulator.click(view.getWidget(1)); // previous
        PEmulator.click(view.getWidget(3)); // page 2 (index 1)
        PEmulator.click(view.getWidget(view.getWidgetCount() - 2)); // next
        PEmulator.click(view.getWidget(view.getWidgetCount() - 1)); // end
        assertEquals(List.of(0, 1, 1, 3, 9), pages);
    }

    @Test
    public void testClearRemovesPageItems() {
        final DefaultPagerView view = new DefaultPagerView();
        view.addPageIndex(0);
        view.addPageIndex(1);
        assertEquals(List.of("<<", "<", "1", "2", ">", ">>"), anchorTexts(view));
        final PAnchor removed = (PAnchor) view.getWidget(2);
        view.clear();
        assertEquals(List.of("<<", "<", ">", ">>"), anchorTexts(view));

        final List<Integer> pages = new ArrayList<>();
        view.addPagerListener(pages::add);
        PEmulator.click(removed);
        assertTrue(pages.isEmpty());

        // A navigation item without page index does not notify
        PEmulator.click(view.getWidget(0));
        assertTrue(pages.isEmpty());
    }
}
