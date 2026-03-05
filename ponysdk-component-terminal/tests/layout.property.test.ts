/**
 * Property-based tests for Responsive Layout CSS Generation.
 *
 * **Property 12: Responsive Layout CSS Generation**
 * *For any* PResponsiveGrid configuration with breakpoints for mobile (0-599px),
 * tablet (600-1023px), and desktop (1024px+), the generated CSS SHALL contain the
 * correct media queries and column definitions for each breakpoint. For any component
 * with conditional display settings (e.g., hideOnMobile=true), the CSS SHALL include
 * a media query that hides the component at the matching breakpoint.
 *
 * **Validates: Requirements 4.2, 4.3, 4.5**
 *
 * @Tag("Feature: ui-library-wrapper, Property 12: Responsive Layout CSS Generation")
 */

import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';
import { ResponsiveGridRenderer } from '../src/layout/ResponsiveGridRenderer.js';
import type { ResponsiveGridProps, BreakpointConfig } from '../src/layout/ResponsiveGridRenderer.js';

// ============================================================================
// Test Arbitraries (Generators)
// ============================================================================

/**
 * Arbitrary for a CSS gap value.
 */
const gapArb: fc.Arbitrary<string> = fc.oneof(
    fc.integer({ min: 1, max: 100 }).map(n => `${n}px`),
    fc.float({ min: 0.25, max: 5, noNaN: true }).map(n => `${n.toFixed(2)}rem`)
);

/**
 * Arbitrary for a single BreakpointConfig.
 */
const breakpointConfigArb = (minWidth: number): fc.Arbitrary<BreakpointConfig> =>
    fc.record({
        minWidth: fc.constant(minWidth),
        columns: fc.integer({ min: 1, max: 24 }),
        gap: gapArb,
    });

/**
 * Arbitrary for a breakpoints map with 1-5 entries, each with a unique ascending minWidth.
 */
const breakpointsMapArb: fc.Arbitrary<Record<string, BreakpointConfig>> =
    fc.array(
        fc.record({
            name: fc.stringMatching(/^[a-z]{2,10}$/),
            minWidth: fc.integer({ min: 0, max: 2000 }),
            columns: fc.integer({ min: 1, max: 24 }),
            gap: gapArb,
        }),
        { minLength: 1, maxLength: 5 }
    )
        .map(entries => {
            // Deduplicate by name and build the map
            const seen = new Set<string>();
            const result: Record<string, BreakpointConfig> = {};
            for (const e of entries) {
                if (!seen.has(e.name)) {
                    seen.add(e.name);
                    result[e.name] = { minWidth: e.minWidth, columns: e.columns, gap: e.gap };
                }
            }
            return result;
        })
        .filter(m => Object.keys(m).length >= 1);

/**
 * Arbitrary for ResponsiveGridProps.
 */
const responsiveGridPropsArb: fc.Arbitrary<ResponsiveGridProps> = fc.record({
    columns: fc.integer({ min: 1, max: 24 }),
    gap: gapArb,
    breakpoints: breakpointsMapArb,
    hideOnMobile: fc.boolean(),
    hideOnTablet: fc.boolean(),
    hideOnDesktop: fc.boolean(),
});

/**
 * Arbitrary for a grid ID string.
 */
const gridIdArb: fc.Arbitrary<string> = fc.stringMatching(/^[a-z][a-z0-9-]{0,19}$/);

// ============================================================================
// Property Tests
// ============================================================================

describe('Property 12: Responsive Layout CSS Generation', () => {
    const renderer = new ResponsiveGridRenderer();

    /**
     * **Validates: Requirements 4.2, 4.3**
     *
     * For any ResponsiveGridProps, the generated CSS SHALL contain one @media rule
     * per breakpoint, each with the correct min-width, column count, and gap.
     */
    describe('Breakpoint media queries match configuration', () => {
        it('should generate one media query per breakpoint with correct columns and gap', () => {
            fc.assert(
                fc.property(
                    gridIdArb,
                    responsiveGridPropsArb,
                    (gridId, props) => {
                        const css = renderer.generateCSS(gridId, props);
                        const bpEntries = Object.entries(props.breakpoints);

                        for (const [, config] of bpEntries) {
                            // Each breakpoint must produce a media query with its minWidth
                            const mediaQueryPattern = `@media (min-width: ${config.minWidth}px)`;
                            expect(css).toContain(mediaQueryPattern);

                            // The media query block must contain the correct column definition
                            const columnPattern = `grid-template-columns: repeat(${config.columns}, 1fr)`;
                            expect(css).toContain(columnPattern);

                            // The media query block must contain the correct gap
                            const gapPattern = `gap: ${config.gap}`;
                            expect(css).toContain(gapPattern);
                        }
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 4.2**
     *
     * The generated CSS SHALL always contain a base grid rule (no media query)
     * with the default columns and gap from the props.
     */
    describe('Base grid rule is always present', () => {
        it('should contain a base grid rule with default columns and gap', () => {
            fc.assert(
                fc.property(
                    gridIdArb,
                    responsiveGridPropsArb,
                    (gridId, props) => {
                        const css = renderer.generateCSS(gridId, props);

                        expect(css).toContain('display: grid');
                        expect(css).toContain(`grid-template-columns: repeat(${props.columns}, 1fr)`);
                        expect(css).toContain(`gap: ${props.gap}`);
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 4.2, 4.3**
     *
     * The number of @media blocks for breakpoints SHALL equal the number of
     * breakpoint entries in the configuration (plus any conditional display blocks).
     */
    describe('Media query count matches breakpoint count', () => {
        it('should have exactly N breakpoint media queries plus conditional display queries', () => {
            fc.assert(
                fc.property(
                    gridIdArb,
                    responsiveGridPropsArb,
                    (gridId, props) => {
                        const css = renderer.generateCSS(gridId, props);
                        const mediaMatches = css.match(/@media\s*\(/g) || [];
                        const bpCount = Object.keys(props.breakpoints).length;

                        let conditionalCount = 0;
                        if (props.hideOnMobile) conditionalCount++;
                        if (props.hideOnTablet) conditionalCount++;
                        if (props.hideOnDesktop) conditionalCount++;

                        expect(mediaMatches.length).toBe(bpCount + conditionalCount);
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 4.5**
     *
     * When hideOnMobile is true, the CSS SHALL contain a media query with
     * max-width: 599px and display: none.
     */
    describe('Conditional display: hideOnMobile', () => {
        it('should hide on mobile when hideOnMobile is true', () => {
            fc.assert(
                fc.property(
                    gridIdArb,
                    responsiveGridPropsArb.filter(p => p.hideOnMobile),
                    (gridId, props) => {
                        const css = renderer.generateCSS(gridId, props);

                        expect(css).toContain('@media (max-width: 599px)');
                        // The mobile hide block must contain display: none
                        const mobileHideIdx = css.indexOf('@media (max-width: 599px)');
                        const blockEnd = css.indexOf('}', css.indexOf('}', mobileHideIdx) + 1);
                        const mobileBlock = css.substring(mobileHideIdx, blockEnd + 1);
                        expect(mobileBlock).toContain('display: none');
                    }
                ),
                { numRuns: 100 }
            );
        });

        it('should NOT contain mobile hide media query when hideOnMobile is false', () => {
            fc.assert(
                fc.property(
                    gridIdArb,
                    responsiveGridPropsArb.filter(p => !p.hideOnMobile),
                    (gridId, props) => {
                        const css = renderer.generateCSS(gridId, props);
                        // No max-width: 599px block with display: none
                        const mobileIdx = css.indexOf('@media (max-width: 599px)');
                        expect(mobileIdx).toBe(-1);
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 4.5**
     *
     * When hideOnTablet is true, the CSS SHALL contain a media query with
     * min-width: 600px and max-width: 1023px and display: none.
     */
    describe('Conditional display: hideOnTablet', () => {
        it('should hide on tablet when hideOnTablet is true', () => {
            fc.assert(
                fc.property(
                    gridIdArb,
                    responsiveGridPropsArb.filter(p => p.hideOnTablet),
                    (gridId, props) => {
                        const css = renderer.generateCSS(gridId, props);

                        expect(css).toContain('@media (min-width: 600px) and (max-width: 1023px)');
                        const tabletIdx = css.indexOf('@media (min-width: 600px) and (max-width: 1023px)');
                        const blockEnd = css.indexOf('}', css.indexOf('}', tabletIdx) + 1);
                        const tabletBlock = css.substring(tabletIdx, blockEnd + 1);
                        expect(tabletBlock).toContain('display: none');
                    }
                ),
                { numRuns: 100 }
            );
        });

        it('should NOT contain tablet hide media query when hideOnTablet is false', () => {
            fc.assert(
                fc.property(
                    gridIdArb,
                    responsiveGridPropsArb.filter(p => !p.hideOnTablet),
                    (gridId, props) => {
                        const css = renderer.generateCSS(gridId, props);
                        const tabletIdx = css.indexOf('@media (min-width: 600px) and (max-width: 1023px)');
                        expect(tabletIdx).toBe(-1);
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 4.5**
     *
     * When hideOnDesktop is true, the CSS SHALL contain a media query with
     * min-width: 1024px and display: none.
     */
    describe('Conditional display: hideOnDesktop', () => {
        it('should hide on desktop when hideOnDesktop is true', () => {
            fc.assert(
                fc.property(
                    gridIdArb,
                    responsiveGridPropsArb.filter(p => p.hideOnDesktop),
                    (gridId, props) => {
                        const css = renderer.generateCSS(gridId, props);

                        // Must contain a min-width: 1024px block with display: none
                        // But we need to distinguish breakpoint queries from the hide query.
                        // The hide query block contains "display: none".
                        const desktopHidePattern = '@media (min-width: 1024px)';
                        const allOccurrences = findAllOccurrences(css, desktopHidePattern);
                        const hasHideBlock = allOccurrences.some(idx => {
                            const blockEnd = css.indexOf('}', css.indexOf('}', idx) + 1);
                            const block = css.substring(idx, blockEnd + 1);
                            return block.includes('display: none');
                        });
                        expect(hasHideBlock).toBe(true);
                    }
                ),
                { numRuns: 100 }
            );
        });

        it('should NOT contain desktop hide media query when hideOnDesktop is false', () => {
            fc.assert(
                fc.property(
                    gridIdArb,
                    responsiveGridPropsArb.filter(p => !p.hideOnDesktop),
                    (gridId, props) => {
                        const css = renderer.generateCSS(gridId, props);
                        // No min-width: 1024px block should contain display: none
                        const pattern = '@media (min-width: 1024px)';
                        const allOccurrences = findAllOccurrences(css, pattern);
                        for (const idx of allOccurrences) {
                            const blockEnd = css.indexOf('}', css.indexOf('}', idx) + 1);
                            const block = css.substring(idx, blockEnd + 1);
                            expect(block).not.toContain('display: none');
                        }
                    }
                ),
                { numRuns: 100 }
            );
        });
    });

    /**
     * **Validates: Requirements 4.2**
     *
     * The generated CSS SHALL always reference the correct grid selector
     * using the provided gridId.
     */
    describe('Grid selector uses correct gridId', () => {
        it('should use data-grid-id selector with the provided gridId', () => {
            fc.assert(
                fc.property(
                    gridIdArb,
                    responsiveGridPropsArb,
                    (gridId, props) => {
                        const css = renderer.generateCSS(gridId, props);
                        expect(css).toContain(`[data-grid-id="${gridId}"]`);
                    }
                ),
                { numRuns: 100 }
            );
        });
    });
});

// ============================================================================
// Helpers
// ============================================================================

/**
 * Find all occurrences of a substring in a string, returning their start indices.
 */
function findAllOccurrences(str: string, sub: string): number[] {
    const indices: number[] = [];
    let idx = str.indexOf(sub);
    while (idx !== -1) {
        indices.push(idx);
        idx = str.indexOf(sub, idx + 1);
    }
    return indices;
}
