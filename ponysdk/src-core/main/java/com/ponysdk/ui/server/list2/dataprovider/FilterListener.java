
package com.ponysdk.ui.server.list2.dataprovider;

import com.ponysdk.ui.server.list2.Sortable;

public interface FilterListener {

    public void onFilterChange();

    public void onSort(Sortable sortingCriteria);

}
