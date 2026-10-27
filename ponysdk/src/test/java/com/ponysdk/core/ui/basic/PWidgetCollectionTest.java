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

import org.junit.jupiter.api.Test;

import com.ponysdk.test.PSuite;

public class PWidgetCollectionTest extends PSuite {

    private static PWidgetCollection newCollection() {
        return new PWidgetCollection(Element.newPFlowPanel());
    }

    @Test
    public void testAddGrowsBeyondInitialCapacity() {
        final PWidgetCollection collection = newCollection();
        final PLabel[] labels = new PLabel[10];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = Element.newPLabel();
            collection.add(labels[i]);
        }
        assertEquals(10, collection.size());
        for (int i = 0; i < labels.length; i++) {
            assertSame(labels[i], collection.get(i));
            assertEquals(i, collection.indexOf(labels[i]));
            assertTrue(collection.contains(labels[i]));
        }
        assertFalse(collection.contains(Element.newPLabel()));
        assertEquals(-1, collection.indexOf(Element.newPLabel()));
    }

    @Test
    public void testInsert() {
        final PWidgetCollection collection = newCollection();
        final PLabel a = Element.newPLabel();
        final PLabel b = Element.newPLabel();
        final PLabel c = Element.newPLabel();
        collection.add(a);
        collection.add(c);
        collection.insert(b, 1);
        assertSame(a, collection.get(0));
        assertSame(b, collection.get(1));
        assertSame(c, collection.get(2));
    }

    @Test
    public void testInsertNegativeIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            newCollection().insert(Element.newPLabel(), -1);
    
        });
    }

    @Test
    public void testInsertAfterEnd() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            newCollection().insert(Element.newPLabel(), 1);
    
        });
    }

    @Test
    public void testGetNegativeIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            newCollection().get(-1);
    
        });
    }

    @Test
    public void testGetAfterEnd() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            newCollection().get(0);
    
        });
    }

    @Test
    public void testRemove() {
        final PWidgetCollection collection = newCollection();
        final PLabel a = Element.newPLabel();
        final PLabel b = Element.newPLabel();
        collection.add(a);
        collection.add(b);
        assertTrue(collection.remove(a));
        assertFalse(collection.remove(a));
        assertEquals(1, collection.size());
        assertSame(b, collection.get(0));
        collection.remove(0);
        assertEquals(0, collection.size());
    }

    @Test
    public void testRemoveNegativeIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            newCollection().remove(-1);
    
        });
    }

    @Test
    public void testRemoveIndexAfterEnd() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            newCollection().remove(0);
    
        });
    }

    @Test
    public void testToString() {
        final PWidgetCollection collection = newCollection();
        final PLabel a = Element.newPLabel();
        collection.add(a);
        assertEquals("[" + a + ", null, null, null]", collection.toString());
    }

    @Test
    public void testIteratorRemoveDelegatesToParent() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel a = Element.newPLabel();
        final PLabel b = Element.newPLabel();
        panel.add(a, b);
        final Iterator<PWidget> it = panel.iterator();
        assertTrue(it.hasNext());
        assertSame(a, it.next());
        it.remove();
        assertEquals(1, panel.getWidgetCount());
        assertTrue(it.hasNext());
        assertSame(b, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testIteratorRemoveBeforeNext() {
        assertThrows(IllegalStateException.class, () -> {
            final PFlowPanel panel = Element.newPFlowPanel();
            panel.add(Element.newPLabel());
            panel.iterator().remove();
    
        });
    }

    @Test
    public void testIteratorNextAfterEnd() {
        assertThrows(NoSuchElementException.class, () -> {
            final PFlowPanel panel = Element.newPFlowPanel();
            panel.add(Element.newPLabel());
            final Iterator<PWidget> it = panel.iterator();
            it.next();
            it.next();
    
        });
    }

}
