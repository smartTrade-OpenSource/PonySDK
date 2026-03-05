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
import { VirtualScroller, type VisibleRange } from './VirtualScroller.js';

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

/** Sort direction cycle: none → asc → desc → none */
type SortDirection = '' | 'asc' | 'desc';

/** Threshold above which virtual scrolling is activated */
const VIRTUAL_SCROLL_THRESHOLD = 100;
/** Default row height in pixels for virtual scrolling */
const DEFAULT_ROW_HEIGHT = 40;

/**
 * Renders a data table with column headers (sort indicators), pagination controls,
 * row selection, and optional virtual scrolling for large datasets.
 */
export class DataTableRenderer {
    private container: HTMLElement;
    private eventBridge: EventBridge;
    private objectId: number;
    private props: DataTableProps;

    private tableElement: HTMLTableElement | null = null;
    private theadElement: HTMLTableSectionElement | null = null;
    private tbodyElement: HTMLTableSectionElement | null = null;
    private paginationElement: HTMLElement | null = null;
    private scrollContainer: HTMLElement | null = null;
    private virtualScroller: VirtualScroller | null = null;
    private selectedRows: Set<string>;

    constructor(container: HTMLElement, eventBridge: EventBridge, objectId: number, props: DataTableProps) {
        this.container = container;
        this.eventBridge = eventBridge;
        this.objectId = objectId;
        this.props = props;
        this.selectedRows = new Set(props.selectedRows ?? []);
    }

    /**
     * Build and mount the full table into the container.
     */
    render(): void {
        this.container.innerHTML = '';

        this.tableElement = document.createElement('table');
        this.tableElement.setAttribute('role', 'grid');

        this.renderHeader();
        this.renderBody();

        if (this.useVirtualScroll()) {
            this.setupVirtualScroll();
        } else {
            this.container.appendChild(this.tableElement);
        }

        this.renderPagination();
    }

    /**
     * Update props and re-render affected parts.
     */
    update(props: DataTableProps): void {
        this.props = props;
        this.selectedRows = new Set(props.selectedRows ?? []);

        if (this.virtualScroller) {
            this.virtualScroller.setTotalRows(props.data.length);
        }

        this.render();
    }

    /**
     * Clean up listeners and virtual scroller.
     */
    destroy(): void {
        if (this.virtualScroller) {
            this.virtualScroller.detach();
            this.virtualScroller = null;
        }
        this.container.innerHTML = '';
    }

    // ========== Header ==========

    private renderHeader(): void {
        this.theadElement = document.createElement('thead');
        const headerRow = document.createElement('tr');

        for (const col of this.props.columns) {
            const th = document.createElement('th');
            th.setAttribute('data-field', col.field);
            th.textContent = col.header;

            if (col.sortable) {
                th.style.cursor = 'pointer';
                th.appendChild(this.createSortIndicator(col.field));
                th.addEventListener('click', () => this.handleSortClick(col.field));
            }

            if (col.width !== undefined) {
                th.style.width = `${col.width}px`;
            }

            headerRow.appendChild(th);
        }

        this.theadElement.appendChild(headerRow);
        this.tableElement!.appendChild(this.theadElement);
    }

    private createSortIndicator(field: string): HTMLSpanElement {
        const span = document.createElement('span');
        span.classList.add('sort-indicator');

        if (this.props.sortField === field) {
            span.textContent = this.props.sortDirection === 'asc' ? ' ▲' : ' ▼';
        } else {
            span.textContent = '';
        }

        return span;
    }

    // ========== Body ==========

    private renderBody(): void {
        this.tbodyElement = document.createElement('tbody');

        if (this.useVirtualScroll()) {
            // Virtual scroll: rows rendered via scroller callback
            const range = this.virtualScroller
                ? this.virtualScroller.calculateVisibleRange()
                : { start: 0, end: Math.min(this.props.data.length, VIRTUAL_SCROLL_THRESHOLD), offsetTop: 0 };
            this.renderRowRange(range);
        } else {
            for (let i = 0; i < this.props.data.length; i++) {
                this.tbodyElement.appendChild(this.createRow(this.props.data[i], i));
            }
        }

        this.tableElement!.appendChild(this.tbodyElement);
    }

    private renderRowRange(range: VisibleRange): void {
        if (!this.tbodyElement) return;
        this.tbodyElement.innerHTML = '';

        for (let i = range.start; i < range.end && i < this.props.data.length; i++) {
            this.tbodyElement.appendChild(this.createRow(this.props.data[i], i));
        }
    }

    private createRow(rowData: Record<string, unknown>, index: number): HTMLTableRowElement {
        const tr = document.createElement('tr');
        const rowId = this.getRowId(rowData, index);
        tr.setAttribute('data-row-id', rowId);

        if (this.selectedRows.has(rowId)) {
            tr.classList.add('selected');
        }

        tr.addEventListener('click', () => this.handleRowClick(rowId));

        for (const col of this.props.columns) {
            const td = document.createElement('td');
            const value = rowData[col.field];
            td.textContent = value != null ? String(value) : '';
            tr.appendChild(td);
        }

        return tr;
    }

    private getRowId(rowData: Record<string, unknown>, index: number): string {
        // Use 'id' field if present, otherwise fall back to row index
        if (rowData['id'] != null) {
            return String(rowData['id']);
        }
        return String(index);
    }

    // ========== Virtual Scroll ==========

    private useVirtualScroll(): boolean {
        return this.props.virtualScroll && this.props.data.length > VIRTUAL_SCROLL_THRESHOLD;
    }

    private setupVirtualScroll(): void {
        this.scrollContainer = document.createElement('div');
        this.scrollContainer.style.overflow = 'auto';
        this.scrollContainer.style.position = 'relative';
        // Default max height; can be overridden via CSS
        this.scrollContainer.style.maxHeight = '400px';

        this.virtualScroller = new VirtualScroller(
            this.scrollContainer,
            DEFAULT_ROW_HEIGHT,
            this.props.data.length,
        );

        this.virtualScroller.onVisibleRangeChange((range) => {
            this.renderRowRange(range);
            this.virtualScroller!.updateSpacers(range);
        });

        // Build DOM: spacerTop → table → spacerBottom inside scroll container
        this.scrollContainer.appendChild(this.virtualScroller.getSpacerTop());
        this.scrollContainer.appendChild(this.tableElement!);
        this.scrollContainer.appendChild(this.virtualScroller.getSpacerBottom());

        this.container.appendChild(this.scrollContainer);
        this.virtualScroller.attach();
    }

    // ========== Pagination ==========

    private renderPagination(): void {
        if (this.paginationElement) {
            this.paginationElement.remove();
        }

        this.paginationElement = document.createElement('div');
        this.paginationElement.classList.add('datatable-pagination');

        const totalPages = Math.max(1, Math.ceil(this.props.totalRows / this.props.pageSize));
        const currentPage = this.props.page;

        // Previous button
        const prevBtn = document.createElement('button');
        prevBtn.textContent = 'Previous';
        prevBtn.disabled = currentPage <= 0;
        prevBtn.addEventListener('click', () => this.handlePageChange(currentPage - 1));

        // Page info
        const pageInfo = document.createElement('span');
        pageInfo.classList.add('page-info');
        pageInfo.textContent = `Page ${currentPage + 1} of ${totalPages}`;

        // Next button
        const nextBtn = document.createElement('button');
        nextBtn.textContent = 'Next';
        nextBtn.disabled = currentPage >= totalPages - 1;
        nextBtn.addEventListener('click', () => this.handlePageChange(currentPage + 1));

        this.paginationElement.appendChild(prevBtn);
        this.paginationElement.appendChild(pageInfo);
        this.paginationElement.appendChild(nextBtn);

        this.container.appendChild(this.paginationElement);
    }

    // ========== Event Handlers ==========

    private handleSortClick(field: string): void {
        const nextDirection = this.getNextSortDirection(field);
        this.eventBridge.dispatch(this.objectId, 'wa-sort', {
            field,
            direction: nextDirection,
        });
    }

    private handlePageChange(page: number): void {
        this.eventBridge.dispatch(this.objectId, 'wa-page-change', { page });
    }

    private handleRowClick(rowId: string): void {
        if (this.selectedRows.has(rowId)) {
            this.selectedRows.delete(rowId);
        } else {
            this.selectedRows.add(rowId);
        }

        this.eventBridge.dispatch(this.objectId, 'wa-selection-change', {
            selectedRows: Array.from(this.selectedRows),
        });

        // Update visual selection
        this.updateRowSelection(rowId);
    }

    private updateRowSelection(rowId: string): void {
        if (!this.tbodyElement) return;
        const row = this.tbodyElement.querySelector(`tr[data-row-id="${rowId}"]`);
        if (row) {
            row.classList.toggle('selected', this.selectedRows.has(rowId));
        }
    }

    /**
     * Cycle sort direction: none → asc → desc → none
     */
    private getNextSortDirection(field: string): SortDirection {
        if (this.props.sortField !== field) return 'asc';
        switch (this.props.sortDirection) {
            case 'asc': return 'desc';
            case 'desc': return '';
            default: return 'asc';
        }
    }
}
