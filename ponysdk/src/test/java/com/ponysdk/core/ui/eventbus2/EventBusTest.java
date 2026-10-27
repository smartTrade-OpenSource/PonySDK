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

package com.ponysdk.core.ui.eventbus2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class EventBusTest {

    @Test
    public void testSubscribePostUnsubscribe() {
        final EventBus bus = new EventBus();
        // Nothing subscribed yet
        bus.post("ignored");
        bus.post(null);

        final List<String> strings = new ArrayList<>();
        final List<Integer> integers = new ArrayList<>();
        final EventBus.EventHandler<String> stringHandler = bus.subscribe(String.class, strings::add);
        final EventBus.EventHandler<Integer> integerHandler = bus.subscribe(Integer.class, integers::add);

        bus.post("a");
        bus.post(1);
        bus.post(2L);
        assertEquals(List.of("a"), strings);
        assertEquals(List.of(1), integers);

        assertTrue(bus.unsubscribe(stringHandler));
        assertFalse(bus.unsubscribe(stringHandler));
        bus.post("b");
        assertEquals(List.of("a"), strings);

        assertTrue(bus.unsubscribe(integerHandler));
        assertFalse(bus.unsubscribe(null));
        assertNull(bus.subscribe(null, o -> {
        }));
    }

    @Test
    public void testHandlersCanUnsubscribeWhilePosting() {
        final EventBus bus = new EventBus();
        final List<String> received = new ArrayList<>();
        final List<EventBus.EventHandler<String>> handlers = new ArrayList<>();
        handlers.add(bus.subscribe(String.class, s -> {
            received.add("first:" + s);
            bus.unsubscribe(handlers.get(1));
        }));
        handlers.add(bus.subscribe(String.class, s -> received.add("second:" + s)));

        bus.post("x");
        // The second handler was part of the snapshot of this post
        assertEquals(2, received.size());
        bus.post("y");
        assertEquals(List.of("first:y"), received.subList(2, received.size()));
    }

    @Test
    public void testInitialValue() {
        final EventBus bus = new EventBus();
        assertThrows(IllegalArgumentException.class, () -> bus.setInitialValueByType(String.class, null));
        bus.setInitialValueByType(String.class, () -> "initial");
        final List<String> received = new ArrayList<>();
        bus.subscribe(String.class, received::add);
        assertEquals(List.of("initial"), received);

        final List<Integer> integers = new ArrayList<>();
        bus.subscribe(Integer.class, integers::add);
        assertTrue(integers.isEmpty());

        final EventBus.EventHandler<String> manual = new EventBus.EventHandler<>(String.class);
        final List<String> manualReceived = new ArrayList<>();
        manual.subscribe(manualReceived::add);
        manual.accept("direct");
        assertEquals(List.of("direct"), manualReceived);
        assertFalse(bus.unsubscribe(new EventBus.EventHandler<>(Long.class)));
    }

    @Test
    public void testFreshBusAndReplacedInitialValue() {
        final EventBus bus = new EventBus();
        assertFalse(bus.unsubscribe(new EventBus.EventHandler<>(String.class)));
        bus.setInitialValueByType(String.class, () -> "first");
        bus.setInitialValueByType(String.class, () -> "second");
        final List<String> received = new ArrayList<>();
        bus.subscribe(String.class, received::add);
        assertEquals(List.of("second"), received);
    }
}
