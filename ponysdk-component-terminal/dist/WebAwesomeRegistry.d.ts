/**
 * Client-side registry initialization for all Web Awesome components.
 *
 * This module registers factory functions for all 52 generated Web Awesome
 * components, enabling the ComponentTerminal to create WebComponentAdapter
 * instances for each wa-* tag name.
 *
 * The factories are simple: they return the tag name and a container element.
 * The WebComponentAdapter handles the actual custom element creation and
 * property updates.
 *
 * Requirements: 12.3, 14.1, 14.2
 */
import type { ComponentTerminal } from './ComponentTerminal.js';
import { WebAwesomeLoader } from './WebAwesomeLoader.js';
/**
 * Registers all Web Awesome component factories with the ComponentTerminal.
 *
 * This function should be called once during application initialization,
 * after the ComponentTerminal is created but before any components are
 * instantiated.
 *
 * @param terminal - The ComponentTerminal instance
 * @param container - The root container element for mounting components
 *
 * @example
 * ```ts
 * const terminal = new ComponentTerminal(websocket);
 * registerWebAwesomeComponents(terminal, document.body);
 * ```
 */
export declare function registerWebAwesomeComponents(terminal: ComponentTerminal, container: HTMLElement): void;
/**
 * Ensures a Web Awesome component is defined before attempting to use it.
 *
 * This function uses customElements.whenDefined() to wait for the component
 * to be registered in the browser's custom elements registry. If the component
 * is not defined within the timeout period (default 10s), it rejects with an error.
 *
 * @param tagName - The wa-* tag name to wait for
 * @returns A promise that resolves when the component is defined
 * @throws Error if the component is not defined within the timeout
 *
 * @example
 * ```ts
 * await ensureWebAwesomeComponentDefined('wa-button');
 * // Now safe to create <wa-button> elements
 * ```
 */
export declare function ensureWebAwesomeComponentDefined(tagName: string): Promise<void>;
/**
 * Checks if a Web Awesome component is already defined.
 *
 * @param tagName - The wa-* tag name to check
 * @returns true if the component is defined, false otherwise
 */
export declare function isWebAwesomeComponentReady(tagName: string): boolean;
/**
 * Returns the list of all registered Web Awesome component tag names.
 */
export declare function getWebAwesomeComponentList(): readonly string[];
/**
 * Returns the Web Awesome loader instance for advanced usage.
 */
export declare function getWebAwesomeLoader(): WebAwesomeLoader;
//# sourceMappingURL=WebAwesomeRegistry.d.ts.map