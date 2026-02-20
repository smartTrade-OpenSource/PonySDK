/**
 * Template Component Renderer for PonySDK
 * Integrates with ComponentTerminal to render HTML template-based components
 * Uses the same JSON props protocol as React/Vue/Svelte components
 */

(function() {
    'use strict';

    /**
     * Simple template engine for variable interpolation
     */
    class TemplateEngine {
        /**
         * Render template with props
         * @param {string} template - HTML template with {{variable}} placeholders
         * @param {object} props - Props object with values
         * @returns {string} Rendered HTML
         */
        static render(template, props) {
            let html = template;

            // Handle conditional blocks: {{#variable}}content{{/variable}}
            html = html.replace(/\{\{#(\w+)\}\}(.*?)\{\{\/\1\}\}/gs, (match, key, content) => {
                const value = props[key];
                return value ? content : '';
            });

            // Handle inverted conditional blocks: {{^variable}}content{{/variable}}
            html = html.replace(/\{\{\^(\w+)\}\}(.*?)\{\{\/\1\}\}/gs, (match, key, content) => {
                const value = props[key];
                return !value ? content : '';
            });

            // Handle simple variable interpolation: {{variable}}
            html = html.replace(/\{\{(\w+)\}\}/g, (match, key) => {
                const value = props[key];
                return value !== undefined && value !== null ? this.escapeHtml(String(value)) : '';
            });

            return html;
        }

        /**
         * Escape HTML to prevent XSS
         */
        static escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }
    }

    /**
     * Template Component Instance
     * Compatible with ComponentTerminal's component interface
     */
    class TemplateComponent {
        constructor(container, template, styles) {
            this.container = container;
            this.template = template;
            this.styles = styles;
            this.props = {};
            this.shadowRoot = null;
            this.eventListeners = [];
            this.eventCallback = null;

            this.init();
        }

        init() {
            // Create shadow DOM for style encapsulation
            this.shadowRoot = this.container.attachShadow({ mode: 'open' });

            // Add styles
            if (this.styles) {
                const styleEl = document.createElement('style');
                styleEl.textContent = this.styles;
                this.shadowRoot.appendChild(styleEl);
            }
        }

        /**
         * Set event callback (called by ComponentTerminal)
         */
        setEventCallback(callback) {
            this.eventCallback = callback;
        }

        /**
         * Update props (called by ComponentTerminal on props changes)
         */
        updateProps(newProps) {
            this.props = newProps;
            this.render();
        }

        render() {
            // Clear existing event listeners
            this.clearEventListeners();

            // Render template
            const html = TemplateEngine.render(this.template, this.props);
            
            // Create wrapper div
            const wrapper = document.createElement('div');
            wrapper.innerHTML = html;

            // Clear shadow root (except styles)
            const styles = this.shadowRoot.querySelector('style');
            this.shadowRoot.innerHTML = '';
            if (styles) {
                this.shadowRoot.appendChild(styles);
            }

            // Append rendered content
            this.shadowRoot.appendChild(wrapper);

            // Attach event listeners
            this.attachEventListeners();
        }

        attachEventListeners() {
            // Find all elements with data-event attribute
            const elements = this.shadowRoot.querySelectorAll('[data-event]');
            
            elements.forEach(element => {
                const events = element.getAttribute('data-event').split(',');
                
                events.forEach(eventType => {
                    const handler = (e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        
                        if (this.eventCallback) {
                            this.eventCallback(eventType.trim(), {
                                target: e.target.tagName,
                                timestamp: Date.now()
                            });
                        }
                    };

                    // Default to click if no specific event type
                    const domEventType = this.mapEventType(eventType.trim());
                    element.addEventListener(domEventType, handler);
                    
                    // Store for cleanup
                    this.eventListeners.push({ element, type: domEventType, handler });
                });
            });
        }

        mapEventType(eventType) {
            // Map custom event names to DOM events
            const mapping = {
                'cardClick': 'click',
                'deleteClick': 'click',
                'submit': 'submit',
                'change': 'change',
                'input': 'input'
            };
            return mapping[eventType] || 'click';
        }

        clearEventListeners() {
            this.eventListeners.forEach(({ element, type, handler }) => {
                element.removeEventListener(type, handler);
            });
            this.eventListeners = [];
        }

        destroy() {
            this.clearEventListeners();
            if (this.shadowRoot) {
                this.shadowRoot.innerHTML = '';
            }
        }

        /**
         * Get container (required by ComponentTerminal)
         */
        getContainer() {
            return this.container;
        }
    }

    /**
     * Registry for template definitions
     */
    const templateRegistry = new Map();

    /**
     * Register a template component with ComponentTerminal
     * @param {string} signature - Component signature (e.g., "card-component")
     * @param {object} config - Configuration with template and styles
     * @param {string} config.template - HTML template with {{variable}} syntax
     * @param {string} [config.styles] - CSS styles (optional)
     */
    function registerTemplateComponent(signature, config) {
        console.log('Registering template component:', signature);
        
        if (!config.template) {
            console.error('Template is required for component:', signature);
            return;
        }

        // Store template definition
        templateRegistry.set(signature, {
            template: config.template,
            styles: config.styles || ''
        });

        // Register with ComponentTerminal using the same pattern as React components
        if (typeof window !== 'undefined' && window.registerWebComponent) {
            window.registerWebComponent(signature, (container) => {
                const def = templateRegistry.get(signature);
                const component = new TemplateComponent(container, def.template, def.styles);
                
                return {
                    getContainer: () => component.getContainer(),
                    getWebComponent: () => component,
                    initialProps: {}
                };
            });
        } else {
            console.warn('ComponentTerminal not available, template component will be registered when loaded');
        }
    }

    // Export to global scope
    if (typeof window !== 'undefined') {
        window.registerTemplateComponent = registerTemplateComponent;
        console.log('Template Component Renderer loaded');
    }

})();
