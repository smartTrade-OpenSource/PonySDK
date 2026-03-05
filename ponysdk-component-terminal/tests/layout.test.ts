/**
 * Unit tests for BreakpointListener and ResponsiveGridRenderer.
 *
 * Requirements: 4.2 - 12-column grid with configurable breakpoints
 * Requirements: 4.3 - Reorganize layout when crossing a breakpoint
 * Requirements: 4.5 - Conditional display based on active breakpoint
 * Requirements: 4.6 - hideOnMobile/hideOnTablet/hideOnDesktop support
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { BreakpointListener } from '../src/layout/BreakpointListener.js';
import { ResponsiveGridRenderer } from '../src/layout/ResponsiveGridRenderer.js';
import type { ResponsiveGridProps } from '../src/layout/ResponsiveGridRenderer.js';
import type { EventBridge } from '../src/EventBridge.js';

// ============================================================================
// BreakpointListener Tests
// ============================================================================

describe('BreakpointListener', () => {
    let mockEventBridge: EventBridge;
    let changeHandlers: Map<string, (e: MediaQueryListEvent) => void>;
    let matchStates: Map<string, boolean>;

    beforeEach(() => {
        mockEventBridge = {
            dispatch: vi.fn(),
        } as unknown as EventBridge;

        changeHandlers = new Map();
        matchStates = new Map([
            ['(max-width: 599px)', false],
            ['(min-width: 600px) and (max-width: 1023px)', false],
            ['(min-width: 1024px)', true],
        ]);

        // Mock window.matchMedia
        vi.stubGlobal('matchMedia', (query: string) => ({
            matches: matchStates.get(query) ?? false,
            media: query,
            addEventListener: (_event: string, handler: (e: MediaQueryListEvent) => void) => {
                changeHandlers.set(query, handler);
            },
            removeEventListener: vi.fn(),
        }));
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('should detect desktop as initial breakpoint when desktop query matches', () => {
        const listener = new BreakpointListener(mockEventBridge, 42);
        expect(listener.getCurrentBreakpoint()).toBe('desktop');
        listener.destroy();
    });

    it('should detect mobile as initial breakpoint when mobile query matches', () => {
        matchStates.set('(max-width: 599px)', true);
        matchStates.set('(min-width: 1024px)', false);

        const listener = new BreakpointListener(mockEventBridge, 42);
        expect(listener.getCurrentBreakpoint()).toBe('mobile');
        listener.destroy();
    });

    it('should detect tablet as initial breakpoint when tablet query matches', () => {
        matchStates.set('(min-width: 600px) and (max-width: 1023px)', true);
        matchStates.set('(min-width: 1024px)', false);

        const listener = new BreakpointListener(mockEventBridge, 42);
        expect(listener.getCurrentBreakpoint()).toBe('tablet');
        listener.destroy();
    });

    it('should dispatch breakpoint-change event when breakpoint changes', () => {
        const listener = new BreakpointListener(mockEventBridge, 42);
        expect(listener.getCurrentBreakpoint()).toBe('desktop');

        // Simulate mobile breakpoint match
        const mobileHandler = changeHandlers.get('(max-width: 599px)')!;
        mobileHandler({ matches: true } as MediaQueryListEvent);

        expect(mockEventBridge.dispatch).toHaveBeenCalledWith(42, 'breakpoint-change', {
            breakpoint: 'mobile',
        });
        expect(listener.getCurrentBreakpoint()).toBe('mobile');
        listener.destroy();
    });

    it('should not dispatch when breakpoint does not actually change', () => {
        const listener = new BreakpointListener(mockEventBridge, 42);

        // Desktop handler fires but we're already on desktop
        const desktopHandler = changeHandlers.get('(min-width: 1024px)')!;
        desktopHandler({ matches: true } as MediaQueryListEvent);

        expect(mockEventBridge.dispatch).not.toHaveBeenCalled();
        listener.destroy();
    });

    it('should not dispatch when media query event has matches=false', () => {
        const listener = new BreakpointListener(mockEventBridge, 42);

        const mobileHandler = changeHandlers.get('(max-width: 599px)')!;
        mobileHandler({ matches: false } as MediaQueryListEvent);

        expect(mockEventBridge.dispatch).not.toHaveBeenCalled();
        listener.destroy();
    });

    it('should transition through multiple breakpoints', () => {
        const listener = new BreakpointListener(mockEventBridge, 42);

        // desktop -> tablet
        const tabletHandler = changeHandlers.get('(min-width: 600px) and (max-width: 1023px)')!;
        tabletHandler({ matches: true } as MediaQueryListEvent);
        expect(listener.getCurrentBreakpoint()).toBe('tablet');

        // tablet -> mobile
        const mobileHandler = changeHandlers.get('(max-width: 599px)')!;
        mobileHandler({ matches: true } as MediaQueryListEvent);
        expect(listener.getCurrentBreakpoint()).toBe('mobile');

        expect(mockEventBridge.dispatch).toHaveBeenCalledTimes(2);
        listener.destroy();
    });
});

// ============================================================================
// ResponsiveGridRenderer Tests
// ============================================================================

describe('ResponsiveGridRenderer', () => {
    let renderer: ResponsiveGridRenderer;

    beforeEach(() => {
        renderer = new ResponsiveGridRenderer();
    });

    function defaultProps(overrides: Partial<ResponsiveGridProps> = {}): ResponsiveGridProps {
        return {
            columns: 12,
            gap: '1rem',
            breakpoints: {
                mobile: { minWidth: 0, columns: 1, gap: '0.5rem' },
                tablet: { minWidth: 600, columns: 6, gap: '0.75rem' },
                desktop: { minWidth: 1024, columns: 12, gap: '1rem' },
            },
            hideOnMobile: false,
            hideOnTablet: false,
            hideOnDesktop: false,
            ...overrides,
        };
    }

    it('should generate base grid CSS with correct columns and gap', () => {
        const css = renderer.generateCSS('grid-1', defaultProps());
        expect(css).toContain('[data-grid-id="grid-1"]');
        expect(css).toContain('display: grid');
        expect(css).toContain('grid-template-columns: repeat(12, 1fr)');
        expect(css).toContain('gap: 1rem');
    });

    it('should generate media queries for each breakpoint', () => {
        const css = renderer.generateCSS('grid-1', defaultProps());
        expect(css).toContain('@media (min-width: 0px)');
        expect(css).toContain('grid-template-columns: repeat(1, 1fr)');
        expect(css).toContain('@media (min-width: 600px)');
        expect(css).toContain('grid-template-columns: repeat(6, 1fr)');
        expect(css).toContain('@media (min-width: 1024px)');
        expect(css).toContain('grid-template-columns: repeat(12, 1fr)');
    });

    it('should sort breakpoint media queries by minWidth ascending', () => {
        const css = renderer.generateCSS('grid-1', defaultProps());
        const mobileIdx = css.indexOf('@media (min-width: 0px)');
        const tabletIdx = css.indexOf('@media (min-width: 600px)');
        const desktopIdx = css.indexOf('@media (min-width: 1024px)');
        expect(mobileIdx).toBeLessThan(tabletIdx);
        expect(tabletIdx).toBeLessThan(desktopIdx);
    });

    it('should generate hideOnMobile media query', () => {
        const css = renderer.generateCSS('grid-1', defaultProps({ hideOnMobile: true }));
        expect(css).toContain('@media (max-width: 599px)');
        expect(css).toContain('display: none');
    });

    it('should generate hideOnTablet media query', () => {
        const css = renderer.generateCSS('grid-1', defaultProps({ hideOnTablet: true }));
        expect(css).toContain('@media (min-width: 600px) and (max-width: 1023px)');
        // Find the conditional display section (not the breakpoint section)
        const conditionalIdx = css.lastIndexOf('@media (min-width: 600px) and (max-width: 1023px)');
        const afterConditional = css.substring(conditionalIdx);
        expect(afterConditional).toContain('display: none');
    });

    it('should generate hideOnDesktop media query', () => {
        const css = renderer.generateCSS('grid-1', defaultProps({ hideOnDesktop: true }));
        // The last @media (min-width: 1024px) block should contain display: none
        const allDesktopMatches = [...css.matchAll(/@media \(min-width: 1024px\)/g)];
        expect(allDesktopMatches.length).toBeGreaterThanOrEqual(2); // breakpoint + conditional
        const lastIdx = css.lastIndexOf('@media (min-width: 1024px)');
        const afterLast = css.substring(lastIdx);
        expect(afterLast).toContain('display: none');
    });

    it('should not generate conditional display rules when all hide flags are false', () => {
        const css = renderer.generateCSS('grid-1', defaultProps());
        expect(css).not.toContain('display: none');
    });

    it('should generate all three conditional display rules when all hide flags are true', () => {
        const css = renderer.generateCSS('grid-1', defaultProps({
            hideOnMobile: true,
            hideOnTablet: true,
            hideOnDesktop: true,
        }));
        const noneCount = (css.match(/display: none/g) || []).length;
        expect(noneCount).toBe(3);
    });

    it('should handle empty breakpoints map', () => {
        const css = renderer.generateCSS('grid-1', defaultProps({ breakpoints: {} }));
        expect(css).toContain('display: grid');
        expect(css).toContain('grid-template-columns: repeat(12, 1fr)');
        // No breakpoint media queries, only base rule
        expect(css).not.toContain('@media (min-width:');
    });

    it('should handle custom column counts', () => {
        const css = renderer.generateCSS('grid-1', defaultProps({ columns: 6 }));
        // Base rule should use 6 columns
        const baseBlock = css.substring(0, css.indexOf('@media'));
        expect(baseBlock).toContain('grid-template-columns: repeat(6, 1fr)');
    });
});
