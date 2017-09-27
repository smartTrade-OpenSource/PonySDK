
package com.ponysdk.core.ui.datagrid;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;

public class DataGridTreeSet<E> extends TreeSet<E> {

    private final Function<E, ?> keyProvider;

    private final Map<Object, E> map = new HashMap<>();

    class Structure {

        E data;
        int index;
    }

    public DataGridTreeSet(final Comparator<E> comparator, final Function<E, ?> keyProvider) {
        super((o1, o2) -> {
            final int compare = comparator.compare(o1, o2);
            if (compare != 0) return compare;
            return Integer.compare(keyProvider.apply(o1).hashCode(), keyProvider.apply(o2).hashCode());
        });
        this.keyProvider = keyProvider;
    }

    public int getPosition(final E e) {
        return headSet(e).size();
    }

    @Override
    public boolean add(final E e) {
        map.put(keyProvider.apply(e), e);
        return super.add(e);
    }

    @Override
    public boolean remove(final Object e) {
        map.remove(keyProvider.apply((E) e));
        return super.remove(e);
    }

    public boolean containsData(final E e) {
        return map.containsKey(keyProvider.apply(e));
    }

}
