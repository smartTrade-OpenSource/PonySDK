
package com.ponysdk.core.ui.datagrid2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.Row;

/**
 *
 */
public class SimpleCacheDataGridSource<K, V> extends SimpleDataSource<K, V> {

    //    public final List<Row<V>> liveData = new ArrayList<>(); // FIXME : liveData is to be private
    //    private int rowCounter = 0;

    //    @Override
    //    public List<V> getNeededRowsForScroll(final int index, int size) {
    //        // Reset size so that it doesn't exceed boundaries
    //        size = index + size > liveData.size() ? liveData.size() - index : size;
    //        final List<V> tmp = new ArrayList<>();
    //        for (int i = index; i < index + size; i++) {
    //            tmp.add(liveData.get(i).getData());
    //        }
    //        return tmp;
    //    }
    @Override
    public List<Row<V>> getNeededRowsForScroll(final int index, int size) {
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

    @Override
    public V getRowData(final int rowIndex) {
        final V v = rowIndex < liveData.size() ? liveData.get(rowIndex).data : null;
        if (v == null) {
            System.out.println("this is null !! ");
            //            v = getNeededRowsForScroll(row, 1).get(0);
            //            setData(v);
            return getNeededRowsForScroll(rowIndex, 1).get(0).getData();
        }
        return v;
    }

    @Override
    public boolean isSelected(final K k) {
        return selectedKeys.contains(k);
    }

    @Override
    public void select(final K k) {
        final Row<V> row = cache.get(k);
        if (row == null || !selectedKeys.add(k) || !row.accepted) return;
        insertRow(liveSelectedData, row);
    }

    @Override
    public void unselect(final K k) {
        final Row<V> row = cache.get(k);
        if (row == null || !selectedKeys.remove(k) || !row.accepted) return;
        removeRow(liveSelectedData, row);
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

    //    //----------------------------------------------------------------------------------------------------------//
    //    //----------------------------------------- Gestion de liveData --------------------------------------------//
    //    //----------------------------------------------------------------------------------------------------------//
    //
    //    // Insert data in the cache or update it if it already exists
    //    @Override
    //    public Interval setData(final V v) {
    //        Objects.requireNonNull(v);
    //        final K k = adapter.getKey(v);
    //        final Row<V> row = cache.get(k);
    //        Interval interval;
    //        if (row != null) {
    //            if (row.data == v) return null;
    //            interval = updateData(k, row, v);
    //        } else {
    //            interval = insertData(k, v);
    //        }
    //        return interval;
    //    }
    //
    //    // Rows are updated when they already exist
    //    private Interval updateData(final K k, final Row<V> row, final V newV) {
    //        if (row.accepted) {
    //            final int oldLiveDataSize = liveData.size();
    //            final int oldRowIndex = removeRow(liveData, row);
    //            final boolean selected = selectedKeys.contains(k);
    //            if (selected) removeRow(liveSelectedData, row);
    //            row.data = newV;
    //            return onWasAcceptedAndRemoved(selected, row, oldLiveDataSize, oldRowIndex);
    //        } else {
    //            row.data = newV;
    //            return onWasNotAccepted(k, row);
    //        }
    //    }
    //
    //    @Override
    //    public Interval updateData(final K k, final Consumer<V> updater) {
    //        final Row<V> row = cache.get(k);
    //        if (row == null) return null;
    //        if (row.accepted) {
    //            final int oldLiveDataSize = liveData.size();
    //            final int oldRowIndex = removeRow(liveData, row);
    //            final boolean selected = selectedKeys.contains(k);
    //            if (selected) removeRow(liveSelectedData, row);
    //            updater.accept(row.data);
    //            return onWasAcceptedAndRemoved(selected, row, oldLiveDataSize, oldRowIndex);
    //        } else {
    //            updater.accept(row.data);
    //            return onWasNotAccepted(k, row);
    //        }
    //    }
    //
    //    @Override
    //    public V removeData(final K k) {
    //
    //        final Row<V> row = cache.remove(k);
    //        final boolean selected = selectedKeys.remove(k);
    //        if (row.accepted) {
    //            final int oldLiveDataSize = liveData.size();
    //            final int rowIndex = removeRow(liveData, row);
    //            if (selected) {
    //                removeRow(liveSelectedData, row);
    //            }
    //            //            refreshRows(rowIndex, oldLiveDataSize);
    //        }
    //        return row.data;
    //    }
    //
    //    // Here rows are created and inserted in liveData
    //    private Interval insertData(final K k, final V data) {
    //        final Row<V> row = new Row<>(rowCounter++, data);
    //        row.accepted = accept(row);
    //        putRow(k, row); //FIXME
    //        if (!row.accepted) return null;
    //        final int rowIndex = insertRow(liveData, row);
    //        return new Interval(rowIndex, liveData.size());
    //    }
    //
    //    private Interval onWasAcceptedAndRemoved(final boolean selected, final Row<V> row, final int oldLiveDataSize,
    //                                             final int oldRowIndex) {
    //        //        clearRenderingHelpers(row);
    //        if (accept(row)) {
    //            final int rowIndex = insertRow(liveData, row);
    //            if (selected) insertRow(liveSelectedData, row);
    //            if (oldRowIndex <= rowIndex) {
    //                return new Interval(oldRowIndex, rowIndex + 1);
    //            } else {
    //                return new Interval(rowIndex, oldRowIndex + 1);
    //            }
    //        } else {
    //            row.accepted = false;
    //            return new Interval(oldRowIndex, oldLiveDataSize);
    //        }
    //    }
    //
    //    private Interval onWasNotAccepted(final K k, final Row<V> row) {
    //        //        clearRenderingHelpers(row);
    //        if (accept(row)) {
    //            row.accepted = true;
    //            final int rowIndex = insertRow(liveData, row);
    //            if (selectedKeys.contains(k)) insertRow(liveSelectedData, row);
    //            return new Interval(rowIndex, liveData.size());
    //        } //else do nothing
    //        return null;
    //    }
    //
    //    private boolean accept(final Row<V> row) {
    //        for (final AbstractFilter<V> filter : getFilters()) {
    //            if (!filter.test(row)) return false;
    //        }
    //        return true;
    //    }
    //
    //    @Override
    //    public void resetLiveData() {
    //        liveSelectedData.clear();
    //        liveData.clear();
    //        for (final Row<V> row : cache.values()) {
    //            row.accepted = accept(row);
    //            if (row.accepted) {
    //                insertRow(liveData, row);
    //                if (selectedKeys.contains(adapter.getKey(row.data))) {
    //                    insertRow(liveSelectedData, row);
    //                }
    //            }
    //        }
    //    }
    //
    //    //    public void refreshRows(final int from, final int to) {
    //    //        this.from = Math.min(this.from, from);
    //    //        this.to = Math.max(this.to, to);
    //    //        if (SimpleDataGridController.bound) doRefreshRows(); //FIXME
    //    //    }
    //    //
    //    //    public void doRefreshRows() {
    //    //        try {
    //    //            if (listener != null) {
    //    //                listener.onUpdateRows(from, to);
    //    //            }
    //    //        } finally {
    //    //            from = Integer.MAX_VALUE;
    //    //            to = 0;
    //    //        }
    //    //    }
    //
    //    //----------------------------------------------------------------------------------------------------------//
    //    //------------------------------- BinarySearch insertion, removal, findIndex -------------------------------//
    //    //----------------------------------------------------------------------------------------------------------//
    //    private int insertRow(final List<Row<V>> rows, final Row<V> row) {
    //        if (rows.size() == 0) {
    //            rows.add(row);
    //            return 0;
    //        }
    //        if (compare(row, rows.get(0)) < 0) { //common case
    //            rows.add(0, row);
    //            return 0;
    //        }
    //        int left = 1;
    //        int right = rows.size() - 1;
    //        int index = left;
    //        int diff = 1;
    //        while (left <= right) {
    //            index = left + right >> 1;
    //            final Row<V> middleRow = rows.get(index);
    //            diff = compare(middleRow, row);
    //            if (diff < 0) left = index + 1;
    //            else if (diff > 0) right = index - 1;
    //            else throw new IllegalArgumentException(
    //                "Cannot insert an already existing row : existing=" + middleRow.data + ", new=" + row.data);
    //        }
    //        if (diff < 0) index++;
    //        rows.add(index, row);
    //        return index;
    //    }
    //
    //    private int removeRow(final List<Row<V>> rows, final Row<V> row) {
    //        final int rowIndex = findRowIndex(rows, row);
    //        if (rowIndex < 0) return rowIndex;
    //        rows.remove(rowIndex);
    //        return rowIndex;
    //    }
    //
    //    private int findRowIndex(final List<Row<V>> rows, final Row<V> row) {
    //        int left = 0;
    //        int right = rows.size() - 1;
    //        while (left <= right) {
    //            final int middle = left + right >> 1;
    //            final Row<V> r = rows.get(middle);
    //            final int diff = compare(r, row);
    //            if (diff < 0) left = middle + 1;
    //            else if (diff > 0) right = middle - 1;
    //            else return middle;
    //        }
    //        return -1;
    //    }
    //

    //----------------------------------------------------------------------------------------------------------//
    //------------------------------------------------ Sorting -------------------------------------------------//
    //----------------------------------------------------------------------------------------------------------//

    //    private final int compare(final Row<V> r1, final Row<V> r2) {
    //
    //        for (final Comparator<Row<V>> sort : getSorts()) {
    //            final int diff = sort.compare(r1, r2);
    //            if (diff != 0) return diff;
    //        }
    //        final int diff = adapter.compareDefault(r1.data, r2.data);
    //        if (diff != 0) return diff;
    //
    //        return adapter.isAscendingSortByInsertionOrder() ? r1.id - r2.id : r2.id - r1.id;
    //    }

    @Override
    public void sort() {
        liveSelectedData.sort(this::compare);
        liveData.sort(this::compare);
        //        refreshRows(0, liveData.size());
    }

    @Override
    public List<V> getNewSortData() {
        return null;
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
    public List<V> onSelectAllLiveData() {
        final List<V> tmp = new ArrayList<>();
        for (final Row<V> element : liveData) {
            tmp.add(element.data);
        }
        return tmp;
    }

    //----------------------------------------------------------------------------------------------------------//
    //----------------------------------------------- Filtering ------------------------------------------------//
    //----------------------------------------------------------------------------------------------------------//

    @Override
    public void setFilter(final Object key, final boolean reinforcing, final AbstractFilter<V> filter) {
        final AbstractFilter<V> oldFilter = putFilter(key, filter);
        if (oldFilter == null || reinforcing) {
            //            final int oldLiveDataSize = liveData.size();
            final int from = reinforceFilter(liveData, filter);
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
