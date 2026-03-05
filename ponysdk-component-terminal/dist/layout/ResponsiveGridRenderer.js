/**
 * Generates CSS grid layout styles with media queries for responsive breakpoints.
 *
 * Requirements: 4.2 - 12-column grid with configurable breakpoints
 * Requirements: 4.3 - Reorganize layout when crossing a breakpoint
 * Requirements: 4.5 - Conditional display based on active breakpoint
 * Requirements: 4.6 - hideOnMobile/hideOnTablet/hideOnDesktop support
 */
/**
 * Generates CSS for a responsive grid layout with media queries per breakpoint,
 * and conditional display rules for hiding components at specific breakpoints.
 */
export class ResponsiveGridRenderer {
    /**
     * Generate a complete CSS string for a responsive grid identified by `gridId`.
     *
     * The output includes:
     * - A base grid rule using the default columns/gap
     * - A `@media` rule for each breakpoint overriding columns/gap
     * - Conditional display `@media` rules for hideOnMobile/hideOnTablet/hideOnDesktop
     */
    generateCSS(gridId, props) {
        const selector = `[data-grid-id="${gridId}"]`;
        const parts = [];
        // Base grid styles (no media query)
        parts.push(`${selector} {\n` +
            `  display: grid;\n` +
            `  grid-template-columns: repeat(${props.columns}, 1fr);\n` +
            `  gap: ${props.gap};\n` +
            `}`);
        // Media queries for each breakpoint, sorted by minWidth ascending
        const sorted = Object.entries(props.breakpoints)
            .sort(([, a], [, b]) => a.minWidth - b.minWidth);
        for (const [, config] of sorted) {
            parts.push(this.generateBreakpointMediaQuery(selector, config));
        }
        // Conditional display media queries
        parts.push(...this.generateConditionalDisplayCSS(selector, props));
        return parts.join('\n\n');
    }
    /**
     * Generate a single `@media` block for a breakpoint configuration.
     */
    generateBreakpointMediaQuery(selector, config) {
        return (`@media (min-width: ${config.minWidth}px) {\n` +
            `  ${selector} {\n` +
            `    grid-template-columns: repeat(${config.columns}, 1fr);\n` +
            `    gap: ${config.gap};\n` +
            `  }\n` +
            `}`);
    }
    /**
     * Generate media queries that hide the grid at matching breakpoints.
     */
    generateConditionalDisplayCSS(selector, props) {
        const rules = [];
        if (props.hideOnMobile) {
            rules.push(`@media (max-width: 599px) {\n` +
                `  ${selector} {\n` +
                `    display: none;\n` +
                `  }\n` +
                `}`);
        }
        if (props.hideOnTablet) {
            rules.push(`@media (min-width: 600px) and (max-width: 1023px) {\n` +
                `  ${selector} {\n` +
                `    display: none;\n` +
                `  }\n` +
                `}`);
        }
        if (props.hideOnDesktop) {
            rules.push(`@media (min-width: 1024px) {\n` +
                `  ${selector} {\n` +
                `    display: none;\n` +
                `  }\n` +
                `}`);
        }
        return rules;
    }
}
//# sourceMappingURL=ResponsiveGridRenderer.js.map