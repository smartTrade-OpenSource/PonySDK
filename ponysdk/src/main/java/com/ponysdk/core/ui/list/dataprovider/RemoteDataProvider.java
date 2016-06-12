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

package com.ponysdk.core.ui.list.dataprovider;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.core.server.service.query.Query;
import com.ponysdk.core.server.service.query.Query.QueryMode;
import com.ponysdk.core.server.service.query.SortingType;
import com.ponysdk.core.ui.list.FilterListener;
import com.ponysdk.core.ui.list.HasCriteria;
import com.ponysdk.core.ui.list.HasPData;
import com.ponysdk.core.ui.list.Queriable;
import com.ponysdk.core.ui.list.Resetable;
import com.ponysdk.core.ui.list.Sortable;
import com.ponysdk.core.ui.list.Validable;
import com.ponysdk.core.ui.list.paging.Pager;
import com.ponysdk.core.ui.list.paging.PagerListener;

public abstract class RemoteDataProvider<T> implements PagerListener, FilterListener {

    protected final Pager<T> pager;
    protected final HasPData<T> hasData;

    protected final List<Sortable> sortableList = new ArrayList<>();
    protected final List<Resetable> resatableList = new ArrayList<>();
    protected final List<HasCriteria> hasCriteriaList = new ArrayList<>();
    protected final List<Validable> validableList = new ArrayList<>();

    public RemoteDataProvider(final Pager<T> pager, final HasPData<T> hasData) {
        this.pager = pager;
        this.hasData = hasData;
        this.pager.addListener(this);
    }

    public void registerHasCriteria(final Queriable queriable) {
        if (queriable.asSortable() != null) sortableList.add(queriable.asSortable());
        if (queriable.asResetable() != null) resatableList.add(queriable.asResetable());
        if (queriable.asHasCriteria() != null) hasCriteriaList.add(queriable.asHasCriteria());
        if (queriable.asValidable() != null) validableList.add(queriable.asValidable());
    }

    public List<T> getData() {
        final Query query = buildQuery();
        query.setPageNum(pager.getCurrentPage());
        query.setPageSize(pager.getPageSize());
        return getData(query);
    }

    public List<T> getFullData() {
        final Query query = buildQuery();
        query.setQueryMode(QueryMode.FULL_RESULT);
        return getFullData(query);
    }

    @Override
    public void onPageChange(final int page) {
        pager.setCurrentPage(page);
        hasData.setData(getData());
    }

    @Override
    public void onFilterChange() {

        if (!isValid()) return;

        pager.setCurrentPage(0);
        hasData.setData(getData());
    }

    @Override
    public void onSort(final Sortable aSortable) {
        sortableList.stream().filter(sortable -> !sortable.equals(aSortable)).forEach(sortable -> {
            sortable.sort(SortingType.NONE);
        });
        hasData.setData(getData());
    }

    public boolean isValid() {
        boolean valid = true;
        for (final Validable validable : validableList) {
            valid = valid & validable.isValid().isValid();
        }
        return valid;
    }

    public void reset() {
        resatableList.forEach(resetable -> resetable.reset());
    }

    public Query buildQuery() {
        final Query query = new Query();
        for (final HasCriteria criteriable : hasCriteriaList) {
            query.addCriteria(criteriable.getCriteria());
        }
        return query;
    }

    protected abstract List<T> getData(Query query);

    protected abstract List<T> getFullData(Query query);

}
