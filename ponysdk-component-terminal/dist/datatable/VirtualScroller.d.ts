/**
 * Virtual scroller for large datasets.
 *
 * Calculates visible row range based on container scroll position and row height,
 * renders only visible rows plus a small buffer, and uses a spacer element
 * to maintain correct scroll height.
 *
 * Requirements: 8.6 - THE PDataTable SHALL support virtual scrolling for datasets > 1000 rows
 */
/** Visible range returned by the scroller */
export interface VisibleRange {
    /** Index of the first visible row (inclusive) */
    start: number;
    /** Index of the last visible row (exclusive) */
    end: number;
    /** Top offset in pixels for the first visible row */
    offsetTop: number;
}
/**
 * Manages virtual scrolling for a table body container.
 *
 * Only renders rows within the visible viewport plus a configurable buffer,
 * using a spacer element to preserve the total scrollable height.
 */
export declare class VirtualScroller {
    private container;
    private rowHeight;
    private totalRows;
    private bufferRows;
    private spacerTop;
    private spacerBottom;
    private scrollHandler;
    private onRangeChange;
    /**
     * @param container - Scrollable container element
     * @param rowHeight - Fixed height per row in pixels
     * @param totalRows - Total number of rows in the dataset
     * @param bufferRows - Number of extra rows to render above/below the viewport
     */
    constructor(container: HTMLElement, rowHeight: number, totalRows: number, bufferRows?: number);
    /**
     * Register a callback invoked whenever the visible range changes.
     */
    onVisibleRangeChange(callback: (range: VisibleRange) => void): void;
    /**
     * Attach scroll listener and insert spacer elements into the container.
     */
    attach(): void;
    /**
     * Detach scroll listener.
     */
    detach(): void;
    /**
     * Update the total row count (e.g. after data changes) and recalculate.
     */
    setTotalRows(totalRows: number): void;
    /**
     * Calculate the currently visible range based on scroll position.
     */
    calculateVisibleRange(): VisibleRange;
    /**
     * Get the total scrollable height for all rows.
     */
    getTotalHeight(): number;
    /**
     * Get the top spacer element (place before visible rows).
     */
    getSpacerTop(): HTMLElement;
    /**
     * Get the bottom spacer element (place after visible rows).
     */
    getSpacerBottom(): HTMLElement;
    /**
     * Update spacer heights based on the current visible range.
     */
    updateSpacers(range: VisibleRange): void;
    private handleScroll;
}
//# sourceMappingURL=VirtualScroller.d.ts.map