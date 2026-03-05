/**
 * Global test setup for vitest.
 * Defines mock Web Awesome custom elements so tests can run synchronously.
 */

// Mock ReactDOM for React adapter tests
globalThis.ReactDOM = {
    createRoot: (container: HTMLElement) => ({
        render: (element: any) => {
            // Simple mock: just append a div to simulate React rendering
            const mockElement = document.createElement('div');
            mockElement.setAttribute('data-react-root', 'true');
            container.appendChild(mockElement);
        },
        unmount: () => {
            // Simple mock: remove all children
        },
    }),
} as any;

globalThis.React = {
    createElement: (...args: any[]) => ({ type: args[0], props: args[1] || {}, children: args.slice(2) }),
} as any;

// List of all Web Awesome components that need to be defined
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
    'wa-form',
    'wa-data-table',
    'wa-aa', // Used in tests
    'wa-ab', // Used in tests
    'wa-range', // Used in tests
];

// Define all Web Awesome components as simple custom elements
for (const tagName of WA_COMPONENTS) {
    if (!customElements.get(tagName)) {
        customElements.define(tagName, class extends HTMLElement {
            constructor() {
                super();
            }
        });
    }
}

// Also define any dynamically generated tag names that tests might create
// The property tests generate random tag names like wa-ac, wa-ad, etc.
// We'll auto-define them, but allow tests to opt-out by setting a flag
const originalDefine = customElements.define.bind(customElements);
const originalGet = customElements.get.bind(customElements);

// Flag to disable auto-definition (for WebAwesomeLoader tests)
(globalThis as any).__disableAutoDefine = false;

// Intercept customElements.get to auto-define wa-* components
customElements.get = function (name: string) {
    const existing = originalGet(name);
    if (!existing && typeof name === 'string' && name.startsWith('wa-') && !(globalThis as any).__disableAutoDefine) {
        // Auto-define any wa-* component that doesn't exist yet
        try {
            originalDefine(name, class extends HTMLElement {
                constructor() {
                    super();
                }
            });
        } catch (e) {
            // Ignore if already defined
        }
        return originalGet(name);
    }
    return existing;
};
