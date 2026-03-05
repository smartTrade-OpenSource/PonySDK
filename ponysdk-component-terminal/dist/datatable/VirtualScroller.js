/**
 * Virtual scroller for large datasets.
 *
 * Calculates visible row range based on container scroll position and row height,
 * renders only visible rows plus a small buffer, and uses a spacer element
 * to maintain correct scroll height.
 *
 * Requirements: 8.6 - THE PDataTable SHALL support virtual scrolling for datasets > 1000 rows
 */
/**
 * Manages virtual scrolling for a table body container.
 *
 * Only renders rows within the visible viewport plus a configurable buffer,
 * using a spacer element to preserve the total scrollable height.
 */
export class VirtualScroller {
    /**
     * @param container - Scrollable container element
     * @param rowHeight - Fixed height per row in pixels
     * @param totalRows - Total number of rows in the dataset
     * @param bufferRows - Number of extra rows to render above/below the viewport
     */
    constructor(container, rowHeight, totalRows, bufferRows = 5) {
        this.scrollHandler = null;
        this.onRangeChange = null;
        this.container = container;
        this.rowHeight = rowHeight;
        this.totalRows = totalRows;
        this.bufferRows = bufferRows;
        this.spacerTop = document.createElement('div');
        this.spacerTop.style.width = '100%';
        this.spacerBottom = document.createElement('div');
        this.spacerBottom.style.width = '100%';
    }
    /**
     * Register a callback invoked whenever the visible range changes.
     */
    onVisibleRangeChange(callback) {
        this.onRangeChange = callback;
    }
    /**
     * Attach scroll listener and insert spacer elements into the container.
     */
    attach() {
        this.scrollHandler = () => this.handleScroll();
        this.container.addEventListener('scroll', this.scrollHandler);
        // Initial calculation
        this.handleScroll();
    }
    /**
     * Detach scroll listener.
     */
    detach() {
        if (this.scrollHandler) {
            this.container.removeEventListener('scroll', this.scrollHandler);
            this.scrollHandler = null;
        }
    }
    /**
     * Update the total row count (e.g. after data changes) and recalculate.
     */
    setTotalRows(totalRows) {
        this.totalRows = totalRows;
        this.handleScroll();
    }
    /**
     * Calculate the currently visible range based on scroll position.
     */
    calculateVisibleRange() {
        const scrollTop = this.container.scrollTop;
        const viewportHeight = this.container.clientHeight;
        const firstVisible = Math.floor(scrollTop / this.rowHeight);
        const visibleCount = Math.ceil(viewportHeight / this.rowHeight);
        const start = Math.max(0, firstVisible - this.bufferRows);
        const end = Math.min(this.totalRows, firstVisible + visibleCount + this.bufferRows);
        return {
            start,
            end,
            offsetTop: start * this.rowHeight,
        };
    }
    /**
     * Get the total scrollable height for all rows.
     */
    getTotalHeight() {
        return this.totalRows * this.rowHeight;
    }
    /**
     * Get the top spacer element (place before visible rows).
     */
    getSpacerTop() {
        return this.spacerTop;
    }
    /**
     * Get the bottom spacer element (place after visible rows).
     */
    getSpacerBottom() {
        return this.spacerBottom;
    }
    /**
     * Update spacer heights based on the current visible range.
     */
    updateSpacers(range) {
        this.spacerTop.style.height = `${range.offsetTop}px`;
        const bottomHeight = (this.totalRows - range.end) * this.rowHeight;
        this.spacerBottom.style.height = `${Math.max(0, bottomHeight)}px`;
    }
    handleScroll() {
        const range = this.calculateVisibleRange();
        this.updateSpacers(range);
        if (this.onRangeChange) {
            this.onRangeChange(range);
        }
    }
}
//# sourceMappingURL=VirtualScroller.js.map