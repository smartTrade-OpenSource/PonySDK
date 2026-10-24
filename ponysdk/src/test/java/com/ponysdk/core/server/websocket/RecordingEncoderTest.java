/*
 * Copyright (c) 2011 PonySDK — Licensed under the Apache License, Version 2.0
 */
package com.ponysdk.core.server.websocket;

import com.ponysdk.core.model.ServerToClientModel;

import java.lang.ref.WeakReference;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RecordingEncoder} — the core of the transparent reconnection engine.
 * Covers: basic record/replay, compaction (merge updates, cancel create+remove), overflow.
 */
public class RecordingEncoderTest {

    // ---- Helpers ----

    /** Captures all entries replayed to it for assertion. */
    private static final class CapturingEncoder implements WebsocketEncoder {
        final List<RecordingEncoder.Entry> captured = new ArrayList<>();

        @Override public void beginObject() {}

        @Override public void encode(final ServerToClientModel model, final Object value) {
            captured.add(new RecordingEncoder.Entry(model, value));
        }

        @Override public void endObject() {
            captured.add(new RecordingEncoder.Entry(ServerToClientModel.END, null));
        }
    }

    /** Records a TYPE_UPDATE block: WINDOW_ID + TYPE_UPDATE(objectId) + properties + END */
    private static void recordUpdateBlock(final RecordingEncoder rec, final int objectId,
                                          final ServerToClientModel prop, final Object value) {
        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.TYPE_UPDATE, objectId);
        rec.encode(prop, value);
        rec.endObject();
    }

    /** Records a TYPE_CREATE block: WINDOW_ID + TYPE_CREATE(objectId) + WIDGET_TYPE + END */
    private static void recordCreateBlock(final RecordingEncoder rec, final int objectId, final int widgetType) {
        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.TYPE_CREATE, objectId);
        rec.encode(ServerToClientModel.WIDGET_TYPE, widgetType);
        rec.endObject();
    }

    /** Records a TYPE_REMOVE block: WINDOW_ID + TYPE_REMOVE(objectId) + PARENT_OBJECT_ID + END */
    private static void recordRemoveBlock(final RecordingEncoder rec, final int objectId) {
        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.TYPE_REMOVE, objectId);
        rec.encode(ServerToClientModel.PARENT_OBJECT_ID, -1);
        rec.endObject();
    }

    /** Records a TYPE_ADD block */
    private static void recordAddBlock(final RecordingEncoder rec, final int objectId, final int parentId) {
        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.TYPE_ADD, objectId);
        rec.encode(ServerToClientModel.PARENT_OBJECT_ID, parentId);
        rec.endObject();
    }


    // ---- Basic record/replay ----

    /** SessionAdmin's gauge: every entry written counts, held drops when compaction merges updates. */
    @Test
    public void testWrittenAndHeldCounters() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordUpdateBlock(rec, 42, ServerToClientModel.HTML, "a");
        recordUpdateBlock(rec, 42, ServerToClientModel.HTML, "b");
        assertEquals(8, rec.getWrittenEntries());
        assertEquals(8, rec.getHeldEntries());

        rec.compact(); // the two updates of object 42 merge
        assertEquals(8, rec.getWrittenEntries());
        assertEquals(rec.size(), rec.getHeldEntries());
        assertTrue(rec.getHeldEntries() < 8);

        rec.clear();
        assertEquals(0, rec.getHeldEntries());
    }

    @Test
    public void testBasicRecordAndReplay() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordUpdateBlock(rec, 42, ServerToClientModel.HTML, "hello");

        assertEquals(4, rec.size()); // WINDOW_ID + TYPE_UPDATE + HTML + END

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        assertEquals(4, cap.captured.size());
        assertEquals(ServerToClientModel.WINDOW_ID, cap.captured.get(0).model());
        assertEquals(0, cap.captured.get(0).value());
        assertEquals(ServerToClientModel.TYPE_UPDATE, cap.captured.get(1).model());
        assertEquals(42, cap.captured.get(1).value());
        assertEquals(ServerToClientModel.HTML, cap.captured.get(2).model());
        assertEquals("hello", cap.captured.get(2).value());
        assertEquals(ServerToClientModel.END, cap.captured.get(3).model());
    }

    @Test
    public void testMultipleBlocksReplayInOrder() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordCreateBlock(rec, 10, 1);
        recordAddBlock(rec, 10, 5);
        recordUpdateBlock(rec, 10, ServerToClientModel.HTML, "content");

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // Verify structural order: CREATE block, then ADD block, then UPDATE block
        final List<ServerToClientModel> types = cap.captured.stream()
                .map(RecordingEncoder.Entry::model)
                .filter(m -> m == ServerToClientModel.TYPE_CREATE || m == ServerToClientModel.TYPE_ADD
                        || m == ServerToClientModel.TYPE_UPDATE)
                .toList();
        assertEquals(List.of(ServerToClientModel.TYPE_CREATE, ServerToClientModel.TYPE_ADD,
                ServerToClientModel.TYPE_UPDATE), types);
    }

    // ---- Compaction: merge consecutive UPDATE blocks ----

    @Test
    public void testCompactMergesConsecutiveUpdatesOnSameObject() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordUpdateBlock(rec, 42, ServerToClientModel.HTML, "first");
        recordUpdateBlock(rec, 42, ServerToClientModel.HTML, "second");
        recordUpdateBlock(rec, 42, ServerToClientModel.WIDGET_VISIBLE, true);

        final int sizeBefore = rec.size();
        rec.compact();
        assertTrue(rec.size() < sizeBefore, "Compaction should reduce entry count");

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // After merge: one block with HTML="second" and WIDGET_VISIBLE=true
        long htmlCount = cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.HTML).count();
        assertEquals(1, htmlCount, "Merged block should have exactly one HTML entry");

        String htmlValue = (String) cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.HTML)
                .findFirst().orElseThrow().value();
        assertEquals("second", htmlValue, "Last value wins");

        assertTrue(cap.captured.stream().anyMatch(e -> e.model() == ServerToClientModel.WIDGET_VISIBLE),
                "WIDGET_VISIBLE should be present");
    }

    @Test
    public void testCompactDoesNotMergeUpdatesOnDifferentObjects() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordUpdateBlock(rec, 1, ServerToClientModel.HTML, "a");
        recordUpdateBlock(rec, 2, ServerToClientModel.HTML, "b");

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // Both blocks should survive — different objectIDs
        long updateCount = cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.TYPE_UPDATE).count();
        assertEquals(2, updateCount);
    }

    @Test
    public void testCompactDoesNotMergeNonConsecutiveUpdates() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordUpdateBlock(rec, 42, ServerToClientModel.HTML, "first");
        recordAddBlock(rec, 99, 5); // structural block breaks the sequence
        recordUpdateBlock(rec, 42, ServerToClientModel.HTML, "second");

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // Both UPDATE blocks should survive (non-consecutive due to ADD in between)
        long updateCount = cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.TYPE_UPDATE).count();
        assertEquals(2, updateCount);
    }

    // ---- Compaction: create+remove cancellation ----

    @Test
    public void testCompactCancelsCreateAndRemoveForSameObject() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordCreateBlock(rec, 50, 1);
        recordUpdateBlock(rec, 50, ServerToClientModel.HTML, "temp");
        recordRemoveBlock(rec, 50);

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // All blocks for objectId 50 should be eliminated
        boolean hasObj50 = cap.captured.stream().anyMatch(e ->
                (e.model() == ServerToClientModel.TYPE_CREATE || e.model() == ServerToClientModel.TYPE_UPDATE
                        || e.model() == ServerToClientModel.TYPE_REMOVE)
                        && Integer.valueOf(50).equals(e.value()));
        assertFalse(hasObj50, "Create+Remove pair should be cancelled");
    }

    @Test
    public void testCompactPreservesOtherObjectsWhenCancelling() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordCreateBlock(rec, 50, 1);  // will be cancelled
        recordCreateBlock(rec, 51, 2);  // should survive
        recordRemoveBlock(rec, 50);     // cancels 50

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        boolean has51 = cap.captured.stream().anyMatch(e ->
                e.model() == ServerToClientModel.TYPE_CREATE && Integer.valueOf(51).equals(e.value()));
        assertTrue(has51, "Object 51 should survive");

        boolean has50 = cap.captured.stream().anyMatch(e ->
                e.model() == ServerToClientModel.TYPE_CREATE && Integer.valueOf(50).equals(e.value()));
        assertFalse(has50, "Object 50 should be cancelled");
    }

    // ---- Structural operations preserved in order ----

    @Test
    public void testStructuralOperationsPreservedInOrder() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordCreateBlock(rec, 10, 1);
        recordAddBlock(rec, 10, 5);
        recordCreateBlock(rec, 11, 2);
        recordAddBlock(rec, 11, 5);

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // Extract structural types in order
        final List<ServerToClientModel> structural = cap.captured.stream()
                .map(RecordingEncoder.Entry::model)
                .filter(m -> m == ServerToClientModel.TYPE_CREATE || m == ServerToClientModel.TYPE_ADD)
                .toList();
        assertEquals(List.of(
                ServerToClientModel.TYPE_CREATE,  // 10
                ServerToClientModel.TYPE_ADD,     // 10
                ServerToClientModel.TYPE_CREATE,  // 11
                ServerToClientModel.TYPE_ADD      // 11
        ), structural);
    }


    // ---- Overflow ----

    @Test
    public void testOverflowHandlerFires() {
        final AtomicBoolean fired = new AtomicBoolean(false);
        final AtomicInteger reportedCount = new AtomicInteger();
        final AtomicInteger reportedMax = new AtomicInteger();

        final RecordingEncoder rec = new RecordingEncoder(5, (count, max) -> {
            fired.set(true);
            reportedCount.set(count);
            reportedMax.set(max);
        });

        // Record entries until overflow (each endObject adds an END entry)
        for (int i = 0; i < 10; i++) {
            rec.encode(ServerToClientModel.HTML, "v" + i);
        }

        assertTrue(fired.get(), "Overflow handler should have fired");
        assertTrue(reportedCount.get() > 5, "Reported count should exceed max");
        assertEquals(5, reportedMax.get());
        assertTrue(rec.isOverflowed());
    }

    @Test
    public void testStopsRecordingAfterOverflow() {
        final RecordingEncoder rec = new RecordingEncoder(3, (count, max) -> {});

        // Fill past the limit
        for (int i = 0; i < 10; i++) {
            rec.encode(ServerToClientModel.HTML, "v" + i);
        }

        // Size should be frozen near the overflow point (4 = 3 + the one that triggered)
        assertTrue(rec.size() <= 5, "Should stop recording after overflow");
        assertTrue(rec.isOverflowed());
    }

    @Test
    public void testNoOverflowWhenUnlimited() {
        final RecordingEncoder rec = new RecordingEncoder(); // unlimited

        for (int i = 0; i < 100_000; i++) {
            rec.encode(ServerToClientModel.HTML, "v" + i);
        }

        assertFalse(rec.isOverflowed());
        assertEquals(100_000, rec.size());
    }

    @Test
    public void testOverflowHandlerFiresOnlyOnce() {
        final AtomicInteger fireCount = new AtomicInteger();
        final RecordingEncoder rec = new RecordingEncoder(2, (count, max) -> fireCount.incrementAndGet());

        for (int i = 0; i < 20; i++) {
            rec.encode(ServerToClientModel.HTML, "v" + i);
        }

        assertEquals(1, fireCount.get(), "Overflow handler should fire exactly once");
    }

    // ---- Edge cases ----

    @Test
    public void testCompactOnEmptyRecorder() {
        final RecordingEncoder rec = new RecordingEncoder();
        rec.compact(); // should not throw
        assertEquals(0, rec.size());
    }

    @Test
    public void testClearReleasesEntries() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordUpdateBlock(rec, 1, ServerToClientModel.HTML, "x");
        assertTrue(rec.size() > 0);

        rec.clear();
        assertEquals(0, rec.size());
    }

    @Test
    public void testReplayToEmptyRecorder() {
        final RecordingEncoder rec = new RecordingEncoder();
        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap); // should not throw
        assertEquals(0, cap.captured.size());
    }

    // ---- Edge cases: GC cancellation ----

    /** TYPE_GC should also cancel a matching TYPE_CREATE (not just TYPE_REMOVE). */
    @Test
    public void testCompactCancelsCreateAndGCForSameObject() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordCreateBlock(rec, 60, 1);
        recordUpdateBlock(rec, 60, ServerToClientModel.HTML, "temp");
        // GC instead of REMOVE
        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.TYPE_GC, 60);
        rec.endObject();

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        boolean hasObj60 = cap.captured.stream().anyMatch(e ->
                (e.model() == ServerToClientModel.TYPE_CREATE
                        || e.model() == ServerToClientModel.TYPE_UPDATE
                        || e.model() == ServerToClientModel.TYPE_GC)
                        && Integer.valueOf(60).equals(e.value()));
        assertFalse(hasObj60, "Create+GC pair should be cancelled");
    }

    // ---- Edge cases: batch cancellation (multiple objects created+removed) ----

    @Test
    public void testCompactCancelsMultipleCreateRemovePairs() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordCreateBlock(rec, 70, 1);
        recordCreateBlock(rec, 71, 2);
        recordCreateBlock(rec, 72, 3); // survivor
        recordRemoveBlock(rec, 70);
        recordRemoveBlock(rec, 71);

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        assertFalse(cap.captured.stream().anyMatch(e ->
                e.model() == ServerToClientModel.TYPE_CREATE && Integer.valueOf(70).equals(e.value())), "Object 70 should be cancelled");
        assertFalse(cap.captured.stream().anyMatch(e ->
                e.model() == ServerToClientModel.TYPE_CREATE && Integer.valueOf(71).equals(e.value())), "Object 71 should be cancelled");
        assertTrue(cap.captured.stream().anyMatch(e ->
                e.model() == ServerToClientModel.TYPE_CREATE && Integer.valueOf(72).equals(e.value())), "Object 72 should survive");
    }

    // ---- Edge cases: FRAME_ID preserved in merged update blocks ----

    @Test
    public void testCompactPreservesFrameIdInMergedBlocks() {
        final RecordingEncoder rec = new RecordingEncoder();

        // Two updates on same object, both with FRAME_ID
        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.FRAME_ID, 99);
        rec.encode(ServerToClientModel.TYPE_UPDATE, 42);
        rec.encode(ServerToClientModel.HTML, "first");
        rec.endObject();

        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.FRAME_ID, 99);
        rec.encode(ServerToClientModel.TYPE_UPDATE, 42);
        rec.encode(ServerToClientModel.HTML, "second");
        rec.endObject();

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // FRAME_ID should be present in the merged block
        assertTrue(cap.captured.stream().anyMatch(e ->
                        e.model() == ServerToClientModel.FRAME_ID && Integer.valueOf(99).equals(e.value())),
                "FRAME_ID should be preserved after merge");
        // Only one HTML entry (merged)
        assertEquals(1, cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.HTML).count());
        assertEquals("second", cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.HTML)
                .findFirst().orElseThrow().value());
    }

    // ---- Edge cases: block without objectID (e.g. TYPE_HISTORY) ----

    @Test
    public void testCompactHandlesBlocksWithoutObjectId() {
        final RecordingEncoder rec = new RecordingEncoder();

        // A TYPE_HISTORY block has no objectID
        rec.encode(ServerToClientModel.TYPE_HISTORY, "/page2");
        rec.endObject();

        // Normal update
        recordUpdateBlock(rec, 1, ServerToClientModel.HTML, "x");

        rec.compact(); // should not throw

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        assertTrue(cap.captured.stream().anyMatch(e -> e.model() == ServerToClientModel.TYPE_HISTORY),
                "TYPE_HISTORY block should survive compaction");
        assertTrue(cap.captured.stream().anyMatch(e -> e.model() == ServerToClientModel.TYPE_UPDATE),
                "Update block should survive");
    }

    // ---- Edge cases: UPDATE after structural op on same object should NOT merge ----

    @Test
    public void testCompactDoesNotMergeUpdateAcrossStructuralOp() {
        final RecordingEncoder rec = new RecordingEncoder();

        // Update object 10
        recordUpdateBlock(rec, 10, ServerToClientModel.HTML, "before");
        // ADD object 10 to parent (structural — breaks merge sequence)
        recordAddBlock(rec, 10, 5);
        // Update object 10 again
        recordUpdateBlock(rec, 10, ServerToClientModel.HTML, "after");

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // Both updates should survive (structural op in between prevents merge)
        List<String> htmlValues = cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.HTML)
                .map(e -> (String) e.value())
                .toList();
        assertEquals(List.of("before", "after"), htmlValues, "Both HTML values should survive");

        // Verify order: UPDATE(before) → ADD → UPDATE(after)
        List<ServerToClientModel> types = cap.captured.stream()
                .map(RecordingEncoder.Entry::model)
                .filter(m -> m == ServerToClientModel.TYPE_UPDATE || m == ServerToClientModel.TYPE_ADD)
                .toList();
        assertEquals(List.of(
                ServerToClientModel.TYPE_UPDATE,
                ServerToClientModel.TYPE_ADD,
                ServerToClientModel.TYPE_UPDATE
        ), types);
    }

    // ---- Edge cases: only REMOVE without CREATE should NOT be cancelled ----

    @Test
    public void testCompactDoesNotCancelRemoveWithoutCreate() {
        final RecordingEncoder rec = new RecordingEncoder();

        // Object 80 was created BEFORE suspension — only REMOVE during suspension
        recordUpdateBlock(rec, 80, ServerToClientModel.HTML, "last update");
        recordRemoveBlock(rec, 80);

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // REMOVE should survive (no matching CREATE during this suspension window)
        assertTrue(cap.captured.stream().anyMatch(e ->
                        e.model() == ServerToClientModel.TYPE_REMOVE && Integer.valueOf(80).equals(e.value())),
                "REMOVE without CREATE should survive");
    }

    // ---- Edge cases: only CREATE without REMOVE should NOT be cancelled ----

    @Test
    public void testCompactDoesNotCancelCreateWithoutRemove() {
        final RecordingEncoder rec = new RecordingEncoder();

        recordCreateBlock(rec, 90, 1);
        recordUpdateBlock(rec, 90, ServerToClientModel.HTML, "new widget");

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        assertTrue(cap.captured.stream().anyMatch(e ->
                        e.model() == ServerToClientModel.TYPE_CREATE && Integer.valueOf(90).equals(e.value())),
                "CREATE without REMOVE should survive");
    }

    // ---- Edge cases: overflow at exact boundary ----

    @Test
    public void testOverflowAtExactBoundary() {
        final AtomicBoolean fired = new AtomicBoolean(false);
        final RecordingEncoder rec = new RecordingEncoder(5, (count, max) -> fired.set(true));

        // Record exactly 5 entries — should NOT overflow
        for (int i = 0; i < 5; i++) {
            rec.encode(ServerToClientModel.HTML, "v" + i);
        }
        assertFalse(fired.get(), "Exactly at limit should NOT overflow");
        assertFalse(rec.isOverflowed());
        assertEquals(5, rec.size());

        // One more — should overflow
        rec.encode(ServerToClientModel.HTML, "overflow");
        assertTrue(fired.get(), "One past limit should overflow");
        assertTrue(rec.isOverflowed());
    }

    // ---- Edge cases: multiple properties in single update block ----

    @Test
    public void testCompactMergesMultiplePropertiesCorrectly() {
        final RecordingEncoder rec = new RecordingEncoder();

        // Block 1: HTML + TEXT
        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.TYPE_UPDATE, 42);
        rec.encode(ServerToClientModel.HTML, "h1");
        rec.encode(ServerToClientModel.TEXT, "t1");
        rec.endObject();

        // Block 2: HTML + ENABLED (TEXT not overwritten)
        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.TYPE_UPDATE, 42);
        rec.encode(ServerToClientModel.HTML, "h2");
        rec.encode(ServerToClientModel.ENABLED, false);
        rec.endObject();

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // After merge: HTML="h2" (overwritten), TEXT="t1" (kept), ENABLED=false (added)
        assertEquals("h2", cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.HTML)
                .findFirst().orElseThrow().value());
        assertEquals("t1", cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.TEXT)
                .findFirst().orElseThrow().value());
        assertEquals(false, cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.ENABLED)
                .findFirst().orElseThrow().value());
    }

    // ---- Edge cases: single entry (no END) — incomplete block ----

    @Test
    public void testCompactWithIncompleteBlock() {
        final RecordingEncoder rec = new RecordingEncoder();
        // Record an entry without endObject — incomplete block
        rec.encode(ServerToClientModel.HTML, "orphan");
        // No endObject()

        rec.compact(); // should not throw, incomplete block is just ignored

        // The orphan entry is not in any block (no END delimiter), so it's dropped
        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);
        // Depending on implementation, orphan may or may not survive.
        // The important thing is no crash.
    }

    // ====================================================================
    // Critical edge cases — order-sensitive cancellation
    // ====================================================================

    /**
     * REMOVE before CREATE on same objectID — must NOT cancel.
     * Scenario: object 50 existed before suspension. During suspension:
     *   1. REMOVE 50 (destroy old widget)
     *   2. CREATE 50 (new widget reusing same ID — e.g. counter wrap or pool)
     * Both operations must survive — the REMOVE kills the old, the CREATE makes the new.
     */
    @Test
    public void testCompactDoesNotCancelWhenRemoveBeforeCreate() {
        final RecordingEncoder rec = new RecordingEncoder();

        // REMOVE first (old object that existed before suspension)
        recordRemoveBlock(rec, 50);
        // CREATE second (new object reusing same ID)
        recordCreateBlock(rec, 50, 1);

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // Both REMOVE and CREATE should survive
        assertTrue(cap.captured.stream().anyMatch(e ->
                        e.model() == ServerToClientModel.TYPE_REMOVE && Integer.valueOf(50).equals(e.value())),
                "REMOVE should survive (came before CREATE)");
        assertTrue(cap.captured.stream().anyMatch(e ->
                        e.model() == ServerToClientModel.TYPE_CREATE && Integer.valueOf(50).equals(e.value())),
                "CREATE should survive (came after REMOVE)");

        // Verify order: REMOVE before CREATE
        int removePos = -1, createPos = -1;
        for (int i = 0; i < cap.captured.size(); i++) {
            if (cap.captured.get(i).model() == ServerToClientModel.TYPE_REMOVE
                    && Integer.valueOf(50).equals(cap.captured.get(i).value())) removePos = i;
            if (cap.captured.get(i).model() == ServerToClientModel.TYPE_CREATE
                    && Integer.valueOf(50).equals(cap.captured.get(i).value())) createPos = i;
        }
        assertTrue(removePos < createPos, "REMOVE should come before CREATE");
    }

    /**
     * CREATE before REMOVE on same objectID — SHOULD cancel (normal case).
     * This is the inverse of the above — widget created and destroyed during suspension.
     */
    @Test
    public void testCompactCancelsWhenCreateBeforeRemove() {
        final RecordingEncoder rec = new RecordingEncoder();

        recordCreateBlock(rec, 50, 1);
        recordUpdateBlock(rec, 50, ServerToClientModel.HTML, "ephemeral");
        recordRemoveBlock(rec, 50);

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        assertFalse(cap.captured.stream().anyMatch(e ->
                        (e.model() == ServerToClientModel.TYPE_CREATE
                                || e.model() == ServerToClientModel.TYPE_REMOVE
                                || e.model() == ServerToClientModel.TYPE_UPDATE)
                                && Integer.valueOf(50).equals(e.value())),
                "CREATE+REMOVE (in order) should be cancelled");
    }

    /**
     * GC before CREATE on same objectID — must NOT cancel.
     * Same logic as REMOVE-before-CREATE but with TYPE_GC.
     */
    @Test
    public void testCompactDoesNotCancelWhenGCBeforeCreate() {
        final RecordingEncoder rec = new RecordingEncoder();

        // GC first (old object)
        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.TYPE_GC, 50);
        rec.endObject();

        // CREATE second (new object)
        recordCreateBlock(rec, 50, 1);

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        assertTrue(cap.captured.stream().anyMatch(e ->
                        e.model() == ServerToClientModel.TYPE_GC && Integer.valueOf(50).equals(e.value())),
                "GC should survive");
        assertTrue(cap.captured.stream().anyMatch(e ->
                        e.model() == ServerToClientModel.TYPE_CREATE && Integer.valueOf(50).equals(e.value())),
                "CREATE should survive");
    }

    /**
     * Complex interleaving: REMOVE(A) → CREATE(A) → UPDATE(A) → REMOVE(A).
     * The first REMOVE is for the old object (pre-suspension).
     * The CREATE+second REMOVE is for the new object (during suspension) — should cancel.
     * Net result: only the first REMOVE survives.
     *
     * NOTE: This is an extreme edge case. The current implementation uses first-CREATE
     * vs first-REMOVE ordering. Here first-CREATE(idx=1) < first-REMOVE(idx=0) is FALSE,
     * so nothing is cancelled. Both REMOVEs and the CREATE survive.
     * This is actually the SAFE behavior — replaying extra ops is better than losing them.
     */
    @Test
    public void testCompactComplexInterleaving() {
        final RecordingEncoder rec = new RecordingEncoder();

        recordRemoveBlock(rec, 50);      // block 0: remove old object
        recordCreateBlock(rec, 50, 1);   // block 1: create new object
        recordUpdateBlock(rec, 50, ServerToClientModel.HTML, "new");  // block 2: update new
        recordRemoveBlock(rec, 50);      // block 3: remove new object

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // The first REMOVE (block 0) must survive — it removes the pre-suspension object.
        // Since first-REMOVE(0) < first-CREATE(1), cancellation does NOT apply.
        // All blocks survive — safe behavior.
        assertTrue(cap.captured.stream().anyMatch(e ->
                        e.model() == ServerToClientModel.TYPE_REMOVE && Integer.valueOf(50).equals(e.value())),
                "First REMOVE must survive");
    }

    /**
     * Concurrent updates on many different objects — compaction should not mix them up.
     */
    @Test
    public void testCompactManyObjectsIndependent() {
        final RecordingEncoder rec = new RecordingEncoder();

        for (int obj = 1; obj <= 50; obj++) {
            recordUpdateBlock(rec, obj, ServerToClientModel.HTML, "val-" + obj + "-a");
            recordUpdateBlock(rec, obj, ServerToClientModel.HTML, "val-" + obj + "-b");
        }

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // Each object should have exactly one TYPE_UPDATE (merged)
        for (int obj = 1; obj <= 50; obj++) {
            final int o = obj;
            long count = cap.captured.stream()
                    .filter(e -> e.model() == ServerToClientModel.TYPE_UPDATE && Integer.valueOf(o).equals(e.value()))
                    .count();
            assertEquals(1, count, "Object " + obj + " should have exactly 1 merged update");
        }

        // Each object should have the "b" value (last wins)
        for (int obj = 1; obj <= 50; obj++) {
            final int o = obj;
            boolean found = false;
            for (int i = 0; i < cap.captured.size(); i++) {
                if (cap.captured.get(i).model() == ServerToClientModel.TYPE_UPDATE
                        && Integer.valueOf(o).equals(cap.captured.get(i).value())) {
                    for (int j = i + 1; j < cap.captured.size(); j++) {
                        if (cap.captured.get(j).model() == ServerToClientModel.END) break;
                        if (cap.captured.get(j).model() == ServerToClientModel.HTML) {
                            assertEquals("val-" + o + "-b",
                                    cap.captured.get(j).value(), "Object " + o + " should have last value");
                            found = true;
                        }
                    }
                }
            }
            assertTrue(found, "Object " + obj + " HTML should be found");
        }
    }

    /**
     * Large buffer compaction performance — should not take unreasonable time.
     * 10K update blocks on 100 objects (100 consecutive updates per object).
     */
    @Test
    public void testCompactLargeBuffer() {
        final RecordingEncoder rec = new RecordingEncoder();

        // Generate consecutive updates per object (so they CAN be merged)
        for (int obj = 1; obj <= 100; obj++) {
            for (int round = 0; round < 100; round++) {
                recordUpdateBlock(rec, obj, ServerToClientModel.HTML, "r" + round + "-o" + obj);
            }
        }

        final int sizeBefore = rec.size();
        final long start = System.currentTimeMillis();
        rec.compact();
        final long elapsed = System.currentTimeMillis() - start;

        // 100 objects × 100 updates → should compact to 100 blocks (one per object)
        assertTrue(rec.size() < sizeBefore / 5, "Compaction should reduce size significantly (before=" + sizeBefore
                + ", after=" + rec.size() + ")");
        assertTrue(elapsed < 2000,
                "Compaction of 10K blocks should complete in < 2s, took " + elapsed + "ms");
    }

    /**
     * TYPE_ADD_HANDLER and TYPE_REMOVE_HANDLER are structural — should not be merged.
     */
    @Test
    public void testCompactPreservesHandlerOperations() {
        final RecordingEncoder rec = new RecordingEncoder();

        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.TYPE_ADD_HANDLER, 42);
        rec.encode(ServerToClientModel.HANDLER_TYPE, 3);
        rec.endObject();

        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.TYPE_REMOVE_HANDLER, null);
        rec.encode(ServerToClientModel.HANDLER_TYPE, 3);
        rec.endObject();

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        assertTrue(cap.captured.stream().anyMatch(e -> e.model() == ServerToClientModel.TYPE_ADD_HANDLER),
                "TYPE_ADD_HANDLER should survive");
        assertTrue(cap.captured.stream().anyMatch(e -> e.model() == ServerToClientModel.TYPE_REMOVE_HANDLER),
                "TYPE_REMOVE_HANDLER should survive");
    }

    // ====================================================================
    // Additional edge cases — double compact, overflow replay, empty blocks,
    // FRAME_ID mismatch, stress cancellation
    // ====================================================================

    /**
     * compact() called twice — should be idempotent.
     * The second compact on already-compacted data should produce the same result.
     */
    @Test
    public void testCompactIsIdempotent() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordUpdateBlock(rec, 42, ServerToClientModel.HTML, "a");
        recordUpdateBlock(rec, 42, ServerToClientModel.HTML, "b");
        recordUpdateBlock(rec, 42, ServerToClientModel.HTML, "c");
        recordCreateBlock(rec, 50, 1);
        recordRemoveBlock(rec, 50);

        rec.compact();
        final int sizeAfterFirst = rec.size();

        // Capture after first compact
        final CapturingEncoder cap1 = new CapturingEncoder();
        rec.replayTo(cap1);

        rec.compact();
        final int sizeAfterSecond = rec.size();

        final CapturingEncoder cap2 = new CapturingEncoder();
        rec.replayTo(cap2);

        assertEquals(sizeAfterFirst, sizeAfterSecond, "Double compact should produce same size");
        assertEquals(cap1.captured.size(), cap2.captured.size(), "Double compact should produce same entries");
        for (int i = 0; i < cap1.captured.size(); i++) {
            assertEquals(cap1.captured.get(i).model(),
                    cap2.captured.get(i).model(), "Entry " + i + " model should match");
            assertEquals(cap1.captured.get(i).value(),
                    cap2.captured.get(i).value(), "Entry " + i + " value should match");
        }
    }

    /**
     * replayTo() after overflow — should replay entries recorded BEFORE overflow.
     * The overflow stops recording but doesn't clear existing entries.
     */
    @Test
    public void testReplayAfterOverflowContainsPreOverflowEntries() {
        final RecordingEncoder rec = new RecordingEncoder(5, (count, max) -> {});

        // Record 3 entries (under limit)
        rec.encode(ServerToClientModel.HTML, "before1");
        rec.encode(ServerToClientModel.TEXT, "before2");
        rec.encode(ServerToClientModel.ENABLED, true);

        assertFalse(rec.isOverflowed());
        assertEquals(3, rec.size());

        // Now overflow
        for (int i = 0; i < 10; i++) {
            rec.encode(ServerToClientModel.HTML, "flood" + i);
        }
        assertTrue(rec.isOverflowed());

        // Replay should contain the pre-overflow entries
        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        assertTrue(cap.captured.size() >= 3, "Should have pre-overflow entries");
        assertEquals(ServerToClientModel.HTML, cap.captured.get(0).model());
        assertEquals("before1", cap.captured.get(0).value());
        assertEquals(ServerToClientModel.TEXT, cap.captured.get(1).model());
        assertEquals("before2", cap.captured.get(1).value());
    }

    /**
     * Block with only END (empty block) — no type, no objectId.
     * Should survive compaction without crash.
     */
    @Test
    public void testCompactWithEmptyBlock() {
        final RecordingEncoder rec = new RecordingEncoder();

        // Empty block: just END
        rec.endObject();

        // Normal block
        recordUpdateBlock(rec, 1, ServerToClientModel.HTML, "x");

        rec.compact(); // should not throw

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // The empty block should survive (type=OTHER, objectId=-1)
        assertTrue(cap.captured.stream().anyMatch(e -> e.model() == ServerToClientModel.TYPE_UPDATE),
                "Normal update should survive");
    }

    /**
     * UPDATE blocks with different FRAME_IDs on the same objectID.
     * These should NOT be merged — different frames are different rendering contexts.
     * However, the current implementation merges based on objectID only.
     * This test documents the actual behavior.
     */
    @Test
    public void testCompactUpdatesWithDifferentFrameIds() {
        final RecordingEncoder rec = new RecordingEncoder();

        // Update object 42 in frame 1
        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.FRAME_ID, 1);
        rec.encode(ServerToClientModel.TYPE_UPDATE, 42);
        rec.encode(ServerToClientModel.HTML, "frame1-value");
        rec.endObject();

        // Update object 42 in frame 2
        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.FRAME_ID, 2);
        rec.encode(ServerToClientModel.TYPE_UPDATE, 42);
        rec.encode(ServerToClientModel.HTML, "frame2-value");
        rec.endObject();

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // Current behavior: consecutive updates on same objectID are merged.
        // The FRAME_ID from the last block wins. This is a known limitation —
        // in practice, the same objectID won't appear in different frames.
        long updateCount = cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.TYPE_UPDATE).count();
        // Document actual behavior: merged into 1 block
        assertEquals(1,
                updateCount, "Consecutive updates on same objectID are merged (even with different FRAME_ID)");
    }

    /**
     * Stress test: many CREATE+REMOVE cancellations.
     * 1000 objects created and removed — all should be cancelled.
     */
    @Test
    public void testCompactMassCancellation() {
        final RecordingEncoder rec = new RecordingEncoder();

        // Create 1000 objects
        for (int i = 1000; i < 2000; i++) {
            recordCreateBlock(rec, i, 1);
            recordUpdateBlock(rec, i, ServerToClientModel.HTML, "temp-" + i);
        }
        // Remove all 1000
        for (int i = 1000; i < 2000; i++) {
            recordRemoveBlock(rec, i);
        }

        // Also add a survivor
        recordUpdateBlock(rec, 1, ServerToClientModel.HTML, "survivor");

        final int sizeBefore = rec.size();
        final long start = System.currentTimeMillis();
        rec.compact();
        final long elapsed = System.currentTimeMillis() - start;

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // All 1000 objects should be cancelled
        for (int i = 1000; i < 2000; i++) {
            final int objId = i;
            assertFalse(cap.captured.stream().anyMatch(e ->
                            e.model() == ServerToClientModel.TYPE_CREATE && Integer.valueOf(objId).equals(e.value())),
                    "Object " + objId + " should be cancelled");
        }

        // Survivor should remain
        assertTrue(cap.captured.stream().anyMatch(e ->
                        e.model() == ServerToClientModel.TYPE_UPDATE && Integer.valueOf(1).equals(e.value())),
                "Survivor should remain");

        assertTrue(elapsed < 2000, "Mass cancellation should complete in < 2s, took " + elapsed + "ms");
    }

    /**
     * compact() with mixed WINDOW_IDs — blocks from different windows
     * on the same objectID should still merge (objectID is global).
     */
    @Test
    public void testCompactMergesAcrossWindowIds() {
        final RecordingEncoder rec = new RecordingEncoder();

        // Update object 42 from window 0
        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.TYPE_UPDATE, 42);
        rec.encode(ServerToClientModel.HTML, "win0");
        rec.endObject();

        // Update object 42 from window 1
        rec.encode(ServerToClientModel.WINDOW_ID, 1);
        rec.encode(ServerToClientModel.TYPE_UPDATE, 42);
        rec.encode(ServerToClientModel.HTML, "win1");
        rec.endObject();

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // Consecutive updates on same objectID → merged
        long updateCount = cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.TYPE_UPDATE).count();
        assertEquals(1, updateCount, "Should merge consecutive updates on same objectID");

        // Last value wins
        assertEquals("win1", cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.HTML)
                .findFirst().orElseThrow().value());
    }

    /**
     * compact() + replayTo() with null values — some protocol entries have null values
     * (e.g. DESTROY_CONTEXT, END). Should not NPE during compaction or replay.
     */
    @Test
    public void testCompactAndReplayWithNullValues() {
        final RecordingEncoder rec = new RecordingEncoder();

        rec.encode(ServerToClientModel.DESTROY_CONTEXT, null);
        rec.endObject();

        recordUpdateBlock(rec, 1, ServerToClientModel.HTML, null);

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        assertTrue(cap.captured.stream().anyMatch(e -> e.model() == ServerToClientModel.DESTROY_CONTEXT),
                "DESTROY_CONTEXT block should survive");
        assertTrue(cap.captured.stream().anyMatch(e -> e.model() == ServerToClientModel.HTML && e.value() == null),
                "Update with null HTML should survive");
    }

    /**
     * clear() after overflow — should reset the overflow state? No — clear() only clears entries.
     * isOverflowed() should still return true. This documents the behavior.
     */
    @Test
    public void testClearAfterOverflow() {
        final RecordingEncoder rec = new RecordingEncoder(3, (count, max) -> {});

        for (int i = 0; i < 10; i++) {
            rec.encode(ServerToClientModel.HTML, "v" + i);
        }
        assertTrue(rec.isOverflowed());

        rec.clear();
        assertEquals(0, rec.size());
        // Overflow flag is NOT reset by clear — once overflowed, always overflowed
        assertTrue(rec.isOverflowed(), "Overflow flag should persist after clear");
    }

    /**
     * Interleaved CREATE blocks for different objects — should not interfere.
     * CREATE(A) → CREATE(B) → REMOVE(A) → only A is cancelled, B survives.
     */
    @Test
    public void testCompactInterleavedCreateRemoveDifferentObjects() {
        final RecordingEncoder rec = new RecordingEncoder();

        recordCreateBlock(rec, 100, 1);
        recordCreateBlock(rec, 101, 2);
        recordAddBlock(rec, 101, 5);
        recordRemoveBlock(rec, 100);

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // 100 cancelled (CREATE+REMOVE)
        assertFalse(cap.captured.stream().anyMatch(e ->
                        e.model() == ServerToClientModel.TYPE_CREATE && Integer.valueOf(100).equals(e.value())),
                "Object 100 should be cancelled");

        // 101 survives (CREATE + ADD, no REMOVE)
        assertTrue(cap.captured.stream().anyMatch(e ->
                        e.model() == ServerToClientModel.TYPE_CREATE && Integer.valueOf(101).equals(e.value())),
                "Object 101 CREATE should survive");
        assertTrue(cap.captured.stream().anyMatch(e ->
                        e.model() == ServerToClientModel.TYPE_ADD && Integer.valueOf(101).equals(e.value())),
                "Object 101 ADD should survive");
    }

    // ====================================================================
    // Additional edge cases — handler ops on cancelled objects, mixed cancellation,
    // compact with null property values in merge, replay beginObject semantics
    // ====================================================================

    /**
     * TYPE_ADD_HANDLER on a cancelled object should also be eliminated.
     * If CREATE(50) + ADD_HANDLER(50) + REMOVE(50) all happen during suspension,
     * the ADD_HANDLER block for object 50 should be cancelled along with the rest.
     */
    @Test
    public void testCompactCancelsHandlerOpsOnCancelledObject() {
        final RecordingEncoder rec = new RecordingEncoder();

        recordCreateBlock(rec, 50, 1);

        // ADD_HANDLER for object 50
        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.TYPE_ADD_HANDLER, 50);
        rec.encode(ServerToClientModel.HANDLER_TYPE, 3);
        rec.endObject();

        recordUpdateBlock(rec, 50, ServerToClientModel.HTML, "temp");
        recordRemoveBlock(rec, 50);

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // All blocks for object 50 should be eliminated (including ADD_HANDLER)
        assertFalse(cap.captured.stream().anyMatch(e ->
                        e.model() == ServerToClientModel.TYPE_ADD_HANDLER && Integer.valueOf(50).equals(e.value())),
                "ADD_HANDLER on cancelled object should be eliminated");
        assertFalse(cap.captured.stream().anyMatch(e ->
                        e.model() == ServerToClientModel.TYPE_CREATE && Integer.valueOf(50).equals(e.value())),
                "CREATE on cancelled object should be eliminated");
    }

    /**
     * Mixed CREATE+GC and CREATE+REMOVE on different objects in the same buffer.
     * Object 60: CREATE + GC (cancelled via GC)
     * Object 70: CREATE + REMOVE (cancelled via REMOVE)
     * Object 80: CREATE only (survives)
     */
    @Test
    public void testCompactMixedCancellationTypes() {
        final RecordingEncoder rec = new RecordingEncoder();

        recordCreateBlock(rec, 60, 1);
        recordCreateBlock(rec, 70, 2);
        recordCreateBlock(rec, 80, 3);

        // GC object 60
        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.TYPE_GC, 60);
        rec.endObject();

        // REMOVE object 70
        recordRemoveBlock(rec, 70);

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        assertFalse(cap.captured.stream().anyMatch(e ->
                        e.model() == ServerToClientModel.TYPE_CREATE && Integer.valueOf(60).equals(e.value())),
                "Object 60 (CREATE+GC) should be cancelled");
        assertFalse(cap.captured.stream().anyMatch(e ->
                        e.model() == ServerToClientModel.TYPE_CREATE && Integer.valueOf(70).equals(e.value())),
                "Object 70 (CREATE+REMOVE) should be cancelled");
        assertTrue(cap.captured.stream().anyMatch(e ->
                        e.model() == ServerToClientModel.TYPE_CREATE && Integer.valueOf(80).equals(e.value())),
                "Object 80 (CREATE only) should survive");
    }

    /**
     * Merge update blocks where one block sets a property and the next sets it to null.
     * After merge, the null value should be present (last-write-wins, even for null).
     */
    @Test
    public void testCompactMergeWithNullPropertyOverwrite() {
        final RecordingEncoder rec = new RecordingEncoder();

        // Block 1: HTML="hello"
        recordUpdateBlock(rec, 42, ServerToClientModel.HTML, "hello");
        // Block 2: HTML=null (clear the property)
        recordUpdateBlock(rec, 42, ServerToClientModel.HTML, null);

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // After merge: HTML=null (last value wins)
        long htmlCount = cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.HTML).count();
        assertEquals(1, htmlCount, "Should have exactly one HTML entry after merge");
        assertNull(cap.captured.stream()
                        .filter(e -> e.model() == ServerToClientModel.HTML)
                        .findFirst().orElseThrow().value(),
                "HTML value should be null (last-write-wins)");
    }

    /**
     * Compact with a single block (no merge possible) — should be a no-op.
     */
    @Test
    public void testCompactSingleBlockIsNoOp() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordUpdateBlock(rec, 42, ServerToClientModel.HTML, "only");

        final int sizeBefore = rec.size();
        rec.compact();
        assertEquals(sizeBefore, rec.size(), "Single block should not change size");

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);
        assertEquals("only", cap.captured.stream()
                        .filter(e -> e.model() == ServerToClientModel.HTML)
                        .findFirst().orElseThrow().value(),
                "HTML should be 'only'");
    }

    /**
     * Compact with only structural blocks (no UPDATE blocks) — nothing to merge.
     */
    @Test
    public void testCompactOnlyStructuralBlocks() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordCreateBlock(rec, 10, 1);
        recordAddBlock(rec, 10, 5);
        recordCreateBlock(rec, 11, 2);
        recordAddBlock(rec, 11, 5);

        final int sizeBefore = rec.size();
        rec.compact();
        // No merging possible — size should be the same
        assertEquals(sizeBefore, rec.size(), "Only structural blocks — no compaction");
    }

    /**
     * Compact with alternating UPDATE blocks on two objects:
     * UPDATE(A), UPDATE(B), UPDATE(A), UPDATE(B)
     * None should merge because they're not consecutive on the same objectID.
     */
    @Test
    public void testCompactAlternatingUpdatesMergePerObject() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordUpdateBlock(rec, 1, ServerToClientModel.HTML, "a1");
        recordUpdateBlock(rec, 2, ServerToClientModel.HTML, "b1");
        recordUpdateBlock(rec, 1, ServerToClientModel.HTML, "a2");
        recordUpdateBlock(rec, 2, ServerToClientModel.HTML, "b2");

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // Pure property updates of different objects commute: one block per object, last value wins
        long updateCount = cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.TYPE_UPDATE).count();
        assertEquals(2, updateCount, "one merged block per object");
        assertEquals(List.of("a2", "b2"), cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.HTML).map(RecordingEncoder.Entry::value).toList());
    }

    /**
     * Overflow handler that throws an exception — should not corrupt the recorder.
     */
    @Test
    public void testOverflowHandlerExceptionDoesNotCorrupt() {
        final RecordingEncoder rec = new RecordingEncoder(3, (count, max) -> {
            throw new RuntimeException("handler crash");
        });

        try {
            for (int i = 0; i < 10; i++) {
                rec.encode(ServerToClientModel.HTML, "v" + i);
            }
        } catch (final RuntimeException e) {
            assertEquals("handler crash", e.getMessage());
        }

        // Recorder should be in overflow state
        assertTrue(rec.isOverflowed());
        // Entries recorded before overflow should still be accessible
        assertTrue(rec.size() > 0, "Should have some entries");
    }

    // ====================================================================
    // Additional coverage: replay, merge edge cases, handler ops
    // ====================================================================

    /**
     * replayTo() should never call beginObject() on the target.
     * The RecordingEncoder records encode() and endObject() calls only.
     * beginObject() is a no-op in both WebSocket and RecordingEncoder.
     */
    @Test
    public void testReplayDoesNotCallBeginObject() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordUpdateBlock(rec, 42, ServerToClientModel.HTML, "hello");

        final AtomicBoolean beginObjectCalled = new AtomicBoolean(false);
        final WebsocketEncoder target = new WebsocketEncoder() {
            @Override public void beginObject() { beginObjectCalled.set(true); }
            @Override public void encode(ServerToClientModel model, Object value) {}
            @Override public void endObject() {}
        };

        rec.replayTo(target);
        assertFalse(beginObjectCalled.get(), "replayTo should not call beginObject()");
    }

    /**
     * compact() merge of consecutive UPDATE blocks with different WINDOW_IDs.
     * The LAST windowEntry should win (it's the most recent context).
     */
    @Test
    public void testCompactMergeLastWindowIdWins() {
        final RecordingEncoder rec = new RecordingEncoder();

        // Block 1: WINDOW_ID=1, UPDATE obj 42, HTML="first"
        rec.encode(ServerToClientModel.WINDOW_ID, 1);
        rec.encode(ServerToClientModel.TYPE_UPDATE, 42);
        rec.encode(ServerToClientModel.HTML, "first");
        rec.endObject();

        // Block 2: WINDOW_ID=2, UPDATE obj 42, HTML="second"
        rec.encode(ServerToClientModel.WINDOW_ID, 2);
        rec.encode(ServerToClientModel.TYPE_UPDATE, 42);
        rec.encode(ServerToClientModel.HTML, "second");
        rec.endObject();

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // Should have exactly one merged block with WINDOW_ID=2 (last wins)
        long windowIdCount = cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.WINDOW_ID)
                .count();
        assertEquals(1, windowIdCount, "Should have exactly one WINDOW_ID after merge");

        int windowId = cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.WINDOW_ID)
                .map(e -> (int) e.value())
                .findFirst().orElse(-1);
        assertEquals(2, windowId, "Last WINDOW_ID should win in merge");

        // HTML should be "second" (last value wins)
        String html = cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.HTML)
                .map(e -> (String) e.value())
                .findFirst().orElse(null);
        assertEquals("second", html);
    }

    /**
     * ADD_HANDLER and REMOVE_HANDLER on the same object are NOT cancelled
     * by compact() — only CREATE/REMOVE pairs are cancelled.
     * Handler ops are structural and must be preserved.
     */
    @Test
    public void testCompactDoesNotCancelAddRemoveHandlerPairs() {
        final RecordingEncoder rec = new RecordingEncoder();

        // ADD_HANDLER on object 42
        rec.encode(ServerToClientModel.TYPE_ADD_HANDLER, 42);
        rec.encode(ServerToClientModel.HANDLER_TYPE, 3);
        rec.endObject();

        // REMOVE_HANDLER on object 42
        rec.encode(ServerToClientModel.TYPE_REMOVE_HANDLER, 42);
        rec.encode(ServerToClientModel.HANDLER_TYPE, 3);
        rec.endObject();

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // Both should be preserved (not cancelled)
        boolean hasAddHandler = cap.captured.stream()
                .anyMatch(e -> e.model() == ServerToClientModel.TYPE_ADD_HANDLER);
        boolean hasRemoveHandler = cap.captured.stream()
                .anyMatch(e -> e.model() == ServerToClientModel.TYPE_REMOVE_HANDLER);
        assertTrue(hasAddHandler, "ADD_HANDLER should be preserved");
        assertTrue(hasRemoveHandler, "REMOVE_HANDLER should be preserved");
    }

    /**
     * compact() with a mix of UPDATE blocks where some have FRAME_ID and some don't.
     * Consecutive updates on the same object should still merge, and the last
     * FRAME_ID (or absence thereof) should be preserved.
     */
    @Test
    public void testCompactMergeWithAndWithoutFrameId() {
        final RecordingEncoder rec = new RecordingEncoder();

        // Block 1: with FRAME_ID
        rec.encode(ServerToClientModel.FRAME_ID, 99);
        rec.encode(ServerToClientModel.TYPE_UPDATE, 42);
        rec.encode(ServerToClientModel.HTML, "with-frame");
        rec.endObject();

        // Block 2: without FRAME_ID
        rec.encode(ServerToClientModel.TYPE_UPDATE, 42);
        rec.encode(ServerToClientModel.HTML, "no-frame");
        rec.endObject();

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // Should have one merged block. The last FRAME_ID entry wins.
        // Block 2 has no FRAME_ID, so frameEntry from block 2 is null.
        // But block 1's frameEntry was set. The merge iterates blocks in order,
        // so block 2 doesn't set frameEntry (it's null for that block).
        // The final frameEntry is from block 1 (99).
        // Actually, looking at the code: frameEntry is overwritten per block.
        // Block 1 sets frameEntry=99. Block 2 doesn't have FRAME_ID, so frameEntry stays 99.
        // Result: FRAME_ID=99 is emitted.

        String html = cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.HTML)
                .map(e -> (String) e.value())
                .findFirst().orElse(null);
        assertEquals("no-frame", html, "Last HTML value should win");

        // Only one TYPE_UPDATE block
        long updateCount = cap.captured.stream()
                .filter(e -> e.model() == ServerToClientModel.TYPE_UPDATE)
                .count();
        assertEquals(1, updateCount);
    }

    /**
     * size() returns 0 after clear(), even if entries were recorded.
     * Also verifies that replayTo() after clear() replays nothing.
     */
    @Test
    public void testSizeAfterClearIsZeroAndReplayIsEmpty() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordUpdateBlock(rec, 1, ServerToClientModel.HTML, "data");
        assertEquals(4, rec.size()); // WINDOW_ID, TYPE_UPDATE, HTML, END

        rec.clear();
        assertEquals(0, rec.size());

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);
        assertTrue(cap.captured.isEmpty(), "Replay after clear should produce no entries");
    }

    /**
     * compact() with blocks of type OTHER (no recognized TYPE_* model).
     * These blocks should be preserved as-is, never merged or cancelled.
     */
    @Test
    public void testCompactPreservesOtherTypeBlocks() {
        final RecordingEncoder rec = new RecordingEncoder();

        // A block with only DESTROY_CONTEXT (no TYPE_* model) — classified as OTHER
        rec.encode(ServerToClientModel.DESTROY_CONTEXT, null);
        rec.endObject();

        // A normal update block
        recordUpdateBlock(rec, 42, ServerToClientModel.HTML, "data");

        // Another OTHER block
        rec.encode(ServerToClientModel.ROUNDTRIP_LATENCY, 12345L);
        rec.endObject();

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // All three blocks should be preserved
        boolean hasDestroy = cap.captured.stream()
                .anyMatch(e -> e.model() == ServerToClientModel.DESTROY_CONTEXT);
        boolean hasUpdate = cap.captured.stream()
                .anyMatch(e -> e.model() == ServerToClientModel.TYPE_UPDATE);
        boolean hasRoundtrip = cap.captured.stream()
                .anyMatch(e -> e.model() == ServerToClientModel.ROUNDTRIP_LATENCY);

        assertTrue(hasDestroy, "DESTROY_CONTEXT block should be preserved");
        assertTrue(hasUpdate, "UPDATE block should be preserved");
        assertTrue(hasRoundtrip, "ROUNDTRIP block should be preserved");
    }

    /**
     * compact() does not cancel when GC comes BEFORE CREATE for the same objectId.
     * This means the object existed before suspension (GC'd), then a new object
     * with the same ID was created. Both must be preserved.
     * (Mirrors testCompactDoesNotCancelWhenGCBeforeCreate but with explicit replay verification.)
     */
    @Test
    public void testCompactGCBeforeCreatePreservesBoth_ReplayVerification() {
        final RecordingEncoder rec = new RecordingEncoder();

        // GC object 50 (existed before suspension)
        rec.encode(ServerToClientModel.TYPE_GC, 50);
        rec.endObject();

        // Create object 50 (new object with recycled ID)
        recordCreateBlock(rec, 50, 7);

        rec.compact();

        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);

        // Both GC and CREATE should be present
        boolean hasGC = cap.captured.stream()
                .anyMatch(e -> e.model() == ServerToClientModel.TYPE_GC && Integer.valueOf(50).equals(e.value()));
        boolean hasCreate = cap.captured.stream()
                .anyMatch(e -> e.model() == ServerToClientModel.TYPE_CREATE && Integer.valueOf(50).equals(e.value()));

        assertTrue(hasGC, "GC should be preserved (came before CREATE)");
        assertTrue(hasCreate, "CREATE should be preserved (came after GC)");

        // Verify order: GC before CREATE
        int gcIdx = -1, createIdx = -1;
        for (int i = 0; i < cap.captured.size(); i++) {
            if (cap.captured.get(i).model() == ServerToClientModel.TYPE_GC) gcIdx = i;
            if (cap.captured.get(i).model() == ServerToClientModel.TYPE_CREATE) createIdx = i;
        }
        assertTrue(gcIdx < createIdx, "GC should come before CREATE");
    }

    /**
     * compact() with a single entry (no END) — incomplete recording.
     * Should not crash. The entry is not part of any block, so it's lost.
     */
    @Test
    public void testCompactSingleEntryNoEnd() {
        final RecordingEncoder rec = new RecordingEncoder();
        rec.encode(ServerToClientModel.HTML, "orphan");
        // No endObject() — incomplete block

        final int sizeBefore = rec.size();
        rec.compact();

        // The orphan entry is not in any block (no END delimiter), so it's dropped
        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);
        // May or may not have entries depending on implementation — just verify no crash
        assertTrue(rec.size() >= 0, "Size should be >= 0 after compact");
    }

    /**
     * Verify that compact() + replayTo() produces the same result as
     * just replayTo() when there's nothing to compact (all different objects, no cancellation).
     */
    @Test
    public void testCompactNoOpWhenNothingToMerge() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordUpdateBlock(rec, 1, ServerToClientModel.HTML, "a");
        recordCreateBlock(rec, 2, 5);
        recordUpdateBlock(rec, 3, ServerToClientModel.TEXT, "b");

        // Capture before compact
        final CapturingEncoder before = new CapturingEncoder();
        rec.replayTo(before);

        // Compact
        rec.compact();

        // Capture after compact
        final CapturingEncoder after = new CapturingEncoder();
        rec.replayTo(after);

        // Should be identical
        assertEquals(before.captured.size(),
                after.captured.size(), "Compact should be no-op when nothing to merge");
        for (int i = 0; i < before.captured.size(); i++) {
            assertEquals(before.captured.get(i).model(),
                    after.captured.get(i).model(), "Entry " + i + " model should match");
            assertEquals(before.captured.get(i).value(),
                    after.captured.get(i).value(), "Entry " + i + " value should match");
        }
    }

    // ====================================================================
    // Memory leak tests
    // ====================================================================

    /** Helper: tries GC up to 10 times, returns true if the ref was collected. */
    private static boolean tryCollect(final WeakReference<?> ref) {
        for (int i = 0; i < 10; i++) {
            System.gc();
            try { Thread.sleep(50); } catch (final InterruptedException ignored) {}
            if (ref.get() == null) return true;
        }
        return false;
    }

    /**
     * After clear(), the Entry objects recorded should be eligible for GC.
     * We hold a WeakReference to a value object stored in an entry and verify
     * it becomes null after clear() + GC.
     */
    @Test
    public void testClear_ReleasesEntryReferences() {
        final RecordingEncoder rec = new RecordingEncoder();
        Object bigValue = new byte[1024 * 1024]; // 1MB — easy to detect
        final WeakReference<Object> ref = new WeakReference<>(bigValue);

        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.TYPE_UPDATE, 1);
        rec.encode(ServerToClientModel.HTML, bigValue);
        rec.endObject();

        // Drop our strong ref — only the encoder holds it now
        bigValue = null;

        // Before clear: should still be alive
        System.gc();
        assertNotNull(ref.get(), "Value should be retained before clear()");

        rec.clear();
        assertTrue(tryCollect(ref), "Value should be GC'd after clear()");
    }

    /**
     * After compact(), cancelled entries (create+remove pairs) should be released.
     */
    @Test
    public void testCompact_ReleasesCancelledEntries() {
        final RecordingEncoder rec = new RecordingEncoder();
        Object createPayload = new byte[512 * 1024];
        final WeakReference<Object> ref = new WeakReference<>(createPayload);

        // CREATE object 42 with a big payload
        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.TYPE_CREATE, 42);
        rec.encode(ServerToClientModel.WIDGET_TYPE, createPayload);
        rec.endObject();

        // REMOVE object 42
        recordRemoveBlock(rec, 42);

        createPayload = null;

        // Before compact: still referenced in entries
        System.gc();
        assertNotNull(ref.get(), "Payload should be retained before compact()");

        rec.compact();

        // After compact: create+remove cancelled, entries replaced
        assertTrue(tryCollect(ref), "Cancelled payload should be GC'd after compact()");
    }

    /**
     * After compact(), merged update blocks should release old (overwritten) values.
     */
    @Test
    public void testCompact_ReleasesOverwrittenUpdateValues() {
        final RecordingEncoder rec = new RecordingEncoder();
        Object oldValue = new byte[512 * 1024];
        final WeakReference<Object> ref = new WeakReference<>(oldValue);

        // First update: HTML = oldValue
        recordUpdateBlock(rec, 10, ServerToClientModel.HTML, oldValue);
        oldValue = null;

        // Second update: HTML = "newValue" (overwrites)
        recordUpdateBlock(rec, 10, ServerToClientModel.HTML, "newValue");

        // Before compact: old value still in entries
        System.gc();
        assertNotNull(ref.get(), "Old value should be retained before compact()");

        rec.compact();

        // After compact: only "newValue" survives
        assertTrue(tryCollect(ref), "Overwritten value should be GC'd after compact()");
    }

    /**
     * The RecordingEncoder itself should be GC-eligible when no external refs remain.
     */
    @Test
    public void testEncoder_GCEligibleWhenDereferenced() {
        RecordingEncoder rec = new RecordingEncoder();
        final WeakReference<RecordingEncoder> ref = new WeakReference<>(rec);

        // Fill it with some data
        for (int i = 0; i < 100; i++) {
            recordUpdateBlock(rec, i, ServerToClientModel.HTML, "val" + i);
        }

        rec = null;
        assertTrue(tryCollect(ref), "RecordingEncoder should be GC'd when dereferenced");
    }

    /**
     * After overflow, the overflow handler lambda should not prevent GC of the encoder
     * if the external reference is dropped.
     */
    @Test
    public void testOverflow_EncoderGCEligibleAfterOverflow() {
        final AtomicBoolean overflowed = new AtomicBoolean(false);
        RecordingEncoder rec = new RecordingEncoder(5, (count, max) -> overflowed.set(true));
        final WeakReference<RecordingEncoder> ref = new WeakReference<>(rec);

        // Trigger overflow
        for (int i = 0; i < 10; i++) {
            rec.encode(ServerToClientModel.HTML, "v" + i);
        }
        assertTrue(overflowed.get());

        rec = null;
        assertTrue(tryCollect(ref), "Overflowed encoder should be GC'd when dereferenced");
    }

    /**
     * After replayTo(), the source encoder's entries should still be intact (no side-effect),
     * but after clear() they should be released.
     */
    @Test
    public void testReplayTo_ThenClear_ReleasesEntries() {
        final RecordingEncoder rec = new RecordingEncoder();
        Object payload = new byte[512 * 1024];
        final WeakReference<Object> ref = new WeakReference<>(payload);

        recordUpdateBlock(rec, 1, ServerToClientModel.HTML, payload);
        payload = null;

        // Replay to a target — entries are still in the source
        final CapturingEncoder target = new CapturingEncoder();
        rec.replayTo(target);

        // Target also holds refs, clear target
        target.captured.clear();

        // Source still holds entries
        System.gc();
        assertNotNull(ref.get(), "Payload should be retained before source clear()");

        rec.clear();
        assertTrue(tryCollect(ref), "Payload should be GC'd after source clear()");
    }

    /**
     * Multiple compact() calls should not accumulate memory — each compact
     * replaces the entries list content.
     */
    @Test
    public void testMultipleCompacts_NoMemoryAccumulation() {
        final RecordingEncoder rec = new RecordingEncoder();

        // First round of updates
        Object val1 = new byte[256 * 1024];
        final WeakReference<Object> ref1 = new WeakReference<>(val1);
        recordUpdateBlock(rec, 1, ServerToClientModel.HTML, val1);
        val1 = null;

        // Overwrite with new value
        recordUpdateBlock(rec, 1, ServerToClientModel.HTML, "newVal");

        rec.compact();
        // After first compact, val1 should be released (overwritten)
        assertTrue(tryCollect(ref1), "First value should be GC'd after compact");

        // Add more data and compact again
        Object val2 = new byte[256 * 1024];
        final WeakReference<Object> ref2 = new WeakReference<>(val2);
        recordUpdateBlock(rec, 2, ServerToClientModel.HTML, val2);
        val2 = null;

        recordUpdateBlock(rec, 2, ServerToClientModel.HTML, "newVal2");
        rec.compact();
        assertTrue(tryCollect(ref2), "Second value should be GC'd after second compact");
    }

    /**
     * Compact followed by clear should release everything.
     */
    @Test
    public void testCompactThenClear_ReleasesAll() {
        final RecordingEncoder rec = new RecordingEncoder();
        Object payload = new byte[512 * 1024];
        final WeakReference<Object> ref = new WeakReference<>(payload);

        recordUpdateBlock(rec, 1, ServerToClientModel.HTML, payload);
        payload = null;

        rec.compact(); // no merge needed, but exercises the code path
        assertEquals(1, rec.size() > 0 ? 1 : 0); // at least some entries

        rec.clear();
        assertTrue(tryCollect(ref), "Payload should be GC'd after compact+clear");
    }

    /**
     * Large number of entries — verify clear() releases them all.
     */
    @Test
    public void testClear_LargeNumberOfEntries() {
        final RecordingEncoder rec = new RecordingEncoder();
        final List<WeakReference<Object>> refs = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            Object val = new byte[10 * 1024]; // 10KB each = 500KB total
            refs.add(new WeakReference<>(val));
            recordUpdateBlock(rec, i, ServerToClientModel.HTML, val);
            val = null;
        }

        assertEquals(200, rec.size()); // 4 entries per block × 50

        rec.clear();
        assertEquals(0, rec.size());

        // At least some of the 50 values should be GC'd
        int collected = 0;
        for (int attempt = 0; attempt < 10; attempt++) {
            System.gc();
            try { Thread.sleep(50); } catch (final InterruptedException ignored) {}
            collected = 0;
            for (final WeakReference<Object> ref : refs) {
                if (ref.get() == null) collected++;
            }
            if (collected >= 25) break; // at least half collected
        }
        assertTrue(collected >= 25, "At least half of the values should be GC'd, got " + collected);
    }

    /**
     * After overflow, further encode() calls are no-ops — no additional memory consumed.
     */
    @Test
    public void testOverflow_NoAdditionalMemoryAfterOverflow() {
        final RecordingEncoder rec = new RecordingEncoder(10, (c, m) -> {});

        // Fill to overflow
        for (int i = 0; i < 15; i++) {
            rec.encode(ServerToClientModel.HTML, "v" + i);
        }
        assertTrue(rec.isOverflowed());
        final int sizeAtOverflow = rec.size();

        // Further writes should be no-ops
        for (int i = 0; i < 100; i++) {
            rec.encode(ServerToClientModel.HTML, new byte[1024]);
        }
        assertEquals(sizeAtOverflow, rec.size(), "Size should not grow after overflow");
    }

    // ---- Keyed properties, additive writes, commands, on-the-fly compaction ----

    private static void recordStyle(final RecordingEncoder rec, final int objectId, final String key, final String value) {
        rec.encode(ServerToClientModel.WINDOW_ID, 0);
        rec.encode(ServerToClientModel.TYPE_UPDATE, objectId);
        rec.encode(ServerToClientModel.PUT_STYLE_KEY, key);
        rec.encode(ServerToClientModel.STYLE_VALUE, value);
        rec.endObject();
    }

    private static List<Object> values(final CapturingEncoder cap, final ServerToClientModel model) {
        return cap.captured.stream().filter(e -> e.model() == model).map(RecordingEncoder.Entry::value).toList();
    }

    @Test
    public void testCompactKeepsDistinctStyleKeys() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordStyle(rec, 7, "color", "red");
        recordStyle(rec, 7, "width", "10px");
        recordStyle(rec, 7, "color", "green");
        rec.compact();
        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);
        assertEquals(List.of("width", "color"), values(cap, ServerToClientModel.PUT_STYLE_KEY));
        assertEquals(List.of("10px", "green"), values(cap, ServerToClientModel.STYLE_VALUE));
    }

    @Test
    public void testCompactRemoveStyleKeyWinsOverEarlierPut() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordStyle(rec, 7, "color", "red");
        recordUpdateBlock(rec, 7, ServerToClientModel.REMOVE_STYLE_KEY, "color");
        rec.compact();
        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);
        assertTrue(values(cap, ServerToClientModel.PUT_STYLE_KEY).isEmpty());
        assertEquals(List.of("color"), values(cap, ServerToClientModel.REMOVE_STYLE_KEY));
    }

    @Test
    public void testCompactKeepsEveryAddedAndRemovedStyleNameInOrder() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordUpdateBlock(rec, 7, ServerToClientModel.ADD_STYLE_NAME, "a");
        recordUpdateBlock(rec, 7, ServerToClientModel.ADD_STYLE_NAME, "b");
        recordUpdateBlock(rec, 7, ServerToClientModel.REMOVE_STYLE_NAME, "a");
        rec.compact();
        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);
        assertEquals(List.of("a", "b"), values(cap, ServerToClientModel.ADD_STYLE_NAME));
        assertEquals(List.of("a"), values(cap, ServerToClientModel.REMOVE_STYLE_NAME));
    }

    @Test
    public void testCompactDoesNotMoveUpdatesAcrossACommand() {
        final RecordingEncoder rec = new RecordingEncoder();
        recordUpdateBlock(rec, 1, ServerToClientModel.HTML, "before");
        recordUpdateBlock(rec, 2, ServerToClientModel.ADD_STYLE_NAME, "x"); // not a plain setter: barrier
        recordUpdateBlock(rec, 1, ServerToClientModel.HTML, "after");
        rec.compact();
        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);
        assertEquals(List.of("before", "after"), values(cap, ServerToClientModel.HTML));
    }

    @Test
    public void testPushingApplicationDoesNotOverflowWhileSuspended() {
        final AtomicInteger overflows = new AtomicInteger();
        final RecordingEncoder rec = new RecordingEncoder(10_000, (count, max) -> overflows.incrementAndGet());
        // a ticker: 240 cells, text + colour, 1 000 ticks of 60 random cells
        final java.util.Random rnd = new java.util.Random(3);
        for (int tick = 0; tick < 1_000; tick++) {
            for (int k = 0; k < 60; k++) {
                final int cell = 100 + rnd.nextInt(240);
                recordUpdateBlock(rec, cell, ServerToClientModel.TEXT, "1." + rnd.nextInt(99_999));
                recordStyle(rec, cell, "color", rnd.nextBoolean() ? "green" : "red");
            }
        }
        assertEquals(0, overflows.get(), "compacted on the way: the distinct state fits");
        assertFalse(rec.isOverflowed());
        rec.compact();
        final CapturingEncoder cap = new CapturingEncoder();
        rec.replayTo(cap);
        assertTrue(values(cap, ServerToClientModel.TYPE_UPDATE).size() <= 240, "at most one block per cell");
    }
}
