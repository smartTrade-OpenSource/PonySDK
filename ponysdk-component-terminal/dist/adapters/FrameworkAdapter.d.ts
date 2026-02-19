/**
 * Base interface for framework-specific adapters.
 *
 * Requirements: 7.5, 7.6 - Framework adapters handle mounting, updating, and unmounting
 * Requirements: 9.3, 9.4 - Adapters can dispatch events through the EventBridge
 */
import type { Operation } from 'fast-json-patch';
import type { EventBridge } from '../EventBridge.js';
/**
 * Framework adapter interface for mounting and updating components.
 * Each framework (React, Vue, Svelte, WebComponent) implements this interface.
 */
export interface FrameworkAdapter<TProps = unknown> {
    /**
     * Mount the component to the DOM.
     * Should be idempotent - calling multiple times has same effect as once.
     */
    mount(): void;
    /**
     * Unmount the component from the DOM and clean up resources.
     * Should be safe to call on already unmounted component.
     */
    unmount(): void;
    /**
     * Set full props object, triggering a re-render.
     * @param props - New props object
     */
    setProps(props: TProps): void;
    /**
     * Apply JSON Patch operations to current props.
     * @param patches - Array of JSON Patch operations (RFC 6902)
     */
    applyPatches(patches: Operation[]): void;
    /**
     * Apply binary-encoded props update for high-frequency updates.
     * @param data - Binary encoded props data
     */
    applyBinary(data: ArrayBuffer): void;
    /**
     * Check if the component is currently mounted.
     */
    isMounted(): boolean;
    /**
     * Dispatch an event to the server through the EventBridge.
     * @param eventType - Type of event to dispatch
     * @param payload - Event payload data
     */
    dispatchEvent(eventType: string, payload: unknown): void;
}
/**
 * Base class providing common functionality for framework adapters.
 * Handles EventBridge integration for event dispatch.
 */
export declare abstract class BaseFrameworkAdapter<TProps = unknown> implements FrameworkAdapter<TProps> {
    protected eventBridge: EventBridge;
    protected objectId: number;
    constructor(eventBridge: EventBridge, objectId: number);
    abstract mount(): void;
    abstract unmount(): void;
    abstract setProps(props: TProps): void;
    abstract applyPatches(patches: Operation[]): void;
    abstract applyBinary(data: ArrayBuffer): void;
    abstract isMounted(): boolean;
    /**
     * Dispatch an event to the server through the EventBridge.
     * Requirements: 9.3 - WHEN a component event is received on the server, THE PComponent SHALL invoke the registered handler
     * Requirements: 9.4 - THE PComponent SHALL support typed event handlers
     * @param eventType - Type of event to dispatch
     * @param payload - Event payload data
     */
    dispatchEvent(eventType: string, payload: unknown): void;
}
//# sourceMappingURL=FrameworkAdapter.d.ts.map