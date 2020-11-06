/*
 * Copyright (c) 2020 PonySDK
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

package com.ponysdk.core.ui.datagrid2.view;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author mabbas
 */

public class DataGridSnapshot {

    public int firstRowIndex;
    public int size;
    public int start;
    public Map<Integer, Integer> sorts;
    public Set<Integer> filters;

    public DataGridSnapshot(final int firstRowIndex, final int size, final int start, final Map<Integer, Integer> sorts,
            final Set<Integer> filters) {
        this.firstRowIndex = firstRowIndex;
        this.size = size;
        this.start = start;
        this.sorts = sorts;
        this.filters = filters;
    }

    public DataGridSnapshot(final DataGridSnapshot snapshot) {
        firstRowIndex = snapshot.firstRowIndex;
        size = snapshot.size;
        sorts = new HashMap<>(snapshot.sorts);
        filters = new HashSet<>(snapshot.filters);
    }

    public boolean equals(final DataGridSnapshot snap) {
        if (firstRowIndex == snap.firstRowIndex && size == snap.size && sorts.equals(snap.sorts) && filters.equals(snap.filters))
            return true;
        return false;
    }
}
