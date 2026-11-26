/*
 * Copyright (c) 2017 PonySDK
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

package com.ponysdk.core.server.websocket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ServerToClientModel;

/**
 * A {@link WebsocketEncoder} that records the protocol writes of a suspended UIContext, to replay them on the
 * WebSocket that resumes it.
 * <p>
 * Backported from PonySDK master (the transparent reconnection engine and its per-property compaction).
 * <p>
 * Each {@code encode / endObject} call is kept as an {@link Entry}. {@link #compact()} merges runs of
 * {@code TYPE_UPDATE} blocks, the last value per property winning, and drops the blocks of an object both
 * created then garbage collected while suspended. Structural blocks ({@code TYPE_CREATE}, {@code TYPE_ADD},
 * {@code TYPE_REMOVE}, ...) keep their order. Compaction also runs on the way, so that an application pushing
 * the same cells (a price grid) does not overflow on stale values.
 * <p>
 * Past {@code maxEntries} held after compaction, the {@link OverflowHandler} is called once and nothing more
 * is recorded: the context cannot be resumed.
 */
public final class RecordingEncoder implements WebsocketEncoder {

    private static final Logger log = LoggerFactory.getLogger(RecordingEncoder.class);

    /** A single recorded protocol call. */
    public static final class Entry {

        private final ServerToClientModel model;
        private final Object value;

        public Entry(final ServerToClientModel model, final Object value) {
            this.model = model;
            this.value = value;
        }

        public ServerToClientModel model() {
            return model;
        }

        public Object value() {
            return value;
        }

        @Override
        public String toString() {
            return model + "=" + value;
        }
    }

    /** Called once when the entry limit is exceeded. */
    @FunctionalInterface
    public interface OverflowHandler {

        void onOverflow(int entryCount, int maxEntries);
    }

    /** Idempotent single-value setters: the last write of the model wins. */
    private static final Set<ServerToClientModel> SETTERS = EnumSet.of(ServerToClientModel.TEXT, ServerToClientModel.HTML,
        ServerToClientModel.WIDGET_TITLE, ServerToClientModel.WIDGET_WIDTH, ServerToClientModel.WIDGET_HEIGHT,
        ServerToClientModel.WIDGET_VISIBLE, ServerToClientModel.ENABLED, ServerToClientModel.STYLE_NAME,
        ServerToClientModel.PLACEHOLDER, ServerToClientModel.VALUE_CHECKBOX, ServerToClientModel.ENSURE_DEBUG_ID);

    private final ArrayList<Entry> entries = new ArrayList<>();
    private final int maxEntries;
    private final OverflowHandler overflowHandler;
    private boolean overflowed = false;

    /** Size right after the last on-the-way compaction. */
    private int compactedSize;

    // Monitoring, read from other threads; a single writer, the context's
    private volatile long written;
    private volatile int held;

    /**
     * @param maxEntries max entries held before overflow (0 = unlimited)
     * @param overflowHandler called once when the limit is exceeded
     */
    public RecordingEncoder(final int maxEntries, final OverflowHandler overflowHandler) {
        this.maxEntries = maxEntries;
        this.overflowHandler = overflowHandler;
    }

    /** Unlimited recording, with no overflow protection: for tests only. */
    public RecordingEncoder() {
        this(0, null);
    }

    @Override
    public void beginObject() {
        // a no-op in WebSocket.beginObject too: nothing to record
    }

    @Override
    public void encode(final ServerToClientModel model, final Object value) {
        if (overflowed) return;
        entries.add(new Entry(model, value));
        written++;
        held = entries.size();
        checkOverflow();
    }

    @Override
    public void endObject() {
        if (overflowed) return;
        entries.add(new Entry(ServerToClientModel.END, null));
        written++;
        held = entries.size();
        // Compact on the way, at a block boundary so no partial block is touched, at most once per
        // maxEntries / 4 new entries
        if (maxEntries > 0 && entries.size() > Math.max(maxEntries / 2, compactedSize + maxEntries / 4)) {
            compact();
            compactedSize = entries.size();
        }
        checkOverflow();
    }

    /** Entries written since recording started, compacted ones included. Safe from any thread. */
    public long getWrittenEntries() {
        return written;
    }

    /** Entries held now, after the compactions so far. Safe from any thread. */
    public int getHeldEntries() {
        return held;
    }

    /**
     * What the entries held are made of, the most frequent models first: tells why a recording does not compact, when
     * it overflows.
     */
    public String describe() {
        final Map<ServerToClientModel, Integer> counts = new HashMap<>();
        for (final Entry e : entries) {
            counts.merge(e.model(), 1, Integer::sum);
        }
        final List<Map.Entry<ServerToClientModel, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(15, sorted.size()); i++) {
            if (i > 0) sb.append(", ");
            sb.append(sorted.get(i).getKey()).append('=').append(sorted.get(i).getValue());
        }
        return sb.toString();
    }

    private void checkOverflow() {
        if (maxEntries > 0 && entries.size() > maxEntries) {
            overflowed = true;
            if (overflowHandler != null) overflowHandler.onOverflow(entries.size(), maxEntries);
            else log.warn("RecordingEncoder overflow: {} entries exceeds limit of {}", entries.size(), maxEntries);
        }
    }

    /** Returns true if the entry limit was exceeded. */
    public boolean isOverflowed() {
        return overflowed;
    }

    /** Returns the number of entries held now. */
    public int size() {
        return entries.size();
    }

    /**
     * Compacts the recorded entries in place:
     * <ol>
     * <li>splits them into blocks ended by {@code END};</li>
     * <li>drops every block of an object created then garbage collected while suspended;</li>
     * <li>merges each run of {@code TYPE_UPDATE} blocks, per property (see {@link #mergeUpdateRun});</li>
     * <li>keeps every other block, in order.</li>
     * </ol>
     */
    public void compact() {
        if (entries.isEmpty()) return;

        final List<Block> blocks = new ArrayList<>();
        int blockStart = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).model() == ServerToClientModel.END) {
                blocks.add(new Block(blockStart, i)); // END included
                blockStart = i + 1;
            }
        }

        // An object is cancelled only if it is created, then garbage collected (TYPE_GC, its end of life) while
        // suspended. TYPE_REMOVE only takes a widget out of its parent: it may be added again elsewhere, so it never
        // cancels. Collected first, the object existed before the suspension and its end must reach the terminal. An
        // object another object's block names as its parent is kept too, so that block never points at nothing.
        final Map<Integer, Integer> firstCreateIdx = new HashMap<>();
        final Map<Integer, Integer> firstGcIdx = new HashMap<>();
        final Set<Integer> parents = new HashSet<>();
        for (int bi = 0; bi < blocks.size(); bi++) {
            final Block b = blocks.get(bi);
            for (int k = b.start; k < b.end; k++) {
                final Entry e = entries.get(k);
                if (e.model() == ServerToClientModel.PARENT_OBJECT_ID && e.value() instanceof Integer && !e.value().equals(b.objectId))
                    parents.add((Integer) e.value());
            }
            if (b.objectId < 0) continue;
            if (b.type == BlockType.CREATE) firstCreateIdx.putIfAbsent(b.objectId, bi);
            else if (b.type == BlockType.GC) firstGcIdx.putIfAbsent(b.objectId, bi);
        }
        final Set<Integer> cancelledIds = new HashSet<>();
        for (final Map.Entry<Integer, Integer> created : firstCreateIdx.entrySet()) {
            final Integer gcIdx = firstGcIdx.get(created.getKey());
            if (gcIdx != null && created.getValue() < gcIdx && !parents.contains(created.getKey()))
                cancelledIds.add(created.getKey());
        }

        // The window each block is written to: WINDOW_ID is written only when the window changes, so a block moved
        // by the merge must carry it again
        final Entry[] windows = new Entry[blocks.size()];
        Entry window = null;
        for (int bi = 0; bi < blocks.size(); bi++) {
            final Block b = blocks.get(bi);
            for (int k = b.start; k < b.end; k++) {
                if (entries.get(k).model() == ServerToClientModel.WINDOW_ID) window = entries.get(k);
            }
            windows[bi] = window;
        }

        final Emitter compacted = new Emitter(new ArrayList<>(entries.size()));
        int i = 0;
        while (i < blocks.size()) {
            final Block b = blocks.get(i);
            if (b.objectId >= 0 && cancelledIds.contains(b.objectId)) {
                i++;
                continue;
            }
            if (b.type == BlockType.UPDATE) {
                int j = i + 1;
                while (j < blocks.size() && blocks.get(j).type == BlockType.UPDATE)
                    j++;
                mergeUpdateRun(blocks, windows, i, j, cancelledIds, compacted);
                i = j;
                continue;
            }
            copyBlock(b, windows[i], compacted);
            i++;
        }

        entries.clear();
        entries.addAll(compacted.target);
        held = entries.size();
    }

    /** Writes compacted blocks, with a WINDOW_ID before each block whose window is not the one written last. */
    private static final class Emitter {

        final List<Entry> target;
        private Object lastWindow;

        Emitter(final List<Entry> target) {
            this.target = target;
        }

        void window(final Entry window) {
            if (window == null || window.value().equals(lastWindow)) return;
            target.add(window);
            lastWindow = window.value();
        }

        void add(final Entry entry) {
            target.add(entry);
        }
    }

    /** Replays the entries held onto the given encoder. */
    public void replayTo(final WebsocketEncoder target) {
        for (final Entry e : entries) {
            if (e.model() == ServerToClientModel.END) target.endObject();
            else target.encode(e.model(), e.value());
        }
    }

    /** Clears the entries and releases their references. */
    public void clear() {
        entries.clear();
        entries.trimToSize();
        held = 0;
    }

    private enum BlockType {
        CREATE, UPDATE, ADD, REMOVE, GC, ADD_HANDLER, REMOVE_HANDLER, OTHER
    }

    private final class Block {

        final int start; // first entry
        final int end; // the END entry, included
        final BlockType type;
        final int objectId;

        Block(final int start, final int end) {
            this.start = start;
            this.end = end;
            BlockType t = BlockType.OTHER;
            int id = -1;
            for (int k = start; k < end; k++) {
                final BlockType bt = typeOf(entries.get(k).model());
                if (bt != null) {
                    t = bt;
                    final Object v = entries.get(k).value();
                    id = v instanceof Integer ? (Integer) v : -1;
                    break;
                }
            }
            this.type = t;
            this.objectId = id;
        }
    }

    /** The block type a structural model denotes, or null for a property or routing model. */
    private static BlockType typeOf(final ServerToClientModel m) {
        if (m == ServerToClientModel.TYPE_CREATE) return BlockType.CREATE;
        if (m == ServerToClientModel.TYPE_UPDATE) return BlockType.UPDATE;
        if (m == ServerToClientModel.TYPE_ADD) return BlockType.ADD;
        if (m == ServerToClientModel.TYPE_REMOVE) return BlockType.REMOVE;
        if (m == ServerToClientModel.TYPE_GC) return BlockType.GC;
        if (m == ServerToClientModel.TYPE_ADD_HANDLER) return BlockType.ADD_HANDLER;
        if (m == ServerToClientModel.TYPE_REMOVE_HANDLER) return BlockType.REMOVE_HANDLER;
        return null;
    }

    private void copyBlock(final Block b, final Entry window, final Emitter target) {
        target.window(window);
        for (int k = b.start; k <= b.end; k++) {
            if (entries.get(k).model() != ServerToClientModel.WINDOW_ID) target.add(entries.get(k));
        }
    }

    /** One property write of an update block: one entry, or a key entry and its value entry. */
    private static final class Write {

        final Object mergeKey;
        final Entry first;
        final Entry second;

        Write(final Object mergeKey, final Entry first, final Entry second) {
            this.mergeKey = mergeKey;
            this.first = first;
            this.second = second;
        }

        void emit(final Emitter target) {
            target.add(first);
            if (second != null) target.add(second);
        }
    }

    /** The writes of one object over a run, in order: an overwritten property moves to the end. */
    private static final class ObjectWrites {

        Entry window;
        Entry frame;
        final int objectId;
        final LinkedHashMap<Object, Write> writes = new LinkedHashMap<>();

        ObjectWrites(final int objectId) {
            this.objectId = objectId;
        }

        void put(final Write w) {
            writes.remove(w.mergeKey); // last value wins, at the position of the last write
            writes.put(w.mergeKey, w);
        }

        void emit(final Emitter target) {
            target.window(window);
            if (frame != null) target.add(frame);
            target.add(new Entry(ServerToClientModel.TYPE_UPDATE, objectId));
            for (final Write w : writes.values()) {
                w.emit(target);
            }
            target.add(new Entry(ServerToClientModel.END, null));
        }
    }

    /**
     * Merges a run of consecutive UPDATE blocks. Writes are keyed per property: setters by model, style,
     * attribute and property puts and removes by their name, so different keys never overwrite each other;
     * any other write (a style name added or removed, a command) is kept, in order.
     * <p>
     * Blocks made only of keyed writes ("pure") of different objects commute: they are folded into one block per
     * object. A block with any other write (a command, an addon method) is a barrier for its own object only: what
     * was accumulated for that object is written out first, so the command keeps its position relative to the
     * object's updates, while the updates of the other objects keep merging across it. A price grid whose cells
     * blink on each tick would otherwise merge nothing.
     */
    private void mergeUpdateRun(final List<Block> blocks, final Entry[] windows, final int fromIdx, final int toIdx,
                                final Set<Integer> cancelledIds, final Emitter target) {
        final LinkedHashMap<Integer, ObjectWrites> pending = new LinkedHashMap<>();
        for (int bi = fromIdx; bi < toIdx; bi++) {
            final Block b = blocks.get(bi);
            final int objId = b.objectId;
            if (cancelledIds.contains(objId)) continue;
            final Entry window = windows[bi];
            Entry frame = null;
            final List<Write> writes = new ArrayList<>();
            boolean pure = true;
            for (int k = b.start; k < b.end; k++) {
                final Entry e = entries.get(k);
                final ServerToClientModel m = e.model();
                if (m == ServerToClientModel.WINDOW_ID) continue;
                if (m == ServerToClientModel.FRAME_ID) {
                    frame = e;
                    continue;
                }
                if (m == ServerToClientModel.TYPE_UPDATE) continue;
                final Entry next = k + 1 < b.end ? entries.get(k + 1) : null;
                if (m == ServerToClientModel.PUT_STYLE_KEY && isValue(next, ServerToClientModel.STYLE_VALUE)) {
                    writes.add(new Write(Arrays.asList("style", e.value()), e, next));
                    k++;
                } else if (m == ServerToClientModel.PUT_ATTRIBUTE_KEY && isValue(next, ServerToClientModel.ATTRIBUTE_VALUE)) {
                    writes.add(new Write(Arrays.asList("attribute", e.value()), e, next));
                    k++;
                } else if (m == ServerToClientModel.PUT_PROPERTY_KEY && isValue(next, ServerToClientModel.PROPERTY_VALUE)) {
                    writes.add(new Write(Arrays.asList("property", e.value()), e, next));
                    k++;
                } else if (m == ServerToClientModel.REMOVE_STYLE_KEY) {
                    writes.add(new Write(Arrays.asList("style", e.value()), e, null));
                } else if (m == ServerToClientModel.REMOVE_ATTRIBUTE_KEY) {
                    writes.add(new Write(Arrays.asList("attribute", e.value()), e, null));
                } else if (SETTERS.contains(m)) {
                    writes.add(new Write(m, e, null));
                } else {
                    writes.add(new Write(new Object(), e, null)); // unique: never merged, kept in order
                    pure = false;
                }
            }
            if (!pure) {
                final ObjectWrites before = pending.remove(objId);
                if (before != null) before.emit(target);
                final ObjectWrites own = new ObjectWrites(objId);
                own.window = window;
                own.frame = frame;
                for (final Write w : writes) {
                    own.writes.put(w.mergeKey, w);
                }
                own.emit(target);
                continue;
            }
            final ObjectWrites acc = pending.computeIfAbsent(objId, ObjectWrites::new);
            acc.window = window;
            if (frame != null) acc.frame = frame;
            for (final Write w : writes) {
                acc.put(w);
            }
        }
        for (final ObjectWrites o : pending.values()) {
            o.emit(target);
        }
    }

    private static boolean isValue(final Entry next, final ServerToClientModel valueModel) {
        return next != null && next.model() == valueModel;
    }
}
