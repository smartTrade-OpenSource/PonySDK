
package com.ponysdk.core;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ponysdk.ui.server.basic.PObject;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.terminal.instruction.GC;

public class PWeakHashMap implements Map<Long, PObject> {

    private final ReferenceQueue<PObject> queue = new ReferenceQueue<PObject>();

    private final Map<Long, WeakReference<PObject>> referenceByObjectID = new ConcurrentHashMap<Long, WeakReference<PObject>>();

    private final Map<WeakReference<PObject>, Long> objectIDByReferences = new ConcurrentHashMap<WeakReference<PObject>, Long>();

    private final Map<WeakReference<PObject>, Long> parentObjectIDByReferences = new ConcurrentHashMap<WeakReference<PObject>, Long>();

    @Override
    public int size() {
        expungeStaleEntries();
        return referenceByObjectID.size();
    }

    @Override
    public boolean isEmpty() {
        expungeStaleEntries();
        return referenceByObjectID.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        expungeStaleEntries();
        return referenceByObjectID.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        expungeStaleEntries();
        return referenceByObjectID.containsValue(new WeakReference<Object>(value));
    }

    @Override
    public PObject get(Object key) {
        expungeStaleEntries();
        final WeakReference<PObject> value = referenceByObjectID.get(key);
        if (value == null) return null;
        return value.get();
    }

    @Override
    public PObject put(Long objectID, PObject value) {
        expungeStaleEntries();
        final WeakReference<PObject> weakReference = new WeakReference<PObject>(value, queue);
        referenceByObjectID.put(objectID, weakReference);
        objectIDByReferences.put(weakReference, objectID);

        if (value instanceof PWidget) {
            final PWidget widget = (PWidget) value;
            if (widget.getParent() != null) {
                parentObjectIDByReferences.put(weakReference, widget.getParent().getID());
            }
        }

        return value;
    }

    @Override
    public PObject remove(Object key) {
        expungeStaleEntries();
        final WeakReference<PObject> reference = referenceByObjectID.remove(key);
        if (reference == null) return null;

        objectIDByReferences.remove(reference);
        parentObjectIDByReferences.remove(reference);

        return reference.get();
    }

    @Override
    public void putAll(Map<? extends Long, ? extends PObject> m) {
        expungeStaleEntries();
    }

    @Override
    public void clear() {
        expungeStaleEntries();
    }

    @Override
    public Set<Long> keySet() {
        expungeStaleEntries();
        return null;
    }

    @Override
    public Collection<PObject> values() {
        expungeStaleEntries();
        return null;
    }

    @Override
    public Set<java.util.Map.Entry<Long, PObject>> entrySet() {
        expungeStaleEntries();
        return null;
    }

    public void assignParentID(Long objectID, Long parentObjectID) {
        parentObjectIDByReferences.put(referenceByObjectID.get(objectID), parentObjectID);
    }

    private void expungeStaleEntries() {
        Reference<? extends PObject> reference = null;

        while ((reference = queue.poll()) != null) {
            final Long objectID = objectIDByReferences.remove(reference);
            final Long parentObjectID = parentObjectIDByReferences.remove(reference);
            referenceByObjectID.remove(objectID);

            if (parentObjectID != null) {
                PonySession.getCurrent().stackInstruction(new GC(objectID, parentObjectID));
            }
        }
    }

}