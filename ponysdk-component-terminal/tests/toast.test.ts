/**
 * Unit tests for ToastQueue.
 *
 * Requirements: 5.4 - Toast queue displays notifications sequentially (FIFO)
 * Requirements: 5.5 - Auto-close toast after defined duration
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { ToastQueue } from '../src/toast/ToastQueue.js';
import type { ToastOptions } from '../src/toast/ToastQueue.js';

// ============================================================================
// Test Helpers
// ============================================================================

function getAlerts(container: HTMLElement): HTMLElement[] {
    return Array.from(container.querySelectorAll('wa-alert'));
}

// ============================================================================
// Tests
// ============================================================================

describe('ToastQueue', () => {
    let container: HTMLElement;
    let queue: ToastQueue;

    beforeEach(() => {
        vi.useFakeTimers();
        container = document.createElement('div');
        document.body.appendChild(container);
        queue = new ToastQueue(container);
    });

    afterEach(() => {
        queue.clear();
        container.remove();
        vi.useRealTimers();
    });

    describe('enqueue and display', () => {
        it('should display the first toast immediately', () => {
            queue.enqueue({ message: 'Hello' });

            const alerts = getAlerts(container);
            expect(alerts).toHaveLength(1);
            expect(alerts[0].textContent).toBe('Hello');
            expect(alerts[0].hasAttribute('open')).toBe(true);
        });

        it('should only show one toast at a time', () => {
            queue.enqueue({ message: 'First' });
            queue.enqueue({ message: 'Second' });

            const alerts = getAlerts(container);
            expect(alerts).toHaveLength(1);
            expect(alerts[0].textContent).toBe('First');
        });

        it('should queue length reflect pending toasts', () => {
            queue.enqueue({ message: 'First' });
            queue.enqueue({ message: 'Second' });
            queue.enqueue({ message: 'Third' });

            // First is active, two are pending
            expect(queue.getQueueLength()).toBe(2);
        });

        it('should report isShowing when a toast is active', () => {
            expect(queue.isShowing).toBe(false);
            queue.enqueue({ message: 'Hello' });
            expect(queue.isShowing).toBe(true);
        });
    });

    describe('variant, icon, closable attributes', () => {
        it('should set variant attribute', () => {
            queue.enqueue({ message: 'Success!', variant: 'success' });

            const alert = getAlerts(container)[0];
            expect(alert.getAttribute('variant')).toBe('success');
        });

        it('should set icon attribute', () => {
            queue.enqueue({ message: 'Info', icon: 'info-circle' });

            const alert = getAlerts(container)[0];
            expect(alert.getAttribute('icon')).toBe('info-circle');
        });

        it('should set closable attribute', () => {
            queue.enqueue({ message: 'Closable', closable: true });

            const alert = getAlerts(container)[0];
            expect(alert.hasAttribute('closable')).toBe(true);
        });

        it('should not set optional attributes when not provided', () => {
            queue.enqueue({ message: 'Plain' });

            const alert = getAlerts(container)[0];
            expect(alert.hasAttribute('variant')).toBe(false);
            expect(alert.hasAttribute('icon')).toBe(false);
            expect(alert.hasAttribute('closable')).toBe(false);
        });
    });

    describe('auto-close with duration', () => {
        it('should auto-close after specified duration', () => {
            queue.enqueue({ message: 'Temporary', duration: 3000 });

            expect(getAlerts(container)).toHaveLength(1);

            vi.advanceTimersByTime(3000);

            expect(getAlerts(container)).toHaveLength(0);
            expect(queue.isShowing).toBe(false);
        });

        it('should not auto-close when duration is 0', () => {
            queue.enqueue({ message: 'Persistent', duration: 0 });

            vi.advanceTimersByTime(10000);

            expect(getAlerts(container)).toHaveLength(1);
        });

        it('should not auto-close when duration is undefined', () => {
            queue.enqueue({ message: 'Persistent' });

            vi.advanceTimersByTime(10000);

            expect(getAlerts(container)).toHaveLength(1);
        });
    });

    describe('FIFO ordering', () => {
        it('should show second toast after first auto-closes', () => {
            queue.enqueue({ message: 'First', duration: 1000 });
            queue.enqueue({ message: 'Second' });

            expect(getAlerts(container)[0].textContent).toBe('First');

            vi.advanceTimersByTime(1000);

            const alerts = getAlerts(container);
            expect(alerts).toHaveLength(1);
            expect(alerts[0].textContent).toBe('Second');
        });

        it('should process three toasts in FIFO order', () => {
            queue.enqueue({ message: 'A', duration: 500 });
            queue.enqueue({ message: 'B', duration: 500 });
            queue.enqueue({ message: 'C', duration: 500 });

            expect(getAlerts(container)[0].textContent).toBe('A');

            vi.advanceTimersByTime(500);
            expect(getAlerts(container)[0].textContent).toBe('B');

            vi.advanceTimersByTime(500);
            expect(getAlerts(container)[0].textContent).toBe('C');

            vi.advanceTimersByTime(500);
            expect(getAlerts(container)).toHaveLength(0);
        });

        it('should show next toast when active toast fires wa-after-hide', () => {
            queue.enqueue({ message: 'First' }); // no duration
            queue.enqueue({ message: 'Second' });

            const firstAlert = getAlerts(container)[0];
            // Simulate manual close via wa-after-hide event
            firstAlert.dispatchEvent(new Event('wa-after-hide'));

            const alerts = getAlerts(container);
            expect(alerts).toHaveLength(1);
            expect(alerts[0].textContent).toBe('Second');
        });
    });

    describe('clear', () => {
        it('should remove active toast and pending queue', () => {
            queue.enqueue({ message: 'First', duration: 5000 });
            queue.enqueue({ message: 'Second' });
            queue.enqueue({ message: 'Third' });

            queue.clear();

            expect(getAlerts(container)).toHaveLength(0);
            expect(queue.getQueueLength()).toBe(0);
            expect(queue.isShowing).toBe(false);
        });

        it('should cancel auto-close timer on clear', () => {
            queue.enqueue({ message: 'Timed', duration: 3000 });
            queue.clear();

            // Advancing time should not cause errors
            vi.advanceTimersByTime(5000);

            expect(getAlerts(container)).toHaveLength(0);
        });

        it('should allow enqueue after clear', () => {
            queue.enqueue({ message: 'Before' });
            queue.clear();
            queue.enqueue({ message: 'After' });

            const alerts = getAlerts(container);
            expect(alerts).toHaveLength(1);
            expect(alerts[0].textContent).toBe('After');
        });
    });

    describe('default container', () => {
        it('should use document.body when no container provided', () => {
            const defaultQueue = new ToastQueue();
            defaultQueue.enqueue({ message: 'Body toast' });

            const alerts = Array.from(document.body.querySelectorAll('wa-alert'));
            expect(alerts.some(a => a.textContent === 'Body toast')).toBe(true);

            defaultQueue.clear();
        });
    });
});
