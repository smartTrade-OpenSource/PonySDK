/**
 * PonySDK Component Terminal
 * 
 * TypeScript-based component terminal for PonySDK PComponent system.
 * Supports React, Vue, Svelte, and Web Components.
 * 
 * Requirements: 6.6 - THE Component_Terminal SHALL coexist with the existing GWT terminal without conflicts
 * Requirements: 13.3 - THE Component_Terminal SHALL be loadable independently of the GWT terminal
 */

// Core exports
export { ComponentTerminal } from './ComponentTerminal.js';
export { ComponentRegistry } from './ComponentRegistry.js';
export { EventBridge } from './EventBridge.js';

// Type exports
export type {
    FrameworkType,
    ComponentMessageType,
    ComponentMessage,
    ComponentEvent,
    ComponentFactory,
} from './types.js';

// Adapter exports
export type { FrameworkAdapter } from './adapters/FrameworkAdapter.js';
export { ReactAdapter } from './adapters/ReactAdapter.js';
export { VueAdapter } from './adapters/VueAdapter.js';
export { SvelteAdapter } from './adapters/SvelteAdapter.js';
export { WebComponentAdapter } from './adapters/WebComponentAdapter.js';
