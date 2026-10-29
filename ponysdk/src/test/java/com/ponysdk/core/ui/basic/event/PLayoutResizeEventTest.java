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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.eventbus.EventBus;
import com.ponysdk.test.PSuite;

public class PLayoutResizeEventTest extends PSuite {

    @Test
    public void carriesTheNewSizeOfEachWidget() {
        final PLabel a = Element.newPLabel();
        final PLabel b = Element.newPLabel();
        final PLayoutResizeEvent event = new PLayoutResizeEvent(this);
        assertTrue(event.getLayoutResizeData().isEmpty());
        event.addLayoutResizeData(new PLayoutResizeEvent.LayoutResizeData(a, 10.5));
        event.addLayoutResizeData(new PLayoutResizeEvent.LayoutResizeData(b, 20));

        assertEquals(2, event.getLayoutResizeData().size());
        assertSame(a, event.getLayoutResizeData().get(0).getWidget());
        assertEquals(10.5, event.getLayoutResizeData().get(0).getSize());
        assertSame(b, event.getLayoutResizeData().get(1).getWidget());
        assertEquals(20.0, event.getLayoutResizeData().get(1).getSize());
    }

    @Test
    public void dispatchedToItsHandlers() {
        final EventBus bus = new EventBus();
        final List<PLayoutResizeEvent> received = new ArrayList<>();
        bus.addHandler(PLayoutResizeEvent.TYPE, (PLayoutResizeEvent.Handler) received::add);
        final PLayoutResizeEvent event = new PLayoutResizeEvent(this);
        assertSame(PLayoutResizeEvent.TYPE, event.getAssociatedType());
        bus.fireEvent(event);
        assertEquals(List.of(event), received);
    }
}
