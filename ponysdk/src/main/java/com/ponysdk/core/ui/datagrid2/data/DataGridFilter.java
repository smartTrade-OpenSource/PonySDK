/*
 * Copyright (c) 2023 PonySDK
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

package com.ponysdk.core.ui.datagrid2.data;

import java.util.Objects;
import java.util.function.Predicate;

public class DataGridFilter<V> {

    private Object key;
    private String id;
    private Predicate<V> predicate;
    private boolean active;
    private boolean reinforcing;

    /**
     * @return an object that can be used to uniquely identify a
     *         filter, so that it can be replaced or removed
     */
    public Object getKey() {
        return key;
    }

    /**
     * @param key an object that can be used to uniquely identify a
     *            filter, so that it can be replaced or removed
     */
    public void setKey(final Object key) {
        this.key = key;
    }

    /**
     * @return the filter string identifier
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the filter string identifier
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * @return a predicate that decides whether a value is accepted
     *         or filtered
     */
    public Predicate<V> getPredicate() {
        return predicate;
    }

    /**
     * @param predicate a predicate that decides whether a value is accepted
     *            or filtered
     */
    public void setPredicate(final Predicate<V> predicate) {
        this.predicate = predicate;
    }

    /**
     * @return specifies if filter is active (has values set)
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @param active specifies if filter is active (has values set)
     */
    public void setActive(final boolean active) {
        this.active = active;
    }

    /**
     * @return {@code true} if the predicate is at least as
     *         intolerant as the replaced predicate of the same key
     *         (i.e. the predicate doesn't accept any value that was
     *         not accepted by the replaced predicate), {@code false}
     *         otherwise. This is an optimization that allows us to
     *         avoid applying the predicate on values that we already
     *         know will not be accepted. If this filter is not
     *         replacing an existing one, the value of the
     *         {@code reinforcing} argument has no impact.
     */
    public boolean isReinforcing() {
        return reinforcing;
    }

    /**
     * @param reinforcing {@code true} if the predicate is at least as
     *            intolerant as the replaced predicate of the same key
     *            (i.e. the predicate doesn't accept any value that was
     *            not accepted by the replaced predicate), {@code false}
     *            otherwise. This is an optimization that allows us to
     *            avoid applying the predicate on values that we already
     *            know will not be accepted. If this filter is not
     *            replacing an existing one, the value of the
     *            {@code reinforcing} argument has no impact.
     */
    public void setReinforcing(final boolean reinforcing) {
        this.reinforcing = reinforcing;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final DataGridFilter other = (DataGridFilter) obj;
        return Objects.equals(id, other.id);
    }
}
