/**
 * Client-side DataTable renderer with sorting, pagination, row selection,
 * and virtual scrolling support.
 *
 * Requirements: 8.2 - Apply changes via JSON Patch for modified rows only
 * Requirements: 8.3 - Sort click dispatches sort event (column + direction) to server
 * Requirements: 8.4 - Page change dispatches page change event to server
 * Requirements: 8.5 - Row selection dispatches selected row IDs to server
 * Requirements: 8.6 - Virtual scrolling for large datasets
 */
import type { EventBridge } from '../EventBridge.js';
/** Column definition matching the server-side ColumnDef record. */
export interface ColumnDef {
    field: string;
    header: string;
    type: string;
    sortable: boolean;
    filterable: boolean;
    width?: number;
}
/** Props for the DataTable, matching the server-side DataTableProps record. */
export interface DataTableProps {
    columns: ColumnDef[];
    data: Record<string, unknown>[];
    page: number;
    pageSize: number;
    totalRows: number;
    sortField: string;
    sortDirection: string;
    selectedRows: string[];
    virtualScroll: boolean;
}
/**
 * Renders a data table with column headers (sort indicators), pagination controls,
 * row selection, and optional virtual scrolling for large datasets.
 */
export declare class DataTableRenderer {
    private container;
    private eventBridge;
    private objectId;
    private props;
    private tableElement;
    private theadElement;
    private tbodyElement;
    private paginationElement;
    private scrollContainer;
    private virtualScroller;
    private selectedRows;
    constructor(container: HTMLElement, eventBridge: EventBridge, objectId: number, props: DataTableProps);
    /**
     * Build and mount the full table into the container.
     */
    render(): void;
    /**
     * Update props and re-render affected parts.
     */
    update(props: DataTableProps): void;
    /**
     * Clean up listeners and virtual scroller.
     */
    destroy(): void;
    private renderHeader;
    private createSortIndicator;
    private renderBody;
    private renderRowRange;
    private createRow;
    private getRowId;
    private useVirtualScroll;
    private setupVirtualScroll;
    private renderPagination;
    private handleSortClick;
    private handlePageChange;
    private handleRowClick;
    private updateRowSelection;
    /**
     * Cycle sort direction: none → asc → desc → none
     */
    private getNextSortDirection;
}
//# sourceMappingURL=DataTableRenderer.d.ts.map