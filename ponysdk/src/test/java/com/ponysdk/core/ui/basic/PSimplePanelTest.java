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

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PSimplePanelTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    @Test
    public void testInit() {
        final PSimplePanel panel = Element.newPSimplePanel();
        assertEquals(WidgetType.SIMPLE_PANEL, panel.getWidgetType());
        assertNull(panel.getWidget());
        assertFalse(panel.iterator().hasNext());
    }

    @Test
    public void testSetWidget() {
        final PSimplePanel panel = Element.newPSimplePanel();
        PWindow.getMain().add(panel);
        writer.clear();
        final PLabel label = Element.newPLabel("a");
        panel.setWidget(label);
        assertSame(label, panel.getWidget());
        assertSame(panel, label.getParent());
        assertTrue(label.isInitialized());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(label.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.PARENT_OBJECT_ID).contains(panel.getID()));

        writer.clear();
        panel.setWidget(label);
        assertTrue(writer.isEmpty());
    }

    @Test
    public void testSetWidgetReplacesPrevious() {
        final PSimplePanel panel = Element.newPSimplePanel();
        PWindow.getMain().add(panel);
        final PLabel first = Element.newPLabel();
        final PLabel second = Element.newPLabel();
        panel.setWidget(first);
        writer.clear();
        panel.setWidget((IsPWidget) second);
        assertSame(second, panel.getWidget());
        assertNull(first.getParent());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_REMOVE).contains(first.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(second.getID()));
    }

    @Test
    public void testSetWidgetMovesFromPreviousParent() {
        final PFlowPanel flow = Element.newPFlowPanel();
        final PLabel label = Element.newPLabel();
        flow.add(label);
        final PSimplePanel panel = Element.newPSimplePanel();
        panel.setWidget(label);
        assertEquals(0, flow.getWidgetCount());
        assertSame(panel, label.getParent());
    }

    @Test
    public void testSetWidgetItself() {
        assertThrows(UnsupportedOperationException.class, () -> {
            final PSimplePanel panel = Element.newPSimplePanel();
            panel.setWidget(panel);
    
        });
    }

    @Test
    public void testAddOnlyOneWidget() {
        final PSimplePanel panel = Element.newPSimplePanel();
        final PLabel first = Element.newPLabel();
        final PLabel second = Element.newPLabel();
        panel.add(first);
        panel.add(second);
        assertSame(first, panel.getWidget());
        assertNull(second.getParent());
    }

    @Test
    public void testRemove() {
        final PSimplePanel panel = Element.newPSimplePanel();
        PWindow.getMain().add(panel);
        final PLabel label = Element.newPLabel();
        assertFalse(panel.remove(label));
        panel.setWidget(label);
        assertFalse(panel.remove(Element.newPLabel()));
        writer.clear();
        assertTrue(panel.remove(label));
        assertNull(panel.getWidget());
        assertNull(label.getParent());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_REMOVE).contains(label.getID()));
    }

    @Test
    public void testIterator() {
        final PSimplePanel panel = Element.newPSimplePanel();
        final PLabel label = Element.newPLabel();
        panel.setWidget(label);
        final Iterator<PWidget> it = panel.iterator();
        it.remove();
        assertSame(label, panel.getWidget());
        assertTrue(it.hasNext());
        assertSame(label, it.next());
        assertFalse(it.hasNext());
        it.remove();
        assertNull(panel.getWidget());
        try {
            it.next();
            fail();
        } catch (final NoSuchElementException e) {
            // expected
        }
    }

    @Test
    public void testIteratorNextWhenWidgetRemovedMeanwhile() {
        assertThrows(NoSuchElementException.class, () -> {
            final PSimplePanel panel = Element.newPSimplePanel();
            final PLabel label = Element.newPLabel();
            panel.setWidget(label);
            final Iterator<PWidget> it = panel.iterator();
            panel.remove(label);
            it.next();
    
        });
    }

    @Test
    public void testClear() {
        final PSimplePanel panel = Element.newPSimplePanel();
        panel.setWidget(Element.newPLabel());
        panel.clear();
        assertNull(panel.getWidget());
    }

    @Test
    public void testDumpDOM() {
        final PSimplePanel panel = Element.newPSimplePanel();
        final PMenuItemSeparator child = Element.newPMenuItemSeparator();
        panel.setWidget(child);
        assertEquals("<div pid=\"" + panel.getID() + "\"><li pid=\"" + child.getID() + "\" separator></li></div>", panel.dumpDOM());
    }

}
