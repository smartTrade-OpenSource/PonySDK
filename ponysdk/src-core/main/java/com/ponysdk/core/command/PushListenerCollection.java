
package com.ponysdk.core.command;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ponysdk.core.event.HandlerRegistration;

public class PushListenerCollection<T> implements Iterable<PusherListener<T>> {

    private final Collection<PusherListener<T>> listeners = new CopyOnWriteArrayList<PusherListener<T>>();

    public HandlerRegistration register(final PusherListener<T> listener) {
        listeners.add(listener);
        return new HandlerRegistration() {

            @Override
            public void removeHandler() {
                listeners.remove(listener);
            }
        };
    }

    @Override
    public Iterator<PusherListener<T>> iterator() {
        return listeners.iterator();
    }

}
