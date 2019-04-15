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

import com.ponysdk.core.ui.basic.event.PCloseHandler;
import com.ponysdk.core.ui.basic.event.POpenHandler;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class PWindowTest extends PSuite {

    @Test
    public void testOpen() {
        final PWindow window = Element.newPWindow(null, null);
        assertFalse(window.isOpened());
        window.open();
        assertTrue(window.isOpened());
    }


    @Test
    public void testOpenHandler() {
        final PWindow window = Element.newPWindow(null, null);
        AtomicInteger handlerFiredCount = new AtomicInteger(0);
        window.addOpenHandler(e -> handlerFiredCount.incrementAndGet());
        assertFalse(window.isOpened());
        window.open();
        assertTrue(window.isOpened());
        assertEquals(1, handlerFiredCount.get());
    }


    @Test
    public void testRemoveOpenHandler() {
        final PWindow window = Element.newPWindow(null, null);
        AtomicInteger handlerFiredCount = new AtomicInteger(0);
        POpenHandler handler = e -> handlerFiredCount.incrementAndGet();
        window.addOpenHandler(handler);
        assertFalse(window.isOpened());
        window.removeOpenHandler(handler);
        window.open();
        assertTrue(window.isOpened());
        assertEquals(0, handlerFiredCount.get());
    }

    @Test
    public void testClose() {
        final PWindow window = Element.newPWindow(null, null);
        AtomicInteger handlerFired = new AtomicInteger(0);
        window.addCloseHandler(e -> handlerFired.incrementAndGet());
        window.open();
        assertTrue(window.isOpened());
        window.close();
        assertFalse(window.isOpened());
        assertEquals(1, handlerFired.get());
    }

    @Test
    public void testRemoveCloseHandler() {
        final PWindow window = Element.newPWindow(null, null);
        AtomicInteger handlerFiredCount = new AtomicInteger(0);
        PCloseHandler handler = e -> handlerFiredCount.incrementAndGet();
        window.addCloseHandler(handler);
        assertFalse(window.isOpened());
        window.removeCloseHandler(handler);
        window.open();
        assertTrue(window.isOpened());
        window.close();
        assertFalse(window.isOpened());
        assertEquals(0, handlerFiredCount.get());
    }

    @Test
    public void testAttachToMain() {
        final PWindow window = Element.newPWindow(PWindow.getMain(), "test", null);
        window.open();
        assertEquals(window.getWindow(), PWindow.getMain());
    }

    @Test
    public void testSetTitle() {
        final PWindow window = Element.newPWindow(PWindow.getMain(), "test", null);
        window.setTitle("title test");
        window.open();
        //assertTrue("title test",window.getTitle()); todo nciaravola api incoherent
    }

    @Test
    public void testSubWindow() {
        final PWindow window = Element.newPWindow(PWindow.getMain(), "test", null);
        window.open();
        assertTrue(window.isOpened());

        final PWindow subWindow = Element.newPWindow(window, "test", null);
        subWindow.open();
        assertTrue(subWindow.isOpened());

        assertEquals(window, subWindow.getParent());

        window.close();
        assertFalse(window.isOpened());
        assertFalse(subWindow.isOpened());
    }

}
