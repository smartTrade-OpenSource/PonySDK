/**
 * Property-based tests for Event Forwarding Correctness.
 *
 * **Property 2: Event Forwarding Correctness**
 * *For any* UI wrapper component and any user interaction event, EventBridge SHALL dispatch
 * an event containing the correct objectId and a payload that faithfully represents the
 * user's action.
 *
 * **Validates: Requirements 1.3, 3.2, 3.5, 5.3, 8.3, 8.4**
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import * as fc from 'fast-check';
import { EventForwarder } from '../src/events/EventForwarder.js';
import { EventBridge } from '../src/EventBridge.js';

// ============================================================================
// Test Arbitraries (Generators)
// ============================================================================

/** Arbitrary for wa-* component tags */
const componentTagArb: fc.Arbitrary<string> = fc.constantFrom(
    'wa-input', 'wa-textarea', 'wa-select',
    'wa-checkbox', 'wa-switch',
    'wa-dialog', 'wa-alert', 'wa-drawer',
    'wa-tab-group', 'wa-menu', 'wa-dropdown',
    'wa-button', 'wa-badge', 'wa-tooltip',
);

/** Tags that report `checked` instead of `value` on wa-change */
const checkedTagArb: fc.Arbitrary<string> = fc.constantFrom('wa-checkbox', 'wa-switch');

/** Tags that report `value` on wa-change */
const valueTagArb: fc.Arbitrary<string> = fc.constantFrom(
    'wa-input', 'wa-textarea', 'wa-select',
    'wa-dialog', 'wa-alert', 'wa-drawer',
    'wa-tab-group', 'wa-menu', 'wa-dropdown',
    'wa-button', 'wa-badge', 'wa-tooltip',
);

/** Arbitrary for positive object IDs */
const objectIdArb: fc.Arbitrary<number> = fc.integer({ min: 1, max: 100000 });

/** Arbitrary for input string values */
const valueArb: fc.Arbitrary<string> = fc.string({ maxLength: 200 });

/** Arbitrary for boolean checked state */
const checkedArb: fc.Arbitrary<boolean> = fc.boolean();

/** Arbitrary for wa-request-close source strings */
const closeSourceArb: fc.Arbitrary<string> = fc.constantFrom(
    'close-button', 'keyboard', 'overlay',
);

/** Events that produce an empty payload */
const emptyPayloadEventArb: fc.Arbitrary<string> = fc.constantFrom(
    'wa-close', 'wa-focus', 'wa-blur',
    'wa-show', 'wa-hide', 'wa-after-show', 'wa-after-hide',
);

// ============================================================================
// Test Helpers
// ============================================================================

function createMockWebSocket(): WebSocket {
    return {
        send: vi.fn(),
        readyState: WebSocket.OPEN,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
    } as unknown as WebSocket;
}

function createElement(tagName: string): HTMLElement {
    const el = document.createElement('div');
    // jsdom doesn't support custom elements, so we override tagName via a property
    Object.defineProperty(el, 'tagName', { value: tagName.toUpperCase(), configurable: true });
    return el;
}

function fireEvent(element: HTMLElement, eventType: string, detail?: unknown): void {
    const event = detail !== undefined
        ? new CustomEvent(eventType, { detail, bubbles: true })
        : new Event(eventType, { bubbles: true });
    element.dispatchEvent(event);
}

// ============================================================================
// Property Tests
// ============================================================================

describe('Property 2: Event Forwarding Correctness', () => {
    let dispatchSpy: ReturnType<typeof vi.fn>;
    let eventBridge: EventBridge;

    beforeEach(() => {
        const ws = createMockWebSocket();
        eventBridge = new EventBridge(ws);
        dispatchSpy = vi.fn();
        eventBridge.dispatch = dispatchSpy;
    });

    /**
     * **Validates: Requirements 1.3, 3.2, 3.5, 5.3, 8.3, 8.4**
     *
     * For any wa-* component tag and any supported event type,
     * the EventForwarder dispatches via EventBridge with the correct objectId.
     */
    describe('ObjectId correctness for all events', () => {
        it('should dispatch with the correct objectId for any component and event', () => {
            fc.assert(
                fc.property(
                    componentTagArb,
                    objectIdArb,
                    emptyPayloadEventArb,
                    (tag, objectId, eventType) => {
                        dispatchSpy.mockClear();
                        const element = createElement(tag);
                        const forwarder = new EventForwarder(element, eventBridge, objectId);
                        forwarder.attach();

                        fireEvent(element, eventType);

                        expect(dispatchSpy).toHaveBeenCalledWith(
                            objectId,
                            eventType,
                            expect.anything(),
                        );

                        forwarder.detach();
                    },
                ),
                { numRuns: 100 },
            );
        });
    });

    /**
     * **Validates: Requirements 1.3**
     *
     * For wa-change events on text input components (not checkbox/switch),
     * the payload contains the current value.
     */
    describe('wa-change payload contains value for text inputs', () => {
        it('should forward the element value for text-based components', () => {
            fc.assert(
                fc.property(
                    valueTagArb,
                    objectIdArb,
                    valueArb,
                    (tag, objectId, value) => {
                        dispatchSpy.mockClear();
                        const element = createElement(tag);
                        (element as any).value = value;

                        const forwarder = new EventForwarder(element, eventBridge, objectId);
                        forwarder.attach();

                        fireEvent(element, 'wa-change');

                        expect(dispatchSpy).toHaveBeenCalledWith(
                            objectId,
                            'wa-change',
                            { value },
                        );

                        forwarder.detach();
                    },
                ),
                { numRuns: 100 },
            );
        });
    });

    /**
     * **Validates: Requirements 1.3**
     *
     * For wa-change events on checkbox/switch, the payload contains the checked state.
     */
    describe('wa-change payload contains checked for checkbox/switch', () => {
        it('should forward the checked state for checkbox and switch', () => {
            fc.assert(
                fc.property(
                    checkedTagArb,
                    objectIdArb,
                    checkedArb,
                    (tag, objectId, checked) => {
                        dispatchSpy.mockClear();
                        const element = createElement(tag);
                        (element as any).checked = checked;

                        const forwarder = new EventForwarder(element, eventBridge, objectId);
                        forwarder.attach();

                        fireEvent(element, 'wa-change');

                        expect(dispatchSpy).toHaveBeenCalledWith(
                            objectId,
                            'wa-change',
                            { checked },
                        );

                        forwarder.detach();
                    },
                ),
                { numRuns: 100 },
            );
        });
    });

    /**
     * **Validates: Requirements 5.3**
     *
     * For wa-request-close events, the payload contains the source string.
     */
    describe('wa-request-close payload contains source', () => {
        it('should forward the close source for request-close events', () => {
            fc.assert(
                fc.property(
                    componentTagArb,
                    objectIdArb,
                    closeSourceArb,
                    (tag, objectId, source) => {
                        dispatchSpy.mockClear();
                        const element = createElement(tag);

                        const forwarder = new EventForwarder(element, eventBridge, objectId);
                        forwarder.attach();

                        fireEvent(element, 'wa-request-close', { source });

                        expect(dispatchSpy).toHaveBeenCalledWith(
                            objectId,
                            'wa-request-close',
                            { source },
                        );

                        forwarder.detach();
                    },
                ),
                { numRuns: 100 },
            );
        });
    });

    /**
     * **Validates: Requirements 3.2, 3.5, 8.3, 8.4**
     *
     * For all other events (wa-focus, wa-blur, wa-show, wa-hide, etc.),
     * the payload is an empty object.
     */
    describe('Empty payload for lifecycle events', () => {
        it('should dispatch an empty payload for non-value events', () => {
            fc.assert(
                fc.property(
                    componentTagArb,
                    objectIdArb,
                    emptyPayloadEventArb,
                    (tag, objectId, eventType) => {
                        dispatchSpy.mockClear();
                        const element = createElement(tag);

                        const forwarder = new EventForwarder(element, eventBridge, objectId);
                        forwarder.attach();

                        fireEvent(element, eventType);

                        expect(dispatchSpy).toHaveBeenCalledWith(
                            objectId,
                            eventType,
                            {},
                        );

                        forwarder.detach();
                    },
                ),
                { numRuns: 100 },
            );
        });
    });
});
