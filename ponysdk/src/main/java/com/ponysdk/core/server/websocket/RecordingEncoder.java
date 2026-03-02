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
        checkOverflow();
    }

    @Override
    public void endObject() {
        if (overflowed) return;
        entries.add(new Entry(ServerToClientModel.END, null));
        checkOverflow();
    }

    private void checkOverflow() {
        if (maxEntries > 0 && entries.size() > maxEntries && !overflowed) {
            overflowed = true;
            log.error("RecordingEncoder overflow: {} entries exceeds limit of {} — triggering destroy",
                    entries.size(), maxEntries);
            if (overflowHandler != null) overflowHandler.onOverflow(entries.size(), maxEntries);
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

        // Phase 3: merge consecutive UPDATE blocks on same objectID, eliminate cancelled
        final List<Entry> compacted = new ArrayList<>(entries.size());
        int i = 0;
        while (i < blocks.size()) {
            final Block b = blocks.get(i);
            final int objId = b.objectId();

            // Eliminate cancelled objects
            if (objId >= 0 && cancelledIds.contains(objId)) {
                i++;
                continue;
            }

            // Merge consecutive UPDATE blocks on same objectID
            if (b.type() == BlockType.UPDATE) {
                int j = i + 1;
                while (j < blocks.size() && blocks.get(j).type() == BlockType.UPDATE
                        && blocks.get(j).objectId() == objId) {
                    j++;
                }
                if (j > i + 1) {
                    mergeUpdateBlocks(blocks, i, j, compacted);
                    i = j;
                    continue;
                }
            }

            // Copy block as-is
            copyBlock(b, compacted);
            i++;
        }

        entries.clear();
        entries.addAll(compacted);
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
        ((ArrayList<Entry>) entries).trimToSize(); // release the backing array
    }

    // ---- Internal block representation ----

    private enum BlockType { CREATE, UPDATE, ADD, REMOVE, GC, ADD_HANDLER, REMOVE_HANDLER, OTHER }

    private final class Block {
        final int start; // index of first entry in the block
        final int end;   // index of END entry (inclusive)

        Block(final int start, final int end) {
            this.start = start;
            this.end = end;
        }

        BlockType type() {
            for (int k = start; k < end; k++) {
                final ServerToClientModel m = entries.get(k).model();
                if (m == ServerToClientModel.TYPE_CREATE) return BlockType.CREATE;
                if (m == ServerToClientModel.TYPE_UPDATE) return BlockType.UPDATE;
                if (m == ServerToClientModel.TYPE_ADD) return BlockType.ADD;
                if (m == ServerToClientModel.TYPE_REMOVE) return BlockType.REMOVE;
                if (m == ServerToClientModel.TYPE_GC) return BlockType.GC;
                if (m == ServerToClientModel.TYPE_ADD_HANDLER) return BlockType.ADD_HANDLER;
                if (m == ServerToClientModel.TYPE_REMOVE_HANDLER) return BlockType.REMOVE_HANDLER;
            }
            return BlockType.OTHER;
        }

        int objectId() {
            for (int k = start; k < end; k++) {
                final ServerToClientModel m = entries.get(k).model();
                if (m == ServerToClientModel.TYPE_CREATE || m == ServerToClientModel.TYPE_UPDATE
                        || m == ServerToClientModel.TYPE_ADD || m == ServerToClientModel.TYPE_REMOVE
                        || m == ServerToClientModel.TYPE_GC || m == ServerToClientModel.TYPE_ADD_HANDLER
                        || m == ServerToClientModel.TYPE_REMOVE_HANDLER) {
                    final Object v = entries.get(k).value();
                    return v instanceof Integer ? (int) v : -1;
                }
            }
            return -1;
        }
    }

    private void copyBlock(final Block b, final List<Entry> target) {
        for (int k = b.start; k <= b.end; k++) {
            target.add(entries.get(k));
        }
    }

    private void mergeUpdateBlocks(final List<Block> blocks, final int fromIdx, final int toIdx,
                                   final List<Entry> target) {
        final var merged = new java.util.LinkedHashMap<ServerToClientModel, Object>();
        int objectId = -1;
        Entry windowEntry = null;
        Entry frameEntry = null;

        for (int bi = fromIdx; bi < toIdx; bi++) {
            final Block b = blocks.get(bi);
            for (int k = b.start; k < b.end; k++) {
                final Entry e = entries.get(k);
                final ServerToClientModel m = e.model();
                if (m == ServerToClientModel.WINDOW_ID) { windowEntry = e; continue; }
                if (m == ServerToClientModel.FRAME_ID) { frameEntry = e; continue; }
                if (m == ServerToClientModel.TYPE_UPDATE) { objectId = (int) e.value(); continue; }
                merged.put(m, e.value());
            }
        }

        if (windowEntry != null) target.add(windowEntry);
        if (frameEntry != null) target.add(frameEntry);
        if (objectId >= 0) target.add(new Entry(ServerToClientModel.TYPE_UPDATE, objectId));
        for (final var entry : merged.entrySet()) {
            target.add(new Entry(entry.getKey(), entry.getValue()));
        }
        target.add(new Entry(ServerToClientModel.END, null));
    }
}
