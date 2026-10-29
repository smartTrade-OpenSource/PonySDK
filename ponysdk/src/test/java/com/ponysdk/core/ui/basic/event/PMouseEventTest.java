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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.ui.eventbus.EventBus;
import com.ponysdk.test.PSuite;

public class PMouseEventTest extends PSuite {

    @Test
    public void nativeButtonFromItsDomValue() {
        assertEquals(PMouseEvent.NativeButton.BUTTON_LEFT, PMouseEvent.NativeButton.fromValue(1));
        assertEquals(PMouseEvent.NativeButton.BUTTON_RIGHT, PMouseEvent.NativeButton.fromValue(2));
        assertEquals(PMouseEvent.NativeButton.BUTTON_MIDDLE, PMouseEvent.NativeButton.fromValue(4));
        assertThrows(IllegalArgumentException.class, () -> PMouseEvent.NativeButton.fromValue(3));
    }

    @Test
    public void toStringListsTheCoordinates() {
        final PClickEvent event = new PClickEvent(this);
        event.setX(1);
        event.setY(2);
        event.setClientX(3);
        event.setClientY(4);
        event.setNativeButton(4);
        event.setSourceAbsoluteLeft(5);
        event.setSourceAbsoluteTop(6);
        event.setSourceOffsetHeight(7);
        event.setSourceOffsetWidth(8);
        assertEquals("PClickEvent with data : null ; x = 1 ; y = 2 ; nativeButton = BUTTON_MIDDLE ; clientX = 3 ; clientY = 4"
                + " ; sourceAbsoluteLeft = 5 ; sourceAbsoluteTop = 6 ; sourceOffsetHeight = 7 ; sourceOffsetWidth = 8",
            event.toString());
    }

    @Test
    public void mouseWheelEvent() {
        final PMouseWhellEvent.Event event = new PMouseWhellEvent.Event(this);
        event.setDeltaY(-3);
        assertEquals(-3, event.getDeltaY());
        assertTrue(event.toString().endsWith(" ; deltaY = -3"));
        assertSame(PMouseWhellEvent.TYPE, event.getAssociatedType());
        assertEquals(DomHandlerType.MOUSE_WHELL, PMouseWhellEvent.TYPE.getDomHandlerType());

        final EventBus bus = new EventBus();
        final List<PMouseWhellEvent.Event> received = new ArrayList<>();
        bus.addHandler(PMouseWhellEvent.TYPE, (PMouseWhellEvent.Handler) received::add);
        bus.fireEvent(event);
        assertEquals(List.of(event), received);

        final PMouseWhellEvent.HasHandlers hasHandlers = handler -> null;
        assertEquals(null, hasHandlers.addMouseWhellHandler(received::add));
        assertTrue(new PMouseWhellEvent() {
        } instanceof PMouseWhellEvent);
    }
}
