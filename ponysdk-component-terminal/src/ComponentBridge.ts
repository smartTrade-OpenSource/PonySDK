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
import type { FrameworkType } from './types.js';

/**
 * Global bridge interface for PTComponent communication.
 */
interface ComponentBridgeAPI {
    handleCreate(objectId: number, framework: number, signature: string, props: string): void;
    handlePatch(objectId: number, patch: string): void;
    handleProps(objectId: number, props: string): void;
    handleBinary(objectId: number, binaryData: ArrayBuffer): void;
    handleDestroy(objectId: number): void;
}

/**
 * Container element registry.
 * Maps objectId to the container DOM element created by PTComponent.
 */
const containerRegistry = new Map<number, HTMLElement>();

/**
 * ComponentTerminal instance (created when WebSocket is available).
 */
let terminal: ComponentTerminal | null = null;

/**
 * Register a container element for a component.
 * Called by PTComponent after creating the container element.
 * 
 * @param objectId - Component object ID
 * @param container - Container DOM element
 */
export function registerContainer(objectId: number, container: HTMLElement): void {
    containerRegistry.set(objectId, container);
}

/**
 * Get the container element for a component.
 * 
 * @param objectId - Component object ID
 * @returns The container element, or undefined if not registered
 */
export function getContainer(objectId: number): HTMLElement | undefined {
    return containerRegistry.get(objectId);
}

/**
 * Initialize the ComponentTerminal with a WebSocket connection.
 * 
 * @param websocket - WebSocket connection to the server
 */
export function initializeTerminal(websocket: WebSocket): void {
    terminal = new ComponentTerminal(websocket);
}

/**
 * Get the ComponentTerminal instance.
 * 
 * @returns The terminal instance, or null if not initialized
 */
export function getTerminal(): ComponentTerminal | null {
    return terminal;
}

/**
 * Framework type mapping from byte values to string types.
 */
const FRAMEWORK_MAP: Record<number, FrameworkType> = {
    0: 'react',
    1: 'vue',
    2: 'svelte',
    3: 'webcomponent'
};

/**
 * Bridge API implementation exposed to PTComponent.
 */
const bridgeAPI: ComponentBridgeAPI = {
    handleCreate(objectId: number, framework: number, signature: string, propsJson: string): void {
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

        let props: unknown;
        try {
            props = JSON.parse(propsJson);
        } catch (error) {
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

    handlePatch(objectId: number, patchJson: string): void {
        if (!terminal) {
            console.warn('ComponentTerminal not initialized for component #' + objectId);
            return;
        }

        let patches: unknown;
        try {
            patches = JSON.parse(patchJson);
        } catch (error) {
            console.error('Failed to parse patch JSON for component #' + objectId, error);
            return;
        }

        terminal.handleMessage({
            objectId,
            type: 'update',
            patches: patches as any
        });
    },

    handleProps(objectId: number, propsJson: string): void {
        if (!terminal) {
            console.warn('ComponentTerminal not initialized for component #' + objectId);
            return;
        }

        let props: unknown;
        try {
            props = JSON.parse(propsJson);
        } catch (error) {
            console.error('Failed to parse props JSON for component #' + objectId, error);
            return;
        }

        terminal.handleMessage({
            objectId,
            type: 'update',
            props
        });
    },

    handleBinary(objectId: number, binaryData: ArrayBuffer): void {
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

    handleDestroy(objectId: number): void {
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
    (window as any).PonySDK = (window as any).PonySDK || {};
    (window as any).PonySDK.ComponentTerminal = bridgeAPI;
    (window as any).PonySDK.ComponentBridge = {
        registerContainer,
        initializeTerminal
    };
}
