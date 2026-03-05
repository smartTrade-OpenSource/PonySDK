/**
 * Generates CSS grid layout styles with media queries for responsive breakpoints.
 *
 * Requirements: 4.2 - 12-column grid with configurable breakpoints
 * Requirements: 4.3 - Reorganize layout when crossing a breakpoint
 * Requirements: 4.5 - Conditional display based on active breakpoint
 * Requirements: 4.6 - hideOnMobile/hideOnTablet/hideOnDesktop support
 */
/**
 * Breakpoint configuration matching the server-side BreakpointConfig record.
 */
export interface BreakpointConfig {
    /** Minimum screen width in pixels for this breakpoint */
    minWidth: number;
    /** Number of grid columns at this breakpoint */
    columns: number;
    /** CSS gap value (e.g. "1rem", "16px") */
    gap: string;
}
/**
 * Props for the responsive grid, matching the server-side ResponsiveGridProps.
 */
export interface ResponsiveGridProps {
    columns: number;
    gap: string;
    breakpoints: Record<string, BreakpointConfig>;
    hideOnMobile: boolean;
    hideOnTablet: boolean;
    hideOnDesktop: boolean;
}
/**
 * Generates CSS for a responsive grid layout with media queries per breakpoint,
 * and conditional display rules for hiding components at specific breakpoints.
 */
export declare class ResponsiveGridRenderer {
    /**
     * Generate a complete CSS string for a responsive grid identified by `gridId`.
     *
     * The output includes:
     * - A base grid rule using the default columns/gap
     * - A `@media` rule for each breakpoint overriding columns/gap
     * - Conditional display `@media` rules for hideOnMobile/hideOnTablet/hideOnDesktop
     */
    generateCSS(gridId: string, props: ResponsiveGridProps): string;
    /**
     * Generate a single `@media` block for a breakpoint configuration.
     */
    private generateBreakpointMediaQuery;
    /**
     * Generate media queries that hide the grid at matching breakpoints.
     */
    private generateConditionalDisplayCSS;
}
//# sourceMappingURL=ResponsiveGridRenderer.d.ts.map