/**
 * PonySDK Component Bridge
 * Handles component lifecycle and communication between server and client frameworks.
 * Supports: React, Vue, Svelte, Web Components, and Template-based components.
 */

(function() {
    'use strict';

    // Component instances by objectId
    const components = {};
    
    // Factory registrations by signature
    const factories = {
        react: {},
        vue: {},
        svelte: {},
        webcomponent: {},
        template: {}
    };
    
    // PonySDK reference
    let pony = null;

    // ========================================
    // Registration Functions (called by component files)
    // ========================================
    
    window.registerReactComponent = function(signature, factory) {
        console.log('[PComponent] Registering React component:', signature);
        factories.react[signature] = factory;
    };

    window.registerVueComponent = function(signature, factory) {
        console.log('[PComponent] Registering Vue component:', signature);
        factories.vue[signature] = factory;
    };

    window.registerSvelteComponent = function(signature, factory) {
        console.log('[PComponent] Registering Svelte component:', signature);
        factories.svelte[signature] = factory;
    };

    window.registerWebComponent = function(signature, factory) {
        console.log('[PComponent] Registering Web Component:', signature);
        factories.webcomponent[signature] = factory;
    };

    window.registerTemplateComponent = function(signature, config) {
        console.log('[PComponent] Registering Template component:', signature);
        factories.template[signature] = config;
    };

    // ========================================
    // Event Communication
    // ========================================
    
    function sendEvent(objectId, eventType, payload) {
        console.log('[PComponent] Sending event:', objectId, eventType, payload);
        if (pony && pony.sendDataToServer) {
            pony.sendDataToServer(objectId, { eventType: eventType, payload: payload || {} });
        } else {
            console.error('[PComponent] Cannot send event: pony not available');
        }
    }

    function createEventCallback(objectId) {
        return function(eventType, payload) {
            sendEvent(objectId, eventType, payload);
        };
    }

    // ========================================
    // Framework Renderers
    // ========================================
    
    // Framework type constants (must match FrameworkType.java)
    const FRAMEWORK = {
        REACT: 0,
        VUE: 1,
        SVELTE: 2,
        WEB_COMPONENT: 3,
        TEMPLATE: 4
    };

    function renderReact(objectId, container, signature, props) {
        const factory = factories.react[signature];
        if (!factory) {
            console.error('[PComponent] React factory not found:', signature);
            return null;
        }
        
        const factoryResult = factory(container);
        const component = factoryResult.getReactComponent();
        
        component.setEventCallback(createEventCallback(objectId));
        component.updateProps(props);
        component.mount();
        
        return {
            update: (newProps) => component.updateProps(newProps),
            destroy: () => component.unmount()
        };
    }

    function renderVue(objectId, container, signature, props) {
        const factory = factories.vue[signature];
        if (!factory) {
            console.error('[PComponent] Vue factory not found:', signature);
            return null;
        }
        
        const factoryResult = factory(container);
        const component = factoryResult.getVueComponent();
        
        component.setEventCallback(createEventCallback(objectId));
        component.updateProps(props);
        component.mount();
        
        return {
            update: (newProps) => component.updateProps(newProps),
            destroy: () => component.unmount()
        };
    }

    function renderSvelte(objectId, container, signature, props) {
        const factory = factories.svelte[signature];
        if (!factory) {
            console.error('[PComponent] Svelte factory not found:', signature);
            return null;
        }
        
        const factoryResult = factory(container);
        const component = factoryResult.getSvelteComponent();
        
        component.setEventCallback(createEventCallback(objectId));
        component.mount();
        component.updateProps(props);
        
        return {
            update: (newProps) => component.updateProps(newProps),
            destroy: () => component.unmount()
        };
    }

    function renderWebComponent(objectId, container, signature, props) {
        const factory = factories.webcomponent[signature];
        if (!factory) {
            console.error('[PComponent] WebComponent factory not found:', signature);
            return null;
        }
        
        const factoryResult = factory(container);
        const component = factoryResult.getWebComponent();
        
        component.setEventCallback(createEventCallback(objectId));
        component.mount();
        component.updateProps(props);
        
        return {
            update: (newProps) => component.updateProps(newProps),
            destroy: () => component.unmount()
        };
    }

    function renderTemplate(objectId, container, signature, props) {
        const config = factories.template[signature];
        if (!config) {
            console.error('[PComponent] Template config not found:', signature);
            return null;
        }
        
        // Use renderer if available
        if (config.renderer) {
            const renderer = new config.renderer(container);
            renderer.eventCallback = createEventCallback(objectId);
            renderer.props = { ...renderer.props, ...props }; // Set props before mount
            renderer.mount();
            
            return {
                update: (newProps) => renderer.setProps(newProps),
                destroy: () => renderer.unmount()
            };
        }
        
        // Fallback to simple template rendering
        const eventCallback = createEventCallback(objectId);
        let currentProps = { ...props };
        
        function render() {
            let html = config.template;
            for (const key in currentProps) {
                html = html.replace(new RegExp('\\{\\{' + key + '\\}\\}', 'g'), currentProps[key]);
            }
            container.innerHTML = html;
            
            // Bind events
            container.querySelectorAll('[data-event]').forEach(el => {
                el.addEventListener('click', (e) => {
                    e.preventDefault();
                    eventCallback(el.getAttribute('data-event'), {});
                });
            });
        }
        
        render();
        
        return {
            update: (newProps) => { currentProps = { ...currentProps, ...newProps }; render(); },
            destroy: () => { container.innerHTML = ''; }
        };
    }

    // ========================================
    // Component Terminal Interface
    // ========================================
    
    function createShowcase() {
        let showcase = document.getElementById('pcomponent-showcase');
        if (!showcase) {
            showcase = document.createElement('div');
            showcase.id = 'pcomponent-showcase';
            showcase.style.cssText = `
                display: flex;
                flex-wrap: wrap;
                gap: 20px;
                padding: 20px;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                border-radius: 16px;
                margin: 20px;
                box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            `;
            
            // Add title
            const title = document.createElement('div');
            title.style.cssText = `
                width: 100%;
                text-align: center;
                color: white;
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                margin-bottom: 10px;
            `;
            title.innerHTML = `
                <h2 style="margin: 0 0 5px 0; font-size: 24px;">🚀 PonySDK PComponent Demo</h2>
                <p style="margin: 0; opacity: 0.9; font-size: 14px;">Same props, different frameworks - all powered by PonySDK</p>
            `;
            showcase.appendChild(title);
            
            document.body.insertBefore(showcase, document.body.firstChild);
        }
        return showcase;
    }

    function init(ponyInstance) {
        console.log('[PComponent] Initializing Component Bridge');
        pony = ponyInstance;
        
        // Expose ComponentTerminal to GWT
        window.PonySDK = window.PonySDK || {};
        window.PonySDK.ComponentTerminal = {
            
            handleCreate: function(objectId, framework, signature, propsJson) {
                console.log('[PComponent] Creating component:', objectId, 'framework:', framework, 'signature:', signature);
                
                // Create container
                const container = document.createElement('div');
                container.id = 'pcomponent-' + objectId;
                container.style.cssText = 'display: inline-block; vertical-align: top;';
                
                // Add to showcase
                const showcase = createShowcase();
                showcase.appendChild(container);
                
                // Parse props
                const props = JSON.parse(propsJson);
                
                // Render based on framework type
                let instance = null;
                switch (framework) {
                    case FRAMEWORK.REACT:
                        instance = renderReact(objectId, container, signature, props);
                        break;
                    case FRAMEWORK.VUE:
                        instance = renderVue(objectId, container, signature, props);
                        break;
                    case FRAMEWORK.SVELTE:
                        instance = renderSvelte(objectId, container, signature, props);
                        break;
                    case FRAMEWORK.WEB_COMPONENT:
                        instance = renderWebComponent(objectId, container, signature, props);
                        break;
                    case FRAMEWORK.TEMPLATE:
                        instance = renderTemplate(objectId, container, signature, props);
                        break;
                    default:
                        console.error('[PComponent] Unknown framework type:', framework);
                }
                
                if (instance) {
                    components[objectId] = {
                        container: container,
                        instance: instance,
                        props: props,
                        framework: framework
                    };
                    console.log('[PComponent] Component created successfully:', objectId);
                }
            },
            
            handlePatch: function(objectId, patchJson) {
                console.log('[PComponent] Applying patch:', objectId, patchJson);
                
                const comp = components[objectId];
                if (!comp) {
                    console.error('[PComponent] Component not found:', objectId);
                    return;
                }
                
                // Apply JSON Patch
                const patches = JSON.parse(patchJson);
                patches.forEach(function(patch) {
                    if (patch.op === 'replace') {
                        const key = patch.path.substring(1); // Remove leading /
                        comp.props[key] = patch.value;
                        console.log('[PComponent] Patch applied:', key, '=', patch.value);
                    } else if (patch.op === 'add') {
                        const key = patch.path.substring(1);
                        comp.props[key] = patch.value;
                    } else if (patch.op === 'remove') {
                        const key = patch.path.substring(1);
                        delete comp.props[key];
                    }
                });
                
                // Update component
                comp.instance.update(comp.props);
                console.log('[PComponent] Component updated:', objectId, comp.props);
            },
            
            handleProps: function(objectId, propsJson) {
                console.log('[PComponent] Full props update:', objectId);
                
                const comp = components[objectId];
                if (!comp) {
                    console.error('[PComponent] Component not found:', objectId);
                    return;
                }
                
                comp.props = JSON.parse(propsJson);
                comp.instance.update(comp.props);
            },
            
            handleBinary: function(objectId, arrayBuffer) {
                console.log('[PComponent] Binary update:', objectId, arrayBuffer.byteLength, 'bytes');
                // Binary updates not yet implemented in demo
            },
            
            handleDestroy: function(objectId) {
                console.log('[PComponent] Destroying component:', objectId);
                
                const comp = components[objectId];
                if (!comp) return;
                
                if (comp.instance && comp.instance.destroy) {
                    comp.instance.destroy();
                }
                
                if (comp.container && comp.container.parentNode) {
                    comp.container.parentNode.removeChild(comp.container);
                }
                
                delete components[objectId];
            }
        };
        
        console.log('[PComponent] Component Bridge ready');
    }

    // Register initialization callback
    document.onPonyLoadedListeners = document.onPonyLoadedListeners || [];
    document.onPonyLoadedListeners.push(init);

})();
