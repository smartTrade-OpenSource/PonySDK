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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PComplexPanelTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private static PWindow openedWindow() {
        final PWindow window = Element.newPWindow(null, null);
        window.open();
        return window;
    }

    @Test
    public void testEmptyPanel() {
        final PFlowPanel panel = Element.newPFlowPanel();
        assertEquals(0, panel.getWidgetCount());
        assertNull(panel.getWidget(0));
        assertEquals(-1, panel.getWidgetIndex(Element.newPLabel()));
        assertNull(panel.getChild(42));
        assertFalse(panel.iterator().hasNext());
        assertFalse(panel.remove((PWidget) null));
        assertFalse(panel.remove(Element.newPLabel()));
    }

    @Test
    public void testAddWritesAddWhenInitialized() {
        final PFlowPanel panel = Element.newPFlowPanel();
        PWindow.getMain().add(panel);
        writer.clear();
        final PLabel label = Element.newPLabel("a");
        panel.add(label);
        assertTrue(label.isInitialized());
        assertSame(panel, label.getParent());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_CREATE).contains(label.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(label.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.PARENT_OBJECT_ID).contains(panel.getID()));
    }

    @Test
    public void testAddStackedUntilAttached() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel label = Element.newPLabel("a");
        panel.add(label);
        assertFalse(label.isInitialized());
        writer.clear();
        PWindow.getMain().add(panel);
        assertTrue(label.isInitialized());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(label.getID()));
    }

    @Test
    public void testAddVarargs() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel a = Element.newPLabel();
        final PLabel b = Element.newPLabel();
        panel.add(a, b);
        assertEquals(2, panel.getWidgetCount());
        assertEquals(1, panel.getWidgetIndex(b));
    }

    @Test
    public void testAddMovesWidgetFromPreviousParent() {
        final PFlowPanel first = Element.newPFlowPanel();
        final PFlowPanel second = Element.newPFlowPanel();
        final PLabel label = Element.newPLabel();
        first.add(label);
        second.add(label);
        assertEquals(0, first.getWidgetCount());
        assertEquals(1, second.getWidgetCount());
        assertSame(second, label.getParent());
    }

    @Test
    public void testAddItself() {
        assertThrows(IllegalStateException.class, () -> {
            final PFlowPanel panel = Element.newPFlowPanel();
            panel.add(panel);
    
        });
    }

    @Test
    public void testInsertItself() {
        assertThrows(IllegalStateException.class, () -> {
            final PFlowPanel panel = Element.newPFlowPanel();
            panel.insert(panel, 0);
    
        });
    }

    @Test
    public void testAddWidgetOfAnotherWindowOnInitializedPanel() {
        final PWindow other = openedWindow();
        final PLabel label = Element.newPLabel();
        other.add(label);
        final PFlowPanel panel = Element.newPFlowPanel();
        PWindow.getMain().add(panel);
        try {
            panel.add(label);
            fail();
        } catch (final IllegalAccessError e) {
            assertTrue(e.getMessage().startsWith("Can't attach widget"));
        }
    }

    @Test
    public void testAddWidgetOfAnotherWindowOnDetachedPanel() {
        final PWindow other = openedWindow();
        final PLabel label = Element.newPLabel();
        other.add(label);
        final PFlowPanel panel = Element.newPFlowPanel();
        try {
            panel.add(label);
            fail();
        } catch (final IllegalAccessError e) {
            assertTrue(e.getMessage().startsWith("Can't only attach widget"));
        }
    }

    @Test
    public void testInsertWidgetOfAnotherWindow() {
        assertThrows(IllegalAccessError.class, () -> {
            final PWindow other = openedWindow();
            final PLabel label = Element.newPLabel();
            other.add(label);
            final PFlowPanel panel = Element.newPFlowPanel();
            PWindow.getMain().add(panel);
            panel.insert(label, 0);
    
        });
    }

    @Test
    public void testAttachToAnotherWindow() {
        assertThrows(IllegalAccessError.class, () -> {
            final PFlowPanel panel = Element.newPFlowPanel();
            PWindow.getMain().add(panel);
            panel.attach(openedWindow(), null);
    
        });
    }

    @Test
    public void testAttachTwiceToSameWindow() {
        final PFlowPanel panel = Element.newPFlowPanel();
        PWindow.getMain().add(panel);
        assertFalse(panel.attach(PWindow.getMain(), null));
    }

    @Test
    public void testInsertWritesIndexOnlyWhenNotLast() {
        final PFlowPanel panel = Element.newPFlowPanel();
        PWindow.getMain().add(panel);
        final PLabel a = Element.newPLabel();
        final PLabel b = Element.newPLabel();
        writer.clear();

        panel.insert(a, 0);
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(a.getID()));
        assertFalse((writer.count(ServerToClientModel.INDEX) > 0));

        panel.insert(b, 0);
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(b.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.INDEX).contains(0));
        assertSame(b, panel.getWidget(0));
        assertSame(a, panel.getWidget(1));
    }

    @Test
    public void testInsertOnDetachedPanel() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel a = Element.newPLabel();
        final PLabel b = Element.newPLabel();
        panel.insert(a, 0);
        panel.insert(b, 0);
        writer.clear();
        PWindow.getMain().add(panel);
        assertFalse((writer.count(ServerToClientModel.INDEX) > 0));
        assertSame(b, panel.getWidget(0));
    }

    @Test
    public void testRemoveWritesRemove() {
        final PFlowPanel panel = Element.newPFlowPanel();
        PWindow.getMain().add(panel);
        final PLabel label = Element.newPLabel();
        panel.add(label);
        writer.clear();
        assertTrue(panel.remove(0));
        assertNull(label.getParent());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_REMOVE).contains(label.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.PARENT_OBJECT_ID).contains(panel.getID()));
    }

    @Test
    public void testIteratorAndClear() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel a = Element.newPLabel();
        final PLabel b = Element.newPLabel();
        panel.add(a, b);
        final Iterator<PWidget> it = panel.iterator();
        assertSame(a, it.next());
        assertSame(b, it.next());
        panel.clear();
        assertEquals(0, panel.getWidgetCount());
        assertNull(a.getParent());
        assertNull(b.getParent());
    }

    @Test
    public void testAssertIsChild() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel child = Element.newPLabel();
        panel.add(child);
        panel.assertIsChild(child);
        panel.assertIsChild(null);
        try {
            panel.assertIsChild(Element.newPLabel());
            fail();
        } catch (final IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void testDumpDOM() {
        final PFlowPanel panel = Element.newPFlowPanel();
        panel.setAttribute("data-x", "1");
        panel.setAttribute("flag");
        final PMenuItemSeparator child = Element.newPMenuItemSeparator();
        panel.add(child);
        final String dom = panel.dumpDOM();
        assertTrue(dom.startsWith("<div pid=\"" + panel.getID() + "\""), dom);
        assertTrue(dom.contains(" data-x = 1"), dom);
        assertTrue(dom.contains(" flag"), dom);
        assertFalse(dom.contains("flag ="), dom);
        assertTrue(dom.endsWith("><li pid=\"" + child.getID() + "\" separator></li></div>"), dom);
    }

    @Test
    public void testDumpDOMOfEmptyPanel() {
        final PFlowPanel panel = Element.newPFlowPanel();
        assertEquals("<div pid=\"" + panel.getID() + "\"></div>", panel.dumpDOM());
    }

    @Test
    public void attachingToNoWindowDoesNothing() {
        final PFlowPanel panel = Element.newPFlowPanel();
        assertFalse(panel.attach(null, null));
        assertNull(panel.getWindow());
        assertFalse(panel.remove(null));
    }

    @Test
    public void anEmptyAttributeIsDumpedWithoutValue() {
        final PFlowPanel panel = Element.newPFlowPanel();
        panel.setAttribute("hidden", "");
        panel.setAttribute("title", "t");
        assertTrue(panel.dumpDOM().contains(" hidden title = t"), panel.dumpDOM());
    }

    @Test
    public void removingNothingFromAPanelWithChildren() {
        final PFlowPanel panel = Element.newPFlowPanel();
        panel.add(Element.newPLabel("a"));
        assertFalse(panel.remove(null));
        assertEquals(1, panel.getWidgetCount());
    }

    @Test
    public void anAttributeWithoutValueIsDumpedByName() {
        final PFlowPanel panel = Element.newPFlowPanel();
        panel.setAttribute("nil", null);
        assertTrue(panel.dumpDOM().contains(" nil>"), panel.dumpDOM());
    }
}
