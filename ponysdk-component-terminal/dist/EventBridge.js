/**
 * Event bridge for client-to-server communication.
 *
 * Requirements: 9.1 - THE Event_Bridge SHALL serialize client events using the existing ClientToServerModel protocol
 * Requirements: 9.2 - WHEN an event is dispatched from a component, THE Event_Bridge SHALL include the component's object ID
 * Requirements: 9.5 - THE Event_Bridge SHALL batch multiple events within the same frame for efficiency
 */
/**
 * Bridge for dispatching events from client components to the server.
 * Batches events within the same animation frame for efficiency.
 */
export class EventBridge {
    constructor(websocket) {
        this.pendingEvents = [];
        this.flushScheduled = false;
        this.websocket = websocket;
    }
    /**
     * Dispatch an event to the server.
     * Events are batched within the same animation frame.
     * @param objectId - Object ID of the component
     * @param eventType - Type of event
     * @param payload - Event payload data
     */
    dispatch(objectId, eventType, payload) {
        this.pendingEvents.push({ objectId, eventType, payload });
        this.scheduleFlush();
    }
    /**
     * Get the number of pending events.
     */
    get pendingCount() {
        return this.pendingEvents.length;
    }
    /**
     * Force flush all pending events immediately.
     */
    flushNow() {
        this.flush();
    }
    scheduleFlush() {
        if (this.flushScheduled) {
            return;
        }
        this.flushScheduled = true;
        requestAnimationFrame(() => {
            this.flush();
            this.flushScheduled = false;
        });
    }
    flush() {
        if (this.pendingEvents.length === 0) {
            return;
        }
        const message = this.encodeEvents(this.pendingEvents);
        this.websocket.send(message);
        this.pendingEvents = [];
    }
    encodeEvents(events) {
        // Encode using ClientToServerModel protocol
        // This follows the existing PonySDK binary format
        // Format: [EventCount(4), Event1, Event2, ...]
        // Event: [ObjectId(4), EventTypeLen(2), EventType(N), PayloadLen(4), Payload(N)]
        const encoder = new TextEncoder();
        // Calculate total size
        let totalSize = 4; // Event count
        for (const event of events) {
            const eventTypeBytes = encoder.encode(event.eventType);
            const payloadBytes = encoder.encode(JSON.stringify(event.payload));
            totalSize += 4 + 2 + eventTypeBytes.length + 4 + payloadBytes.length;
        }
        const buffer = new ArrayBuffer(totalSize);
        const view = new DataView(buffer);
        let offset = 0;
        // Write event count
        view.setUint32(offset, events.length, false);
        offset += 4;
        // Write each event
        for (const event of events) {
            const eventTypeBytes = encoder.encode(event.eventType);
            const payloadBytes = encoder.encode(JSON.stringify(event.payload));
            // Object ID
            view.setUint32(offset, event.objectId, false);
            offset += 4;
            // Event type length and data
            view.setUint16(offset, eventTypeBytes.length, false);
            offset += 2;
            new Uint8Array(buffer, offset, eventTypeBytes.length).set(eventTypeBytes);
            offset += eventTypeBytes.length;
            // Payload length and data
            view.setUint32(offset, payloadBytes.length, false);
            offset += 4;
            new Uint8Array(buffer, offset, payloadBytes.length).set(payloadBytes);
            offset += payloadBytes.length;
        }
        return buffer;
    }
}
//# sourceMappingURL=EventBridge.js.map