/**
 * Event forwarder for Web Awesome component events.
 *
 * Attaches event listeners to wa-* elements and forwards events
 * through EventBridge to the server with faithful payloads.
 *
 * Requirements: 1.3 - Transmit input value changes to server
 * Requirements: 2.4 - Transmit alert close events to server
 * Requirements: 3.2 - Transmit selected tab ID to server
 * Requirements: 3.5 - Transmit selected menu item ID to server
 * Requirements: 5.3 - Transmit dialog close events to server
 */
import type { EventBridge } from '../EventBridge.js';
/** All Web Awesome events we listen to */
declare const WA_EVENTS: readonly ["wa-change", "wa-input", "wa-select", "wa-close", "wa-request-close", "wa-focus", "wa-blur", "wa-show", "wa-hide", "wa-after-show", "wa-after-hide"];
export type WaEventType = (typeof WA_EVENTS)[number];
/**
 * Forwards Web Awesome events from a DOM element to the server via EventBridge.
 *
 * Usage:
 * ```ts
 * const forwarder = new EventForwarder(element, eventBridge, objectId);
 * forwarder.attach();
 * // ... later
 * forwarder.detach();
 * ```
 */
export declare class EventForwarder {
    private element;
    private eventBridge;
    private objectId;
    private listeners;
    private attached;
    constructor(element: HTMLElement, eventBridge: EventBridge, objectId: number);
    /**
     * Attach event listeners for all supported wa-* events.
     * Safe to call multiple times — only attaches once.
     */
    attach(): void;
    /**
     * Detach all event listeners. Safe to call multiple times.
     */
    detach(): void;
    /**
     * Whether listeners are currently attached.
     */
    isAttached(): boolean;
}
export {};
//# sourceMappingURL=EventForwarder.d.ts.map