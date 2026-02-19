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

package com.ponysdk.core.ui.component;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for UpdatePrioritizer.
 * <p>
 * Feature: pcomponent, Property 5: Priority Ordering
 * </p>
 * <p>
 * **Validates: Requirements 5.2**
 * </p>
 * <p>
 * The property being tested:
 * For any queue containing updates with mixed priorities (HIGH, NORMAL, LOW),
 * THE UpdatePrioritizer SHALL process all HIGH priority updates before any NORMAL updates,
 * and all NORMAL updates before any LOW updates.
 * </p>
 */
public class UpdatePrioritizerPropertyTest {

    // ========== Property 5: Priority Ordering ==========

    /**
     * Property 5: Priority Ordering - All HIGH updates execute before any NORMAL or LOW
     * <p>
     * **Validates: Requirements 5.2**
     * </p>
     * <p>
     * For any queue containing updates with mixed priorities (HIGH, NORMAL, LOW),
     * THE UpdatePrioritizer SHALL process all HIGH priority updates before any NORMAL updates,
     * and all NORMAL updates before any LOW updates.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 5: Priority Ordering - HIGH before NORMAL before LOW")
    void priorityOrderingIsRespected(
            @ForAll("mixedPriorityUpdates") List<UpdatePriority> priorities
    ) {
        // Skip if no mixed priorities
        Assume.that(priorities.size() >= 2);
        Assume.that(hasMixedPriorities(priorities));

        UpdatePrioritizer prioritizer = new UpdatePrioritizer();
        List<UpdatePriority> executionOrder = Collections.synchronizedList(new ArrayList<>());

        // Enqueue all updates with their priorities
        for (UpdatePriority priority : priorities) {
            prioritizer.enqueue(priority, () -> executionOrder.add(priority));
        }

        // Flush the queue
        int processedCount = prioritizer.flush();

        // Verify all updates were processed
        assertEquals(priorities.size(), processedCount,
                "All updates should be processed");
        assertEquals(priorities.size(), executionOrder.size(),
                "Execution order should contain all updates");

        // Verify priority ordering: HIGH before NORMAL before LOW
        assertPriorityOrdering(executionOrder);
    }

    /**
     * Property 5: Priority Ordering - Verify ordering with specific counts
     * <p>
     * **Validates: Requirements 5.2**
     * </p>
     * <p>
     * For any combination of HIGH, NORMAL, and LOW priority updates,
     * the execution order SHALL respect priority levels.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 5: Priority Ordering - Specific counts maintain ordering")
    void priorityOrderingWithSpecificCounts(
            @ForAll @IntRange(min = 0, max = 20) int highCount,
            @ForAll @IntRange(min = 0, max = 20) int normalCount,
            @ForAll @IntRange(min = 0, max = 20) int lowCount
    ) {
        // Skip if total is too small or no mixed priorities
        int total = highCount + normalCount + lowCount;
        Assume.that(total >= 2);
        Assume.that((highCount > 0 && normalCount > 0) ||
                    (highCount > 0 && lowCount > 0) ||
                    (normalCount > 0 && lowCount > 0));

        UpdatePrioritizer prioritizer = new UpdatePrioritizer();
        List<UpdatePriority> executionOrder = Collections.synchronizedList(new ArrayList<>());

        // Enqueue in random order (LOW, NORMAL, HIGH) to test sorting
        for (int i = 0; i < lowCount; i++) {
            prioritizer.enqueue(UpdatePriority.LOW, () -> executionOrder.add(UpdatePriority.LOW));
        }
        for (int i = 0; i < normalCount; i++) {
            prioritizer.enqueue(UpdatePriority.NORMAL, () -> executionOrder.add(UpdatePriority.NORMAL));
        }
        for (int i = 0; i < highCount; i++) {
            prioritizer.enqueue(UpdatePriority.HIGH, () -> executionOrder.add(UpdatePriority.HIGH));
        }

        // Flush the queue
        prioritizer.flush();

        // Verify counts
        assertEquals(total, executionOrder.size(),
                "All updates should be executed");

        // Verify priority ordering
        assertPriorityOrdering(executionOrder);

        // Verify exact counts per priority
        long actualHighCount = executionOrder.stream().filter(p -> p == UpdatePriority.HIGH).count();
        long actualNormalCount = executionOrder.stream().filter(p -> p == UpdatePriority.NORMAL).count();
        long actualLowCount = executionOrder.stream().filter(p -> p == UpdatePriority.LOW).count();

        assertEquals(highCount, actualHighCount, "HIGH count should match");
        assertEquals(normalCount, actualNormalCount, "NORMAL count should match");
        assertEquals(lowCount, actualLowCount, "LOW count should match");
    }

    /**
     * Property 5: Priority Ordering - Interleaved enqueue maintains ordering
     * <p>
     * **Validates: Requirements 5.2**
     * </p>
     * <p>
     * When updates are enqueued in any arbitrary order (interleaved priorities),
     * the flush SHALL still process them in priority order.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 5: Priority Ordering - Interleaved enqueue maintains ordering")
    void interleavedEnqueueMaintainsOrdering(
            @ForAll("shuffledPrioritySequence") List<UpdatePriority> shuffledPriorities
    ) {
        Assume.that(shuffledPriorities.size() >= 3);
        Assume.that(hasMixedPriorities(shuffledPriorities));

        UpdatePrioritizer prioritizer = new UpdatePrioritizer();
        List<UpdatePriority> executionOrder = Collections.synchronizedList(new ArrayList<>());

        // Enqueue in shuffled order
        for (UpdatePriority priority : shuffledPriorities) {
            prioritizer.enqueue(priority, () -> executionOrder.add(priority));
        }

        // Flush
        prioritizer.flush();

        // Verify ordering
        assertEquals(shuffledPriorities.size(), executionOrder.size(),
                "All updates should be executed");
        assertPriorityOrdering(executionOrder);
    }

    /**
     * Property 5: Priority Ordering - Empty queue flush is safe
     * <p>
     * **Validates: Requirements 5.2**
     * </p>
     * <p>
     * Flushing an empty queue SHALL return 0 and have no side effects.
     * </p>
     */
    @Example
    @Label("Property 5: Priority Ordering - Empty queue flush is safe")
    void emptyQueueFlushIsSafe() {
        UpdatePrioritizer prioritizer = new UpdatePrioritizer();

        int processedCount = prioritizer.flush();

        assertEquals(0, processedCount, "Empty queue should process 0 updates");
        assertTrue(prioritizer.isEmpty(), "Queue should remain empty");
    }

    /**
     * Property 5: Priority Ordering - Single priority type maintains FIFO within priority
     * <p>
     * **Validates: Requirements 5.2**
     * </p>
     * <p>
     * When all updates have the same priority, they should be processed
     * (though order within same priority is not strictly guaranteed by PriorityBlockingQueue).
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 5: Priority Ordering - Single priority processes all updates")
    void singlePriorityProcessesAllUpdates(
            @ForAll UpdatePriority priority,
            @ForAll @IntRange(min = 1, max = 50) int count
    ) {
        UpdatePrioritizer prioritizer = new UpdatePrioritizer();
        List<UpdatePriority> executionOrder = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < count; i++) {
            prioritizer.enqueue(priority, () -> executionOrder.add(priority));
        }

        int processedCount = prioritizer.flush();

        assertEquals(count, processedCount, "All updates should be processed");
        assertEquals(count, executionOrder.size(), "All updates should be in execution order");
        assertTrue(executionOrder.stream().allMatch(p -> p == priority),
                "All executed updates should have the same priority");
    }

    /**
     * Property 5: Priority Ordering - Queue size reflects pending updates
     * <p>
     * **Validates: Requirements 5.2**
     * </p>
     * <p>
     * After enqueuing N updates, size() SHALL return N.
     * After flush(), size() SHALL return 0.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 5: Priority Ordering - Queue size is accurate")
    void queueSizeIsAccurate(
            @ForAll("mixedPriorityUpdates") List<UpdatePriority> priorities
    ) {
        Assume.that(!priorities.isEmpty());

        UpdatePrioritizer prioritizer = new UpdatePrioritizer();

        // Enqueue all
        for (UpdatePriority priority : priorities) {
            prioritizer.enqueue(priority, () -> {});
        }

        // Verify size before flush
        assertEquals(priorities.size(), prioritizer.size(),
                "Size should equal number of enqueued updates");
        assertFalse(prioritizer.isEmpty(), "Queue should not be empty");

        // Flush
        prioritizer.flush();

        // Verify size after flush
        assertEquals(0, prioritizer.size(), "Size should be 0 after flush");
        assertTrue(prioritizer.isEmpty(), "Queue should be empty after flush");
    }

    /**
     * Property 5: Priority Ordering - Clear removes all pending updates
     * <p>
     * **Validates: Requirements 5.2**
     * </p>
     * <p>
     * Calling clear() SHALL remove all pending updates without executing them.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 5: Priority Ordering - Clear removes all pending updates")
    void clearRemovesAllPendingUpdates(
            @ForAll("mixedPriorityUpdates") List<UpdatePriority> priorities
    ) {
        Assume.that(!priorities.isEmpty());

        UpdatePrioritizer prioritizer = new UpdatePrioritizer();
        List<UpdatePriority> executionOrder = Collections.synchronizedList(new ArrayList<>());

        // Enqueue all
        for (UpdatePriority priority : priorities) {
            prioritizer.enqueue(priority, () -> executionOrder.add(priority));
        }

        // Clear instead of flush
        prioritizer.clear();

        // Verify queue is empty
        assertTrue(prioritizer.isEmpty(), "Queue should be empty after clear");
        assertEquals(0, prioritizer.size(), "Size should be 0 after clear");

        // Flush should process nothing
        int processedCount = prioritizer.flush();
        assertEquals(0, processedCount, "Flush after clear should process 0 updates");
        assertTrue(executionOrder.isEmpty(), "No updates should have been executed");
    }

    // ========== Helper Methods ==========

    /**
     * Asserts that the execution order respects priority ordering:
     * All HIGH before any NORMAL, all NORMAL before any LOW.
     */
    private void assertPriorityOrdering(List<UpdatePriority> executionOrder) {
        int firstNormalIndex = -1;
        int lastHighIndex = -1;
        int firstLowIndex = -1;
        int lastNormalIndex = -1;

        for (int i = 0; i < executionOrder.size(); i++) {
            UpdatePriority priority = executionOrder.get(i);
            switch (priority) {
                case HIGH:
                    lastHighIndex = i;
                    break;
                case NORMAL:
                    if (firstNormalIndex == -1) firstNormalIndex = i;
                    lastNormalIndex = i;
                    break;
                case LOW:
                    if (firstLowIndex == -1) firstLowIndex = i;
                    break;
            }
        }

        // Verify HIGH before NORMAL
        if (lastHighIndex >= 0 && firstNormalIndex >= 0) {
            assertTrue(lastHighIndex < firstNormalIndex,
                    "All HIGH updates (last at index " + lastHighIndex +
                    ") should execute before any NORMAL updates (first at index " + firstNormalIndex + ")");
        }

        // Verify HIGH before LOW
        if (lastHighIndex >= 0 && firstLowIndex >= 0) {
            assertTrue(lastHighIndex < firstLowIndex,
                    "All HIGH updates (last at index " + lastHighIndex +
                    ") should execute before any LOW updates (first at index " + firstLowIndex + ")");
        }

        // Verify NORMAL before LOW
        if (lastNormalIndex >= 0 && firstLowIndex >= 0) {
            assertTrue(lastNormalIndex < firstLowIndex,
                    "All NORMAL updates (last at index " + lastNormalIndex +
                    ") should execute before any LOW updates (first at index " + firstLowIndex + ")");
        }
    }

    /**
     * Checks if the list contains at least two different priority levels.
     */
    private boolean hasMixedPriorities(List<UpdatePriority> priorities) {
        if (priorities.isEmpty()) return false;
        UpdatePriority first = priorities.get(0);
        return priorities.stream().anyMatch(p -> p != first);
    }

    // ========== Arbitraries (Generators) ==========

    /**
     * Generates a list of mixed priority updates.
     */
    @Provide
    Arbitrary<List<UpdatePriority>> mixedPriorityUpdates() {
        return Arbitraries.of(UpdatePriority.values())
                .list()
                .ofMinSize(2)
                .ofMaxSize(100);
    }

    /**
     * Generates a shuffled sequence of priorities ensuring all three types are present.
     */
    @Provide
    Arbitrary<List<UpdatePriority>> shuffledPrioritySequence() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 20),  // HIGH count
                Arbitraries.integers().between(1, 20),  // NORMAL count
                Arbitraries.integers().between(1, 20)   // LOW count
        ).as((highCount, normalCount, lowCount) -> {
            List<UpdatePriority> priorities = new ArrayList<>();
            for (int i = 0; i < highCount; i++) priorities.add(UpdatePriority.HIGH);
            for (int i = 0; i < normalCount; i++) priorities.add(UpdatePriority.NORMAL);
            for (int i = 0; i < lowCount; i++) priorities.add(UpdatePriority.LOW);
            Collections.shuffle(priorities);
            return priorities;
        });
    }
}
