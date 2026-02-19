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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ThrottleController.
 * <p>
 * Feature: pcomponent, Property 4: Throttle Batching Preserves Latest State
 * </p>
 * <p>
 * **Validates: Requirements 4.2**
 * </p>
 * <p>
 * Since ThrottleController uses PComponent which requires UIContext (not available in unit tests),
 * we test the core batching behavior through the ScheduledUpdate inner class directly.
 * The ScheduledUpdate class is the mechanism that preserves the latest state when multiple
 * updates occur within the throttle window.
 * </p>
 * <p>
 * The property being tested:
 * For any sequence of N rapid props updates (where N > 1) occurring within the throttle interval,
 * THE PComponent SHALL send exactly one update containing only the final props state.
 * </p>
 * <p>
 * This is achieved by:
 * <ul>
 *   <li>ScheduledUpdate.setUpdateAction() replaces the pending action with the latest one</li>
 *   <li>ScheduledUpdate.execute() runs exactly once (idempotent)</li>
 *   <li>Cancelled updates are not executed</li>
 * </ul>
 * </p>
 */
public class ThrottleControllerPropertyTest {

    // ========== Property 4: Throttle Batching Preserves Latest State ==========

    /**
     * Property 4: ScheduledUpdate preserves latest action after N replacements
     * <p>
     * **Validates: Requirements 4.2**
     * </p>
     * <p>
     * For any sequence of N rapid props updates (where N > 1), when setUpdateAction is called
     * N times on a ScheduledUpdate, only the final (Nth) action SHALL be executed.
     * This ensures that only the latest props state is sent to the client.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 4: Throttle Batching Preserves Latest State - ScheduledUpdate preserves latest action")
    void scheduledUpdatePreservesLatestAction(
            @ForAll @IntRange(min = 2, max = 50) int numberOfUpdates
    ) {
        // Track which action was executed
        AtomicReference<Integer> executedValue = new AtomicReference<>(null);
        AtomicInteger executionCount = new AtomicInteger(0);

        // Create initial action (simulating first props update)
        ThrottleController.ScheduledUpdate scheduledUpdate =
                new ThrottleController.ScheduledUpdate(() -> {
                    executionCount.incrementAndGet();
                    executedValue.set(0);
                });

        // Replace the action N-1 times (simulating N rapid props updates)
        for (int i = 1; i < numberOfUpdates; i++) {
            final int value = i;
            scheduledUpdate.setUpdateAction(() -> {
                executionCount.incrementAndGet();
                executedValue.set(value);
            });
        }

        // Execute the scheduled update (simulating throttle interval elapsed)
        scheduledUpdate.execute();

        // Verify: exactly one execution occurred
        assertEquals(1, executionCount.get(),
                "ScheduledUpdate should execute exactly once for " + numberOfUpdates + " rapid updates");

        // Verify: the executed action is the last one set (final props state)
        assertEquals(Integer.valueOf(numberOfUpdates - 1), executedValue.get(),
                "ScheduledUpdate should execute the latest (final) action after " + numberOfUpdates + " updates");

        // Verify: the update is marked as executed
        assertTrue(scheduledUpdate.isExecuted(),
                "ScheduledUpdate should be marked as executed");
    }

    /**
     * Property 4: ScheduledUpdate executes exactly once (idempotent)
     * <p>
     * **Validates: Requirements 4.2**
     * </p>
     * <p>
     * Calling execute() multiple times on a ScheduledUpdate SHALL only execute
     * the action once. This ensures that even if the scheduler triggers multiple
     * times, only one update is sent.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 4: Throttle Batching - ScheduledUpdate executes exactly once")
    void scheduledUpdateExecutesExactlyOnce(
            @ForAll @IntRange(min = 2, max = 20) int numberOfExecuteCalls
    ) {
        AtomicInteger executionCount = new AtomicInteger(0);

        ThrottleController.ScheduledUpdate scheduledUpdate =
                new ThrottleController.ScheduledUpdate(executionCount::incrementAndGet);

        // Call execute multiple times
        for (int i = 0; i < numberOfExecuteCalls; i++) {
            scheduledUpdate.execute();
        }

        // Verify: action was executed exactly once
        assertEquals(1, executionCount.get(),
                "ScheduledUpdate should execute exactly once regardless of " + numberOfExecuteCalls + " execute() calls");

        // Verify: marked as executed
        assertTrue(scheduledUpdate.isExecuted(),
                "ScheduledUpdate should be marked as executed");
    }

    /**
     * Property 4: ScheduledUpdate with replacements and multiple execute calls
     * <p>
     * **Validates: Requirements 4.2**
     * </p>
     * <p>
     * For any sequence of N action replacements followed by M execute calls,
     * only the final action SHALL be executed exactly once.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 4: Throttle Batching - Replacements followed by multiple executes")
    void scheduledUpdateReplacementsThenMultipleExecutes(
            @ForAll @IntRange(min = 2, max = 30) int numberOfReplacements,
            @ForAll @IntRange(min = 2, max = 10) int numberOfExecuteCalls
    ) {
        AtomicReference<Integer> executedValue = new AtomicReference<>(null);
        AtomicInteger executionCount = new AtomicInteger(0);

        // Create initial action
        ThrottleController.ScheduledUpdate scheduledUpdate =
                new ThrottleController.ScheduledUpdate(() -> {
                    executionCount.incrementAndGet();
                    executedValue.set(0);
                });

        // Replace action multiple times
        for (int i = 1; i <= numberOfReplacements; i++) {
            final int value = i;
            scheduledUpdate.setUpdateAction(() -> {
                executionCount.incrementAndGet();
                executedValue.set(value);
            });
        }

        // Call execute multiple times
        for (int i = 0; i < numberOfExecuteCalls; i++) {
            scheduledUpdate.execute();
        }

        // Verify: exactly one execution
        assertEquals(1, executionCount.get(),
                "Should execute exactly once after " + numberOfReplacements + " replacements and " + numberOfExecuteCalls + " execute calls");

        // Verify: final value is the last replacement
        assertEquals(Integer.valueOf(numberOfReplacements), executedValue.get(),
                "Should execute the final (latest) action");
    }

    /**
     * Property 4: Cancelled ScheduledUpdate is not executed
     * <p>
     * **Validates: Requirements 4.2**
     * </p>
     * <p>
     * When a ScheduledUpdate is cancelled before execution, calling execute()
     * SHALL have no effect. This is important for component cleanup when a
     * component is destroyed before its throttled update fires.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 4: Throttle Batching - Cancelled updates are not executed")
    void cancelledUpdatesAreNotExecuted(
            @ForAll @IntRange(min = 1, max = 20) int numberOfReplacements
    ) {
        AtomicInteger executionCount = new AtomicInteger(0);

        ThrottleController.ScheduledUpdate scheduledUpdate =
                new ThrottleController.ScheduledUpdate(executionCount::incrementAndGet);

        // Replace action multiple times
        for (int i = 0; i < numberOfReplacements; i++) {
            scheduledUpdate.setUpdateAction(executionCount::incrementAndGet);
        }

        // Cancel before execution
        scheduledUpdate.cancel();

        // Try to execute
        scheduledUpdate.execute();

        // Verify: no execution occurred
        assertEquals(0, executionCount.get(),
                "Cancelled ScheduledUpdate should not execute after " + numberOfReplacements + " replacements");

        // Verify: marked as cancelled
        assertTrue(scheduledUpdate.isCancelled(),
                "ScheduledUpdate should be marked as cancelled");

        // Verify: not marked as executed
        assertFalse(scheduledUpdate.isExecuted(),
                "Cancelled ScheduledUpdate should not be marked as executed");
    }

    /**
     * Property 4: Cancel after execute has no additional effect
     * <p>
     * **Validates: Requirements 4.2**
     * </p>
     * <p>
     * If execute() is called before cancel(), the action runs once and
     * subsequent cancel() calls have no effect on the execution count.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 4: Throttle Batching - Cancel after execute has no effect")
    void cancelAfterExecuteHasNoEffect(
            @ForAll @IntRange(min = 1, max = 10) int numberOfCancelCalls
    ) {
        AtomicInteger executionCount = new AtomicInteger(0);

        ThrottleController.ScheduledUpdate scheduledUpdate =
                new ThrottleController.ScheduledUpdate(executionCount::incrementAndGet);

        // Execute first
        scheduledUpdate.execute();

        // Then cancel multiple times
        for (int i = 0; i < numberOfCancelCalls; i++) {
            scheduledUpdate.cancel();
        }

        // Try to execute again
        scheduledUpdate.execute();

        // Verify: still only one execution
        assertEquals(1, executionCount.get(),
                "Should still have exactly one execution after cancel");

        // Verify: marked as executed
        assertTrue(scheduledUpdate.isExecuted(),
                "Should be marked as executed");
    }

    /**
     * Property 4: ScheduledUpdate preserves latest state with complex props sequences
     * <p>
     * **Validates: Requirements 4.2**
     * </p>
     * <p>
     * For any sequence of props updates with varying values, only the final
     * props state SHALL be preserved and executed.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 4: Throttle Batching - Complex props sequences preserve final state")
    void complexPropsSequencesPreserveFinalState(
            @ForAll("propsValueSequence") List<Integer> propsValues
    ) {
        // Skip if sequence is too small
        Assume.that(propsValues.size() >= 2);

        AtomicReference<Integer> executedValue = new AtomicReference<>(null);
        AtomicInteger executionCount = new AtomicInteger(0);

        // Create initial action with first props value
        ThrottleController.ScheduledUpdate scheduledUpdate =
                new ThrottleController.ScheduledUpdate(() -> {
                    executionCount.incrementAndGet();
                    executedValue.set(propsValues.get(0));
                });

        // Replace with subsequent props values
        for (int i = 1; i < propsValues.size(); i++) {
            final int value = propsValues.get(i);
            scheduledUpdate.setUpdateAction(() -> {
                executionCount.incrementAndGet();
                executedValue.set(value);
            });
        }

        // Execute
        scheduledUpdate.execute();

        // Verify: exactly one execution
        assertEquals(1, executionCount.get(),
                "Should execute exactly once for " + propsValues.size() + " props updates");

        // Verify: final props value is preserved
        Integer expectedFinalValue = propsValues.get(propsValues.size() - 1);
        assertEquals(expectedFinalValue, executedValue.get(),
                "Should preserve the final props value: " + expectedFinalValue);
    }

    /**
     * Property 4: getUpdateAction returns the latest action
     * <p>
     * **Validates: Requirements 4.2**
     * </p>
     * <p>
     * After N setUpdateAction calls, getUpdateAction() SHALL return the
     * most recently set action.
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 4: Throttle Batching - getUpdateAction returns latest action")
    void getUpdateActionReturnsLatestAction(
            @ForAll @IntRange(min = 1, max = 30) int numberOfReplacements
    ) {
        List<Runnable> actions = new ArrayList<>();

        // Create initial action
        Runnable initialAction = () -> {};
        actions.add(initialAction);

        ThrottleController.ScheduledUpdate scheduledUpdate =
                new ThrottleController.ScheduledUpdate(initialAction);

        // Replace with new actions
        Runnable latestAction = null;
        for (int i = 0; i < numberOfReplacements; i++) {
            latestAction = () -> {};
            actions.add(latestAction);
            scheduledUpdate.setUpdateAction(latestAction);
        }

        // Verify: getUpdateAction returns the latest
        assertSame(latestAction, scheduledUpdate.getUpdateAction(),
                "getUpdateAction should return the most recently set action");
    }

    /**
     * Property 4: Initial state is not executed and not cancelled
     * <p>
     * **Validates: Requirements 4.2**
     * </p>
     * <p>
     * A newly created ScheduledUpdate SHALL not be marked as executed or cancelled.
     * </p>
     */
    @Example
    @Label("Property 4: Throttle Batching - Initial state is clean")
    void initialStateIsClean() {
        ThrottleController.ScheduledUpdate scheduledUpdate =
                new ThrottleController.ScheduledUpdate(() -> {});

        assertFalse(scheduledUpdate.isExecuted(),
                "New ScheduledUpdate should not be marked as executed");
        assertFalse(scheduledUpdate.isCancelled(),
                "New ScheduledUpdate should not be marked as cancelled");
        assertNotNull(scheduledUpdate.getUpdateAction(),
                "New ScheduledUpdate should have an action");
    }

    // ========== Arbitraries (Generators) ==========

    /**
     * Generates sequences of props values for testing.
     */
    @Provide
    Arbitrary<List<Integer>> propsValueSequence() {
        return Arbitraries.integers()
                .between(0, 1000)
                .list()
                .ofMinSize(2)
                .ofMaxSize(50);
    }
}
