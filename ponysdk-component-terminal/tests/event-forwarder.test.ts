/**
 * Unit tests for EventForwarder.
 *
 * Requirements: 1.3 - Transmit input value changes to server via EventBridge
 * Requirements: 2.4 - Transmit alert close events to server
 * Requirements: 3.2 - Transmit selected tab ID to server
 * Requirements: 3.5 - Transmit selected menu item ID to server
 * Requirements: 5.3 - Transmit dialog close events (close button, Escape, overlay) to server
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { EventForwarder } from '../src/events/EventForwarder.js';
import type { EventBridge } from '../src/EventBridge.js';

// ============================================================================
// Test Helpers
// ============================================================================

function createMockEventBridge(): EventBridge {
    return {
        dispatch: vi.fn(),
        pendingCount: 0,
        flushNow: vi.fn(),
    } as unknown as EventBridge;
}

function createElement(tagName: string, props: Record<string, unknown> = {}): HTMLElement {
    const el = document.createElement(tagName);
    for (const [key, value] of Object.entries(props)) {
        Object.defineProperty(el, key, { value, writable: true, configurable: true });
    }
    return el;
}

function fireEvent(el: HTMLElement, eventType: string, detail?: unknown): void {
    const event = detail !== undefined
        ? new CustomEvent(eventType, { bubbles: true, detail })
        : new Event(eventType, { bubbles: true });
    el.dispatchEvent(event);
}

// ============================================================================
// Tests
// ============================================================================

describe('EventForwarder', () => {
    let eventBridge: EventBridge;
    const objectId = 42;

    beforeEach(() => {
        eventBridge = createMockEventBridge();
    });

    describe('wa-change', () => {
        it('should forward value for text input', () => {
            const el = createElement('wa-input', { value: 'hello' });
            const forwarder = new EventForwarder(el, eventBridge, objectId);
            forwarder.attach();

            fireEvent(el, 'wa-change');

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-change', { value: 'hello' });
            forwarder.detach();
        });

        it('should forward checked for checkbox', () => {
            const el = createElement('wa-checkbox', { checked: true });
            const forwarder = new EventForwarder(el, eventBridge, objectId);
            forwarder.attach();

            fireEvent(el, 'wa-change');

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-change', { checked: true });
            forwarder.detach();
        });

        it('should forward checked for switch', () => {
            const el = createElement('wa-switch', { checked: false });
            const forwarder = new EventForwarder(el, eventBridge, objectId);
            forwarder.attach();

            fireEvent(el, 'wa-change');

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-change', { checked: false });
            forwarder.detach();
        });
    });

    describe('wa-input', () => {
        it('should forward current value', () => {
            const el = createElement('wa-input', { value: 'typing...' });
            const forwarder = new EventForwarder(el, eventBridge, objectId);
            forwarder.attach();

            fireEvent(el, 'wa-input');

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-input', { value: 'typing...' });
            forwarder.detach();
        });
    });

    describe('wa-select', () => {
        it('should forward selected value', () => {
            const el = createElement('wa-select', { value: 'option-2' });
            const forwarder = new EventForwarder(el, eventBridge, objectId);
            forwarder.attach();

            fireEvent(el, 'wa-select');

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-select', { value: 'option-2' });
            forwarder.detach();
        });
    });

    describe('wa-close', () => {
        it('should forward empty payload for alert close', () => {
            const el = createElement('wa-alert');
            const forwarder = new EventForwarder(el, eventBridge, objectId);
            forwarder.attach();

            fireEvent(el, 'wa-close');

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-close', {});
            forwarder.detach();
        });
    });

    describe('wa-request-close (dialog)', () => {
        it('should forward source: close-button', () => {
            const el = createElement('wa-dialog');
            const forwarder = new EventForwarder(el, eventBridge, objectId);
            forwarder.attach();

            fireEvent(el, 'wa-request-close', { source: 'close-button' });

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-request-close', { source: 'close-button' });
            forwarder.detach();
        });

        it('should forward source: keyboard (Escape)', () => {
            const el = createElement('wa-dialog');
            const forwarder = new EventForwarder(el, eventBridge, objectId);
            forwarder.attach();

            fireEvent(el, 'wa-request-close', { source: 'keyboard' });

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-request-close', { source: 'keyboard' });
            forwarder.detach();
        });

        it('should forward source: overlay', () => {
            const el = createElement('wa-dialog');
            const forwarder = new EventForwarder(el, eventBridge, objectId);
            forwarder.attach();

            fireEvent(el, 'wa-request-close', { source: 'overlay' });

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-request-close', { source: 'overlay' });
            forwarder.detach();
        });

        it('should handle missing detail gracefully', () => {
            const el = createElement('wa-dialog');
            const forwarder = new EventForwarder(el, eventBridge, objectId);
            forwarder.attach();

            // Fire without detail
            fireEvent(el, 'wa-request-close');

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-request-close', { source: 'unknown' });
            forwarder.detach();
        });
    });

    describe('wa-focus / wa-blur', () => {
        it('should forward empty payload for focus', () => {
            const el = createElement('wa-input');
            const forwarder = new EventForwarder(el, eventBridge, objectId);
            forwarder.attach();

            fireEvent(el, 'wa-focus');

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-focus', {});
            forwarder.detach();
        });

        it('should forward empty payload for blur', () => {
            const el = createElement('wa-input');
            const forwarder = new EventForwarder(el, eventBridge, objectId);
            forwarder.attach();

            fireEvent(el, 'wa-blur');

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-blur', {});
            forwarder.detach();
        });
    });

    describe('wa-show / wa-hide / wa-after-show / wa-after-hide', () => {
        it('should forward empty payload for show/hide lifecycle events', () => {
            const el = createElement('wa-dialog');
            const forwarder = new EventForwarder(el, eventBridge, objectId);
            forwarder.attach();

            for (const eventType of ['wa-show', 'wa-hide', 'wa-after-show', 'wa-after-hide']) {
                fireEvent(el, eventType);
                expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, eventType, {});
            }

            forwarder.detach();
        });
    });

    describe('lifecycle', () => {
        it('should not forward events before attach', () => {
            const el = createElement('wa-input', { value: 'test' });
            const forwarder = new EventForwarder(el, eventBridge, objectId);

            fireEvent(el, 'wa-change');

            expect(eventBridge.dispatch).not.toHaveBeenCalled();
        });

        it('should not forward events after detach', () => {
            const el = createElement('wa-input', { value: 'test' });
            const forwarder = new EventForwarder(el, eventBridge, objectId);
            forwarder.attach();
            forwarder.detach();

            fireEvent(el, 'wa-change');

            expect(eventBridge.dispatch).not.toHaveBeenCalled();
        });

        it('attach should be idempotent', () => {
            const el = createElement('wa-input', { value: 'test' });
            const forwarder = new EventForwarder(el, eventBridge, objectId);
            forwarder.attach();
            forwarder.attach(); // second call should be no-op

            fireEvent(el, 'wa-change');

            // Should only be called once (not twice from double-attach)
            expect(eventBridge.dispatch).toHaveBeenCalledTimes(1);
            forwarder.detach();
        });

        it('detach should be idempotent', () => {
            const el = createElement('wa-input');
            const forwarder = new EventForwarder(el, eventBridge, objectId);
            forwarder.attach();

            forwarder.detach();
            forwarder.detach(); // second call should be no-op

            expect(forwarder.isAttached()).toBe(false);
        });

        it('isAttached should reflect state', () => {
            const el = createElement('wa-input');
            const forwarder = new EventForwarder(el, eventBridge, objectId);

            expect(forwarder.isAttached()).toBe(false);
            forwarder.attach();
            expect(forwarder.isAttached()).toBe(true);
            forwarder.detach();
            expect(forwarder.isAttached()).toBe(false);
        });
    });

    describe('value reads current element state', () => {
        it('should read the value at event time, not at attach time', () => {
            const el = createElement('wa-input', { value: 'initial' });
            const forwarder = new EventForwarder(el, eventBridge, objectId);
            forwarder.attach();

            // Mutate value after attach
            (el as any).value = 'updated';
            fireEvent(el, 'wa-change');

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-change', { value: 'updated' });
            forwarder.detach();
        });
    });
});
