
package com.ponysdk.ui.server.list2.dataprovider;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.core.query.Query;
import com.ponysdk.core.query.SortingType;
import com.ponysdk.ui.server.list2.HasCriteria;
import com.ponysdk.ui.server.list2.HasPData;
import com.ponysdk.ui.server.list2.Queriable;
import com.ponysdk.ui.server.list2.Resetable;
import com.ponysdk.ui.server.list2.Sortable;
import com.ponysdk.ui.server.list2.paging.Pager;
import com.ponysdk.ui.server.list2.paging.PagerListener;

public abstract class RemoteDataProvider<T> implements PagerListener, FilterListener {

    private final Pager<T> pager;
    private final HasPData<T> hasData;

    private final List<Sortable> sortableList = new ArrayList<Sortable>();
    private final List<Resetable> resatableList = new ArrayList<Resetable>();
    private final List<HasCriteria> hasCriteriaList = new ArrayList<HasCriteria>();

    public RemoteDataProvider(final Pager<T> pager, final HasPData<T> hasData) {
        this.pager = pager;
        this.hasData = hasData;
        this.pager.addListener(this);
    }

    public void registerHasCriteria(final Queriable queriable) {
        if (queriable.asSortable() != null) sortableList.add(queriable.asSortable());
        if (queriable.asResetable() != null) resatableList.add(queriable.asResetable());
        if (queriable.asHasCriteria() != null) hasCriteriaList.add(queriable.asHasCriteria());
    }

    public List<T> getData() {
        final Query query = buildQuery();
        query.setPageNum(pager.getCurrentPage());
        query.setPageSize(pager.getPageSize());
        return getData(query);
    }

    @Override
    public void onPageChange(final int page) {
        pager.setCurrentPage(page);
        hasData.setData(getData());
    }

    @Override
    public void onFilterChange() {
        hasData.setData(getData());
    }

    @Override
    public void onSort(final Sortable aSortable) {
        for (final Sortable sortable : sortableList) {
            if (!sortable.equals(aSortable)) {
                sortable.sort(SortingType.NONE);
            }
        }
        hasData.setData(getData());
    }

    private Query buildQuery() {
        final Query query = new Query();
        for (final HasCriteria criteriable : hasCriteriaList) {
            query.addCriteria(criteriable.getCriteria());
        }
        return query;
    }

    protected abstract List<T> getData(Query query);

}
