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

package com.ponysdk.core.ui.basic.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.eventbus.EventBus;
import com.ponysdk.test.PSuite;

public class PScrollEventTest extends PSuite {

    @Test
    public void positionAtTheTopLeft() {
        final PScrollEvent event = new PScrollEvent(this, 200, 100, 0, 0);
        assertTrue(event.isScrollTop());
        assertFalse(event.isScrollBottom());
        assertTrue(event.isScrollLeft());
        assertFalse(event.isScrollRight());
        assertEquals(0.0, event.getVerticalScrollFactor());
        assertEquals(0.0, event.getHorizontalScrollFactor());
    }

    @Test
    public void positionAtTheBottomRight() {
        final PScrollEvent event = new PScrollEvent(this, 200, 100, 200, 100);
        assertFalse(event.isScrollTop());
        assertTrue(event.isScrollBottom());
        assertFalse(event.isScrollLeft());
        assertTrue(event.isScrollRight());
        assertEquals(1.0, event.getVerticalScrollFactor());
        assertEquals(1.0, event.getHorizontalScrollFactor());
    }

    @Test
    public void positionInTheMiddle() {
        final PScrollEvent event = new PScrollEvent(this, 200, 100, 50, 25);
        assertEquals(200, event.getHeight());
        assertEquals(100, event.getWidth());
        assertEquals(50, event.getVerticalPostion());
        assertEquals(25, event.getHorizontalPosition());
        assertEquals(0.25, event.getVerticalScrollFactor());
        assertEquals(0.25, event.getHorizontalScrollFactor());
    }

    @Test
    public void dispatchedToItsHandlers() {
        final EventBus bus = new EventBus();
        final List<PScrollEvent> received = new ArrayList<>();
        bus.addHandler(PScrollEvent.TYPE, (PScrollEvent.PScrollHandler) received::add);
        final PScrollEvent event = new PScrollEvent(this, 1, 1, 0, 0);
        assertSame(PScrollEvent.TYPE, event.getAssociatedType());
        bus.fireEvent(event);
        assertEquals(List.of(event), received);
        assertSame(this, event.getSource());
    }
}
