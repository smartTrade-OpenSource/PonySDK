/**
 * Bridge between PonySDK protocol and ComponentTerminal.
 * Registers React components similar to AbstractAddon.defineAddon pattern.
 * 
 * This script must be loaded AFTER component-terminal.js bundle.
 */

// Global ComponentTerminal instance
var componentTerminal = null;

// Queue for components registered before PonySDK loads
var pendingRegistrations = [];

// Map to store container elements by objectId
var containers = {};

// Map to store factory functions by signature
var factoryFunctions = {};

/**
 * Decode binary event buffer from EventBridge.
 * Format: [EventCount(4), Event1, Event2, ...]
 * Event: [ObjectId(4), EventTypeLen(2), EventType(N), PayloadLen(4), Payload(N)]
 */
function decodeEventBuffer(buffer) {
    var view = new DataView(buffer);
    var offset = 0;
    var decoder = new TextDecoder();

    // Read event count
    var eventCount = view.getUint32(offset, false);
    offset += 4;

    var events = [];

    for (var i = 0; i < eventCount; i++) {
        // Read object ID
        var objectId = view.getUint32(offset, false);
        offset += 4;

        // Read event type
        var eventTypeLen = view.getUint16(offset, false);
        offset += 2;
        var eventTypeBytes = new Uint8Array(buffer, offset, eventTypeLen);
        var eventType = decoder.decode(eventTypeBytes);
        offset += eventTypeLen;

        // Read payload
        var payloadLen = view.getUint32(offset, false);
        offset += 4;
        var payloadBytes = new Uint8Array(buffer, offset, payloadLen);
        var payloadStr = decoder.decode(payloadBytes);
        var payload = JSON.parse(payloadStr);
        offset += payloadLen;

        events.push({
            objectId: objectId,
            eventType: eventType,
            payload: payload
        });
    }

    return events;
}

/**
 * Register a React component factory with ComponentTerminal.
 * Can be called before or after PonySDK loads.
 * 
 * @param {string} signature - Component signature (e.g., "trading-grid")
 * @param {Function} factoryFn - Factory function (container) => ComponentFactory
 */
function registerReactComponent(signature, factoryFn) {
    console.log("Registering React component:", signature);

    // Store the factory function for later use
    factoryFunctions[signature] = factoryFn;

    if (componentTerminal) {
        // Don't register with ComponentTerminal yet - we'll create the factory
        // when we have a container in handleCreate
    } else {
        pendingRegistrations.push({ signature: signature, factory: factoryFn });
    }
}

/**
 * Initialize ComponentTerminal when PonySDK loads.
 * Called automatically via document.onPonyLoaded.
 */
function initComponentTerminal(pony) {
    console.log("Initializing ComponentTerminal");

    try {
        // Check if ComponentTerminal is available
        if (typeof ComponentTerminal === 'undefined') {
            console.error("ComponentTerminal not found - ensure component-terminal.js is loaded");
            return;
        }

        // ComponentTerminal is a module object, get the actual constructor
        var ComponentTerminalClass = ComponentTerminal.ComponentTerminal;
        if (!ComponentTerminalClass) {
            console.error("ComponentTerminal.ComponentTerminal not found");
            return;
        }

        // Create a mock WebSocket interface for ComponentTerminal
        var mockWebSocket = {
            send: function (data) {
                // EventBridge sends ArrayBuffer, but PonySDK expects JSON
                // Decode the binary format and convert to JSON
                if (data instanceof ArrayBuffer) {
                    console.log('[component-bridge] Received ArrayBuffer, decoding...');
                    var events = decodeEventBuffer(data);
                    console.log('[component-bridge] Decoded events:', events);
                    // Send each event separately to PonySDK
                    for (var i = 0; i < events.length; i++) {
                        var event = events[i];
                        // Create JSON object with eventType and payload
                        var jsonEvent = {
                            eventType: event.eventType,
                            payload: event.payload
                        };
                        console.log('[component-bridge] Sending to server - objectId:', event.objectId, 'event:', jsonEvent);
                        if (pony && pony.sendDataToServer) {
                            pony.sendDataToServer(event.objectId, jsonEvent);
                        }
                    }
                } else if (pony && pony.sendDataToServer) {
                    pony.sendDataToServer(data);
                } else {
                    console.warn("Cannot send component event - pony.sendDataToServer not available");
                }
            }
        };

        // Create ComponentTerminal instance
        componentTerminal = new ComponentTerminalClass(mockWebSocket);
    } catch (error) {
        console.error("Error initializing ComponentTerminal:", error, error.message, error.stack);
        throw error;
    }

    // Register all pending components
    for (var i = 0; i < pendingRegistrations.length; i++) {
        var reg = pendingRegistrations[i];
        factoryFunctions[reg.signature] = reg.factory;
    }
    pendingRegistrations = [];

    // Expose ComponentTerminal API for PTComponent (GWT) to call
    if (!window.PonySDK) {
        window.PonySDK = {};
    }

    window.PonySDK.ComponentTerminal = {
        /**
         * Handle component creation from PTComponent.
         */
        handleCreate: function (objectId, framework, signature, propsJson) {
            console.log("ComponentTerminal.handleCreate:", objectId, framework, signature);

            // Create container element
            var container = document.createElement('div');
            container.id = 'pcomponent-' + objectId;
            container.style.width = '100%';
            container.style.height = '600px'; // Fixed height for now
            container.style.position = 'relative';

            // Store container BEFORE creating the component
            containers[objectId] = container;

            // Append container to body BEFORE creating the component
            // This ensures the container is in the DOM when React tries to mount
            document.body.appendChild(container);

            // Get the factory function and create a factory instance with the container
            var factoryFn = factoryFunctions[signature];
            if (!factoryFn) {
                console.error("No factory function registered for signature:", signature);
                return;
            }

            // Call the factory function with the container to get the ComponentFactory
            var factory = factoryFn(container);

            // Register the factory instance with ComponentTerminal
            componentTerminal.registerFactory(signature + '-' + objectId, factory);

            // Parse props
            var props = JSON.parse(propsJson);

            // Map framework byte to string
            var frameworkMap = ['react', 'vue', 'svelte', 'webcomponent'];
            var frameworkType = frameworkMap[framework] || 'react';

            // Create component message with unique signature
            var message = {
                objectId: objectId,
                type: 'create',
                framework: frameworkType,
                signature: signature + '-' + objectId,
                props: props
            };

            componentTerminal.handleMessage(message);
        },

        /**
         * Handle JSON Patch update from PTComponent.
         */
        handlePatch: function (objectId, patchJson) {
            var patches = JSON.parse(patchJson);
            var message = {
                objectId: objectId,
                type: 'update',
                patches: patches
            };

            componentTerminal.handleMessage(message);
        },

        /**
         * Handle full props update from PTComponent.
         */
        handleProps: function (objectId, propsJson) {
            var props = JSON.parse(propsJson);
            var message = {
                objectId: objectId,
                type: 'update',
                props: props
            };

            componentTerminal.handleMessage(message);
        },

        /**
         * Handle binary update from PTComponent.
         */
        handleBinary: function (objectId, binaryData) {
            var message = {
                objectId: objectId,
                type: 'update',
                binaryData: binaryData
            };

            componentTerminal.handleMessage(message);
        },

        /**
         * Handle component destruction from PTComponent.
         */
        handleDestroy: function (objectId) {
            var message = {
                objectId: objectId,
                type: 'destroy'
            };

            componentTerminal.handleMessage(message);

            // Remove container
            var container = containers[objectId];
            if (container && container.parentNode) {
                container.parentNode.removeChild(container);
            }
            delete containers[objectId];
        }
    };

    console.log("ComponentTerminal initialized");
}

// Auto-initialize when PonySDK loads
if (typeof document !== 'undefined') {
    if (!document.onPonyLoadedListeners) {
        document.onPonyLoadedListeners = [];
    }
    document.onPonyLoadedListeners.push(initComponentTerminal);
}

// Export for module systems
if (typeof module !== 'undefined' && module.hasOwnProperty('exports')) {
    module.exports.registerReactComponent = registerReactComponent;
    module.exports.initComponentTerminal = initComponentTerminal;
} else if (typeof window !== 'undefined') {
    window.registerReactComponent = registerReactComponent;
    window.initComponentTerminal = initComponentTerminal;
}
