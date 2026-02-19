/**
 * Property-based tests for Mount State Idempotence.
 * 
 * **Property 11: Mount State Idempotence**
 * *For any* component, calling mount() multiple times SHALL have the same effect as calling it once 
 * (no duplicate DOM elements, no errors). Similarly, calling unmount() on an already unmounted 
 * component SHALL have no effect.
 * 
 * **Validates: Requirements 11.5**
 */

import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import * as fc from 'fast-check';
import { ReactAdapter } from '../src/adapters/ReactAdapter.js';
import { VueAdapter } from '../src/adapters/VueAdapter.js';
import { SvelteAdapter } from '../src/adapters/SvelteAdapter.js';
import { WebComponentAdapter } from '../src/adapters/WebComponentAdapter.js';
import type { FrameworkAdapter } from '../src/adapters/FrameworkAdapter.js';
import type { ComponentFactory } from '../src/types.js';

// ============================================================================
// Test Arbitraries (Generators)
// ============================================================================

/**
 * Arbitrary for number of mount calls (1-10).
 */
const mountCallCountArb: fc.Arbitrary<number> = fc.integer({ min: 1, max: 10 });

/**
 * Arbitrary for number of unmount calls (1-10).
 */
const unmountCallCountArb: fc.Arbitrary<number> = fc.integer({ min: 1, max: 10 });

/**
 * Arbitrary for simple props objects.
 */
const propsArb: fc.Arbitrary<Record<string, unknown>> = fc.record({
    title: fc.string({ minLength: 0, maxLength: 50 }),
    count: fc.integer(),
    enabled: fc.boolean(),
    value: fc.double({ noNaN: true, noDefaultInfinity: true }),
});

/**
 * Arbitrary for adapter type selection.
 */
type AdapterType = 'react' | 'vue' | 'svelte' | 'webcomponent';
const adapterTypeArb: fc.Arbitrary<AdapterType> = fc.constantFrom(
    'react', 'vue', 'svelte', 'webcomponent'
);

// ============================================================================
// Test Helpers
// ============================================================================

/**
 * Creates a mock ComponentFactory for testing.
 */
function createMockFactory<TProps>(tagName: string = 'test-component'): ComponentFactory<TProps> {
    const container = document.createElement('div');
    document.body.appendChild(container);

    return {
        getContainer: () => container,
        getReactComponent: () => ({}),
        getVueComponent: () => ({}),
        getSvelteComponent: () => ({}),
        getTagName: () => tagName,
    };
}

/**
 * Creates an adapter of the specified type.
 */
function createAdapter<TProps>(
    type: AdapterType,
    factory: ComponentFactory<TProps>,
    initialProps: TProps
): FrameworkAdapter<TProps> {
    switch (type) {
        case 'react':
            return new ReactAdapter(factory, initialProps);
        case 'vue':
            return new VueAdapter(factory, initialProps);
        case 'svelte':
            return new SvelteAdapter(factory, initialProps);
        case 'webcomponent':
            return new WebComponentAdapter(factory, initialProps);
    }
}

/**
 * Cleans up the container from the DOM.
 */
function cleanupContainer(factory: ComponentFactory): void {
    const container = factory.getContainer();
    if (container.parentNode) {
        container.parentNode.removeChild(container);
    }
}

// ============================================================================
// Property Tests
// ============================================================================

describe('Property 11: Mount State Idempotence', () => {
    /**
     * **Validates: Requirements 11.5**
     * 
     * Property: Calling mount() multiple times has the same effect as calling it once.
     */
    describe('Mount Idempotence', () => {
        it('should return isMounted() = true after first mount and remain true after subsequent mounts', () => {
            fc.assert(
                fc.property(
                    adapterTypeArb,
                    mountCallCountArb,
                    propsArb,
                    (adapterType, mountCount, props) => {
                        const factory = createMockFactory<typeof props>();
                        const adapter = createAdapter(adapterType, factory, props);

                        try {
                            // Initially not mounted
                            expect(adapter.isMounted()).toBe(false);

                            // First mount
                            adapter.mount();
                            expect(adapter.isMounted()).toBe(true);

                            // Subsequent mounts should not change state
                            for (let i = 1; i < mountCount; i++) {
                                adapter.mount();
                                expect(adapter.isMounted()).toBe(true);
                            }
                        } finally {
                            // Cleanup
                            adapter.unmount();
                            cleanupContainer(factory);
                        }
                    }
                ),
                { numRuns: 100 }
            );
        });

        it('should not throw errors when mount() is called multiple times', () => {
            fc.assert(
                fc.property(
                    adapterTypeArb,
                    mountCallCountArb,
                    propsArb,
                    (adapterType, mountCount, props) => {
                        const factory = createMockFactory<typeof props>();
                        const adapter = createAdapter(adapterType, factory, props);

                        try {
                            // Multiple mount calls should not throw
                            for (let i = 0; i < mountCount; i++) {
                                expect(() => adapter.mount()).not.toThrow();
                            }
                        } finally {
                            adapter.unmount();
                            cleanupContainer(factory);
                        }
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 11.5**
     * 
     * Property: Calling unmount() on an already unmounted component has no effect.
     */
    describe('Unmount Idempotence', () => {
        it('should return isMounted() = false after first unmount and remain false after subsequent unmounts', () => {
            fc.assert(
                fc.property(
                    adapterTypeArb,
                    unmountCallCountArb,
                    propsArb,
                    (adapterType, unmountCount, props) => {
                        const factory = createMockFactory<typeof props>();
                        const adapter = createAdapter(adapterType, factory, props);

                        try {
                            // Mount first
                            adapter.mount();
                            expect(adapter.isMounted()).toBe(true);

                            // First unmount
                            adapter.unmount();
                            expect(adapter.isMounted()).toBe(false);

                            // Subsequent unmounts should not change state
                            for (let i = 1; i < unmountCount; i++) {
                                adapter.unmount();
                                expect(adapter.isMounted()).toBe(false);
                            }
                        } finally {
                            cleanupContainer(factory);
                        }
                    }
                ),
                { numRuns: 100 }
            );
        });

        it('should not throw errors when unmount() is called multiple times', () => {
            fc.assert(
                fc.property(
                    adapterTypeArb,
                    unmountCallCountArb,
                    propsArb,
                    (adapterType, unmountCount, props) => {
                        const factory = createMockFactory<typeof props>();
                        const adapter = createAdapter(adapterType, factory, props);

                        try {
                            // Mount first
                            adapter.mount();

                            // Multiple unmount calls should not throw
                            for (let i = 0; i < unmountCount; i++) {
                                expect(() => adapter.unmount()).not.toThrow();
                            }
                        } finally {
                            cleanupContainer(factory);
                        }
                    }
                ),
                { numRuns: 100 }
            );
        });

        it('should not throw errors when unmount() is called on never-mounted component', () => {
            fc.assert(
                fc.property(
                    adapterTypeArb,
                    unmountCallCountArb,
                    propsArb,
                    (adapterType, unmountCount, props) => {
                        const factory = createMockFactory<typeof props>();
                        const adapter = createAdapter(adapterType, factory, props);

                        try {
                            // Never mount, just unmount multiple times
                            expect(adapter.isMounted()).toBe(false);

                            for (let i = 0; i < unmountCount; i++) {
                                expect(() => adapter.unmount()).not.toThrow();
                                expect(adapter.isMounted()).toBe(false);
                            }
                        } finally {
                            cleanupContainer(factory);
                        }
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 11.5**
     * 
     * Property: Mount/unmount cycles work correctly regardless of call counts.
     */
    describe('Mount/Unmount Cycle Idempotence', () => {
        it('should handle arbitrary mount/unmount sequences correctly', () => {
            fc.assert(
                fc.property(
                    adapterTypeArb,
                    mountCallCountArb,
                    unmountCallCountArb,
                    propsArb,
                    (adapterType, mountCount, unmountCount, props) => {
                        const factory = createMockFactory<typeof props>();
                        const adapter = createAdapter(adapterType, factory, props);

                        try {
                            // Multiple mounts
                            for (let i = 0; i < mountCount; i++) {
                                adapter.mount();
                            }
                            expect(adapter.isMounted()).toBe(true);

                            // Multiple unmounts
                            for (let i = 0; i < unmountCount; i++) {
                                adapter.unmount();
                            }
                            expect(adapter.isMounted()).toBe(false);

                            // Can mount again after unmount
                            adapter.mount();
                            expect(adapter.isMounted()).toBe(true);
                        } finally {
                            adapter.unmount();
                            cleanupContainer(factory);
                        }
                    }
                ),
                { numRuns: 100 }
            );
        });

        it('should maintain consistent state through multiple mount/unmount cycles', () => {
            fc.assert(
                fc.property(
                    adapterTypeArb,
                    fc.integer({ min: 1, max: 5 }), // Number of cycles
                    propsArb,
                    (adapterType, cycleCount, props) => {
                        const factory = createMockFactory<typeof props>();
                        const adapter = createAdapter(adapterType, factory, props);

                        try {
                            for (let cycle = 0; cycle < cycleCount; cycle++) {
                                // Mount phase
                                expect(adapter.isMounted()).toBe(false);
                                adapter.mount();
                                expect(adapter.isMounted()).toBe(true);

                                // Extra mounts should be idempotent
                                adapter.mount();
                                adapter.mount();
                                expect(adapter.isMounted()).toBe(true);

                                // Unmount phase
                                adapter.unmount();
                                expect(adapter.isMounted()).toBe(false);

                                // Extra unmounts should be idempotent
                                adapter.unmount();
                                adapter.unmount();
                                expect(adapter.isMounted()).toBe(false);
                            }
                        } finally {
                            cleanupContainer(factory);
                        }
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 11.5**
     * 
     * Tests specific to each adapter type to ensure idempotence is implemented correctly.
     */
    describe('Adapter-Specific Mount Idempotence', () => {
        describe('ReactAdapter', () => {
            it('should be idempotent for mount operations', () => {
                fc.assert(
                    fc.property(mountCallCountArb, propsArb, (mountCount, props) => {
                        const factory = createMockFactory<typeof props>();
                        const adapter = new ReactAdapter(factory, props);

                        try {
                            for (let i = 0; i < mountCount; i++) {
                                adapter.mount();
                                expect(adapter.isMounted()).toBe(true);
                            }
                        } finally {
                            adapter.unmount();
                            cleanupContainer(factory);
                        }
                    }),
                    { numRuns: 100 }
                );
            });

            it('should be idempotent for unmount operations', () => {
                fc.assert(
                    fc.property(unmountCallCountArb, propsArb, (unmountCount, props) => {
                        const factory = createMockFactory<typeof props>();
                        const adapter = new ReactAdapter(factory, props);

                        try {
                            adapter.mount();
                            for (let i = 0; i < unmountCount; i++) {
                                adapter.unmount();
                                expect(adapter.isMounted()).toBe(false);
                            }
                        } finally {
                            cleanupContainer(factory);
                        }
                    }),
                    { numRuns: 100 }
                );
            });
        });

        describe('VueAdapter', () => {
            it('should be idempotent for mount operations', () => {
                fc.assert(
                    fc.property(mountCallCountArb, propsArb, (mountCount, props) => {
                        const factory = createMockFactory<typeof props>();
                        const adapter = new VueAdapter(factory, props);

                        try {
                            for (let i = 0; i < mountCount; i++) {
                                adapter.mount();
                                expect(adapter.isMounted()).toBe(true);
                            }
                        } finally {
                            adapter.unmount();
                            cleanupContainer(factory);
                        }
                    }),
                    { numRuns: 100 }
                );
            });

            it('should be idempotent for unmount operations', () => {
                fc.assert(
                    fc.property(unmountCallCountArb, propsArb, (unmountCount, props) => {
                        const factory = createMockFactory<typeof props>();
                        const adapter = new VueAdapter(factory, props);

                        try {
                            adapter.mount();
                            for (let i = 0; i < unmountCount; i++) {
                                adapter.unmount();
                                expect(adapter.isMounted()).toBe(false);
                            }
                        } finally {
                            cleanupContainer(factory);
                        }
                    }),
                    { numRuns: 100 }
                );
            });
        });

        describe('SvelteAdapter', () => {
            it('should be idempotent for mount operations', () => {
                fc.assert(
                    fc.property(mountCallCountArb, propsArb, (mountCount, props) => {
                        const factory = createMockFactory<typeof props>();
                        const adapter = new SvelteAdapter(factory, props);

                        try {
                            for (let i = 0; i < mountCount; i++) {
                                adapter.mount();
                                expect(adapter.isMounted()).toBe(true);
                            }
                        } finally {
                            adapter.unmount();
                            cleanupContainer(factory);
                        }
                    }),
                    { numRuns: 100 }
                );
            });

            it('should be idempotent for unmount operations', () => {
                fc.assert(
                    fc.property(unmountCallCountArb, propsArb, (unmountCount, props) => {
                        const factory = createMockFactory<typeof props>();
                        const adapter = new SvelteAdapter(factory, props);

                        try {
                            adapter.mount();
                            for (let i = 0; i < unmountCount; i++) {
                                adapter.unmount();
                                expect(adapter.isMounted()).toBe(false);
                            }
                        } finally {
                            cleanupContainer(factory);
                        }
                    }),
                    { numRuns: 100 }
                );
            });
        });

        describe('WebComponentAdapter', () => {
            it('should be idempotent for mount operations', () => {
                fc.assert(
                    fc.property(mountCallCountArb, propsArb, (mountCount, props) => {
                        const factory = createMockFactory<typeof props>('test-web-component');
                        const adapter = new WebComponentAdapter(factory, props);

                        try {
                            for (let i = 0; i < mountCount; i++) {
                                adapter.mount();
                                expect(adapter.isMounted()).toBe(true);
                            }
                        } finally {
                            adapter.unmount();
                            cleanupContainer(factory);
                        }
                    }),
                    { numRuns: 100 }
                );
            });

            it('should be idempotent for unmount operations', () => {
                fc.assert(
                    fc.property(unmountCallCountArb, propsArb, (unmountCount, props) => {
                        const factory = createMockFactory<typeof props>('test-web-component');
                        const adapter = new WebComponentAdapter(factory, props);

                        try {
                            adapter.mount();
                            for (let i = 0; i < unmountCount; i++) {
                                adapter.unmount();
                                expect(adapter.isMounted()).toBe(false);
                            }
                        } finally {
                            cleanupContainer(factory);
                        }
                    }),
                    { numRuns: 100 }
                );
            });

            it('should not create duplicate DOM elements on multiple mounts', () => {
                fc.assert(
                    fc.property(mountCallCountArb, propsArb, (mountCount, props) => {
                        const factory = createMockFactory<typeof props>('test-web-component');
                        const adapter = new WebComponentAdapter(factory, props);
                        const container = factory.getContainer();

                        try {
                            // Multiple mounts
                            for (let i = 0; i < mountCount; i++) {
                                adapter.mount();
                            }

                            // Should only have one child element
                            expect(container.children.length).toBe(1);
                        } finally {
                            adapter.unmount();
                            cleanupContainer(factory);
                        }
                    }),
                    { numRuns: 100 }
                );
            });
        });
    });
});

