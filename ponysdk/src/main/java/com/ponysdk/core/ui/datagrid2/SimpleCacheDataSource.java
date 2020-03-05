
package com.ponysdk.core.ui.datagrid2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.Row;

/**
 *
 */
public class SimpleCacheDataSource<K, V> extends SimpleDataSource<K, V> {

    @Override
    public List<Row<V>> getRows(final int index, int size) {
        // Reset size so that it doesn't exceed boundaries
        size = index + size > liveData.size() ? liveData.size() - index : size;
        final List<Row<V>> tmp = new ArrayList<>();
        for (int i = index; i < index + size; i++) {
            tmp.add(liveData.get(i));
        }
        return tmp;
    }

    @Override
    public int getRowCount() {
        return liveData.size();
    }

    //----------------------------------------------------------------------------------------------------------//
    //------------------------------------------------ Sorting -------------------------------------------------//
    //----------------------------------------------------------------------------------------------------------//

    @Override
    public void sort() {
        liveSelectedData.sort(this::compare);
        liveData.sort(this::compare);
        //        refreshRows(0, liveData.size());
    }

    @Override
    public String toString() {
        return cache.toString();
    }

    @Override
    public void forEach(final BiConsumer<K, V> action) {
        cache.forEach((k, r) -> action.accept(k, r.data));
    }

    @Override
    public void selectAllLiveData() {
        // ToDo generalise this method for all dataSources
        liveSelectedData.clear();
        for (final Row<V> row : liveData) {
            liveSelectedData.add(row);
            selectedKeys.add(adapter.getKey(row.data));
        }
    }

    @Override
    public void unselectAllData() {
        liveSelectedData.clear();
        selectedKeys.clear();
    }

    //    @Override
    //    public List<V> onSelectAllLiveData() {
    //        final List<V> tmp = new ArrayList<>();
    //        for (final Row<V> element : liveData) {
    //            tmp.add(element.data);
    //        }
    //        return tmp;
    //    }

    //----------------------------------------------------------------------------------------------------------//
    //----------------------------------------------- Filtering ------------------------------------------------//
    //----------------------------------------------------------------------------------------------------------//

    @Override
    public void setFilter(final Object key, final boolean reinforcing, final AbstractFilter<V> filter) {
        final AbstractFilter<V> oldFilter = filters.put(key, filter);
        if (oldFilter == null || reinforcing) {
            //            final int oldLiveDataSize = liveData.size();
            //                        final int from = reinforceFilter(liveData, filter);
            reinforceFilter(liveData, filter);
            reinforceFilter(liveSelectedData, filter);
            //            if (from >= 0) {
            //                refreshRows(from, oldLiveDataSize);
            //            }
        } else {
            resetLiveData();
        }
    }

    private int reinforceFilter(final List<Row<V>> rows, final AbstractFilter<V> filter) {
        final Iterator<Row<V>> iterator = rows.iterator();
        int from = -1;
        for (int i = 0; iterator.hasNext(); i++) {
            final Row<V> row = iterator.next();
            if (!filter.test(row)) {
                row.accepted = false;
                iterator.remove();
                if (from < 0) from = i;
            }
        }
        return from;
    }
}
