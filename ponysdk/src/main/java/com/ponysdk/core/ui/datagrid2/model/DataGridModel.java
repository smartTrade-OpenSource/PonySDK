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

package com.ponysdk.core.ui.datagrid2;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The data model for the {@link DataGridView}. It can be used to update/read the data available for the view.
 * The keys used in the model must be immutable.
 * The values used in the model can be mutable BUT should be only mutated via the
 * {@link DataGridModel#updateData(Object, Consumer)}, {@link DataGridModel#updateData(Map)} methods, and must
 * always be identifiable by the same key.
 *
 * @author mbagdouri
 */
public interface DataGridModel<K, V> {

    /**
     * Insert or replace the value
     */
    void setData(V v);

    /**
     * Insert or replace the collection of values
     *
     * @param c
     */
    void setData(Collection<V> c);

    /**
     * Update an existing value identified by key {@code k}, if it is present, using the {@code updater}
     */
    void updateData(K k, Consumer<V> updater);

    /**
     * Update existing values identified by the key, if they are present, using the {@code updaters}
     */
    void updateData(Map<K, Consumer<V>> updaters);

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this model contains no mapping for the key.
     */
    V getData(K k);
    //    Row<V> getData(K k);

    /**
     * Removes the value identified by the key {@code k} from this model if it is present
     */
    V removeData(K k);

    /**
     * Performs the given action for each entry in this map until all entries
     * have been processed or the action throws an exception. There is no guarantee on the order of elements
     */
    //    void forEach(BiConsumer<K, V> action);

    /**
     * @return the number of key-value mappings in this model
     */
    //    int size();

    /**
     * If {@code bound} is true (default), the view is notified when the model is updated. Otherwise, the view is not
     * notified but it will continue to see the most recent version of the model
     *
     * @param bound
     */
    void setBound(boolean bound);

    /**
     * Whether the model is bound to the view.
     *
     * @see {@link DataGridModel#setBound(boolean)}
     */
    boolean getBound();
}
