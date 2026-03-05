/**
 * Web Awesome Component Registry
 * Registers generic factories for all wa-* custom elements.
 * @generated from custom-elements.json
 */

(function () {
    'use strict';

    // List of Web Awesome component tag names (using wa- prefix)
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
        'wa-number-input'
    ];

    /**
     * Generic Web Component adapter for wa-* elements.
     * Creates the custom element and applies props as attributes/properties.
     */
    class GenericWebComponentAdapter {
        constructor(container, tagName) {
            this.container = container;
            this.tagName = tagName;
            this.element = null;
            this.eventCallback = null;
        }

        setEventCallback(callback) {
            this.eventCallback = callback;
        }

        mount() {
            // Create the custom element
            this.element = document.createElement(this.tagName);
            this.container.appendChild(this.element);
            console.log('[WebAwesome] Mounted:', this.tagName);
        }

        updateProps(props) {
            if (!this.element) return;

            // Apply each prop as an attribute or property
            for (const [key, value] of Object.entries(props)) {
                if (typeof value === 'boolean') {
                    // Boolean attributes
                    if (value) {
                        this.element.setAttribute(key, '');
                    } else {
                        this.element.removeAttribute(key);
                    }
                } else if (typeof value === 'string' || typeof value === 'number') {
                    // String/number attributes
                    this.element.setAttribute(key, String(value));
                } else {
                    // Complex values as properties
                    this.element[key] = value;
                }
            }
            console.log('[WebAwesome] Props updated:', this.tagName, props);
        }

        unmount() {
            if (this.element && this.element.parentNode) {
                this.element.parentNode.removeChild(this.element);
            }
            this.element = null;
            console.log('[WebAwesome] Unmounted:', this.tagName);
        }
    }

    /**
     * Generic factory function for Web Awesome components.
     */
    function createWebAwesomeFactory(tagName) {
        return function (container) {
            const adapter = new GenericWebComponentAdapter(container, tagName);
            return {
                getWebComponent: () => adapter
            };
        };
    }

    // Register all Web Awesome components
    function registerAll() {
        if (typeof window.registerWebComponent !== 'function') {
            console.warn('[WebAwesome] registerWebComponent not available yet, deferring registration');
            setTimeout(registerAll, 100);
            return;
        }

        WA_COMPONENTS.forEach(tagName => {
            window.registerWebComponent(tagName, createWebAwesomeFactory(tagName));
        });

        console.log('[WebAwesome] Registered', WA_COMPONENTS.length, 'component factories');
    }

    // Register when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', registerAll);
    } else {
        registerAll();
    }

})();
