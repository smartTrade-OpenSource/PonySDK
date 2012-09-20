
package com.ponysdk.core.command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ponysdk.core.event.HandlerRegistration;

public class PushListenerMap<K, V> implements Iterable<PusherListener<V>> {

    private final ConcurrentHashMap<K, List<PusherListener<V>>> listeners = new ConcurrentHashMap<K, List<PusherListener<V>>>();

    public HandlerRegistration register(final K key, final PusherListener<V> listener) {

        List<PusherListener<V>> nlist = new CopyOnWriteArrayList<PusherListener<V>>();
        final List<PusherListener<V>> l = listeners.putIfAbsent(key, nlist);
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
    public Iterator<PusherListener<V>> iterator() {
        final List<PusherListener<V>> snapshot = new ArrayList<PusherListener<V>>();
        for (final List<PusherListener<V>> l : listeners.values()) {
            snapshot.addAll(l);
        }
        return snapshot.iterator();
    }

    public List<PusherListener<V>> get(final K key) {
        return listeners.get(key);
    }

}
