
package com.ponysdk.ui.server.dataprovider;

import java.util.List;

import com.ponysdk.core.query.Query;
import com.ponysdk.ui.server.list2.HasPData;

public abstract class RemoteDataProvider<T> implements PagerListener {

    private final Pager<T> pager;
    private final QueryBuilder<T> queryBuilder;
    private final HasPData<T> hasData;

    public RemoteDataProvider(final QueryBuilder<T> queryBuilder, final Pager<T> pager, final HasPData<T> hasData) {
        this.queryBuilder = queryBuilder;
        this.pager = pager;
        this.hasData = hasData;
        this.pager.addListener(this);
    }

    public List<T> getData() {
        final Query query = queryBuilder.build();
        query.setPageNum(pager.getCurrentPage());
        query.setPageSize(pager.getPageSize());
        return getData(query);
    }

    @Override
    public void onPageChange(final int page) {
        hasData.setData(getData());
    }

    protected abstract List<T> getData(Query query);

}
