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

    /** Grown by a burst far beyond a normal transaction: not worth keeping in a pool. */
    boolean isOversized() {
        return targets.length > 4096;
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

    /** Upper bound of objects per update run: keeps blocks small, so the terminal processes them incrementally. */
    static final int MAX_RUN = 512;

    /**
     * Writes every pending update, then empties the buffer.
     * <p>
     * Version 1 terminals: in write order, consecutive updates of one object share a block.<br>
     * Update-run terminals: sorted by object (stable per object, so properties of one object keep their
     * order) and written as runs - one block whose objects are chained by UPDATE_NEXT markers. Reordering
     * across objects is safe: every deferred update is a plain assignment of one object's property.
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
        final boolean runs = writer.supportsUpdateRuns();
        final int[] order = runs ? sortedOrder(t, n) : null;
        final int m = order != null ? orderLength : n;
        UpdateTarget open = null;
        Object openGroup = null;
        int openId = -1;
        int runLength = 0;
        for (int i = 0; i < m; i++) {
            final int s = order != null ? order[i] : i;
            final UpdateTarget target = t[s];
            if (target == null || !target.isUpdatable()) continue; // overwritten or destroyed meanwhile
            if (target != open) {
                if (runs && open != null && runLength < MAX_RUN && target.updateGroup() == openGroup) {
                    writeRunMarker(writer, openId, target.getID());
                    runLength++;
                } else {
                    if (open != null) writer.endObjectUnchecked();
                    if (!target.beginCoalescedUpdate(writer)) {
                        open = null;
                        continue;
                    }
                    openGroup = target.updateGroup();
                    runLength = 1;
                }
                open = target;
                openId = target.getID();
            }
            if (k[s] == null) {
                writer.writeUnchecked(km[s], v[s]);
            } else {
                writer.writeUnchecked(km[s], k[s]);
                if (vm[s] != null) writer.writeUnchecked(vm[s], v[s]); // keyed updates without value: a class added...
            }
        }
        if (open != null) writer.endObjectUnchecked();
        // release references for GC (arrays are reused)
        Arrays.fill(t, 0, n, null);
        Arrays.fill(k, 0, n, null);
        Arrays.fill(v, 0, n, null);
    }

    /** Pre-boxed delta bytes (Byte.valueOf would box-cache them too, this just skips the call). */
    private static final Byte[] DELTA_BYTES = new Byte[256];
    static {
        for (int i = 0; i < 256; i++) DELTA_BYTES[i] = (byte) i;
    }

    /** Drops everything pending (context no longer writable). */
    void discard() {
        final int n = count;
        reset(n);
        Arrays.fill(targets, 0, n, null);
        Arrays.fill(keys, 0, n, null);
        Arrays.fill(values, 0, n, null);
    }

    /** Next object of a run: +1, a small forward delta, or an absolute id. */
    static void writeRunMarker(final ModelWriter writer, final int previousId, final int id) {
        final int delta = id - previousId;
        if (delta == 1) writer.writeUnchecked(ServerToClientModel.UPDATE_NEXT, null);
        else if (delta >= 2 && delta <= 257) writer.writeUnchecked(ServerToClientModel.UPDATE_NEXT_DELTA, DELTA_BYTES[delta - 2]);
        else writer.writeIntUnchecked(ServerToClientModel.UPDATE_NEXT_ID, id);
    }

    /** Reused counting-sort buckets (single-group fast path). */
    private int[] buckets = new int[256];
    /** Reused order of the fast path: its first {@link #orderLength} entries are valid. */
    private int[] orderBuffer = new int[64];
    private int orderLength;

    /**
     * Live slots ordered by (update group, object id, slot), the first {@link #orderLength} entries, or null
     * to keep the slot order (more than 255 groups or 16M slots: never in practice). Groups are numbered by
     * first appearance.
     */
    private int[] sortedOrder(final UpdateTarget[] t, final int n) {
        final int[] fast = countingOrder(t, n);
        if (fast != null) return fast;
        final int[] slow = comparisonOrder(t, n);
        if (slow != null) orderLength = slow.length;
        return slow;
    }

    /**
     * Fast path: all live slots in one update group and object ids within a small range (ids of a
     * session are dense). Stable counting sort by id - O(n + range), no comparisons - keeps the slot
     * order of each object. Returns null when it does not apply.
     */
    private int[] countingOrder(final UpdateTarget[] t, final int n) {
        Object group = null;
        int live = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int s = 0; s < n; s++) {
            final UpdateTarget target = t[s];
            if (target == null) continue;
            final Object g = target.updateGroup();
            if (live == 0) group = g;
            else if (g != group) return null;
            final int id = target.getID();
            if (id < min) min = id;
            if (id > max) max = id;
            live++;
        }
        orderLength = live;
        if (live == 0) return orderBuffer;
        final long range = (long) max - min + 1;
        if (range > Math.max(4096, 8L * live)) return null;
        final int r = (int) range;
        if (buckets.length < r + 1) buckets = new int[Math.max(r + 1, buckets.length * 2)];
        final int[] b = buckets;
        Arrays.fill(b, 0, r + 1, 0);
        for (int s = 0; s < n; s++) if (t[s] != null) b[t[s].getID() - min + 1]++;
        for (int i = 1; i <= r; i++) b[i] += b[i - 1];
        if (orderBuffer.length < live) orderBuffer = new int[Math.max(live, orderBuffer.length * 2)];
        final int[] o = orderBuffer;
        for (int s = 0; s < n; s++) if (t[s] != null) o[b[t[s].getID() - min]++] = s;
        return o;
    }

    private static int[] comparisonOrder(final UpdateTarget[] t, final int n) {
        if (n >= 1 << 24) return null;
        Object[] groups = new Object[4];
        int groupCount = 0;
        long[] sortKeys = new long[n];
        int live = 0;
        for (int s = 0; s < n; s++) {
            final UpdateTarget target = t[s];
            if (target == null) continue;
            final Object group = target.updateGroup();
            int g = 0;
            if (groupCount == 0 || groups[0] != group) { // fast path: almost always one window
                while (g < groupCount && groups[g] != group) g++;
            }
            if (g == groupCount) {
                if (groupCount == 255) return null;
                if (groupCount == groups.length) groups = Arrays.copyOf(groups, groupCount * 2);
                groups[groupCount++] = group;
            }
            sortKeys[live++] = (long) g << 55 | (long) target.getID() << 24 | s;
        }
        if (live < n) sortKeys = Arrays.copyOf(sortKeys, live);
        Arrays.sort(sortKeys);
        final int[] order = new int[live];
        for (int i = 0; i < live; i++) order[i] = (int) (sortKeys[i] & 0xFF_FFFF);
        return order;
    }

    /**
     * Empties the index. It only grows (the buffer is pooled per thread), so after one burst a full clear
     * would cost every later small drain tens of KB: a sparse drain clears just the entries of its live slots
     * (each live slot owns exactly one entry, an overwrite repoints it). Called before the slots are released.
     */
    private void reset(final int usedSlots) {
        count = 0;
        live = 0;
        if (usedSlots == 0) return;
        if (usedSlots * 8 >= index.length) {
            Arrays.fill(index, 0);
            return;
        }
        for (int s = 0; s < usedSlots; s++) {
            if (targets[s] == null) continue;
            int h = hash(targets[s].getID(), keyModels[s], keys[s]) & indexMask;
            while (index[h] != s + 1) h = (h + 1) & indexMask;
            index[h] = 0;
        }
    }
}
