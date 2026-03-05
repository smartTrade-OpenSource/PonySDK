/**
 * Unit tests for client-side slot handling in WebComponentAdapter and ComponentTerminal.
 *
 * Requirements: 7.2 - Insert child into the corresponding Web Component slot
 * Requirements: 7.3 - Remove child from the slot DOM
 * Requirements: 7.4 - Default slot (null slotName) mounts without slot attribute
 */

import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { WebComponentAdapter } from '../src/adapters/WebComponentAdapter.js';
import { ComponentTerminal } from '../src/ComponentTerminal.js';
import type { ComponentFactory, SlotOperation, ComponentMessage } from '../src/types.js';

// ============================================================================
// Test Helpers
// ============================================================================

function createMockFactory(tagName: string = 'wa-input'): ComponentFactory {
    const container = document.createElement('div');
    document.body.appendChild(container);
    return {
        getContainer: () => container,
        getTagName: () => tagName,
    };
}

function createMockWebSocket(): WebSocket {
    return {
        send: () => { },
        readyState: WebSocket.OPEN,
        addEventListener: () => { },
        removeEventListener: () => { },
    } as unknown as WebSocket;
}

function cleanupContainer(factory: ComponentFactory): void {
    const container = factory.getContainer();
    if (container.parentNode) {
        container.parentNode.removeChild(container);
    }
}

// ============================================================================
// WebComponentAdapter.handleSlotOperation tests
// ============================================================================

describe('WebComponentAdapter slot handling', () => {
    let parentFactory: ComponentFactory;
    let childFactory: ComponentFactory;
    let parentAdapter: WebComponentAdapter<Record<string, unknown>>;
    let childAdapter: WebComponentAdapter<Record<string, unknown>>;

    beforeEach(() => {
        parentFactory = createMockFactory('wa-input');
        childFactory = createMockFactory('wa-icon');
        parentAdapter = new WebComponentAdapter(parentFactory, {}, createMockWebSocket() as unknown as any, 1);
        childAdapter = new WebComponentAdapter(childFactory, {}, createMockWebSocket() as unknown as any, 2);
        parentAdapter.mount();
        childAdapter.mount();
    });

    afterEach(() => {
        parentAdapter.unmount();
        childAdapter.unmount();
        cleanupContainer(parentFactory);
        cleanupContainer(childFactory);
    });

    describe('add operation with named slot', () => {
        it('should append child to parent element with slot attribute', () => {
            const slotOp: SlotOperation = {
                type: 'slot',
                slotName: 'prefix',
                childObjectId: 2,
                operation: 'add',
            };
            const childElement = childAdapter.getElement()!;

            parentAdapter.handleSlotOperation(slotOp, childElement);

            expect(childElement.getAttribute('slot')).toBe('prefix');
            expect(childElement.parentNode).toBe(parentAdapter.getElement());
        });
    });

    describe('add operation with default slot (null slotName)', () => {
        it('should append child to parent element without slot attribute', () => {
            const slotOp: SlotOperation = {
                type: 'slot',
                slotName: null,
                childObjectId: 2,
                operation: 'add',
            };
            const childElement = childAdapter.getElement()!;

            parentAdapter.handleSlotOperation(slotOp, childElement);

            expect(childElement.hasAttribute('slot')).toBe(false);
            expect(childElement.parentNode).toBe(parentAdapter.getElement());
        });
    });

    describe('remove operation', () => {
        it('should detach child from parent element', () => {
            const childElement = childAdapter.getElement()!;
            // First add
            const addOp: SlotOperation = { type: 'slot', slotName: 'suffix', childObjectId: 2, operation: 'add' };
            parentAdapter.handleSlotOperation(addOp, childElement);
            expect(childElement.parentNode).toBe(parentAdapter.getElement());

            // Then remove
            const removeOp: SlotOperation = { type: 'slot', slotName: 'suffix', childObjectId: 2, operation: 'remove' };
            parentAdapter.handleSlotOperation(removeOp, childElement);

            expect(childElement.parentNode).not.toBe(parentAdapter.getElement());
        });
    });

    describe('remove on non-child element', () => {
        it('should not throw when child is not a child of parent', () => {
            const childElement = childAdapter.getElement()!;
            const removeOp: SlotOperation = { type: 'slot', slotName: null, childObjectId: 2, operation: 'remove' };

            // Child was never added to parent — should not throw
            expect(() => parentAdapter.handleSlotOperation(removeOp, childElement)).not.toThrow();
        });
    });

    describe('slot operation on unmounted parent', () => {
        it('should not throw and should warn', () => {
            parentAdapter.unmount();
            const childElement = document.createElement('wa-icon');
            const slotOp: SlotOperation = { type: 'slot', slotName: 'prefix', childObjectId: 2, operation: 'add' };

            expect(() => parentAdapter.handleSlotOperation(slotOp, childElement)).not.toThrow();
        });
    });

    describe('default slot removes existing slot attribute', () => {
        it('should remove slot attribute when adding to default slot', () => {
            const childElement = childAdapter.getElement()!;
            // First add to named slot
            childElement.setAttribute('slot', 'old-slot');
            const slotOp: SlotOperation = { type: 'slot', slotName: null, childObjectId: 2, operation: 'add' };

            parentAdapter.handleSlotOperation(slotOp, childElement);

            expect(childElement.hasAttribute('slot')).toBe(false);
        });
    });
});

// ============================================================================
// ComponentTerminal slot message handling tests
// ============================================================================

describe('ComponentTerminal slot message handling', () => {
    let terminal: ComponentTerminal;
    let parentFactory: ComponentFactory;
    let childFactory: ComponentFactory;

    beforeEach(() => {
        const ws = createMockWebSocket();
        terminal = new ComponentTerminal(ws);

        parentFactory = createMockFactory('wa-card');
        childFactory = createMockFactory('wa-icon');

        terminal.registerFactory('wa-card', parentFactory);
        terminal.registerFactory('wa-icon', childFactory);

        // Create parent (objectId=10) and child (objectId=20)
        terminal.handleMessage({
            objectId: 10,
            type: 'create',
            framework: 'webcomponent',
            signature: 'wa-card',
            props: {},
        });
        terminal.handleMessage({
            objectId: 20,
            type: 'create',
            framework: 'webcomponent',
            signature: 'wa-icon',
            props: {},
        });
    });

    afterEach(() => {
        terminal.handleMessage({ objectId: 10, type: 'destroy' });
        terminal.handleMessage({ objectId: 20, type: 'destroy' });
        cleanupContainer(parentFactory);
        cleanupContainer(childFactory);
    });

    it('should add child to named slot via slot message', () => {
        const slotMsg: ComponentMessage = {
            objectId: 10,
            type: 'slot',
            slotOperation: {
                type: 'slot',
                slotName: 'header',
                childObjectId: 20,
                operation: 'add',
            },
        };

        terminal.handleMessage(slotMsg);

        const parentAdapter = terminal.getRegistry().get(10) as WebComponentAdapter<unknown>;
        const childAdapter = terminal.getRegistry().get(20) as WebComponentAdapter<unknown>;
        const parentEl = parentAdapter.getElement()!;
        const childEl = childAdapter.getElement()!;

        expect(childEl.getAttribute('slot')).toBe('header');
        expect(childEl.parentNode).toBe(parentEl);
    });

    it('should add child to default slot via slot message', () => {
        const slotMsg: ComponentMessage = {
            objectId: 10,
            type: 'slot',
            slotOperation: {
                type: 'slot',
                slotName: null,
                childObjectId: 20,
                operation: 'add',
            },
        };

        terminal.handleMessage(slotMsg);

        const parentAdapter = terminal.getRegistry().get(10) as WebComponentAdapter<unknown>;
        const childAdapter = terminal.getRegistry().get(20) as WebComponentAdapter<unknown>;
        const childEl = childAdapter.getElement()!;

        expect(childEl.hasAttribute('slot')).toBe(false);
        expect(childEl.parentNode).toBe(parentAdapter.getElement());
    });

    it('should remove child from slot via slot message', () => {
        // First add
        terminal.handleMessage({
            objectId: 10,
            type: 'slot',
            slotOperation: { type: 'slot', slotName: 'footer', childObjectId: 20, operation: 'add' },
        });

        // Then remove
        terminal.handleMessage({
            objectId: 10,
            type: 'slot',
            slotOperation: { type: 'slot', slotName: 'footer', childObjectId: 20, operation: 'remove' },
        });

        const parentAdapter = terminal.getRegistry().get(10) as WebComponentAdapter<unknown>;
        const childAdapter = terminal.getRegistry().get(20) as WebComponentAdapter<unknown>;
        const childEl = childAdapter.getElement()!;

        expect(childEl.parentNode).not.toBe(parentAdapter.getElement());
    });

    it('should warn and ignore when parent objectId is unknown', () => {
        const slotMsg: ComponentMessage = {
            objectId: 999,
            type: 'slot',
            slotOperation: { type: 'slot', slotName: 'header', childObjectId: 20, operation: 'add' },
        };

        // Should not throw
        expect(() => terminal.handleMessage(slotMsg)).not.toThrow();
    });

    it('should warn and ignore when child objectId is unknown', () => {
        const slotMsg: ComponentMessage = {
            objectId: 10,
            type: 'slot',
            slotOperation: { type: 'slot', slotName: 'header', childObjectId: 999, operation: 'add' },
        };

        expect(() => terminal.handleMessage(slotMsg)).not.toThrow();
    });

    it('should warn and ignore when slotOperation is missing', () => {
        const slotMsg: ComponentMessage = {
            objectId: 10,
            type: 'slot',
        };

        expect(() => terminal.handleMessage(slotMsg)).not.toThrow();
    });
});
