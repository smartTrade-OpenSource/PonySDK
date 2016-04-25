/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

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

import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.server.basic.PObject;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.PWindow;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

public class WeakHashMap implements Map<Integer, PObject> {

    private final Logger log = LoggerFactory.getLogger(WeakHashMap.class);

    private final ReferenceQueue<PObject> queue = new ReferenceQueue<>();

    private final Map<Integer, WeakReference<PObject>> referenceByObjectID = new ConcurrentHashMap<>();

    private final Map<WeakReference<PObject>, Integer> objectIDByReferences = new ConcurrentHashMap<>();

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
        return referenceByObjectID.containsValue(new WeakReference<>(value));
    }

    @Override
    public PObject get(final Object key) {
        expungeStaleEntries();
        final WeakReference<PObject> value = referenceByObjectID.get(key);
        if (value == null)
            return null;
        return value.get();
    }

    @Override
    public PObject put(final Integer objectID, final PObject value) {
        expungeStaleEntries();
        final WeakReference<PObject> weakReference = new WeakReference<>(value, queue);
        referenceByObjectID.put(objectID, weakReference);
        objectIDByReferences.put(weakReference, objectID);

        if (log.isDebugEnabled())
            log.debug("Registering object: " + value);

        // if (value instanceof PWidget) {
        // final PWidget widget = (PWidget) value;
        // if (widget.getParent() != null) {
        // if (log.isDebugEnabled()) log.debug("Attaching object #" + objectID +
        // " to parent: " + widget);
        // parentObjectIDByReferences.put(weakReference,
        // widget.getParent().getID());
        // }
        // }

        return value;
    }

    @Override
    public PObject remove(final Object key) {
        expungeStaleEntries();
        final WeakReference<PObject> reference = referenceByObjectID.remove(key);

        if (log.isDebugEnabled())
            log.debug("Removing reference on object #" + key);
        if (reference == null)
            return null;

        objectIDByReferences.remove(reference);
        // parentObjectIDByReferences.remove(reference);

        return reference.get();
    }

    @Override
    public void putAll(final Map<? extends Integer, ? extends PObject> m) {
        expungeStaleEntries();
    }

    @Override
    public void clear() {
        expungeStaleEntries();
    }

    @Override
    public Set<Integer> keySet() {
        expungeStaleEntries();
        return null;
    }

    @Override
    public Collection<PObject> values() {
        expungeStaleEntries();
        return null;
    }

    @Override
    public Set<java.util.Map.Entry<Integer, PObject>> entrySet() {
        expungeStaleEntries();
        return null;
    }

    // public void assignParentID(final Integer objectID, final Integer
    // parentObjectID) {
    // if (referenceByObjectID.get(objectID) == null) {
    // log.warn("Unkwnown reference to object: " + objectID);
    // return;
    // }
    // parentObjectIDByReferences.put(referenceByObjectID.get(objectID),
    // parentObjectID);
    // }

    private void expungeStaleEntries() {
        Reference<? extends PObject> reference = null;

        while ((reference = queue.poll()) != null) {
            final Integer objectID = objectIDByReferences.remove(reference);
            // final Integer parentObjectID =
            // parentObjectIDByReferences.remove(reference);
            final WeakReference<PObject> removedObject = referenceByObjectID.remove(objectID);
            if (log.isDebugEnabled())
                log.debug("Removing reference on object #" + objectID);

            // if (parentObjectID != null) {
            final Parser parser = Txn.get().getTxnContext().getParser();
            parser.beginObject();
            parser.parse(ServerToClientModel.TYPE_GC, objectID);

            if (removedObject.get() instanceof PWidget) {
                System.err.println("GC object ");
                final PWidget widget = (PWidget) removedObject.get();
                final PWindow window = widget.getWindow();
                parser.parse(ServerToClientModel.WINDOW_ID, window.getID());
            }

            // parser.comma();
            // parser.parse(Model.PARENT_OBJECT_ID, parentObjectID);
            parser.endObject();
            // }
        }
    }

}