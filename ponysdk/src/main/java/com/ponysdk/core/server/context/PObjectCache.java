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

package com.ponysdk.core.server.context;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.basic.PWindowManager;
import com.ponysdk.core.writer.ModelWriter;

public class PObjectCache {

    private static final Logger log = LoggerFactory.getLogger(PObjectCache.class);

    private final Map<Integer, PObjectWeakReference> referenceByObjectID = new ConcurrentHashMap<>();
    private final ReferenceQueue<PObject> queue = new ReferenceQueue<>();

    public PObject add(final PObject pObject) {
        expungeStaleEntries();
        final PObjectWeakReference weakReference = new PObjectWeakReference(pObject, queue);
        referenceByObjectID.put(weakReference.getObjectID(), weakReference);
        if (log.isDebugEnabled()) log.debug("Registering object: {}", pObject);
        return pObject;
    }

    public PObject get(final int objectID) {
        expungeStaleEntries();
        final Reference<PObject> value = referenceByObjectID.get(objectID);
        if (value == null) return null;
        return value.get();
    }

    private void expungeStaleEntries() {
        PObjectWeakReference reference;
        while ((reference = (PObjectWeakReference) queue.poll()) != null) {
            final int objectID = reference.getObjectID();
            referenceByObjectID.remove(objectID);

            final int windowID = reference.getWindowID();
            final int frameID = reference.getFrameID();
            if (log.isDebugEnabled()) log.debug("Removing reference on object #{} in window #{}", objectID, windowID);

            // No need to send to the terminal if the window doesn't exist any more
            if (PWindowManager.getWindow(windowID) == null) continue;

            final ModelWriter writer = Txn.get().getWriter();
            writer.beginObject();
            if (windowID != -1 && PWindow.getMain().getID() != windowID) writer.write(ServerToClientModel.WINDOW_ID, windowID);
            if (frameID != -1) writer.write(ServerToClientModel.FRAME_ID, frameID);
            writer.write(ServerToClientModel.TYPE_GC, objectID);
            writer.endObject();
        }
    }

    private static class PObjectWeakReference extends WeakReference<PObject> {

        private final int objectID;
        private final int windowID;
        private final int frameID;

        public PObjectWeakReference(final PObject pObject, final ReferenceQueue<? super PObject> queue) {
            super(pObject, queue);
            objectID = pObject.getID();
            windowID = pObject.getWindow() != null ? pObject.getWindow().getID() : -1;
            frameID = pObject.getFrame() != null ? pObject.getFrame().getID() : -1;
        }

        public int getObjectID() {
            return objectID;
        }

        public int getWindowID() {
            return windowID;
        }

        public int getFrameID() {
            return frameID;
        }

    }

}
