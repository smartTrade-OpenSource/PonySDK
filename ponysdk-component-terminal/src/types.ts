/**
 * Core type definitions for PComponent terminal.
 * 
 * Requirements: 6.1 - THE Component_Terminal SHALL parse binary messages from the existing PonySDK protocol
 */

import type { Operation } from 'fast-json-patch';

/**
 * Framework types supported by PComponent.
 */
export type FrameworkType = 'react' | 'vue' | 'svelte' | 'webcomponent';

/**
 * Message types for component lifecycle.
 */
export type ComponentMessageType = 'create' | 'update' | 'destroy' | 'slot' | 'serverErrors';

/**
 * Slot operation received from server for Web Component slot composition.
 * Requirements: 7.2, 7.3, 7.4 - Slot add/remove operations
 */
export interface SlotOperation {
    /** Operation type */
    type: 'slot';
    /** Slot name, null for default slot */
    slotName: string | null;
    /** Object ID of the child component to add/remove */
    childObjectId: number;
    /** Operation to perform */
    operation: 'add' | 'remove';
}

/**
 * Component message received from server.
 */
export interface ComponentMessage {
    /** Unique object ID for the component */
    objectId: number;
    /** Type of message */
    type: ComponentMessageType;
    /** Framework type (required for create) */
    framework?: FrameworkType;
    /** Component signature/identifier (required for create) */
    signature?: string;
    /** Full props object (for create or full update) */
    props?: unknown;
    /** JSON Patch operations (for differential update) */
    patches?: Operation[];
    /** Binary data (for high-frequency updates) */
    binaryData?: ArrayBuffer;
    /** Slot operation (for slot composition) */
    slotOperation?: SlotOperation;
    /** Server validation errors (for form validation) */
    serverErrors?: Record<string, string[]>;
}

/**
 * Event dispatched from client component to server.
 */
export interface ComponentEvent {
    /** Object ID of the component dispatching the event */
    objectId: number;
    /** Type of event */
    eventType: string;
    /** Event payload data */
    payload: unknown;
}

/**
 * Factory for creating component instances.
 */
export interface ComponentFactory<TProps = unknown> {
    /** Get the container element for mounting */
    getContainer(): HTMLElement;
    /** Get React component (if React framework) */
    getReactComponent?(): unknown;
    /** Get Vue component (if Vue framework) */
    getVueComponent?(): unknown;
    /** Get Svelte component (if Svelte framework) */
    getSvelteComponent?(): unknown;
    /** Get custom element tag name (if WebComponent framework) */
    getTagName?(): string;
    /** Initial props for the component */
    initialProps?: TProps;
}
