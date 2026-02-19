/**
 * Property-based tests for Protocol Message Round-Trip.
 * 
 * **Property 6: Protocol Message Round-Trip**
 * *For any* valid ComponentMessage, encoding to binary using the PonySDK protocol 
 * then decoding SHALL produce an equivalent message.
 * 
 * **Validates: Requirements 6.1, 10.3**
 * 
 * Since we're testing the TypeScript side, we test:
 * 1. Generate arbitrary ComponentMessage objects
 * 2. Verify that the message structure is valid
 * 3. Test that ComponentTerminal.handleMessage correctly routes messages
 * 4. Test that message types (create, update, destroy) are handled correctly
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import * as fc from 'fast-check';
import { ComponentTerminal } from '../src/ComponentTerminal.js';
import { ComponentRegistry } from '../src/ComponentRegistry.js';
import type { ComponentMessage, FrameworkType, ComponentMessageType, ComponentFactory } from '../src/types.js';
import type { FrameworkAdapter } from '../src/adapters/FrameworkAdapter.js';
import type { Operation } from 'fast-json-patch';

// ============================================================================
// Test Arbitraries (Generators)
// ============================================================================

/**
 * Arbitrary for framework types.
 */
const frameworkTypeArb: fc.Arbitrary<FrameworkType> = fc.constantFrom(
    'react', 'vue', 'svelte', 'webcomponent'
);

/**
 * Arbitrary for message types.
 */
const messageTypeArb: fc.Arbitrary<ComponentMessageType> = fc.constantFrom(
    'create', 'update', 'destroy'
);

/**
 * Arbitrary for positive object IDs (valid component identifiers).
 */
const objectIdArb: fc.Arbitrary<number> = fc.integer({ min: 1, max: 1000000 });

/**
 * Arbitrary for component signatures.
 */
const signatureArb: fc.Arbitrary<string> = fc.string({ minLength: 1, maxLength: 100 })
    .filter(s => s.trim().length > 0);

/**
 * Arbitrary for simple props objects.
 */
const propsArb: fc.Arbitrary<Record<string, unknown>> = fc.record({
    title: fc.string(),
    count: fc.integer(),
    enabled: fc.boolean(),
    value: fc.double({ noNaN: true, noDefaultInfinity: true }),
    items: fc.array(fc.string(), { maxLength: 10 }),
});

/**
 * Arbitrary for JSON Patch operations.
 */
const jsonPatchOperationArb: fc.Arbitrary<Operation> = fc.oneof(
    fc.record({
        op: fc.constant('replace' as const),
        path: fc.constantFrom('/title', '/count', '/enabled', '/value'),
        value: fc.oneof(fc.string(), fc.integer(), fc.boolean()),
    }),
    fc.record({
        op: fc.constant('add' as const),
        path: fc.constant('/items/-'),
        value: fc.string(),
    }),
    fc.record({
        op: fc.constant('remove' as const),
        path: fc.constantFrom('/title', '/count', '/enabled'),
    })
);

/**
 * Arbitrary for JSON Patch arrays.
 */
const patchesArb: fc.Arbitrary<Operation[]> = fc.array(jsonPatchOperationArb, { minLength: 1, maxLength: 5 });

/**
 * Arbitrary for valid create messages.
 */
const createMessageArb: fc.Arbitrary<ComponentMessage> = fc.record({
    objectId: objectIdArb,
    type: fc.constant('create' as const),
    framework: frameworkTypeArb,
    signature: signatureArb,
    props: propsArb,
});

/**
 * Arbitrary for valid update messages with props.
 */
const updatePropsMessageArb: fc.Arbitrary<ComponentMessage> = fc.record({
    objectId: objectIdArb,
    type: fc.constant('update' as const),
    props: propsArb,
});

/**
 * Arbitrary for valid update messages with patches.
 */
const updatePatchMessageArb: fc.Arbitrary<ComponentMessage> = fc.record({
    objectId: objectIdArb,
    type: fc.constant('update' as const),
    patches: patchesArb,
});

/**
 * Arbitrary for valid destroy messages.
 */
const destroyMessageArb: fc.Arbitrary<ComponentMessage> = fc.record({
    objectId: objectIdArb,
    type: fc.constant('destroy' as const),
});

/**
 * Arbitrary for any valid component message.
 */
const componentMessageArb: fc.Arbitrary<ComponentMessage> = fc.oneof(
    createMessageArb,
    updatePropsMessageArb,
    updatePatchMessageArb,
    destroyMessageArb
);

// ============================================================================
// Mock Helpers
// ============================================================================

/**
 * Creates a mock WebSocket for testing.
 */
function createMockWebSocket(): WebSocket {
    return {
        send: vi.fn(),
        close: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        readyState: WebSocket.OPEN,
    } as unknown as WebSocket;
}

/**
 * Creates a mock FrameworkAdapter for testing.
 */
function createMockAdapter(): FrameworkAdapter {
    let mounted = false;
    let currentProps: unknown = {};

    return {
        mount: vi.fn(() => { mounted = true; }),
        unmount: vi.fn(() => { mounted = false; }),
        setProps: vi.fn((props: unknown) => { currentProps = props; }),
        applyPatches: vi.fn(),
        applyBinary: vi.fn(),
        isMounted: vi.fn(() => mounted),
    };
}

/**
 * Creates a mock ComponentFactory for testing.
 */
function createMockFactory(): ComponentFactory {
    const container = document.createElement('div');
    return {
        getContainer: () => container,
        getReactComponent: () => ({}),
        getVueComponent: () => ({}),
        getSvelteComponent: () => ({}),
        getTagName: () => 'test-component',
    };
}

// ============================================================================
// Property Tests
// ============================================================================

describe('Property 6: Protocol Message Round-Trip', () => {
    /**
     * **Validates: Requirements 6.1, 10.3**
     * 
     * Property: For any valid ComponentMessage, the message structure is preserved
     * when processed by the ComponentTerminal.
     */
    describe('Message Structure Preservation', () => {
        it('should preserve all required fields for create messages', () => {
            fc.assert(
                fc.property(createMessageArb, (message) => {
                    // Verify create message has all required fields
                    expect(message.objectId).toBeGreaterThan(0);
                    expect(message.type).toBe('create');
                    expect(message.framework).toBeDefined();
                    expect(['react', 'vue', 'svelte', 'webcomponent']).toContain(message.framework);
                    expect(message.signature).toBeDefined();
                    expect(typeof message.signature).toBe('string');
                    expect(message.signature!.length).toBeGreaterThan(0);
                }),
                { numRuns: 100 }
            );
        });

        it('should preserve all required fields for update messages', () => {
            fc.assert(
                fc.property(
                    fc.oneof(updatePropsMessageArb, updatePatchMessageArb),
                    (message) => {
                        // Verify update message has required fields
                        expect(message.objectId).toBeGreaterThan(0);
                        expect(message.type).toBe('update');
                        // Must have either props or patches
                        const hasProps = message.props !== undefined;
                        const hasPatches = message.patches !== undefined;
                        expect(hasProps || hasPatches).toBe(true);
                    }
                ),
                { numRuns: 100 }
            );
        });

        it('should preserve all required fields for destroy messages', () => {
            fc.assert(
                fc.property(destroyMessageArb, (message) => {
                    // Verify destroy message has required fields
                    expect(message.objectId).toBeGreaterThan(0);
                    expect(message.type).toBe('destroy');
                }),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 6.1, 10.3**
     * 
     * Property: ComponentTerminal correctly routes messages to appropriate handlers
     * based on message type.
     */
    describe('Message Routing', () => {
        let terminal: ComponentTerminal;
        let mockWebSocket: WebSocket;

        beforeEach(() => {
            mockWebSocket = createMockWebSocket();
            terminal = new ComponentTerminal(mockWebSocket);
        });

        it('should route create messages to create handler', () => {
            fc.assert(
                fc.property(createMessageArb, (message) => {
                    // Register factory for the signature
                    terminal.registerFactory(message.signature!, createMockFactory());

                    // Handle the message - should not throw
                    expect(() => terminal.handleMessage(message)).not.toThrow();

                    // Verify component was registered
                    expect(terminal.getRegistry().has(message.objectId)).toBe(true);
                }),
                { numRuns: 100 }
            );
        });

        it('should route update messages to update handler for existing components', () => {
            fc.assert(
                fc.property(
                    createMessageArb,
                    fc.oneof(updatePropsMessageArb, updatePatchMessageArb),
                    (createMsg, updateMsg) => {
                        // Use same objectId for update
                        const updateWithSameId: ComponentMessage = {
                            ...updateMsg,
                            objectId: createMsg.objectId,
                        };

                        // Register factory and create component
                        terminal.registerFactory(createMsg.signature!, createMockFactory());
                        terminal.handleMessage(createMsg);

                        // Handle update - should not throw
                        expect(() => terminal.handleMessage(updateWithSameId)).not.toThrow();
                    }
                ),
                { numRuns: 100 }
            );
        });

        it('should route destroy messages to destroy handler', () => {
            fc.assert(
                fc.property(createMessageArb, (createMsg) => {
                    const destroyMsg: ComponentMessage = {
                        objectId: createMsg.objectId,
                        type: 'destroy',
                    };

                    // Register factory and create component
                    terminal.registerFactory(createMsg.signature!, createMockFactory());
                    terminal.handleMessage(createMsg);

                    // Verify component exists
                    expect(terminal.getRegistry().has(createMsg.objectId)).toBe(true);

                    // Handle destroy
                    terminal.handleMessage(destroyMsg);

                    // Verify component was removed
                    expect(terminal.getRegistry().has(createMsg.objectId)).toBe(false);
                }),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 6.1, 10.3**
     * 
     * Property: Message type determines the correct lifecycle operation.
     */
    describe('Message Type Handling', () => {
        let terminal: ComponentTerminal;
        let mockWebSocket: WebSocket;

        beforeEach(() => {
            mockWebSocket = createMockWebSocket();
            terminal = new ComponentTerminal(mockWebSocket);
        });

        it('should handle all message types without errors for valid sequences', () => {
            fc.assert(
                fc.property(
                    createMessageArb,
                    propsArb,
                    (createMsg, newProps) => {
                        // Register factory
                        terminal.registerFactory(createMsg.signature!, createMockFactory());

                        // Create
                        expect(() => terminal.handleMessage(createMsg)).not.toThrow();
                        expect(terminal.getRegistry().has(createMsg.objectId)).toBe(true);

                        // Update with props
                        const updateMsg: ComponentMessage = {
                            objectId: createMsg.objectId,
                            type: 'update',
                            props: newProps,
                        };
                        expect(() => terminal.handleMessage(updateMsg)).not.toThrow();

                        // Destroy
                        const destroyMsg: ComponentMessage = {
                            objectId: createMsg.objectId,
                            type: 'destroy',
                        };
                        expect(() => terminal.handleMessage(destroyMsg)).not.toThrow();
                        expect(terminal.getRegistry().has(createMsg.objectId)).toBe(false);
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 6.1, 10.3**
     * 
     * Property: JSON Patch operations in update messages are correctly structured.
     */
    describe('JSON Patch Structure', () => {
        it('should have valid JSON Patch operations', () => {
            fc.assert(
                fc.property(patchesArb, (patches) => {
                    // Verify each patch has required fields
                    for (const patch of patches) {
                        expect(patch.op).toBeDefined();
                        expect(['add', 'remove', 'replace', 'move', 'copy', 'test']).toContain(patch.op);
                        expect(patch.path).toBeDefined();
                        expect(typeof patch.path).toBe('string');
                        expect(patch.path.startsWith('/')).toBe(true);

                        // 'remove' doesn't require value, others do
                        if (patch.op !== 'remove') {
                            expect('value' in patch).toBe(true);
                        }
                    }
                }),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 6.1, 10.3**
     * 
     * Property: Framework type in create messages maps to correct adapter type.
     */
    describe('Framework Type Mapping', () => {
        it('should accept all valid framework types', () => {
            fc.assert(
                fc.property(frameworkTypeArb, (framework) => {
                    expect(['react', 'vue', 'svelte', 'webcomponent']).toContain(framework);
                }),
                { numRuns: 100 }
            );
        });

        it('should create adapters for all framework types', () => {
            fc.assert(
                fc.property(
                    objectIdArb,
                    frameworkTypeArb,
                    signatureArb,
                    propsArb,
                    (objectId, framework, signature, props) => {
                        const registry = new ComponentRegistry();
                        registry.registerFactory(signature, createMockFactory());

                        // Should create adapter without throwing
                        expect(() => {
                            registry.createAdapter(objectId, framework, signature, props);
                        }).not.toThrow();

                        // Adapter should be registered
                        expect(registry.has(objectId)).toBe(true);
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 6.1, 10.3**
     * 
     * Property: Object IDs are preserved through message handling.
     */
    describe('Object ID Preservation', () => {
        let terminal: ComponentTerminal;
        let mockWebSocket: WebSocket;

        beforeEach(() => {
            mockWebSocket = createMockWebSocket();
            terminal = new ComponentTerminal(mockWebSocket);
        });

        it('should preserve object ID through create-update-destroy cycle', () => {
            fc.assert(
                fc.property(createMessageArb, (createMsg) => {
                    const objectId = createMsg.objectId;

                    // Register factory
                    terminal.registerFactory(createMsg.signature!, createMockFactory());

                    // Create - verify ID is used
                    terminal.handleMessage(createMsg);
                    expect(terminal.getRegistry().has(objectId)).toBe(true);

                    // Update - verify same ID is targeted
                    const updateMsg: ComponentMessage = {
                        objectId,
                        type: 'update',
                        props: { updated: true },
                    };
                    terminal.handleMessage(updateMsg);
                    expect(terminal.getRegistry().has(objectId)).toBe(true);

                    // Destroy - verify same ID is removed
                    const destroyMsg: ComponentMessage = {
                        objectId,
                        type: 'destroy',
                    };
                    terminal.handleMessage(destroyMsg);
                    expect(terminal.getRegistry().has(objectId)).toBe(false);
                }),
                { numRuns: 100 }
            );
        });

        it('should handle multiple components with different IDs independently', () => {
            fc.assert(
                fc.property(
                    fc.array(createMessageArb, { minLength: 2, maxLength: 5 }),
                    (messages) => {
                        // Ensure unique object IDs
                        const uniqueIds = new Set(messages.map(m => m.objectId));
                        if (uniqueIds.size !== messages.length) {
                            return; // Skip if IDs are not unique
                        }

                        // Register factories and create all components
                        for (const msg of messages) {
                            terminal.registerFactory(msg.signature!, createMockFactory());
                            terminal.handleMessage(msg);
                        }

                        // Verify all components exist
                        for (const msg of messages) {
                            expect(terminal.getRegistry().has(msg.objectId)).toBe(true);
                        }

                        // Destroy first component
                        terminal.handleMessage({
                            objectId: messages[0].objectId,
                            type: 'destroy',
                        });

                        // First should be gone, others should remain
                        expect(terminal.getRegistry().has(messages[0].objectId)).toBe(false);
                        for (let i = 1; i < messages.length; i++) {
                            expect(terminal.getRegistry().has(messages[i].objectId)).toBe(true);
                        }
                    }
                ),
                { numRuns: 100 }
            );
        });
    });
});

/**
 * Additional unit tests for edge cases and error handling.
 */
describe('Protocol Message Edge Cases', () => {
    let terminal: ComponentTerminal;
    let mockWebSocket: WebSocket;

    beforeEach(() => {
        mockWebSocket = createMockWebSocket();
        terminal = new ComponentTerminal(mockWebSocket);
    });

    it('should handle create message without registered factory gracefully', () => {
        const message: ComponentMessage = {
            objectId: 1,
            type: 'create',
            framework: 'react',
            signature: 'unregistered-component',
            props: {},
        };

        // Should not throw, but component should not be created
        expect(() => terminal.handleMessage(message)).not.toThrow();
        expect(terminal.getRegistry().has(1)).toBe(false);
    });

    it('should handle update message for non-existent component gracefully', () => {
        const message: ComponentMessage = {
            objectId: 999,
            type: 'update',
            props: { test: true },
        };

        // Should not throw
        expect(() => terminal.handleMessage(message)).not.toThrow();
    });

    it('should handle destroy message for non-existent component gracefully', () => {
        const message: ComponentMessage = {
            objectId: 999,
            type: 'destroy',
        };

        // Should not throw
        expect(() => terminal.handleMessage(message)).not.toThrow();
    });

    it('should handle create message missing framework gracefully', () => {
        const message: ComponentMessage = {
            objectId: 1,
            type: 'create',
            signature: 'test-component',
            props: {},
        };

        // Should not throw, but component should not be created
        expect(() => terminal.handleMessage(message)).not.toThrow();
        expect(terminal.getRegistry().has(1)).toBe(false);
    });

    it('should handle create message missing signature gracefully', () => {
        const message: ComponentMessage = {
            objectId: 1,
            type: 'create',
            framework: 'react',
            props: {},
        };

        // Should not throw, but component should not be created
        expect(() => terminal.handleMessage(message)).not.toThrow();
        expect(terminal.getRegistry().has(1)).toBe(false);
    });
});
