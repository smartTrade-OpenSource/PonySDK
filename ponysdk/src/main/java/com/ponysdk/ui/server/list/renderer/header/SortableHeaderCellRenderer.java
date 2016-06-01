/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.ui.server.list.renderer.header;

import java.util.Arrays;
import java.util.List;

import com.ponysdk.core.query.Criterion;
import com.ponysdk.core.query.SortingType;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.list.FilterListener;
import com.ponysdk.ui.server.list.HasCriteria;
import com.ponysdk.ui.server.list.Queriable;
import com.ponysdk.ui.server.list.Resetable;
import com.ponysdk.ui.server.list.Sortable;
import com.ponysdk.ui.server.list.Validable;

public class SortableHeaderCellRenderer implements Queriable, HeaderCellRenderer, HasCriteria, Sortable {

    private final PLabel title;
    private final String key;

    private SortingType sortingType = SortingType.NONE;

    private FilterListener filterListener;

    public SortableHeaderCellRenderer(final String caption, final String key) {
        this(caption, key, null);
    }

    public SortableHeaderCellRenderer(final String caption, final String key, final FilterListener filterListener) {
        this.title = new PLabel(caption);
        this.key = key;
        this.filterListener = filterListener;

        builGUI();
    }

    private void builGUI() {
        title.addStyleName("sortable");
        title.addClickHandler((event) -> {
            sort(HeaderSortingHelper.getNextSortingType(sortingType));
        });
    }

    public void setFilterListener(final FilterListener filterListener) {
        this.filterListener = filterListener;
    }

    @Override
    public IsPWidget render() {
        return title;
    }

    @Override
    public List<Criterion> getCriteria() {
        final Criterion criterion = new Criterion(key);
        criterion.setSortingType(sortingType);

        return Arrays.asList(criterion);
    }

    @Override
    public void sort(final SortingType newSortingType) {
        title.removeStyleName(HeaderSortingHelper.getAssociatedStyleName(sortingType));
        sortingType = newSortingType;
        title.addStyleName(HeaderSortingHelper.getAssociatedStyleName(newSortingType));
        if (SortingType.NONE != newSortingType) {
            filterListener.onSort(this);
        }
    }

    public SortingType getSortingType() {
        return sortingType;
    }

    @Override
    public Sortable asSortable() {
        return this;
    }

    @Override
    public HasCriteria asHasCriteria() {
        return this;
    }

    @Override
    public Resetable asResetable() {
        return null;
    }

    @Override
    public Validable asValidable() {
        return null;
    }

}
