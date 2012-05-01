/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.ui.server.basic;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ponysdk.ui.server.basic.event.HasPWidgets;

/**
 * A simple collection of widgets to be used by {@link com.google.gwt.user.client.ui.Panel panels} and
 * {@link com.google.gwt.user.client.ui.Composite composites}.
 * <p>
 * The main purpose of this specialized collection is to implement {@link java.util.Iterator#remove()} in a
 * way that delegates removal to its panel. This makes it much easier for the panel to implement an
 * {@link com.google.gwt.user.client.ui.HasWidgets#iterator() iterator} that supports removal of widgets.
 * </p>
 */
public class PWidgetCollection implements Iterable<PWidget> {

    private class WidgetIterator implements Iterator<PWidget> {

        private int index = -1;

        @Override
        public boolean hasNext() {
            return index < (size - 1);
        }

        @Override
        public PWidget next() {
            if (index >= size) { throw new NoSuchElementException(); }
            return array[++index];
        }

        @Override
        public void remove() {
            if ((index < 0) || (index >= size)) { throw new IllegalStateException(); }
            parent.remove(array[index--]);
        }
    }

    private static final int INITIAL_SIZE = 4;

    private PWidget[] array;

    private final HasPWidgets parent;

    private int size;

    public PWidgetCollection(final HasPWidgets parent) {
        this.parent = parent;
        array = new PWidget[INITIAL_SIZE];
    }

    public void add(final PWidget w) {
        insert(w, size);
    }

    public boolean contains(final PWidget w) {
        return (indexOf(w) != -1);
    }

    public PWidget get(final int index) {
        if ((index < 0) || (index >= size)) { throw new IndexOutOfBoundsException(); }

        return array[index];
    }

    public int indexOf(final PWidget w) {
        for (int i = 0; i < size; ++i) {
            if (array[i] == w) { return i; }
        }

        return -1;
    }

    public void insert(final PWidget w, final int beforeIndex) {
        if ((beforeIndex < 0) || (beforeIndex > size)) {
            if ((beforeIndex < 0)) throw new IndexOutOfBoundsException("(beforeIndex (" + beforeIndex + ") < 0)");
            else throw new IndexOutOfBoundsException("beforeIndex (" + beforeIndex + ") > size (" + size + ")");
        }

        // Realloc array if necessary (doubling).
        if (size == array.length) {
            final PWidget[] newArray = new PWidget[array.length * 2];
            for (int i = 0; i < array.length; ++i) {
                newArray[i] = array[i];
            }
            array = newArray;
        }

        ++size;

        // Move all widgets after 'beforeIndex' back a slot.
        for (int i = size - 1; i > beforeIndex; --i) {
            array[i] = array[i - 1];
        }

        array[beforeIndex] = w;
    }

    @Override
    public Iterator<PWidget> iterator() {
        return new WidgetIterator();
    }

    public void remove(final int index) {
        if ((index < 0) || (index >= size)) { throw new IndexOutOfBoundsException(); }

        --size;
        for (int i = index; i < size; ++i) {
            array[i] = array[i + 1];
        }

        array[size] = null;
    }

    public void remove(final PWidget w) {
        final int index = indexOf(w);
        if (index == -1) { throw new NoSuchElementException(); }

        remove(index);
    }

    public int size() {
        return size;
    }
}
