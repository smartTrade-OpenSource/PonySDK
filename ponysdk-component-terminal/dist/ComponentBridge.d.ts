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
 * Register a container element for a component.
 * Called by PTComponent after creating the container element.
 *
 * @param objectId - Component object ID
 * @param container - Container DOM element
 */
export declare function registerContainer(objectId: number, container: HTMLElement): void;
/**
 * Get the container element for a component.
 *
 * @param objectId - Component object ID
 * @returns The container element, or undefined if not registered
 */
export declare function getContainer(objectId: number): HTMLElement | undefined;
/**
 * Initialize the ComponentTerminal with a WebSocket connection.
 *
 * @param websocket - WebSocket connection to the server
 */
export declare function initializeTerminal(websocket: WebSocket): void;
/**
 * Get the ComponentTerminal instance.
 *
 * @returns The terminal instance, or null if not initialized
 */
export declare function getTerminal(): ComponentTerminal | null;
//# sourceMappingURL=ComponentBridge.d.ts.map