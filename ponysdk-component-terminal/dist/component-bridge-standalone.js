/**
 * Standalone bridge between GWT PTComponent and ComponentTerminal.
 * 
 * This file can be loaded as a regular script (not ES6 module).
 * It provides basic slot functionality without requiring the full ComponentTerminal.
 */
(function() {
    'use strict';

    /**
     * Container element registry.
     * Maps objectId to the container DOM element created by PTComponent.
     */
    const containerRegistry = new Map();

    /**
     * ComponentTerminal instance (created when WebSocket is available).
     * For now, we don't use the full ComponentTerminal - just handle slots directly.
     */
    let terminal = null;

    /**
     * Framework type mapping from byte values to string types.
     */
    const FRAMEWORK_MAP = {
        0: 'react',
        1: 'vue',
        2: 'svelte',
        3: 'webcomponent'
    };

    /**
     * Bridge API implementation exposed to PTComponent.
     */
    const bridgeAPI = {
        handleCreate(objectId, framework, signature, propsJson) {
            console.log('handleCreate called for component #' + objectId + ', signature: ' + signature);
            
            const container = containerRegistry.get(objectId);
            if (!container) {
                console.error('Container not registered for component #' + objectId);
                return;
            }

            // For Web Components (framework === 3), create the custom element
            if (framework === 3) {
                // Clear container first
                while (container.firstChild) {
                    container.removeChild(container.firstChild);
                }

                // Create the Web Awesome element
                const element = document.createElement(signature);
                
                // Parse and apply initial props if provided
                if (propsJson) {
                    try {
                        const props = JSON.parse(propsJson);
                        // Apply props as attributes/properties
                        for (const [key, value] of Object.entries(props)) {
                            if (value !== null && value !== undefined) {
                                element.setAttribute(key, String(value));
                            }
                        }
                    } catch (error) {
                        console.error('Failed to parse initial props for component #' + objectId, error);
                    }
                }
                
                // Add the element to the container
                container.appendChild(element);
                console.log('Created Web Component <' + signature + '> in container #' + objectId);
            } else {
                console.warn('Framework type ' + framework + ' not yet supported');
            }
        },

        handlePatch(objectId, patchJson) {
            console.log('handlePatch called for component #' + objectId);
            // Not implemented yet
        },

        handleProps(objectId, propsJson) {
            console.log('handleProps called for component #' + objectId);
            // Not implemented yet
        },

        handleBinary(objectId, binaryData) {
            console.log('handleBinary called for component #' + objectId);
            // Not implemented yet
        },

        handleSlotOperation(objectId, slotOperationJson) {
            console.log('[ComponentBridge] handleSlotOperation called for #' + objectId);
            console.log('[ComponentBridge] Raw JSON:', slotOperationJson);
            
            let slotOperation;
            try {
                slotOperation = JSON.parse(slotOperationJson);
                console.log('[ComponentBridge] Parsed slot operation:', slotOperation);
            } catch (error) {
                console.error('Failed to parse slot operation JSON for component #' + objectId, error);
                return;
            }

            // For simple text slots, we can handle them directly without ComponentTerminal
            if (slotOperation.type === 'text' && (slotOperation.slotName === 'default' || slotOperation.slotName === null)) {
                console.log('[ComponentBridge] Handling text slot operation');
                
                const container = containerRegistry.get(objectId);
                if (!container) {
                    console.error('Container not registered for component #' + objectId);
                    return;
                }

                // Find the Web Awesome component inside the container
                // First try direct child (most common case after handleCreate)
                const waComponent = container.firstElementChild;
                
                if (waComponent && waComponent.tagName && waComponent.tagName.toLowerCase().startsWith('wa-')) {
                    waComponent.textContent = slotOperation.content || '';
                    console.log('[ComponentBridge] ✅ Updated slot for component #' + objectId + ' (<' + waComponent.tagName.toLowerCase() + '>):', slotOperation.content);
                } else {
                    console.warn('No Web Awesome component found in container for #' + objectId, 'firstChild:', waComponent);
                }
                return;
            }

            // For complex slot operations, log a warning
            console.warn('Complex slot operations not yet supported for component #' + objectId, slotOperation);
        },

        handleDestroy(objectId) {
            console.log('handleDestroy called for component #' + objectId);
            // Clean up container registry
            containerRegistry.delete(objectId);
        }
    };

    /**
     * Helper functions for ComponentBridge.
     */
    const componentBridge = {
        registerContainer(objectId, container) {
            containerRegistry.set(objectId, container);
            console.log('Registered container for component #' + objectId);
        },

        initializeTerminal(websocket) {
            console.log('initializeTerminal called (not implemented yet)');
            // Not implemented yet - we don't need the full terminal for basic slots
        },

        getContainer(objectId) {
            return containerRegistry.get(objectId);
        },

        getTerminal() {
            return terminal;
        }
    };

    /**
     * Expose the bridge API to the global window object.
     * PTComponent uses JSNI to call these methods.
     */
    if (typeof window !== 'undefined') {
        window.PonySDK = window.PonySDK || {};
        window.PonySDK.ComponentTerminal = bridgeAPI;
        window.PonySDK.ComponentBridge = componentBridge;
        
        console.log('PonySDK ComponentBridge initialized (standalone mode)');
        console.log('Available methods:', Object.keys(bridgeAPI));
    }
})();
