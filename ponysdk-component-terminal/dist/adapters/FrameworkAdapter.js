/**
 * Base interface for framework-specific adapters.
 *
 * Requirements: 7.5, 7.6 - Framework adapters handle mounting, updating, and unmounting
 * Requirements: 9.3, 9.4 - Adapters can dispatch events through the EventBridge
 */
/**
 * Base class providing common functionality for framework adapters.
 * Handles EventBridge integration for event dispatch.
 */
export class BaseFrameworkAdapter {
    constructor(eventBridge, objectId) {
        this.eventBridge = eventBridge;
        this.objectId = objectId;
    }
    /**
     * Dispatch an event to the server through the EventBridge.
     * Requirements: 9.3 - WHEN a component event is received on the server, THE PComponent SHALL invoke the registered handler
     * Requirements: 9.4 - THE PComponent SHALL support typed event handlers
     * @param eventType - Type of event to dispatch
     * @param payload - Event payload data
     */
    dispatchEvent(eventType, payload) {
        this.eventBridge.dispatch(this.objectId, eventType, payload);
    }
}
//# sourceMappingURL=FrameworkAdapter.js.map