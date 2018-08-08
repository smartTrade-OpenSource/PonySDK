/*
 * Copyright (c) 2018 PonySDK
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

package com.ponysdk.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * ArrayList based Set
 * WARNING : should not be used as a List even if it does implement it
 */
public class ArraySet<E> extends ArrayList<E> implements Set<E> {

    public ArraySet() {
        super();
    }

    public ArraySet(final Collection<? extends E> c) {
        super(c.size());
        addAll(c);
    }

    /**
     * @param initialCapacity
     *            ArrayList's initialCapacity
     */
    public ArraySet(final int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public boolean add(final E e) {
        if (contains(e)) return false;
        return super.add(e);
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        boolean added = false;
        for (final E e : c) {
            added |= add(e);
        }
        return added;
    }

    @Override
    public E set(final int index, final E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(final int index, final E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

}
