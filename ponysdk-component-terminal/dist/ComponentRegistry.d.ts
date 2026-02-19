/**
 * Registry for managing component instances and factories.
 *
 * Requirements: 6.2 - THE Component_Terminal SHALL maintain a registry of active component instances by object ID
 * Requirements: 6.3 - WHEN a component creation message is received, THE Component_Terminal SHALL instantiate the appropriate Framework_Adapter
 * Requirements: 9.3, 9.4 - Adapters receive EventBridge for event dispatch
 */
import type { FrameworkAdapter } from './adapters/FrameworkAdapter.js';
import type { ComponentFactory, FrameworkType } from './types.js';
import type { EventBridge } from './EventBridge.js';
/**
 * Registry for component instances and factories.
 * Maps object IDs to framework adapters and signatures to factories.
 */
export declare class ComponentRegistry {
    private components;
    private factories;
    private eventBridge;
    /**
     * Create a new ComponentRegistry.
     * @param eventBridge - EventBridge for dispatching events from adapters
     */
    constructor(eventBridge: EventBridge);
    /**
     * Register a component factory for a given signature.
     * @param signature - Unique component signature
     * @param factory - Factory for creating component instances
     */
    registerFactory(signature: string, factory: ComponentFactory): void;
    /**
     * Create and register a framework adapter for a component.
     * @param objectId - Unique object ID
     * @param framework - Target framework type
     * @param signature - Component signature
     * @param initialProps - Initial props for the component
     * @returns The created framework adapter
     * @throws Error if no factory is registered for the signature
     */
    createAdapter(objectId: number, framework: FrameworkType, signature: string, initialProps: unknown): FrameworkAdapter;
    /**
     * Get an adapter by object ID.
     * @param objectId - Object ID to look up
     * @returns The adapter or undefined if not found
     */
    get(objectId: number): FrameworkAdapter | undefined;
    /**
     * Remove an adapter from the registry.
     * @param objectId - Object ID to remove
     */
    remove(objectId: number): void;
    /**
     * Check if a component exists in the registry.
     * @param objectId - Object ID to check
     */
    has(objectId: number): boolean;
    /**
     * Get the number of registered components.
     */
    get size(): number;
    /**
     * Clear all registered components.
     */
    clear(): void;
    /**
     * Get all registered factories (for debugging).
     */
    getFactories(): Map<string, ComponentFactory>;
    private createFrameworkAdapter;
}
//# sourceMappingURL=ComponentRegistry.d.ts.map