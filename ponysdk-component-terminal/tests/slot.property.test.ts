/**
 * Property-based tests for Slot Add/Remove Round-Trip.
 *
 * **Property 6: Slot Add/Remove Round-Trip**
 * *For any* component with declared slots and any child component, adding the child to a named slot
 * then removing it SHALL result in the slot being empty. Conversely, adding a child to a slot SHALL
 * result in the child being present in that slot's DOM position.
 *
 * **Validates: Requirements 7.1, 7.2, 7.3**
 */

import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import * as fc from 'fast-check';
import { WebComponentAdapter } from '../src/adapters/WebComponentAdapter.js';
import type { ComponentFactory, SlotOperation } from '../src/types.js';

// ============================================================================
// Test Arbitraries (Generators)
// ============================================================================

/**
 * Arbitrary for slot names: either a named slot (non-empty string) or null (default slot).
 */
const slotNameArb: fc.Arbitrary<string | null> = fc.oneof(
    fc.constant(null),
    fc.stringMatching(/^[a-z][a-z0-9-]{0,28}[a-z0-9]$/)
);

/**
 * Arbitrary for slot operations as defined in the design's slotOperationArb.
 */
const slotOperationArb: fc.Arbitrary<SlotOperation> = fc.record({
    type: fc.constant('slot' as const),
    slotName: slotNameArb,
    childObjectId: fc.integer({ min: 1, max: 100000 }),
    operation: fc.oneof(fc.constant('add' as const), fc.constant('remove' as const)),
});

/**
 * Arbitrary for Web Component tag names (wa-* prefix).
 */
const tagNameArb: fc.Arbitrary<string> = fc.stringMatching(/^[a-z]{2,15}$/).map(s => `wa-${s}`);

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

function createMockWebSocket(): any {
    return {
        send: () => { },
        readyState: WebSocket.OPEN,
        addEventListener: () => { },
        removeEventListener: () => { },
    };
}

function cleanupContainer(factory: ComponentFactory): void {
    const container = factory.getContainer();
    if (container.parentNode) {
        container.parentNode.removeChild(container);
    }
}

// ============================================================================
// Property Tests
// ============================================================================

describe('Property 6: Slot Add/Remove Round-Trip', () => {
    /**
     * **Validates: Requirements 7.1, 7.2**
     *
     * After adding a child to any slot, the child SHALL be present in the parent element
     * with the correct slot attribute (or no slot attribute for default slot).
     */
    describe('Add results in child present', () => {
        it('should have child in parent with correct slot attribute after add', () => {
            fc.assert(
                fc.property(
                    tagNameArb,
                    slotNameArb,
                    (parentTag, slotName) => {
                        const parentFactory = createMockFactory(parentTag);
                        const childFactory = createMockFactory('wa-icon');
                        const ws = createMockWebSocket();
                        const parentAdapter = new WebComponentAdapter(parentFactory, {}, ws, 1);
                        const childAdapter = new WebComponentAdapter(childFactory, {}, ws, 2);

                        try {
                            parentAdapter.mount();
                            childAdapter.mount();

                            const childElement = childAdapter.getElement()!;
                            const parentElement = parentAdapter.getElement()!;

                            const addOp: SlotOperation = {
                                type: 'slot',
                                slotName,
                                childObjectId: 2,
                                operation: 'add',
                            };

                            parentAdapter.handleSlotOperation(addOp, childElement);

                            // Child must be a child of the parent element
                            expect(childElement.parentNode).toBe(parentElement);

                            // Slot attribute correctness
                            if (slotName !== null) {
                                expect(childElement.getAttribute('slot')).toBe(slotName);
                            } else {
                                expect(childElement.hasAttribute('slot')).toBe(false);
                            }
                        } finally {
                            parentAdapter.unmount();
                            childAdapter.unmount();
                            cleanupContainer(parentFactory);
                            cleanupContainer(childFactory);
                        }
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 7.1, 7.2, 7.3**
     *
     * For any component and any child, adding then removing SHALL result in the child
     * no longer being a child of the parent element.
     */
    describe('Add then remove results in empty slot', () => {
        it('should not have child in parent after add then remove', () => {
            fc.assert(
                fc.property(
                    tagNameArb,
                    slotNameArb,
                    (parentTag, slotName) => {
                        const parentFactory = createMockFactory(parentTag);
                        const childFactory = createMockFactory('wa-icon');
                        const ws = createMockWebSocket();
                        const parentAdapter = new WebComponentAdapter(parentFactory, {}, ws, 1);
                        const childAdapter = new WebComponentAdapter(childFactory, {}, ws, 2);

                        try {
                            parentAdapter.mount();
                            childAdapter.mount();

                            const childElement = childAdapter.getElement()!;
                            const parentElement = parentAdapter.getElement()!;

                            // Add
                            const addOp: SlotOperation = {
                                type: 'slot',
                                slotName,
                                childObjectId: 2,
                                operation: 'add',
                            };
                            parentAdapter.handleSlotOperation(addOp, childElement);

                            // Verify add worked
                            expect(childElement.parentNode).toBe(parentElement);

                            // Remove
                            const removeOp: SlotOperation = {
                                type: 'slot',
                                slotName,
                                childObjectId: 2,
                                operation: 'remove',
                            };
                            parentAdapter.handleSlotOperation(removeOp, childElement);

                            // Child must no longer be a child of parent
                            expect(childElement.parentNode).not.toBe(parentElement);
                        } finally {
                            parentAdapter.unmount();
                            childAdapter.unmount();
                            cleanupContainer(parentFactory);
                            cleanupContainer(childFactory);
                        }
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 7.1, 7.3**
     *
     * Adding to default slot (null slotName) SHALL result in child having no slot attribute,
     * and removing SHALL detach the child.
     */
    describe('Default slot round-trip', () => {
        it('should handle default slot add/remove without slot attribute', () => {
            fc.assert(
                fc.property(
                    tagNameArb,
                    (parentTag) => {
                        const parentFactory = createMockFactory(parentTag);
                        const childFactory = createMockFactory('wa-badge');
                        const ws = createMockWebSocket();
                        const parentAdapter = new WebComponentAdapter(parentFactory, {}, ws, 1);
                        const childAdapter = new WebComponentAdapter(childFactory, {}, ws, 2);

                        try {
                            parentAdapter.mount();
                            childAdapter.mount();

                            const childElement = childAdapter.getElement()!;
                            const parentElement = parentAdapter.getElement()!;

                            // Add to default slot
                            const addOp: SlotOperation = {
                                type: 'slot',
                                slotName: null,
                                childObjectId: 2,
                                operation: 'add',
                            };
                            parentAdapter.handleSlotOperation(addOp, childElement);

                            expect(childElement.parentNode).toBe(parentElement);
                            expect(childElement.hasAttribute('slot')).toBe(false);

                            // Remove from default slot
                            const removeOp: SlotOperation = {
                                type: 'slot',
                                slotName: null,
                                childObjectId: 2,
                                operation: 'remove',
                            };
                            parentAdapter.handleSlotOperation(removeOp, childElement);

                            expect(childElement.parentNode).not.toBe(parentElement);
                        } finally {
                            parentAdapter.unmount();
                            childAdapter.unmount();
                            cleanupContainer(parentFactory);
                            cleanupContainer(childFactory);
                        }
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 7.1, 7.2, 7.3**
     *
     * Multiple add/remove cycles on the same slot SHALL always leave the parent
     * in a consistent state: child present after add, absent after remove.
     */
    describe('Repeated add/remove cycles', () => {
        it('should maintain consistent state through multiple add/remove cycles', () => {
            fc.assert(
                fc.property(
                    tagNameArb,
                    slotNameArb,
                    fc.integer({ min: 1, max: 5 }),
                    (parentTag, slotName, cycleCount) => {
                        const parentFactory = createMockFactory(parentTag);
                        const childFactory = createMockFactory('wa-icon');
                        const ws = createMockWebSocket();
                        const parentAdapter = new WebComponentAdapter(parentFactory, {}, ws, 1);
                        const childAdapter = new WebComponentAdapter(childFactory, {}, ws, 2);

                        try {
                            parentAdapter.mount();
                            childAdapter.mount();

                            const childElement = childAdapter.getElement()!;
                            const parentElement = parentAdapter.getElement()!;

                            for (let i = 0; i < cycleCount; i++) {
                                // Add
                                parentAdapter.handleSlotOperation(
                                    { type: 'slot', slotName, childObjectId: 2, operation: 'add' },
                                    childElement
                                );
                                expect(childElement.parentNode).toBe(parentElement);

                                if (slotName !== null) {
                                    expect(childElement.getAttribute('slot')).toBe(slotName);
                                } else {
                                    expect(childElement.hasAttribute('slot')).toBe(false);
                                }

                                // Remove
                                parentAdapter.handleSlotOperation(
                                    { type: 'slot', slotName, childObjectId: 2, operation: 'remove' },
                                    childElement
                                );
                                expect(childElement.parentNode).not.toBe(parentElement);
                            }
                        } finally {
                            parentAdapter.unmount();
                            childAdapter.unmount();
                            cleanupContainer(parentFactory);
                            cleanupContainer(childFactory);
                        }
                    }
                ),
                { numRuns: 100 }
            );
        });
    });
});
