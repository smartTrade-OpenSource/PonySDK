
package com.ponysdk.core.command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ponysdk.core.event.HandlerRegistration;

public class PushListenerMap<K, V> implements Iterable<PushListener<V>> {

    private final ConcurrentHashMap<K, List<PushListener<V>>> listeners = new ConcurrentHashMap<K, List<PushListener<V>>>();

    public HandlerRegistration register(final K key, final PushListener<V> listener) {

        List<PushListener<V>> nlist = new CopyOnWriteArrayList<PushListener<V>>();
        final List<PushListener<V>> l = listeners.putIfAbsent(key, nlist);
        if (l != null) {
            nlist = l;
        }
        nlist.add(listener);

        return new HandlerRegistration() {

            @Override
            public void removeHandler() {
                listeners.get(key).remove(listener);
            }
        };
    }

    @Override
    public Iterator<PushListener<V>> iterator() {
        final List<PushListener<V>> snapshot = new ArrayList<PushListener<V>>();
        for (final List<PushListener<V>> l : listeners.values()) {
            snapshot.addAll(l);
        }
        return snapshot.iterator();
    }

    public List<PushListener<V>> get(final K key) {
        return listeners.get(key);
    }

}
