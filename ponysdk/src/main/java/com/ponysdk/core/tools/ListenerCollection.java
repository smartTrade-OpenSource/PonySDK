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

package com.ponysdk.core.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ListenerCollection<C> implements Collection<C> {

    private final Set<C> listeners = Collections.newSetFromMap(new ConcurrentHashMap<C, Boolean>());

    public boolean register(final C listener) {
        return listeners.add(listener);
    }

    public boolean unregister(final C listener) {
        return listeners.remove(listener);
    }

    @Override
    public Iterator<C> iterator() {
        return listeners.iterator();
    }

    @Override
    public int size() {
        return listeners.size();
    }

    @Override
    public boolean isEmpty() {
        return listeners.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return listeners.contains(o);
    }

    @Override
    public Object[] toArray() {
        return listeners.toArray();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        return listeners.toArray(a);
    }

    @Override
    public boolean add(final C e) {
        return listeners.add(e);
    }

    @Override
    public boolean remove(final Object o) {
        return listeners.remove(o);
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return listeners.containsAll(c);
    }

    @Override
    public boolean addAll(final Collection<? extends C> c) {
        return listeners.addAll(c);
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        return listeners.removeAll(c);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        return listeners.retainAll(c);
    }

    @Override
    public void clear() {
        listeners.clear();
    }
}
