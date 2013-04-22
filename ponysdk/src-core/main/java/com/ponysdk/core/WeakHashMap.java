
package com.ponysdk.core;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.instruction.GC;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.server.basic.PObject;
import com.ponysdk.ui.server.basic.PWidget;

public class WeakHashMap implements Map<Long, PObject> {

    private final Logger log = LoggerFactory.getLogger(WeakHashMap.class);

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
    public boolean containsKey(final Object key) {
        expungeStaleEntries();
        return referenceByObjectID.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        expungeStaleEntries();
        return referenceByObjectID.containsValue(new WeakReference<Object>(value));
    }

    @Override
    public PObject get(final Object key) {
        expungeStaleEntries();
        final WeakReference<PObject> value = referenceByObjectID.get(key);
        if (value == null) return null;
        return value.get();
    }

    @Override
    public PObject put(final Long objectID, final PObject value) {
        expungeStaleEntries();
        final WeakReference<PObject> weakReference = new WeakReference<PObject>(value, queue);
        referenceByObjectID.put(objectID, weakReference);
        objectIDByReferences.put(weakReference, objectID);

        if (log.isDebugEnabled()) log.debug("Registering object: " + value);

        if (value instanceof PWidget) {
            final PWidget widget = (PWidget) value;
            if (widget.getParent() != null) {
                if (log.isDebugEnabled()) log.debug("Attaching object #" + objectID + " to parent: " + widget);
                parentObjectIDByReferences.put(weakReference, widget.getParent().getID());
            }
        }

        return value;
    }

    @Override
    public PObject remove(final Object key) {
        expungeStaleEntries();
        final WeakReference<PObject> reference = referenceByObjectID.remove(key);

        if (log.isDebugEnabled()) log.debug("Removing reference on object #" + key);
        if (reference == null) return null;

        objectIDByReferences.remove(reference);
        parentObjectIDByReferences.remove(reference);

        return reference.get();
    }

    @Override
    public void putAll(final Map<? extends Long, ? extends PObject> m) {
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

    public void assignParentID(final Long objectID, final Long parentObjectID) {
        if (referenceByObjectID.get(objectID) == null) {
            log.warn("Unkwnown reference to object: " + objectID);
            return;
        }
        parentObjectIDByReferences.put(referenceByObjectID.get(objectID), parentObjectID);
    }

    private void expungeStaleEntries() {
        Reference<? extends PObject> reference = null;

        while ((reference = queue.poll()) != null) {
            final Long objectID = objectIDByReferences.remove(reference);
            final Long parentObjectID = parentObjectIDByReferences.remove(reference);
            referenceByObjectID.remove(objectID);
            if (log.isDebugEnabled()) log.debug("Removing reference on object #" + objectID);

            if (parentObjectID != null) {
                Txn.get().getTxnContext().save(new GC(objectID, parentObjectID));
            }
        }
    }

}