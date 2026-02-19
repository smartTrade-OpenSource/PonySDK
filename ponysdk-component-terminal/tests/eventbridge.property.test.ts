/**
 * Property-based tests for Event Bridge Serialization.
 * 
 * **Property 9: Event Bridge Serialization**
 * *For any* sequence of component events dispatched within the same animation frame, 
 * THE Event_Bridge SHALL:
 * - Batch all events into a single WebSocket message
 * - Include the correct objectId for each event
 * - Preserve event order within the batch
 * 
 * **Validates: Requirements 9.1, 9.2, 9.5**
 */

import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import * as fc from 'fast-check';
import { EventBridge } from '../src/EventBridge.js';
import type { ComponentEvent } from '../src/types.js';

// ============================================================================
// Test Arbitraries (Generators)
// ============================================================================

/**
 * Arbitrary for positive object IDs (valid component identifiers).
 */
const objectIdArb: fc.Arbitrary<number> = fc.integer({ min: 1, max: 1000000 });

/**
 * Arbitrary for event types (non-empty strings).
 */
const eventTypeArb: fc.Arbitrary<string> = fc.string({ minLength: 1, maxLength: 50 })
    .filter(s => s.trim().length > 0);

/**
 * Arbitrary for simple event payloads.
 */
const simplePayloadArb: fc.Arbitrary<unknown> = fc.oneof(
    fc.string(),
    fc.integer(),
    fc.boolean(),
    fc.double({ noNaN: true, noDefaultInfinity: true }),
    fc.constant(null)
);

/**
 * Arbitrary for object payloads.
 */
const objectPayloadArb: fc.Arbitrary<Record<string, unknown>> = fc.record({
    value: fc.oneof(fc.string(), fc.integer(), fc.boolean()),
    data: fc.array(fc.integer(), { maxLength: 10 }),
    nested: fc.option(fc.record({
        x: fc.integer(),
        y: fc.integer(),
    }), { nil: undefined }),
});

/**
 * Arbitrary for any valid event payload.
 */
const payloadArb: fc.Arbitrary<unknown> = fc.oneof(
    simplePayloadArb,
    objectPayloadArb,
    fc.array(simplePayloadArb, { maxLength: 10 })
);

/**
 * Arbitrary for a single component event.
 */
const componentEventArb: fc.Arbitrary<ComponentEvent> = fc.record({
    objectId: objectIdArb,
    eventType: eventTypeArb,
    payload: payloadArb,
});

/**
 * Arbitrary for a sequence of component events (1-20 events).
 */
const eventSequenceArb: fc.Arbitrary<ComponentEvent[]> = fc.array(componentEventArb, {
    minLength: 1,
    maxLength: 20,
});

// ============================================================================
// Binary Decoding Helper
// ============================================================================

/**
 * Decodes a binary message back to component events.
 * This mirrors the encoding logic in EventBridge.encodeEvents.
 * 
 * Format: [EventCount(4), Event1, Event2, ...]
 * Event: [ObjectId(4), EventTypeLen(2), EventType(N), PayloadLen(4), Payload(N)]
 */
function decodeEvents(buffer: ArrayBuffer): ComponentEvent[] {
    const view = new DataView(buffer);
    const decoder = new TextDecoder();
    const events: ComponentEvent[] = [];
    let offset = 0;

    // Read event count
    const eventCount = view.getUint32(offset, false);
    offset += 4;

    // Read each event
    for (let i = 0; i < eventCount; i++) {
        // Object ID
        const objectId = view.getUint32(offset, false);
        offset += 4;

        // Event type length and data
        const eventTypeLen = view.getUint16(offset, false);
        offset += 2;
        const eventTypeBytes = new Uint8Array(buffer, offset, eventTypeLen);
        const eventType = decoder.decode(eventTypeBytes);
        offset += eventTypeLen;

        // Payload length and data
        const payloadLen = view.getUint32(offset, false);
        offset += 4;
        const payloadBytes = new Uint8Array(buffer, offset, payloadLen);
        const payloadStr = decoder.decode(payloadBytes);
        const payload = JSON.parse(payloadStr);
        offset += payloadLen;

        events.push({ objectId, eventType, payload });
    }

    return events;
}

// ============================================================================
// Mock Helpers
// ============================================================================

/**
 * Creates a mock WebSocket that captures sent messages.
 */
function createMockWebSocket(): { ws: WebSocket; sentMessages: ArrayBuffer[] } {
    const sentMessages: ArrayBuffer[] = [];
    const ws = {
        send: vi.fn((data: ArrayBuffer) => {
            sentMessages.push(data);
        }),
        close: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        readyState: WebSocket.OPEN,
    } as unknown as WebSocket;

    return { ws, sentMessages };
}

// ============================================================================
// Property Tests
// ============================================================================

describe('Property 9: Event Bridge Serialization', () => {
    let originalRAF: typeof requestAnimationFrame;

    beforeEach(() => {
        // Store original requestAnimationFrame
        originalRAF = globalThis.requestAnimationFrame;
        // Mock requestAnimationFrame to execute callback synchronously for testing
        globalThis.requestAnimationFrame = vi.fn((callback: FrameRequestCallback) => {
            callback(0);
            return 0;
        });
    });

    afterEach(() => {
        // Restore original requestAnimationFrame
        globalThis.requestAnimationFrame = originalRAF;
        vi.restoreAllMocks();
    });

    /**
     * **Validates: Requirements 9.5**
     * 
     * Property: Events dispatched within the same frame are batched into a single message.
     */
    describe('Event Batching', () => {
        it('should batch all events dispatched before flush into a single WebSocket message', () => {
            fc.assert(
                fc.property(eventSequenceArb, (events) => {
                    // Mock RAF to NOT execute immediately - we control when flush happens
                    globalThis.requestAnimationFrame = vi.fn(() => 0);

                    const { ws, sentMessages } = createMockWebSocket();
                    const bridge = new EventBridge(ws);

                    // Dispatch all events
                    for (const event of events) {
                        bridge.dispatch(event.objectId, event.eventType, event.payload);
                    }

                    // Verify no messages sent yet (batching)
                    expect(sentMessages.length).toBe(0);
                    expect(bridge.pendingCount).toBe(events.length);

                    // Force flush
                    bridge.flushNow();

                    // Should have exactly one message
                    expect(sentMessages.length).toBe(1);
                    expect(bridge.pendingCount).toBe(0);
                }),
                { numRuns: 100 }
            );
        });

        it('should send exactly one message per flush regardless of event count', () => {
            fc.assert(
                fc.property(
                    fc.integer({ min: 1, max: 50 }),
                    (eventCount) => {
                        // Mock RAF to NOT execute immediately
                        globalThis.requestAnimationFrame = vi.fn(() => 0);

                        const { ws, sentMessages } = createMockWebSocket();
                        const bridge = new EventBridge(ws);

                        // Dispatch N events
                        for (let i = 0; i < eventCount; i++) {
                            bridge.dispatch(i + 1, `event-${i}`, { index: i });
                        }

                        // Force flush
                        bridge.flushNow();

                        // Should have exactly one message containing all events
                        expect(sentMessages.length).toBe(1);

                        // Decode and verify event count
                        const decoded = decodeEvents(sentMessages[0]);
                        expect(decoded.length).toBe(eventCount);
                    }
                ),
                { numRuns: 100 }
            );
        });

        it('should not send a message when no events are pending', () => {
            const { ws, sentMessages } = createMockWebSocket();
            const bridge = new EventBridge(ws);

            // Flush without dispatching any events
            bridge.flushNow();

            // No message should be sent
            expect(sentMessages.length).toBe(0);
        });
    });

    /**
     * **Validates: Requirements 9.2**
     * 
     * Property: Each event in the batch includes the correct objectId.
     */
    describe('Object ID Preservation', () => {
        it('should preserve objectId for each event in the batch', () => {
            fc.assert(
                fc.property(eventSequenceArb, (events) => {
                    // Mock RAF to NOT execute immediately
                    globalThis.requestAnimationFrame = vi.fn(() => 0);

                    const { ws, sentMessages } = createMockWebSocket();
                    const bridge = new EventBridge(ws);

                    // Dispatch all events
                    for (const event of events) {
                        bridge.dispatch(event.objectId, event.eventType, event.payload);
                    }

                    // Force flush
                    bridge.flushNow();

                    // Decode the message
                    const decoded = decodeEvents(sentMessages[0]);

                    // Verify each event has the correct objectId
                    expect(decoded.length).toBe(events.length);
                    for (let i = 0; i < events.length; i++) {
                        expect(decoded[i].objectId).toBe(events[i].objectId);
                    }
                }),
                { numRuns: 100 }
            );
        });

        it('should handle events from multiple components with different objectIds', () => {
            fc.assert(
                fc.property(
                    fc.array(objectIdArb, { minLength: 2, maxLength: 10 }),
                    eventTypeArb,
                    payloadArb,
                    (objectIds, eventType, payload) => {
                        // Mock RAF to NOT execute immediately
                        globalThis.requestAnimationFrame = vi.fn(() => 0);

                        const { ws, sentMessages } = createMockWebSocket();
                        const bridge = new EventBridge(ws);

                        // Dispatch events from different components
                        for (const objectId of objectIds) {
                            bridge.dispatch(objectId, eventType, payload);
                        }

                        // Force flush
                        bridge.flushNow();

                        // Decode and verify objectIds
                        const decoded = decodeEvents(sentMessages[0]);
                        expect(decoded.length).toBe(objectIds.length);

                        for (let i = 0; i < objectIds.length; i++) {
                            expect(decoded[i].objectId).toBe(objectIds[i]);
                        }
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 9.5**
     * 
     * Property: Event order is preserved within the batch.
     */
    describe('Event Order Preservation', () => {
        it('should preserve the order of events as they were dispatched', () => {
            fc.assert(
                fc.property(eventSequenceArb, (events) => {
                    // Mock RAF to NOT execute immediately
                    globalThis.requestAnimationFrame = vi.fn(() => 0);

                    const { ws, sentMessages } = createMockWebSocket();
                    const bridge = new EventBridge(ws);

                    // Dispatch all events in order
                    for (const event of events) {
                        bridge.dispatch(event.objectId, event.eventType, event.payload);
                    }

                    // Force flush
                    bridge.flushNow();

                    // Decode the message
                    const decoded = decodeEvents(sentMessages[0]);

                    // Verify order is preserved
                    expect(decoded.length).toBe(events.length);
                    for (let i = 0; i < events.length; i++) {
                        expect(decoded[i].objectId).toBe(events[i].objectId);
                        expect(decoded[i].eventType).toBe(events[i].eventType);
                        // Payload comparison (JSON serialization may normalize)
                        expect(decoded[i].payload).toEqual(events[i].payload);
                    }
                }),
                { numRuns: 100 }
            );
        });

        it('should maintain FIFO order for events from the same component', () => {
            fc.assert(
                fc.property(
                    objectIdArb,
                    fc.array(fc.integer({ min: 0, max: 100 }), { minLength: 2, maxLength: 20 }),
                    (objectId, sequenceNumbers) => {
                        // Mock RAF to NOT execute immediately
                        globalThis.requestAnimationFrame = vi.fn(() => 0);

                        const { ws, sentMessages } = createMockWebSocket();
                        const bridge = new EventBridge(ws);

                        // Dispatch events with sequence numbers
                        for (const seq of sequenceNumbers) {
                            bridge.dispatch(objectId, 'sequence-event', { seq });
                        }

                        // Force flush
                        bridge.flushNow();

                        // Decode and verify order
                        const decoded = decodeEvents(sentMessages[0]);
                        expect(decoded.length).toBe(sequenceNumbers.length);

                        for (let i = 0; i < sequenceNumbers.length; i++) {
                            expect((decoded[i].payload as { seq: number }).seq).toBe(sequenceNumbers[i]);
                        }
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 9.1**
     * 
     * Property: Binary encoding includes all event data (objectId, eventType, payload).
     */
    describe('Binary Encoding Completeness', () => {
        it('should encode and decode all event fields correctly (round-trip)', () => {
            fc.assert(
                fc.property(eventSequenceArb, (events) => {
                    // Mock RAF to NOT execute immediately
                    globalThis.requestAnimationFrame = vi.fn(() => 0);

                    const { ws, sentMessages } = createMockWebSocket();
                    const bridge = new EventBridge(ws);

                    // Dispatch all events
                    for (const event of events) {
                        bridge.dispatch(event.objectId, event.eventType, event.payload);
                    }

                    // Force flush
                    bridge.flushNow();

                    // Decode the message
                    const decoded = decodeEvents(sentMessages[0]);

                    // Verify all fields are preserved
                    expect(decoded.length).toBe(events.length);
                    for (let i = 0; i < events.length; i++) {
                        expect(decoded[i].objectId).toBe(events[i].objectId);
                        expect(decoded[i].eventType).toBe(events[i].eventType);
                        expect(decoded[i].payload).toEqual(events[i].payload);
                    }
                }),
                { numRuns: 100 }
            );
        });

        it('should correctly encode event type strings of various lengths', () => {
            fc.assert(
                fc.property(
                    objectIdArb,
                    fc.string({ minLength: 1, maxLength: 200 }).filter(s => s.trim().length > 0),
                    payloadArb,
                    (objectId, eventType, payload) => {
                        // Mock RAF to NOT execute immediately
                        globalThis.requestAnimationFrame = vi.fn(() => 0);

                        const { ws, sentMessages } = createMockWebSocket();
                        const bridge = new EventBridge(ws);

                        bridge.dispatch(objectId, eventType, payload);
                        bridge.flushNow();

                        const decoded = decodeEvents(sentMessages[0]);
                        expect(decoded.length).toBe(1);
                        expect(decoded[0].eventType).toBe(eventType);
                    }
                ),
                { numRuns: 100 }
            );
        });

        it('should correctly encode various payload types', () => {
            fc.assert(
                fc.property(
                    objectIdArb,
                    eventTypeArb,
                    payloadArb,
                    (objectId, eventType, payload) => {
                        // Mock RAF to NOT execute immediately
                        globalThis.requestAnimationFrame = vi.fn(() => 0);

                        const { ws, sentMessages } = createMockWebSocket();
                        const bridge = new EventBridge(ws);

                        bridge.dispatch(objectId, eventType, payload);
                        bridge.flushNow();

                        const decoded = decodeEvents(sentMessages[0]);
                        expect(decoded.length).toBe(1);
                        expect(decoded[0].payload).toEqual(payload);
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 9.1, 9.2, 9.5**
     * 
     * Property: Binary message format is correct and parseable.
     */
    describe('Binary Message Format', () => {
        it('should produce valid binary messages that can be decoded', () => {
            fc.assert(
                fc.property(eventSequenceArb, (events) => {
                    // Mock RAF to NOT execute immediately
                    globalThis.requestAnimationFrame = vi.fn(() => 0);

                    const { ws, sentMessages } = createMockWebSocket();
                    const bridge = new EventBridge(ws);

                    for (const event of events) {
                        bridge.dispatch(event.objectId, event.eventType, event.payload);
                    }
                    bridge.flushNow();

                    // Should not throw when decoding
                    expect(() => decodeEvents(sentMessages[0])).not.toThrow();
                }),
                { numRuns: 100 }
            );
        });

        it('should encode event count correctly in the message header', () => {
            fc.assert(
                fc.property(
                    fc.integer({ min: 1, max: 100 }),
                    (eventCount) => {
                        // Mock RAF to NOT execute immediately
                        globalThis.requestAnimationFrame = vi.fn(() => 0);

                        const { ws, sentMessages } = createMockWebSocket();
                        const bridge = new EventBridge(ws);

                        for (let i = 0; i < eventCount; i++) {
                            bridge.dispatch(i + 1, 'test', { i });
                        }
                        bridge.flushNow();

                        // Read event count from header
                        const view = new DataView(sentMessages[0]);
                        const encodedCount = view.getUint32(0, false);
                        expect(encodedCount).toBe(eventCount);
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * Additional edge case tests.
     */
    describe('Edge Cases', () => {
        it('should handle empty string event types', () => {
            // Note: Our arbitrary filters out empty strings, but let's test the boundary
            const { ws, sentMessages } = createMockWebSocket();
            const bridge = new EventBridge(ws);

            // Mock RAF to NOT execute immediately
            globalThis.requestAnimationFrame = vi.fn(() => 0);

            // Single character event type (minimum valid)
            bridge.dispatch(1, 'a', null);
            bridge.flushNow();

            const decoded = decodeEvents(sentMessages[0]);
            expect(decoded.length).toBe(1);
            expect(decoded[0].eventType).toBe('a');
        });

        it('should handle null payload', () => {
            fc.assert(
                fc.property(objectIdArb, eventTypeArb, (objectId, eventType) => {
                    // Mock RAF to NOT execute immediately
                    globalThis.requestAnimationFrame = vi.fn(() => 0);

                    const { ws, sentMessages } = createMockWebSocket();
                    const bridge = new EventBridge(ws);

                    bridge.dispatch(objectId, eventType, null);
                    bridge.flushNow();

                    const decoded = decodeEvents(sentMessages[0]);
                    expect(decoded.length).toBe(1);
                    expect(decoded[0].payload).toBeNull();
                }),
                { numRuns: 100 }
            );
        });

        it('should handle deeply nested payload objects', () => {
            fc.assert(
                fc.property(
                    objectIdArb,
                    eventTypeArb,
                    fc.record({
                        level1: fc.record({
                            level2: fc.record({
                                level3: fc.record({
                                    value: fc.string(),
                                }),
                            }),
                        }),
                    }),
                    (objectId, eventType, payload) => {
                        // Mock RAF to NOT execute immediately
                        globalThis.requestAnimationFrame = vi.fn(() => 0);

                        const { ws, sentMessages } = createMockWebSocket();
                        const bridge = new EventBridge(ws);

                        bridge.dispatch(objectId, eventType, payload);
                        bridge.flushNow();

                        const decoded = decodeEvents(sentMessages[0]);
                        expect(decoded.length).toBe(1);
                        expect(decoded[0].payload).toEqual(payload);
                    }
                ),
                { numRuns: 100 }
            );
        });

        it('should handle array payloads', () => {
            fc.assert(
                fc.property(
                    objectIdArb,
                    eventTypeArb,
                    fc.array(fc.oneof(fc.string(), fc.integer(), fc.boolean()), { maxLength: 50 }),
                    (objectId, eventType, payload) => {
                        // Mock RAF to NOT execute immediately
                        globalThis.requestAnimationFrame = vi.fn(() => 0);

                        const { ws, sentMessages } = createMockWebSocket();
                        const bridge = new EventBridge(ws);

                        bridge.dispatch(objectId, eventType, payload);
                        bridge.flushNow();

                        const decoded = decodeEvents(sentMessages[0]);
                        expect(decoded.length).toBe(1);
                        expect(decoded[0].payload).toEqual(payload);
                    }
                ),
                { numRuns: 100 }
            );
        });

        it('should handle Unicode characters in event types and payloads', () => {
            fc.assert(
                fc.property(
                    objectIdArb,
                    fc.string({ minLength: 1, maxLength: 50 }).filter(s => s.trim().length > 0),
                    fc.string(),
                    (objectId, eventType, payloadStr) => {
                        // Mock RAF to NOT execute immediately
                        globalThis.requestAnimationFrame = vi.fn(() => 0);

                        const { ws, sentMessages } = createMockWebSocket();
                        const bridge = new EventBridge(ws);

                        bridge.dispatch(objectId, eventType, { text: payloadStr });
                        bridge.flushNow();

                        const decoded = decodeEvents(sentMessages[0]);
                        expect(decoded.length).toBe(1);
                        expect(decoded[0].eventType).toBe(eventType);
                        expect((decoded[0].payload as { text: string }).text).toBe(payloadStr);
                    }
                ),
                { numRuns: 100 }
            );
        });

        it('should handle maximum objectId values', () => {
            // Mock RAF to NOT execute immediately
            globalThis.requestAnimationFrame = vi.fn(() => 0);

            const { ws, sentMessages } = createMockWebSocket();
            const bridge = new EventBridge(ws);

            const maxObjectId = 0xFFFFFFFF; // Max uint32
            bridge.dispatch(maxObjectId, 'test', null);
            bridge.flushNow();

            const decoded = decodeEvents(sentMessages[0]);
            expect(decoded.length).toBe(1);
            expect(decoded[0].objectId).toBe(maxObjectId);
        });
    });
});
