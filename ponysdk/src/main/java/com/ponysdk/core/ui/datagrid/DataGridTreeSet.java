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
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;

public class DataGridTreeSet<E> extends TreeSet<E> {

    private final Function<E, ?> keyProvider;

    private final Map<Object, E> map = new HashMap<>();

    public DataGridTreeSet(final Comparator<E> comparator, final Function<E, ?> keyProvider) {
        super((o1, o2) -> {
            final int compare = comparator.compare(o1, o2);
            if (compare != 0) return compare;
            return Integer.compare(keyProvider.apply(o1).hashCode(), keyProvider.apply(o2).hashCode());
        });
        this.keyProvider = keyProvider;
    }

    public int getPosition(final E e) {
        return headSet(e).size();
    }

    @Override
    public boolean add(final E e) {
        map.put(keyProvider.apply(e), e);
        return super.add(e);
    }

    @Override
    public boolean remove(final Object e) {
        map.remove(keyProvider.apply((E) e));
        return super.remove(e);
    }

    public boolean containsData(final E e) {
        return map.containsKey(keyProvider.apply(e));
    }

}
