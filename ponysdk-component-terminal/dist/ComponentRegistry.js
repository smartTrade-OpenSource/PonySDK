/**
 * Registry for managing component instances and factories.
 *
 * Requirements: 6.2 - THE Component_Terminal SHALL maintain a registry of active component instances by object ID
 * Requirements: 6.3 - WHEN a component creation message is received, THE Component_Terminal SHALL instantiate the appropriate Framework_Adapter
 * Requirements: 9.3, 9.4 - Adapters receive EventBridge for event dispatch
 */
import { ReactAdapter } from './adapters/ReactAdapter.js';
import { VueAdapter } from './adapters/VueAdapter.js';
import { SvelteAdapter } from './adapters/SvelteAdapter.js';
import { WebComponentAdapter } from './adapters/WebComponentAdapter.js';
/**
 * Registry for component instances and factories.
 * Maps object IDs to framework adapters and signatures to factories.
 */
export class ComponentRegistry {
    /**
     * Create a new ComponentRegistry.
     * @param eventBridge - EventBridge for dispatching events from adapters
     */
    constructor(eventBridge) {
        this.components = new Map();
        this.factories = new Map();
        this.eventBridge = eventBridge;
    }
    /**
     * Register a component factory for a given signature.
     * @param signature - Unique component signature
     * @param factory - Factory for creating component instances
     */
    registerFactory(signature, factory) {
        this.factories.set(signature, factory);
    }
    /**
     * Create and register a framework adapter for a component.
     * @param objectId - Unique object ID
     * @param framework - Target framework type
     * @param signature - Component signature
     * @param initialProps - Initial props for the component
     * @returns The created framework adapter
     * @throws Error if no factory is registered for the signature
     */
    createAdapter(objectId, framework, signature, initialProps) {
        const factory = this.factories.get(signature);
        if (!factory) {
            throw new Error(`No factory registered for signature: ${signature}`);
        }
        const adapter = this.createFrameworkAdapter(framework, factory, initialProps, objectId);
        this.components.set(objectId, adapter);
        return adapter;
    }
    /**
     * Get an adapter by object ID.
     * @param objectId - Object ID to look up
     * @returns The adapter or undefined if not found
     */
    get(objectId) {
        return this.components.get(objectId);
    }
    /**
     * Remove an adapter from the registry.
     * @param objectId - Object ID to remove
     */
    remove(objectId) {
        this.components.delete(objectId);
    }
    /**
     * Check if a component exists in the registry.
     * @param objectId - Object ID to check
     */
    has(objectId) {
        return this.components.has(objectId);
    }
    /**
     * Get the number of registered components.
     */
    get size() {
        return this.components.size;
    }
    /**
     * Clear all registered components.
     */
    clear() {
        this.components.clear();
    }
    /**
     * Get all registered factories (for debugging).
     */
    getFactories() {
        return this.factories;
    }
    createFrameworkAdapter(framework, factory, props, objectId) {
        switch (framework) {
            case 'react':
                return new ReactAdapter(factory, props, this.eventBridge, objectId);
            case 'vue':
                return new VueAdapter(factory, props, this.eventBridge, objectId);
            case 'svelte':
                return new SvelteAdapter(factory, props, this.eventBridge, objectId);
            case 'webcomponent':
                return new WebComponentAdapter(factory, props, this.eventBridge, objectId);
            default:
                throw new Error(`Unknown framework type: ${framework}`);
        }
    }
}
//# sourceMappingURL=ComponentRegistry.js.map