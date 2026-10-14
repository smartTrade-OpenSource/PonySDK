/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.core.writer;

import java.util.Arrays;
import java.util.Objects;

import com.ponysdk.core.model.ServerToClientModel;

/**
 * Buffer of "last value wins" property updates waiting to be written (see
 * {@link ModelWriter#deferUpdate}). Setting the same property of the same object again replaces the
 * pending value and moves it to the end, so the final order equals the order of the last writes:
 * interfering properties of one object (e.g. TEXT then HTML) still end with the last one written.
 * <p>
 * Flat arrays reused across drains plus an open-addressing index: no allocation per update once warm.
 * Not thread-safe (used under the UIContext lock).
 */
final class PendingUpdates {

    private UpdateTarget[] targets = new UpdateTarget[32];
    private ServerToClientModel[] keyModels = new ServerToClientModel[32];
    private String[] keys = new String[32];
    private ServerToClientModel[] valueModels = new ServerToClientModel[32];
    private Object[] values = new Object[32];
    private int count;  // slots used, including dead (overwritten) ones
    private int live;

    // index: slot number + 1 (0 = empty), linear probing
    private int[] index = new int[64];
    private int indexMask = 63;

    boolean isEmpty() {
        return live == 0;
    }

    int size() {
        return live;
    }

    void put(final UpdateTarget target, final ServerToClientModel keyModel, final String key,
             final ServerToClientModel valueModel, final Object value) {
        int h = hash(target.getID(), keyModel, key) & indexMask;
        int e;
        while ((e = index[h]) != 0) {
            final int s = e - 1;
            if (targets[s] == target && keyModels[s] == keyModel && Objects.equals(keys[s], key)) {
                // same property again: kill the old slot, append the new value (move to end)
                targets[s] = null;
                values[s] = null;
                live--;
                index[h] = append(target, keyModel, key, valueModel, value) + 1;
                return;
            }
            h = (h + 1) & indexMask;
        }
        index[h] = append(target, keyModel, key, valueModel, value) + 1;
        if (count * 2 > index.length) rebuildIndex(index.length * 2);
    }

    private int append(final UpdateTarget target, final ServerToClientModel keyModel, final String key,
                       final ServerToClientModel valueModel, final Object value) {
        if (count == targets.length) {
            final int n = count * 2;
            targets = Arrays.copyOf(targets, n);
            keyModels = Arrays.copyOf(keyModels, n);
            keys = Arrays.copyOf(keys, n);
            valueModels = Arrays.copyOf(valueModels, n);
            values = Arrays.copyOf(values, n);
        }
        final int s = count++;
        targets[s] = target;
        keyModels[s] = keyModel;
        keys[s] = key;
        valueModels[s] = valueModel;
        values[s] = value;
        live++;
        return s;
    }

    private static int hash(final int id, final ServerToClientModel keyModel, final String key) {
        int h = id * 31 + keyModel.ordinal();
        if (key != null) h = h * 31 + key.hashCode();
        h *= 0x9E3779B9;
        return h ^ (h >>> 16);
    }

    private void rebuildIndex(final int capacity) {
        index = new int[capacity];
        indexMask = capacity - 1;
        for (int s = 0; s < count; s++) {
            if (targets[s] == null) continue;
            int h = hash(targets[s].getID(), keyModels[s], keys[s]) & indexMask;
            while (index[h] != 0) h = (h + 1) & indexMask;
            index[h] = s + 1;
        }
    }

    /**
     * Writes every pending update in order, merging consecutive updates of the same object into a
     * single update block, then empties the buffer.
     */
    void drainTo(final ModelWriter writer) {
        // Detach first: writing may re-enter the writer (beginObject drains), which must see it empty.
        final int n = count;
        final UpdateTarget[] t = targets;
        final ServerToClientModel[] km = keyModels;
        final String[] k = keys;
        final ServerToClientModel[] vm = valueModels;
        final Object[] v = values;
        reset(n);
        UpdateTarget open = null;
        for (int s = 0; s < n; s++) {
            final UpdateTarget target = t[s];
            if (target == null) continue;
            if (target != open) {
                if (open != null) writer.endObject();
                open = target.beginCoalescedUpdate(writer) ? target : null;
                if (open == null) continue; // destroyed meanwhile
            }
            if (k[s] == null) {
                writer.write(km[s], v[s]);
            } else {
                writer.write(km[s], k[s]);
                writer.write(vm[s], v[s]);
            }
        }
        if (open != null) writer.endObject();
        // release references for GC (arrays are reused)
        Arrays.fill(t, 0, n, null);
        Arrays.fill(k, 0, n, null);
        Arrays.fill(v, 0, n, null);
    }

    private void reset(final int usedSlots) {
        count = 0;
        live = 0;
        if (usedSlots > 0) Arrays.fill(index, 0);
    }
}
