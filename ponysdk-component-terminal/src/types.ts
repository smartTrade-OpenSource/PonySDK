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
export type ComponentMessageType = 'create' | 'update' | 'destroy';

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
