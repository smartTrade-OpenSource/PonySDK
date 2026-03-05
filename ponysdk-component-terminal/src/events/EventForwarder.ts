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

/** Tags that use `checked` instead of `value` for wa-change */
const CHECKED_TAGS = new Set(['wa-checkbox', 'wa-switch']);

/** All Web Awesome events we listen to */
const WA_EVENTS = [
    'wa-change',
    'wa-input',
    'wa-select',
    'wa-close',
    'wa-request-close',
    'wa-focus',
    'wa-blur',
    'wa-show',
    'wa-hide',
    'wa-after-show',
    'wa-after-hide',
] as const;

export type WaEventType = (typeof WA_EVENTS)[number];

/**
 * Extracts the appropriate payload from a Web Awesome event.
 */
function extractPayload(eventType: string, event: Event, element: HTMLElement): Record<string, unknown> {
    const tag = element.tagName.toLowerCase();
    const el = element as HTMLElement & { value?: unknown; checked?: boolean };

    switch (eventType) {
        case 'wa-change':
            if (CHECKED_TAGS.has(tag)) {
                return { checked: el.checked ?? false };
            }
            return { value: el.value ?? '' };

        case 'wa-input':
            return { value: el.value ?? '' };

        case 'wa-select':
            return { value: el.value ?? '' };

        case 'wa-request-close': {
            const detail = (event as CustomEvent).detail;
            const source = typeof detail?.source === 'string' ? detail.source : 'unknown';
            return { source };
        }

        case 'wa-close':
        case 'wa-focus':
        case 'wa-blur':
        case 'wa-show':
        case 'wa-hide':
        case 'wa-after-show':
        case 'wa-after-hide':
            return {};

        default:
            return {};
    }
}

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
export class EventForwarder {
    private element: HTMLElement;
    private eventBridge: EventBridge;
    private objectId: number;
    private listeners: Map<string, EventListener> = new Map();
    private attached = false;

    constructor(element: HTMLElement, eventBridge: EventBridge, objectId: number) {
        this.element = element;
        this.eventBridge = eventBridge;
        this.objectId = objectId;
    }

    /**
     * Attach event listeners for all supported wa-* events.
     * Safe to call multiple times — only attaches once.
     */
    attach(): void {
        if (this.attached) {
            return;
        }

        for (const eventType of WA_EVENTS) {
            const listener = (event: Event) => {
                const payload = extractPayload(eventType, event, this.element);
                this.eventBridge.dispatch(this.objectId, eventType, payload);
            };
            this.listeners.set(eventType, listener);
            this.element.addEventListener(eventType, listener);
        }

        this.attached = true;
    }

    /**
     * Detach all event listeners. Safe to call multiple times.
     */
    detach(): void {
        if (!this.attached) {
            return;
        }

        for (const [eventType, listener] of this.listeners) {
            this.element.removeEventListener(eventType, listener);
        }
        this.listeners.clear();
        this.attached = false;
    }

    /**
     * Whether listeners are currently attached.
     */
    isAttached(): boolean {
        return this.attached;
    }
}
