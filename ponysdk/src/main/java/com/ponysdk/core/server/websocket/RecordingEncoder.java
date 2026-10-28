/*
 * Copyright (c) 2011 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.websocket;

import com.ponysdk.core.model.ServerToClientModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link WebsocketEncoder} that records all protocol operations during UIContext suspension.
 * <p>
 * Every {@code beginObject / encode / endObject} call is captured as a list of
 * {@link Entry} records. Before replay, {@link #compact()} merges consecutive
 * {@code TYPE_UPDATE} blocks targeting the same objectID — only the last property
 * value for each {@link ServerToClientModel} key survives. Structural operations
 * ({@code TYPE_CREATE}, {@code TYPE_ADD}, {@code TYPE_REMOVE}, etc.) are never
 * compacted and are replayed in order.
 * <p>
 * Create/Remove cancellation: if a widget is created and then removed during the
 * same suspension window, both the CREATE block and the REMOVE block (and any
 * intermediate UPDATE blocks for that objectID) are eliminated entirely.
 * <p>
 * A configurable entry limit prevents unbounded memory growth. When exceeded,
 * the {@link #overflowHandler} is called (typically destroys the UIContext).
 */
public final class RecordingEncoder implements WebsocketEncoder {

    private static final Logger log = LoggerFactory.getLogger(RecordingEncoder.class);

    /** A single recorded protocol call. */
    public record Entry(ServerToClientModel model, Object value) {}

    /** Called when the entry limit is exceeded. */
    @FunctionalInterface
    public interface OverflowHandler {
        void onOverflow(int entryCount, int maxEntries);
    }

    private final List<Entry> entries = new ArrayList<>();
    private final int maxEntries;
    private final OverflowHandler overflowHandler;
    private boolean overflowed = false;

    /**
     * @param maxEntries    max entries before overflow (0 = unlimited)
     * @param overflowHandler called once when the limit is exceeded
     */
    public RecordingEncoder(final int maxEntries, final OverflowHandler overflowHandler) {
        this.maxEntries = maxEntries;
        this.overflowHandler = overflowHandler;
    }

    /** Unlimited recording (no overflow protection — use for tests only). */
    public RecordingEncoder() {
        this(0, null);
    }

    @Override
    public void beginObject() {
        // beginObject is a no-op in WebSocket.encode too — nothing to record
    }

    @Override
    public void encode(final ServerToClientModel model, final Object value) {
        if (overflowed) return; // stop recording after overflow
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
        // A pushing application keeps rewriting the same properties while the client is away: compact on the
        // way (at a block boundary, so no partial block is touched) instead of overflowing on stale values.
        // Amortized: at most once per maxEntries/4 new entries.
        if (maxEntries > 0 && entries.size() > Math.max(maxEntries / 2, compactedSize + maxEntries / 4)) {
            compact();
            compactedSize = entries.size();
        }
        checkOverflow();
    }

    /** Size right after the last on-the-fly compaction. */
    private int compactedSize;

    // Monitoring (SessionAdmin), read from other threads; a single writer, the context's
    private volatile long written;
    private volatile int held;

    /** Entries written since recording started, compacted ones included. Safe from any thread. */
    public long getWrittenEntries() {
        return written;
    }

    /** Entries held now, after the compactions so far. Safe from any thread. */
    public int getHeldEntries() {
        return held;
    }

    private void checkOverflow() {
        if (maxEntries > 0 && entries.size() > maxEntries) { // encode() and endObject() record nothing once overflowed
            overflowed = true;
            // the handler says what it does about it (UIContext: logs and destroys the session)
            if (overflowHandler != null) overflowHandler.onOverflow(entries.size(), maxEntries);
            else log.warn("RecordingEncoder overflow: {} entries exceeds limit of {}", entries.size(), maxEntries);
        }
    }

    /** Returns true if the entry limit was exceeded. */
    public boolean isOverflowed() {
        return overflowed;
    }

    /** Returns the number of recorded entries (before compaction). */
    public int size() {
        return entries.size();
    }

    /**
     * Compacts the recorded entries in-place:
     * <ol>
     *   <li>Identifies blocks delimited by {@code END} entries</li>
     *   <li>For consecutive {@code TYPE_UPDATE} blocks on the same objectID,
     *       merges property writes — last value wins per model key</li>
     *   <li>If a {@code TYPE_CREATE} and {@code TYPE_REMOVE/TYPE_GC} target the same
     *       objectID, both blocks (and any intermediate updates) are eliminated</li>
     *   <li>Structural blocks ({@code TYPE_CREATE}, {@code TYPE_ADD}, {@code TYPE_REMOVE},
     *       {@code TYPE_ADD_HANDLER}, {@code TYPE_REMOVE_HANDLER}, {@code TYPE_GC})
     *       are preserved in order</li>
     * </ol>
     */
    public void compact() {
        if (entries.isEmpty()) return;

        // Phase 1: parse into blocks
        final List<Block> blocks = new ArrayList<>();
        int blockStart = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).model() == ServerToClientModel.END) {
                blocks.add(new Block(blockStart, i)); // inclusive of END
                blockStart = i + 1;
            }
        }

        // Phase 2: identify created and removed objectIDs for cancellation.
        // Only cancel if CREATE comes BEFORE REMOVE/GC in block order — if REMOVE
        // comes first, the object existed before suspension and must not be cancelled.
        final var firstCreateIdx = new java.util.HashMap<Integer, Integer>(); // objId → block index of first CREATE
        final var firstRemoveIdx = new java.util.HashMap<Integer, Integer>(); // objId → block index of first REMOVE/GC
        for (int bi = 0; bi < blocks.size(); bi++) {
            final Block b = blocks.get(bi);
            final BlockType type = b.type();
            final int objId = b.objectId();
            if (objId < 0) continue;
            if (type == BlockType.CREATE) firstCreateIdx.putIfAbsent(objId, bi);
            else if (type == BlockType.REMOVE || type == BlockType.GC) firstRemoveIdx.putIfAbsent(objId, bi);
        }
        // IDs that were both created and removed during suspension, with CREATE before REMOVE
        final var cancelledIds = new java.util.HashSet<Integer>();
        for (final var entry : firstCreateIdx.entrySet()) {
            final Integer removeIdx = firstRemoveIdx.get(entry.getKey());
            if (removeIdx != null && entry.getValue() < removeIdx) {
                cancelledIds.add(entry.getKey());
            }
        }

        // Phase 3: merge each run of consecutive UPDATE blocks (last value per property), eliminate cancelled
        final List<Entry> compacted = new ArrayList<>(entries.size());
        int i = 0;
        while (i < blocks.size()) {
            final Block b = blocks.get(i);
            if (b.objectId() >= 0 && cancelledIds.contains(b.objectId())) {
                i++;
                continue;
            }
            if (b.type() == BlockType.UPDATE) {
                int j = i + 1;
                while (j < blocks.size() && blocks.get(j).type() == BlockType.UPDATE) j++;
                mergeUpdateRun(blocks, i, j, cancelledIds, compacted);
                i = j;
                continue;
            }
            copyBlock(b, compacted);
            i++;
        }

        entries.clear();
        entries.addAll(compacted);
        held = entries.size();
    }

    /**
     * Replays all (compacted) entries onto the given encoder.
     */
    public void replayTo(final WebsocketEncoder target) {
        for (final Entry e : entries) {
            if (e.model() == ServerToClientModel.END) {
                target.endObject();
            } else {
                target.encode(e.model(), e.value());
            }
        }
    }

    /** Clears all recorded entries and releases references. */
    public void clear() {
        entries.clear();
        held = 0;
        ((ArrayList<Entry>) entries).trimToSize(); // release the backing array
    }

    // ---- Internal block representation ----

    private enum BlockType { CREATE, UPDATE, ADD, REMOVE, GC, ADD_HANDLER, REMOVE_HANDLER, OTHER }

    private final class Block {
        final int start; // index of first entry in the block
        final int end;   // index of END entry (inclusive)
        // Classified once: compact() asks for type/objectId several times per block, and each
        // lookup used to rescan the block's entries.
        private final BlockType type;
        private final int objectId;

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
                    id = v instanceof Integer ? (int) v : -1;
                    break;
                }
            }
            this.type = t;
            this.objectId = id;
        }

        BlockType type() {
            return type;
        }

        int objectId() {
            return objectId;
        }
    }

    /** The block type a structural model key denotes, or null for a property / routing key. */
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

    private void copyBlock(final Block b, final List<Entry> target) {
        for (int k = b.start; k <= b.end; k++) {
            target.add(entries.get(k));
        }
    }

    /** Idempotent single-value setters: the last write of the model wins. */
    private static final java.util.EnumSet<ServerToClientModel> SETTERS = java.util.EnumSet.of(
        ServerToClientModel.TEXT, ServerToClientModel.HTML, ServerToClientModel.WIDGET_TITLE,
        ServerToClientModel.WIDGET_WIDTH, ServerToClientModel.WIDGET_HEIGHT, ServerToClientModel.WIDGET_VISIBLE,
        ServerToClientModel.ENABLED, ServerToClientModel.STYLE_NAME, ServerToClientModel.PLACEHOLDER,
        ServerToClientModel.VALUE_CHECKBOX, ServerToClientModel.ENSURE_DEBUG_ID);

    /** One property write of an update block: one entry, or a key entry + its value entry. */
    private record Write(Object mergeKey, Entry first, Entry second) {
        void emit(final List<Entry> target) {
            target.add(first);
            if (second != null) target.add(second);
        }
    }

    /** The writes of one object accumulated over a run, in order (an overwritten property moves to the end). */
    private static final class ObjectWrites {
        Entry window;
        Entry frame;
        final int objectId;
        final java.util.LinkedHashMap<Object, Write> writes = new java.util.LinkedHashMap<>();

        ObjectWrites(final int objectId) {
            this.objectId = objectId;
        }

        void put(final Write w) {
            writes.remove(w.mergeKey()); // last value wins, in the position of the last write
            writes.put(w.mergeKey(), w);
        }

        void emit(final List<Entry> target) {
            if (window != null) target.add(window);
            if (frame != null) target.add(frame);
            target.add(new Entry(ServerToClientModel.TYPE_UPDATE, objectId));
            writes.values().forEach(w -> w.emit(target));
            target.add(new Entry(ServerToClientModel.END, null));
        }
    }

    /**
     * Merges a run of consecutive UPDATE blocks. Writes are keyed per property: setters by model, style /
     * attribute / property puts and removes by their name, so different keys never overwrite each other;
     * any other write (style names added or removed, commands, method calls) is kept, in order.
     * <p>
     * Blocks made only of keyed writes ("pure") of different objects commute: they are folded into one block
     * per object. A block with any other write is a barrier: what was accumulated is written out first, so
     * commands keep their position relative to the updates around them.
     */
    private void mergeUpdateRun(final List<Block> blocks, final int fromIdx, final int toIdx,
                                final java.util.Set<Integer> cancelledIds, final List<Entry> target) {
        final var pending = new java.util.LinkedHashMap<Integer, ObjectWrites>();
        for (int bi = fromIdx; bi < toIdx; bi++) {
            final Block b = blocks.get(bi);
            final int objId = b.objectId(); // an UPDATE block's id is the int of its TYPE_UPDATE (UINT31)
            if (cancelledIds.contains(objId)) continue;
            Entry window = null, frame = null;
            final List<Write> writes = new ArrayList<>();
            boolean pure = true;
            for (int k = b.start; k < b.end; k++) {
                final Entry e = entries.get(k);
                final ServerToClientModel m = e.model();
                if (m == ServerToClientModel.WINDOW_ID) { window = e; continue; }
                if (m == ServerToClientModel.FRAME_ID) { frame = e; continue; }
                if (m == ServerToClientModel.TYPE_UPDATE) continue;
                final Entry next = k + 1 < b.end ? entries.get(k + 1) : null;
                if (m == ServerToClientModel.PUT_STYLE_KEY && isValue(next, ServerToClientModel.STYLE_VALUE)) {
                    writes.add(new Write(List.of("style", e.value()), e, next));
                    k++;
                } else if (m == ServerToClientModel.PUT_ATTRIBUTE_KEY && isValue(next, ServerToClientModel.ATTRIBUTE_VALUE)) {
                    writes.add(new Write(List.of("attribute", e.value()), e, next));
                    k++;
                } else if (m == ServerToClientModel.PUT_PROPERTY_KEY && isValue(next, ServerToClientModel.PROPERTY_VALUE)) {
                    writes.add(new Write(List.of("property", e.value()), e, next));
                    k++;
                } else if (m == ServerToClientModel.REMOVE_STYLE_KEY) {
                    writes.add(new Write(List.of("style", e.value()), e, null));
                } else if (m == ServerToClientModel.REMOVE_ATTRIBUTE_KEY) {
                    writes.add(new Write(List.of("attribute", e.value()), e, null));
                } else if (SETTERS.contains(m)) {
                    writes.add(new Write(m, e, null));
                } else {
                    writes.add(new Write(new Object(), e, null)); // unique: never merged, kept in order
                    pure = false;
                }
            }
            if (!pure) {
                // a barrier: what came before is written out first, then this block, unchanged in its order
                pending.values().forEach(o -> o.emit(target));
                pending.clear();
                final ObjectWrites own = new ObjectWrites(objId);
                own.window = window;
                own.frame = frame;
                writes.forEach(w -> own.writes.put(w.mergeKey(), w));
                own.emit(target);
                continue;
            }
            final ObjectWrites acc = pending.computeIfAbsent(objId, ObjectWrites::new);
            if (window != null) acc.window = window;
            if (frame != null) acc.frame = frame;
            writes.forEach(acc::put);
        }
        pending.values().forEach(o -> o.emit(target));
    }

    private static boolean isValue(final Entry next, final ServerToClientModel valueModel) {
        return next != null && next.model() == valueModel;
    }
}
