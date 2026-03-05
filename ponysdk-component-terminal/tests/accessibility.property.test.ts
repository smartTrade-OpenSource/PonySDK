/**
 * Property-based tests for Label Accessibility Association.
 *
 * **Property 16: Label Accessibility Association**
 * *For any* input component (wa-input, wa-select, wa-textarea, wa-checkbox, wa-switch,
 * wa-radio-group, wa-range, wa-color-picker, wa-rating) with a non-empty `label` property,
 * the rendered `wa-*` element SHALL have the label correctly associated for accessibility
 * (the `label` property is set on the `wa-*` element, which natively generates the
 * accessible label/input association).
 *
 * **Validates: Requirements 11.2**
 *
 * @Tag("Feature: ui-library-wrapper, Property 16: Label Accessibility Association")
 */

import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';
import { WebComponentAdapter } from '../src/adapters/WebComponentAdapter.js';
import type { ComponentFactory } from '../src/types.js';

// ============================================================================
// Test Arbitraries (Generators)
// ============================================================================

/**
 * Arbitrary for Web Awesome input component tag names that support the `label` property.
 */
const inputComponentTagArb: fc.Arbitrary<string> = fc.constantFrom(
    'wa-input',
    'wa-select',
    'wa-textarea',
    'wa-checkbox',
    'wa-switch',
    'wa-radio-group',
    'wa-range',
    'wa-color-picker',
    'wa-rating',
);

/**
 * Arbitrary for non-empty label strings.
 * Labels are non-empty strings of printable characters.
 */
const nonEmptyLabelArb: fc.Arbitrary<string> = fc.string({ minLength: 1, maxLength: 100 })
    .filter(s => s.trim().length > 0);

// ============================================================================
// Test Helpers
// ============================================================================

function createMockFactory(tagName: string): ComponentFactory {
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

describe('Property 16: Label Accessibility Association', () => {
    /**
     * **Validates: Requirements 11.2**
     *
     * For any input component with a non-empty label property, the rendered wa-* element
     * SHALL have the label property correctly set. Web Awesome components natively handle
     * label/input association when the label property is set.
     */
    describe('Label property forwarded to wa-* element', () => {
        it('should set the label property on the wa-* element for any input component', () => {
            fc.assert(
                fc.property(
                    inputComponentTagArb,
                    nonEmptyLabelArb,
                    (tagName, label) => {
                        const factory = createMockFactory(tagName);
                        const ws = createMockWebSocket();
                        const adapter = new WebComponentAdapter(factory, { label }, ws, 1);

                        try {
                            adapter.mount();

                            const element = adapter.getElement()!;

                            // The label property must be set on the element
                            expect((element as any).label).toBe(label);
                        } finally {
                            adapter.unmount();
                            cleanupContainer(factory);
                        }
                    },
                ),
                { numRuns: 100 },
            );
        });
    });

    /**
     * **Validates: Requirements 11.2**
     *
     * When the label property is updated via setProps, the wa-* element SHALL reflect
     * the new label value for accessibility.
     */
    describe('Label property updated on props change', () => {
        it('should update the label property when props change', () => {
            fc.assert(
                fc.property(
                    inputComponentTagArb,
                    nonEmptyLabelArb,
                    nonEmptyLabelArb,
                    (tagName, initialLabel, updatedLabel) => {
                        const factory = createMockFactory(tagName);
                        const ws = createMockWebSocket();
                        const adapter = new WebComponentAdapter(factory, { label: initialLabel }, ws, 1);

                        try {
                            adapter.mount();

                            const element = adapter.getElement()!;

                            // Initial label must be set
                            expect((element as any).label).toBe(initialLabel);

                            // Update props with new label
                            adapter.setProps({ label: updatedLabel });

                            // Updated label must be reflected
                            expect((element as any).label).toBe(updatedLabel);
                        } finally {
                            adapter.unmount();
                            cleanupContainer(factory);
                        }
                    },
                ),
                { numRuns: 100 },
            );
        });
    });

    /**
     * **Validates: Requirements 11.2**
     *
     * The label property must be correctly associated regardless of other props
     * being set simultaneously on the input component.
     */
    describe('Label association with other props present', () => {
        it('should correctly set label even when other props are present', () => {
            fc.assert(
                fc.property(
                    inputComponentTagArb,
                    nonEmptyLabelArb,
                    fc.boolean(),
                    fc.boolean(),
                    fc.string({ maxLength: 50 }),
                    (tagName, label, disabled, required, placeholder) => {
                        const factory = createMockFactory(tagName);
                        const ws = createMockWebSocket();
                        const props = { label, disabled, required, placeholder };
                        const adapter = new WebComponentAdapter(factory, props, ws, 1);

                        try {
                            adapter.mount();

                            const element = adapter.getElement()!;

                            // Label must be correctly set alongside other props
                            expect((element as any).label).toBe(label);
                            // Other props must also be set (not clobbered)
                            expect((element as any).disabled).toBe(disabled);
                            expect((element as any).required).toBe(required);
                        } finally {
                            adapter.unmount();
                            cleanupContainer(factory);
                        }
                    },
                ),
                { numRuns: 100 },
            );
        });
    });
});
