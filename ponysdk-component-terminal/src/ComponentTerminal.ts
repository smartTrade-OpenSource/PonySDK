/**
 * Main terminal for handling PComponent messages.
 * 
 * Requirements: 6.1 - THE Component_Terminal SHALL parse binary messages from the existing PonySDK protocol
 * Requirements: 6.3 - WHEN a component creation message is received, THE Component_Terminal SHALL instantiate the appropriate Framework_Adapter
 * Requirements: 6.4 - WHEN a props update message is received, THE Component_Terminal SHALL apply the JSON Patch to the current props
 * Requirements: 6.5 - WHEN a destroy message is received, THE Component_Terminal SHALL unmount the component and clean up resources
 * Requirements: 6.6 - THE Component_Terminal SHALL coexist with the existing GWT terminal without conflicts
 * Requirements: 9.3, 9.4 - EventBridge is passed to registry for adapter event dispatch
 */

import { ComponentRegistry } from './ComponentRegistry.js';
import { EventBridge } from './EventBridge.js';
import type { ComponentMessage, ComponentFactory } from './types.js';

/**
 * Component terminal for handling PComponent lifecycle messages.
 * Coexists with the existing GWT terminal without conflicts.
 */
export class ComponentTerminal {
    private registry: ComponentRegistry;
    private eventBridge: EventBridge;

    constructor(websocket: WebSocket) {
        this.eventBridge = new EventBridge(websocket);
        this.registry = new ComponentRegistry(this.eventBridge);
    }

    /**
     * Register a component factory for a given signature.
     * @param signature - Unique component signature
     * @param factory - Factory for creating component instances
     */
    registerFactory(signature: string, factory: ComponentFactory): void {
        this.registry.registerFactory(signature, factory);
    }

    /**
     * Handle an incoming component message.
     * @param message - The component message to handle
     */
    handleMessage(message: ComponentMessage): void {
        switch (message.type) {
            case 'create':
                this.handleCreate(message);
                break;
            case 'update':
                this.handleUpdate(message);
                break;
            case 'destroy':
                this.handleDestroy(message);
                break;
        }
    }

    /**
     * Get the event bridge for dispatching events to the server.
     */
    getEventBridge(): EventBridge {
        return this.eventBridge;
    }

    /**
     * Get the component registry.
     */
    getRegistry(): ComponentRegistry {
        return this.registry;
    }

    private handleCreate(message: ComponentMessage): void {
        if (!message.framework || !message.signature) {
            console.warn('Invalid create message: missing framework or signature', message);
            return;
        }

        try {
            const adapter = this.registry.createAdapter(
                message.objectId,
                message.framework,
                message.signature,
                message.props
            );
            adapter.mount();
        } catch (error) {
            console.error('Failed to create component:', error);
        }
    }

    private handleUpdate(message: ComponentMessage): void {
        const adapter = this.registry.get(message.objectId);
        if (!adapter) {
            console.warn('Update for unknown component:', message.objectId);
            return;
        }

        // Prevent updates to unmounted components
        if (!adapter.isMounted()) {
            console.warn('Update for unmounted component:', message.objectId);
            return;
        }

        if (message.patches) {
            adapter.applyPatches(message.patches);
        } else if (message.binaryData) {
            adapter.applyBinary(message.binaryData);
        } else if (message.props !== undefined) {
            adapter.setProps(message.props);
        }
    }

    private handleDestroy(message: ComponentMessage): void {
        const adapter = this.registry.get(message.objectId);
        if (adapter) {
            adapter.unmount();
            this.registry.remove(message.objectId);
        }
    }
}
