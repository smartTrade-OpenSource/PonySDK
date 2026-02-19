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
export declare class EventBridge {
    private websocket;
    private pendingEvents;
    private flushScheduled;
    constructor(websocket: WebSocket);
    /**
     * Dispatch an event to the server.
     * Events are batched within the same animation frame.
     * @param objectId - Object ID of the component
     * @param eventType - Type of event
     * @param payload - Event payload data
     */
    dispatch(objectId: number, eventType: string, payload: unknown): void;
    /**
     * Get the number of pending events.
     */
    get pendingCount(): number;
    /**
     * Force flush all pending events immediately.
     */
    flushNow(): void;
    private scheduleFlush;
    private flush;
    private encodeEvents;
}
//# sourceMappingURL=EventBridge.d.ts.map