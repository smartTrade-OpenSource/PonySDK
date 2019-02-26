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

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * A thread-safe cache that weakly references the objects that it contains
 *
 * @see WeakReference
 *
 * @param <E>
 *            MUST be immutable
 */
public class SynchronizedWeakCache<E> {

    private final WeakHashMap<E, WeakReference<E>> map;

    public SynchronizedWeakCache() {
        map = new WeakHashMap<>();
    }

    public SynchronizedWeakCache(final int initialCapcity) {
        map = new WeakHashMap<>(initialCapcity);
    }

    /**
     * If there is an instance equivalent to {@code key} present in the cache, return it. Otherwise, use the
     * {@code mappingFuction} to create a new instance based on the {@code key}, put it in the cache and return it.<br/>
     * <i><b>NB :</b> If the {@code mappingFunction} is applied, its result is not guaranteed to be inserted in the
     * cache.</i>
     *
     * @return an existing instance, or a newly created one if absent
     */
    public <T> E getOrCompute(final T key, final Function<T, E> mappingFunction) {
        WeakReference<E> ref = null;
        synchronized (map) {
            ref = map.get(key);
        }
        if (ref != null) {
            final E e = ref.get();
            if (e != null) return e;
        }
        final E newE = mappingFunction.apply(key);
        final WeakReference<E> newRef = new WeakReference<>(newE);
        synchronized (map) {
            ref = map.putIfAbsent(newE, newRef);
            if (ref != null) {
                final E e = ref.get();
                if (e != null) return e;

                /*
                 * Remove the existing key, since if it was not dereferenced, the following put() will keep the
                 * existing key and update only the value. As a result we will not have the same instance for key and
                 * value, and as a consequence the entry will be removed (when the key is dereferenced)
                 * while the value may still be referenced somewhere else
                 */
                map.remove(newE);
                map.put(newE, newRef);
            }
        }
        return newE;
    }

}
