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

package com.ponysdk.core.ui.datagrid;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;

public class DataGridTreeSet<E> {

    private TreeSet<E> set;
    private final Map<E, Integer> indexByObject = new TreeMap<>();
    private final Map<Integer, E> objectByIndex = new TreeMap<>();

    public DataGridTreeSet(final Comparator<E> comparator) {
        set = new TreeSet<>((o1, o2) -> {
            final int compare = comparator.compare(o1, o2);
            if (compare != 0) return compare;
            return Integer.compare(o1.hashCode(), o2.hashCode());
        });
    }

    public int getPosition(final E e) {
        return indexByObject.get(e);
    }


    public void put(E e, BiConsumer<Integer, E> consumer) {
        Integer initialPosition = Integer.MAX_VALUE;
        if (set.remove(e)) {
            initialPosition = indexByObject.get(e);
        }
        set.add(e);

        int newSize = set.headSet(e).size();

        if (newSize == initialPosition) return DELTA.UPDATED;


        int index = Math.min(set.headSet(e).size(), initialPosition);
        E old;
        do {
            old = objectByIndex.put(index, e);
            indexByObject.put(e, index);
            consumer.accept(index, e);
            e = old;
            index++;
        } while (old != null);

        return true;
    }

    public boolean remove(E e, BiConsumer<Integer, E> consumer) {
        boolean removed = set.remove(e);
        if (removed) {
            Integer index = indexByObject.remove(e);
            E old;
            for (int i = index; i < objectByIndex.size(); i++) {
                old = objectByIndex.get(i + 1);
                objectByIndex.put(i, old);
                indexByObject.put(old, i);
                consumer.accept(i, old);
            }

            objectByIndex.remove(objectByIndex.size() - 1);
        }

        return removed;
    }


    public boolean contains(final E e) {
        return set.contains(e);
    }

    public void clear() {
        set.clear();
        objectByIndex.clear();
        indexByObject.clear();
    }

    TreeSet<E> asSet() {
        return set;
    }

    public int size() {
        return set.size();
    }

    enum DELTA {
        NEW, UPDATED, MOVE;
    }
}
