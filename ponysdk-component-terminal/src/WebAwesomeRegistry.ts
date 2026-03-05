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
import type { ComponentFactory } from './types.js';
import { WebAwesomeLoader } from './WebAwesomeLoader.js';

/**
 * List of all Web Awesome component tag names.
 * Generated from custom-elements.json - 57 components total.
 */
const WA_COMPONENTS = [
    'wa-icon',
    'wa-checkbox',
    'wa-spinner',
    'wa-tree-item',
    'wa-carousel-item',
    'wa-button',
    'wa-animated-image',
    'wa-animation',
    'wa-avatar',
    'wa-badge',
    'wa-breadcrumb-item',
    'wa-breadcrumb',
    'wa-button-group',
    'wa-callout',
    'wa-card',
    'wa-carousel',
    'wa-input',
    'wa-popup',
    'wa-color-picker',
    'wa-comparison',
    'wa-tooltip',
    'wa-copy-button',
    'wa-details',
    'wa-dialog',
    'wa-divider',
    'wa-drawer',
    'wa-dropdown-item',
    'wa-dropdown',
    'wa-format-bytes',
    'wa-format-date',
    'wa-format-number',
    'wa-include',
    'wa-intersection-observer',
    'wa-mutation-observer',
    'wa-tag',
    'wa-select',
    'wa-option',
    'wa-popover',
    'wa-progress-bar',
    'wa-progress-ring',
    'wa-qr-code',
    'wa-radio',
    'wa-radio-group',
    'wa-rating',
    'wa-relative-time',
    'wa-resize-observer',
    'wa-scroller',
    'wa-skeleton',
    'wa-slider',
    'wa-split-panel',
    'wa-switch',
    'wa-tab',
    'wa-tab-panel',
    'wa-tab-group',
    'wa-textarea',
    'wa-tree',
    'wa-zoomable-frame',
    'wa-number-input',
] as const;

/**
 * Global Web Awesome loader instance for ensuring components are defined
 * before mounting.
 */
const waLoader = new WebAwesomeLoader();

/**
 * Creates a factory function for a Web Awesome component.
 *
 * The factory returns an object with:
 * - getTagName(): returns the wa-* tag name
 * - getContainer(): returns the mount container element
 *
 * @param tagName - The wa-* tag name (e.g., 'wa-button')
 * @param container - The DOM element where the component will be mounted
 * @returns A component factory
 */
function createWaFactory<TProps>(
    tagName: string,
    container: HTMLElement
): ComponentFactory<TProps> {
    return {
        getTagName: () => tagName,
        getContainer: () => container,
    };
}

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
export function registerWebAwesomeComponents(
    terminal: ComponentTerminal,
    container: HTMLElement
): void {
    console.log('[WebAwesome] Registering', WA_COMPONENTS.length, 'component factories');

    for (const tagName of WA_COMPONENTS) {
        const factory = createWaFactory(tagName, container);
        terminal.registerFactory(tagName, factory);
    }

    console.log('[WebAwesome] Registration complete');
}

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
export async function ensureWebAwesomeComponentDefined(tagName: string): Promise<void> {
    return waLoader.ensureDefined(tagName);
}

/**
 * Checks if a Web Awesome component is already defined.
 *
 * @param tagName - The wa-* tag name to check
 * @returns true if the component is defined, false otherwise
 */
export function isWebAwesomeComponentReady(tagName: string): boolean {
    return waLoader.isReady(tagName);
}

/**
 * Returns the list of all registered Web Awesome component tag names.
 */
export function getWebAwesomeComponentList(): readonly string[] {
    return WA_COMPONENTS;
}

/**
 * Returns the Web Awesome loader instance for advanced usage.
 */
export function getWebAwesomeLoader(): WebAwesomeLoader {
    return waLoader;
}
