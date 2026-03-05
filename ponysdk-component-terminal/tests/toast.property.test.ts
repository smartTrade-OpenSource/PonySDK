/**
 * Property-based tests for Toast Queue Ordering.
 *
 * **Property 15: Toast Queue Ordering**
 * *For any* sequence of N toast notifications, the toast queue SHALL display them
 * in FIFO order, and each toast with a defined duration SHALL be automatically
 * removed after its duration expires.
 *
 * **Validates: Requirements 5.4**
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import * as fc from 'fast-check';
import { ToastQueue, ToastOptions } from '../src/toast/ToastQueue.js';

// ============================================================================
// Test Arbitraries (Generators)
// ============================================================================

/** Arbitrary for toast variant */
const variantArb: fc.Arbitrary<ToastOptions['variant']> = fc.constantFrom(
    'primary', 'success', 'neutral', 'warning', 'danger',
);

/** Arbitrary for a toast with a positive auto-close duration */
const toastWithDurationArb: fc.Arbitrary<ToastOptions> = fc.record({
    message: fc.string({ minLength: 1, maxLength: 100 }),
    variant: variantArb,
    duration: fc.integer({ min: 100, max: 10000 }),
});

/** Arbitrary for a non-empty sequence of toasts with durations */
const toastSequenceArb: fc.Arbitrary<ToastOptions[]> = fc.array(
    toastWithDurationArb,
    { minLength: 1, maxLength: 10 },
);

// ============================================================================
// Property Tests
// ============================================================================

describe('Property 15: Toast Queue Ordering @Tag("Feature: ui-library-wrapper, Property 15: Toast Queue Ordering")', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    /**
     * **Validates: Requirements 5.4**
     *
     * For any sequence of N toasts enqueued at once, the first toast
     * is displayed immediately.
     */
    it('first enqueued toast is displayed immediately', () => {
        fc.assert(
            fc.property(toastSequenceArb, (toasts) => {
                const container = document.createElement('div');
                const queue = new ToastQueue(container);

                for (const toast of toasts) {
                    queue.enqueue(toast);
                }

                // The first toast should be showing
                expect(queue.isShowing).toBe(true);
                // The container should have exactly one wa-alert child
                const alerts = container.querySelectorAll('wa-alert');
                expect(alerts.length).toBe(1);
                expect(alerts[0].textContent).toBe(toasts[0].message);

                queue.clear();
            }),
            { numRuns: 100 },
        );
    });

    /**
     * **Validates: Requirements 5.4**
     *
     * For any sequence of N toasts with durations, advancing time by each
     * toast's duration causes the next toast in FIFO order to be displayed.
     */
    it('toasts display in FIFO order as each auto-closes', () => {
        fc.assert(
            fc.property(toastSequenceArb, (toasts) => {
                const container = document.createElement('div');
                const queue = new ToastQueue(container);

                for (const toast of toasts) {
                    queue.enqueue(toast);
                }

                // Walk through each toast in order
                for (let i = 0; i < toasts.length; i++) {
                    expect(queue.isShowing).toBe(true);
                    const alert = container.querySelector('wa-alert');
                    expect(alert).not.toBeNull();
                    expect(alert!.textContent).toBe(toasts[i].message);

                    // Advance time by this toast's duration to trigger auto-close
                    vi.advanceTimersByTime(toasts[i].duration!);
                }

                // After all durations have elapsed, no toast should be showing
                expect(queue.isShowing).toBe(false);
                expect(container.querySelectorAll('wa-alert').length).toBe(0);
            }),
            { numRuns: 100 },
        );
    });

    /**
     * **Validates: Requirements 5.4**
     *
     * After all N toasts with durations have auto-closed, the queue is
     * empty and no toast is showing.
     */
    it('queue is empty after all toasts auto-close', () => {
        fc.assert(
            fc.property(toastSequenceArb, (toasts) => {
                const container = document.createElement('div');
                const queue = new ToastQueue(container);

                for (const toast of toasts) {
                    queue.enqueue(toast);
                }

                // Advance time past all durations combined
                const totalDuration = toasts.reduce((sum, t) => sum + t.duration!, 0);
                vi.advanceTimersByTime(totalDuration);

                expect(queue.isShowing).toBe(false);
                expect(queue.getQueueLength()).toBe(0);
                expect(container.querySelectorAll('wa-alert').length).toBe(0);
            }),
            { numRuns: 100 },
        );
    });

    /**
     * **Validates: Requirements 5.4**
     *
     * The order in which toasts are displayed matches the order in which
     * they were enqueued (FIFO).
     */
    it('display order matches enqueue order (FIFO)', () => {
        fc.assert(
            fc.property(toastSequenceArb, (toasts) => {
                const container = document.createElement('div');
                const queue = new ToastQueue(container);
                const displayedMessages: string[] = [];

                for (const toast of toasts) {
                    queue.enqueue(toast);
                }

                // Collect displayed messages in order
                for (let i = 0; i < toasts.length; i++) {
                    const alert = container.querySelector('wa-alert');
                    if (alert) {
                        displayedMessages.push(alert.textContent ?? '');
                    }
                    vi.advanceTimersByTime(toasts[i].duration!);
                }

                // Verify FIFO ordering
                const expectedMessages = toasts.map(t => t.message);
                expect(displayedMessages).toEqual(expectedMessages);
            }),
            { numRuns: 100 },
        );
    });
});
