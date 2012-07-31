
package com.ponysdk.core.command;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ponysdk.core.event.HandlerRegistration;

public class PushListenerCollection<T> implements Iterable<PushListener<T>> {

    private final Collection<PushListener<T>> listeners = new CopyOnWriteArrayList<PushListener<T>>();

    public HandlerRegistration register(final PushListener<T> listener) {
        listeners.add(listener);
        return new HandlerRegistration() {

            @Override
            public void removeHandler() {
                listeners.remove(listener);
            }
        };
    }

    @Override
    public Iterator<PushListener<T>> iterator() {
        return listeners.iterator();
    }

}
