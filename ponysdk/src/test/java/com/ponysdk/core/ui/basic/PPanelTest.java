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

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.ponysdk.test.PSuite;

public class PPanelTest extends PSuite {

    @Test
    public void testAddNotSupportedByDefault() {
        assertThrows(UnsupportedOperationException.class, () -> {
            Element.newPGrid().add(Element.newPLabel());
    
        });
    }

    @Test
    public void testAddIsPWidget() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel label = Element.newPLabel();
        final IsPWidget wrapper = () -> label;
        panel.add(wrapper);
        assertSame(label, panel.getWidget(0));
    }

    @Test
    public void testAdoptWidgetWithParent() {
        assertThrows(IllegalStateException.class, () -> {
            final PFlowPanel parent = Element.newPFlowPanel();
            final PLabel label = Element.newPLabel();
            parent.add(label);
            Element.newPFlowPanel().adopt(label);
    
        });
    }

    @Test
    public void testOrphan() {
        final PFlowPanel parent = Element.newPFlowPanel();
        final PLabel label = Element.newPLabel();
        parent.add(label);
        parent.orphan(null);
        try {
            Element.newPFlowPanel().orphan(label);
            fail();
        } catch (final IllegalStateException e) {
            assertSame(parent, label.getParent());
        }
        parent.orphan(label);
        assertNull(label.getParent());
    }

    @Test
    public void testChildrenAttachedOnInit() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel label = Element.newPLabel();
        panel.add(label);
        PWindow.getMain().add(panel);
        assertSame(PWindow.getMain(), label.getWindow());
        assertTrue(label.isInitialized());
    }

    @Test
    public void testDestroyPropagatesToChildren() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel a = Element.newPLabel();
        final PLabel b = Element.newPLabel();
        panel.add(a, b);
        PWindow.getMain().add(panel);
        final AtomicInteger destroyed = new AtomicInteger();
        a.addDestroyListener(o -> destroyed.incrementAndGet());
        b.addDestroyListener(o -> destroyed.incrementAndGet());
        panel.onDestroy();
        assertEquals(2, destroyed.get());
        assertNull(a.getWindow());
    }

}
