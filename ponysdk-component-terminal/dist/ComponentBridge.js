/**
 * Bridge between GWT PTComponent and TypeScript ComponentTerminal.
 *
 * This module exposes the ComponentTerminal to the global window object
 * so that PTComponent (Java/GWT) can communicate with it via JSNI calls.
 *
 * Requirements: 5.1, 6.1 - Container element is provided by PTComponent
 * Requirements: 6.2 - PWidget properties are applied to the container element
 */
import { ComponentTerminal } from './ComponentTerminal.js';
/**
 * Container element registry.
 * Maps objectId to the container DOM element created by PTComponent.
 */
const containerRegistry = new Map();
/**
 * ComponentTerminal instance (created when WebSocket is available).
 */
let terminal = null;
/**
 * Register a container element for a component.
 * Called by PTComponent after creating the container element.
 *
 * @param objectId - Component object ID
 * @param container - Container DOM element
 */
export function registerContainer(objectId, container) {
    containerRegistry.set(objectId, container);
}
/**
 * Get the container element for a component.
 *
 * @param objectId - Component object ID
 * @returns The container element, or undefined if not registered
 */
export function getContainer(objectId) {
    return containerRegistry.get(objectId);
}
/**
 * Initialize the ComponentTerminal with a WebSocket connection.
 *
 * @param websocket - WebSocket connection to the server
 */
export function initializeTerminal(websocket) {
    terminal = new ComponentTerminal(websocket);
}
/**
 * Get the ComponentTerminal instance.
 *
 * @returns The terminal instance, or null if not initialized
 */
export function getTerminal() {
    return terminal;
}
/**
 * Framework type mapping from byte values to string types.
 */
const FRAMEWORK_MAP = {
    0: 'react',
    1: 'vue',
    2: 'svelte',
    3: 'webcomponent'
};
/**
 * Bridge API implementation exposed to PTComponent.
 */
const bridgeAPI = {
    handleCreate(objectId, framework, signature, propsJson) {
        if (!terminal) {
            console.warn('ComponentTerminal not initialized for component #' + objectId);
            return;
        }
        const container = containerRegistry.get(objectId);
        if (!container) {
            console.error('Container not registered for component #' + objectId);
            return;
        }
        const frameworkType = FRAMEWORK_MAP[framework];
        if (!frameworkType) {
            console.error('Unknown framework type: ' + framework);
            return;
        }
        let props;
        try {
            props = JSON.parse(propsJson);
        }
        catch (error) {
            console.error('Failed to parse props JSON for component #' + objectId, error);
            return;
        }
        // Register a factory that returns the PTComponent container
        const factorySignature = `${objectId}-${signature}`;
        terminal.registerFactory(factorySignature, {
            getContainer: () => container,
            getTagName: () => signature
        });
        // Handle the create message
        terminal.handleMessage({
            objectId,
            type: 'create',
            framework: frameworkType,
            signature: factorySignature,
            props
        });
    },
    handlePatch(objectId, patchJson) {
        if (!terminal) {
            console.warn('ComponentTerminal not initialized for component #' + objectId);
            return;
        }
        let patches;
        try {
            patches = JSON.parse(patchJson);
        }
        catch (error) {
            console.error('Failed to parse patch JSON for component #' + objectId, error);
            return;
        }
        terminal.handleMessage({
            objectId,
            type: 'update',
            patches: patches
        });
    },
    handleProps(objectId, propsJson) {
        if (!terminal) {
            console.warn('ComponentTerminal not initialized for component #' + objectId);
            return;
        }
        let props;
        try {
            props = JSON.parse(propsJson);
        }
        catch (error) {
            console.error('Failed to parse props JSON for component #' + objectId, error);
            return;
        }
        terminal.handleMessage({
            objectId,
            type: 'update',
            props
        });
    },
    handleBinary(objectId, binaryData) {
        if (!terminal) {
            console.warn('ComponentTerminal not initialized for component #' + objectId);
            return;
        }
        terminal.handleMessage({
            objectId,
            type: 'update',
            binaryData
        });
    },
    handleDestroy(objectId) {
        if (!terminal) {
            return;
        }
        terminal.handleMessage({
            objectId,
            type: 'destroy'
        });
        // Clean up container registry
        containerRegistry.delete(objectId);
    }
};
/**
 * Expose the bridge API to the global window object.
 * PTComponent uses JSNI to call these methods.
 */
if (typeof window !== 'undefined') {
    window.PonySDK = window.PonySDK || {};
    window.PonySDK.ComponentTerminal = bridgeAPI;
    window.PonySDK.ComponentBridge = {
        registerContainer,
        initializeTerminal
    };
}
//# sourceMappingURL=ComponentBridge.js.map