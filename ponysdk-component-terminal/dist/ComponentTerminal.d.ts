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
export declare class ComponentTerminal {
    private registry;
    private eventBridge;
    constructor(websocket: WebSocket);
    /**
     * Register a component factory for a given signature.
     * @param signature - Unique component signature
     * @param factory - Factory for creating component instances
     */
    registerFactory(signature: string, factory: ComponentFactory): void;
    /**
     * Handle an incoming component message.
     * @param message - The component message to handle
     */
    handleMessage(message: ComponentMessage): void;
    /**
     * Get the event bridge for dispatching events to the server.
     */
    getEventBridge(): EventBridge;
    /**
     * Get the component registry.
     */
    getRegistry(): ComponentRegistry;
    private handleCreate;
    private handleUpdate;
    private handleDestroy;
}
//# sourceMappingURL=ComponentTerminal.d.ts.map