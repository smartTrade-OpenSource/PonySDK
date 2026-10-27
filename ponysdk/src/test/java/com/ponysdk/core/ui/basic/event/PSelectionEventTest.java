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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.eventbus.EventBus;
import com.ponysdk.test.PSuite;

public class PSelectionEventTest extends PSuite {

    @Test
    public void selectedItemCanBeReplaced() {
        final PSelectionEvent<String> event = new PSelectionEvent<>(this, "a");
        assertEquals("a", event.getSelectedItem());
        event.setSelectedItem("b");
        assertEquals("b", event.getSelectedItem());
    }

    @Test
    public void dispatchedToItsHandlers() {
        final EventBus bus = new EventBus();
        final List<String> received = new ArrayList<>();
        bus.addHandler(PSelectionEvent.TYPE, (PSelectionHandler<String>) e -> received.add(e.getSelectedItem()));
        final PSelectionEvent<String> event = new PSelectionEvent<>(this, "x");
        assertSame(PSelectionEvent.TYPE, event.getAssociatedType());
        bus.fireEvent(event);
        assertEquals(List.of("x"), received);
    }
}
