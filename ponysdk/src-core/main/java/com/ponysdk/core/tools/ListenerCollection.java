
package com.ponysdk.core.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ListenerCollection<T> implements Collection<T> {

    private final Set<T> listeners = Collections.newSetFromMap(new ConcurrentHashMap<T, Boolean>());

    public void register(final T listener) {
        listeners.add(listener);
    }

    public boolean unregister(final T listener) {
        return listeners.remove(listener);
    }

    @Override
    public Iterator<T> iterator() {
        return listeners.iterator();
    }

    @Override
    public int size() {
        return listeners.size();
    }

    @Override
    public boolean isEmpty() {
        return listeners.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return listeners.contains(o);
    }

    @Override
    public Object[] toArray() {
        return listeners.toArray();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        return listeners.toArray(a);
    }

    @Override
    public boolean add(final T e) {
        return listeners.add(e);
    }

    @Override
    public boolean remove(final Object o) {
        return listeners.remove(o);
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return listeners.containsAll(c);
    }

    @Override
    public boolean addAll(final Collection<? extends T> c) {
        return listeners.addAll(c);
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        return listeners.removeAll(c);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        return listeners.retainAll(c);
    }

    @Override
    public void clear() {
        listeners.clear();
    }
}
