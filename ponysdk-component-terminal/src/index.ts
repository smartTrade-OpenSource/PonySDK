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
export { registerContainer, getContainer, initializeTerminal, getTerminal } from './ComponentBridge.js';

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

// Layout exports
export { BreakpointListener } from './layout/BreakpointListener.js';
export type { BreakpointName } from './layout/BreakpointListener.js';
export { ResponsiveGridRenderer } from './layout/ResponsiveGridRenderer.js';
export type { BreakpointConfig, ResponsiveGridProps } from './layout/ResponsiveGridRenderer.js';

// Form exports
export { FormHandler } from './form/FormHandler.js';
export type { ServerValidationErrors, FormSubmitPayload } from './form/FormHandler.js';

// DataTable exports
export { DataTableRenderer } from './datatable/DataTableRenderer.js';
export type { ColumnDef, DataTableProps } from './datatable/DataTableRenderer.js';
export { VirtualScroller } from './datatable/VirtualScroller.js';
export type { VisibleRange } from './datatable/VirtualScroller.js';

// Event exports
export { EventForwarder } from './events/EventForwarder.js';
export type { WaEventType } from './events/EventForwarder.js';

// Overlay exports
export { OverlayController } from './overlay/OverlayController.js';

// Toast exports
export { ToastQueue } from './toast/ToastQueue.js';
export type { ToastOptions } from './toast/ToastQueue.js';

// Web Awesome loader exports
export { WebAwesomeLoader } from './WebAwesomeLoader.js';

// Web Awesome registry exports
export {
    registerWebAwesomeComponents,
    ensureWebAwesomeComponentDefined,
    isWebAwesomeComponentReady,
    getWebAwesomeComponentList,
    getWebAwesomeLoader,
} from './WebAwesomeRegistry.js';
