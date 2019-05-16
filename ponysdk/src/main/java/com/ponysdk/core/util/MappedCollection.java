/*
 * Copyright (c) 2019 PonySDK
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

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

/**
 *
 */
public class MappedCollection<T, E> extends AbstractCollection<E> {

    private final Collection<T> c;
    private final Function<T, E> mappingFunction;

    public MappedCollection(final Collection<T> c, final Function<T, E> mappingFunction) {
        super();
        this.c = c;
        this.mappingFunction = mappingFunction;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {

            private final Iterator<T> iterator = c.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public E next() {
                return mappingFunction.apply(iterator.next());
            }
        };
    }

    @Override
    public int size() {
        return c.size();
    }

}
