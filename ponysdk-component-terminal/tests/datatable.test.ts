/**
 * Unit tests for DataTableRenderer and VirtualScroller.
 *
 * Requirements: 8.2 - Apply changes via JSON Patch for modified rows only
 * Requirements: 8.3 - Sort click dispatches sort event (column + direction) to server
 * Requirements: 8.4 - Page change dispatches page change event to server
 * Requirements: 8.5 - Row selection dispatches selected row IDs to server
 * Requirements: 8.6 - Virtual scrolling for large datasets
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { DataTableRenderer, type DataTableProps, type ColumnDef } from '../src/datatable/DataTableRenderer.js';
import { VirtualScroller } from '../src/datatable/VirtualScroller.js';
import type { EventBridge } from '../src/EventBridge.js';

// ============================================================================
// Test Helpers
// ============================================================================

function createMockEventBridge(): EventBridge {
    return {
        dispatch: vi.fn(),
        pendingCount: 0,
        flushNow: vi.fn(),
    } as unknown as EventBridge;
}

function defaultColumns(): ColumnDef[] {
    return [
        { field: 'id', header: 'ID', type: 'number', sortable: true, filterable: false },
        { field: 'name', header: 'Name', type: 'string', sortable: true, filterable: true },
        { field: 'email', header: 'Email', type: 'string', sortable: false, filterable: false },
    ];
}

function defaultData(count = 5): Record<string, unknown>[] {
    return Array.from({ length: count }, (_, i) => ({
        id: `row-${i}`,
        name: `User ${i}`,
        email: `user${i}@example.com`,
    }));
}

function defaultProps(overrides: Partial<DataTableProps> = {}): DataTableProps {
    return {
        columns: defaultColumns(),
        data: defaultData(),
        page: 0,
        pageSize: 10,
        totalRows: 5,
        sortField: '',
        sortDirection: '',
        selectedRows: [],
        virtualScroll: false,
        ...overrides,
    };
}

// ============================================================================
// VirtualScroller
// ============================================================================

describe('VirtualScroller', () => {
    let container: HTMLElement;

    beforeEach(() => {
        container = document.createElement('div');
        // Simulate a scrollable container
        Object.defineProperty(container, 'clientHeight', { value: 200, configurable: true });
        Object.defineProperty(container, 'scrollTop', { value: 0, writable: true, configurable: true });
        document.body.appendChild(container);
    });

    afterEach(() => {
        container.remove();
    });

    it('should calculate visible range from scroll position', () => {
        const scroller = new VirtualScroller(container, 40, 1000, 5);
        const range = scroller.calculateVisibleRange();

        // scrollTop=0, clientHeight=200, rowHeight=40 → 5 visible rows
        // buffer=5 → start=0, end=10
        expect(range.start).toBe(0);
        expect(range.end).toBe(10);
        expect(range.offsetTop).toBe(0);
    });

    it('should account for scroll position', () => {
        Object.defineProperty(container, 'scrollTop', { value: 400, configurable: true });
        const scroller = new VirtualScroller(container, 40, 1000, 5);
        const range = scroller.calculateVisibleRange();

        // scrollTop=400 → firstVisible=10, visibleCount=5
        // buffer=5 → start=5, end=20
        expect(range.start).toBe(5);
        expect(range.end).toBe(20);
        expect(range.offsetTop).toBe(5 * 40);
    });

    it('should clamp range to total rows', () => {
        Object.defineProperty(container, 'scrollTop', { value: 39500, configurable: true });
        const scroller = new VirtualScroller(container, 40, 1000, 5);
        const range = scroller.calculateVisibleRange();

        expect(range.end).toBeLessThanOrEqual(1000);
    });

    it('should return correct total height', () => {
        const scroller = new VirtualScroller(container, 40, 500);
        expect(scroller.getTotalHeight()).toBe(20000);
    });

    it('should update spacer heights', () => {
        const scroller = new VirtualScroller(container, 40, 100);
        const range = { start: 10, end: 20, offsetTop: 400 };

        scroller.updateSpacers(range);

        expect(scroller.getSpacerTop().style.height).toBe('400px');
        expect(scroller.getSpacerBottom().style.height).toBe(`${(100 - 20) * 40}px`);
    });

    it('should invoke onVisibleRangeChange callback on scroll', () => {
        const callback = vi.fn();
        const scroller = new VirtualScroller(container, 40, 100, 2);
        scroller.onVisibleRangeChange(callback);
        scroller.attach();

        // Trigger scroll event
        container.dispatchEvent(new Event('scroll'));

        expect(callback).toHaveBeenCalled();
        const range = callback.mock.calls[callback.mock.calls.length - 1][0];
        expect(range).toHaveProperty('start');
        expect(range).toHaveProperty('end');

        scroller.detach();
    });

    it('should update total rows and recalculate', () => {
        const callback = vi.fn();
        const scroller = new VirtualScroller(container, 40, 50, 2);
        scroller.onVisibleRangeChange(callback);
        scroller.attach();

        scroller.setTotalRows(200);

        // Callback should have been invoked with updated range
        expect(callback).toHaveBeenCalled();

        scroller.detach();
    });
});

// ============================================================================
// DataTableRenderer — Header rendering
// ============================================================================

describe('DataTableRenderer', () => {
    let container: HTMLElement;
    let eventBridge: EventBridge;
    const objectId = 42;

    beforeEach(() => {
        container = document.createElement('div');
        document.body.appendChild(container);
        eventBridge = createMockEventBridge();
    });

    afterEach(() => {
        container.remove();
    });

    describe('header rendering', () => {
        it('should render column headers', () => {
            const renderer = new DataTableRenderer(container, eventBridge, objectId, defaultProps());
            renderer.render();

            const headers = container.querySelectorAll('th');
            expect(headers).toHaveLength(3);
            expect(headers[0].getAttribute('data-field')).toBe('id');
            expect(headers[1].getAttribute('data-field')).toBe('name');
            expect(headers[2].getAttribute('data-field')).toBe('email');
        });

        it('should show sort indicator on sorted column', () => {
            const props = defaultProps({ sortField: 'id', sortDirection: 'asc' });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            const idHeader = container.querySelector('th[data-field="id"] .sort-indicator');
            expect(idHeader?.textContent).toBe(' ▲');
        });

        it('should show descending sort indicator', () => {
            const props = defaultProps({ sortField: 'name', sortDirection: 'desc' });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            const nameHeader = container.querySelector('th[data-field="name"] .sort-indicator');
            expect(nameHeader?.textContent).toBe(' ▼');
        });

        it('should not show sort indicator on unsorted columns', () => {
            const props = defaultProps({ sortField: 'id', sortDirection: 'asc' });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            const nameHeader = container.querySelector('th[data-field="name"] .sort-indicator');
            expect(nameHeader?.textContent).toBe('');
        });

        it('should apply column width when specified', () => {
            const columns: ColumnDef[] = [
                { field: 'id', header: 'ID', type: 'number', sortable: false, filterable: false, width: 80 },
            ];
            const props = defaultProps({ columns, data: [{ id: '1' }] });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            const th = container.querySelector('th');
            expect(th?.style.width).toBe('80px');
        });
    });

    // ========================================================================
    // Sort events
    // ========================================================================

    describe('sort events', () => {
        it('should dispatch wa-sort with asc when clicking unsorted column', () => {
            const renderer = new DataTableRenderer(container, eventBridge, objectId, defaultProps());
            renderer.render();

            const idHeader = container.querySelector('th[data-field="id"]') as HTMLElement;
            idHeader.click();

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-sort', {
                field: 'id',
                direction: 'asc',
            });
        });

        it('should cycle sort direction: asc → desc', () => {
            const props = defaultProps({ sortField: 'id', sortDirection: 'asc' });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            const idHeader = container.querySelector('th[data-field="id"]') as HTMLElement;
            idHeader.click();

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-sort', {
                field: 'id',
                direction: 'desc',
            });
        });

        it('should cycle sort direction: desc → none', () => {
            const props = defaultProps({ sortField: 'id', sortDirection: 'desc' });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            const idHeader = container.querySelector('th[data-field="id"]') as HTMLElement;
            idHeader.click();

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-sort', {
                field: 'id',
                direction: '',
            });
        });

        it('should not dispatch sort for non-sortable columns', () => {
            const renderer = new DataTableRenderer(container, eventBridge, objectId, defaultProps());
            renderer.render();

            // 'email' column is not sortable
            const emailHeader = container.querySelector('th[data-field="email"]') as HTMLElement;
            emailHeader.click();

            expect(eventBridge.dispatch).not.toHaveBeenCalled();
        });
    });

    // ========================================================================
    // Pagination
    // ========================================================================

    describe('pagination', () => {
        it('should render pagination controls', () => {
            const renderer = new DataTableRenderer(container, eventBridge, objectId, defaultProps());
            renderer.render();

            const pagination = container.querySelector('.datatable-pagination');
            expect(pagination).not.toBeNull();

            const buttons = pagination!.querySelectorAll('button');
            expect(buttons).toHaveLength(2); // prev + next

            const pageInfo = pagination!.querySelector('.page-info');
            expect(pageInfo?.textContent).toBe('Page 1 of 1');
        });

        it('should disable Previous button on first page', () => {
            const props = defaultProps({ page: 0, totalRows: 30, pageSize: 10 });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            const prevBtn = container.querySelector('.datatable-pagination button:first-child') as HTMLButtonElement;
            expect(prevBtn.disabled).toBe(true);
        });

        it('should disable Next button on last page', () => {
            const props = defaultProps({ page: 2, totalRows: 30, pageSize: 10 });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            const nextBtn = container.querySelector('.datatable-pagination button:last-child') as HTMLButtonElement;
            expect(nextBtn.disabled).toBe(true);
        });

        it('should dispatch wa-page-change on Next click', () => {
            const props = defaultProps({ page: 0, totalRows: 30, pageSize: 10 });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            const nextBtn = container.querySelector('.datatable-pagination button:last-child') as HTMLButtonElement;
            nextBtn.click();

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-page-change', { page: 1 });
        });

        it('should dispatch wa-page-change on Previous click', () => {
            const props = defaultProps({ page: 1, totalRows: 30, pageSize: 10 });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            const prevBtn = container.querySelector('.datatable-pagination button:first-child') as HTMLButtonElement;
            prevBtn.click();

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-page-change', { page: 0 });
        });

        it('should show correct page info', () => {
            const props = defaultProps({ page: 1, totalRows: 25, pageSize: 10 });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            const pageInfo = container.querySelector('.page-info');
            expect(pageInfo?.textContent).toBe('Page 2 of 3');
        });
    });

    // ========================================================================
    // Row selection
    // ========================================================================

    describe('row selection', () => {
        it('should dispatch wa-selection-change when clicking a row', () => {
            const renderer = new DataTableRenderer(container, eventBridge, objectId, defaultProps());
            renderer.render();

            const firstRow = container.querySelector('tbody tr') as HTMLElement;
            firstRow.click();

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-selection-change', {
                selectedRows: ['row-0'],
            });
        });

        it('should toggle selection on repeated click', () => {
            const renderer = new DataTableRenderer(container, eventBridge, objectId, defaultProps());
            renderer.render();

            const firstRow = container.querySelector('tbody tr') as HTMLElement;
            firstRow.click(); // select
            firstRow.click(); // deselect

            const lastCall = (eventBridge.dispatch as ReturnType<typeof vi.fn>).mock.calls.at(-1);
            expect(lastCall).toEqual([objectId, 'wa-selection-change', { selectedRows: [] }]);
        });

        it('should support multiple row selection', () => {
            const renderer = new DataTableRenderer(container, eventBridge, objectId, defaultProps());
            renderer.render();

            const rows = container.querySelectorAll('tbody tr');
            (rows[0] as HTMLElement).click();
            (rows[2] as HTMLElement).click();

            const lastCall = (eventBridge.dispatch as ReturnType<typeof vi.fn>).mock.calls.at(-1);
            expect(lastCall![2].selectedRows).toContain('row-0');
            expect(lastCall![2].selectedRows).toContain('row-2');
        });

        it('should render pre-selected rows with selected class', () => {
            const props = defaultProps({ selectedRows: ['row-1'] });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            const rows = container.querySelectorAll('tbody tr');
            expect(rows[1].classList.contains('selected')).toBe(true);
            expect(rows[0].classList.contains('selected')).toBe(false);
        });

        it('should use row index as ID when no id field present', () => {
            const data = [{ name: 'Alice' }, { name: 'Bob' }];
            const columns: ColumnDef[] = [
                { field: 'name', header: 'Name', type: 'string', sortable: false, filterable: false },
            ];
            const props = defaultProps({ columns, data, totalRows: 2 });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            const firstRow = container.querySelector('tbody tr') as HTMLElement;
            firstRow.click();

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'wa-selection-change', {
                selectedRows: ['0'],
            });
        });
    });

    // ========================================================================
    // Body rendering
    // ========================================================================

    describe('body rendering', () => {
        it('should render all data rows', () => {
            const renderer = new DataTableRenderer(container, eventBridge, objectId, defaultProps());
            renderer.render();

            const rows = container.querySelectorAll('tbody tr');
            expect(rows).toHaveLength(5);
        });

        it('should render cell values from data', () => {
            const renderer = new DataTableRenderer(container, eventBridge, objectId, defaultProps());
            renderer.render();

            const firstRowCells = container.querySelectorAll('tbody tr:first-child td');
            expect(firstRowCells[0].textContent).toBe('row-0');
            expect(firstRowCells[1].textContent).toBe('User 0');
            expect(firstRowCells[2].textContent).toBe('user0@example.com');
        });

        it('should render empty string for null/undefined values', () => {
            const data = [{ id: '1', name: null, email: undefined }];
            const props = defaultProps({ data: data as any, totalRows: 1 });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            const cells = container.querySelectorAll('tbody tr td');
            expect(cells[1].textContent).toBe('');
            expect(cells[2].textContent).toBe('');
        });

        it('should set role=grid on the table element', () => {
            const renderer = new DataTableRenderer(container, eventBridge, objectId, defaultProps());
            renderer.render();

            const table = container.querySelector('table');
            expect(table?.getAttribute('role')).toBe('grid');
        });
    });

    // ========================================================================
    // Virtual scrolling
    // ========================================================================

    describe('virtual scrolling', () => {
        it('should not use virtual scroll when data is below threshold', () => {
            const props = defaultProps({ virtualScroll: true, data: defaultData(50) });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            // No scroll container wrapper
            const scrollContainer = container.querySelector('div[style*="overflow"]');
            expect(scrollContainer).toBeNull();
        });

        it('should activate virtual scroll when data exceeds threshold', () => {
            const props = defaultProps({
                virtualScroll: true,
                data: defaultData(200),
                totalRows: 200,
            });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            const scrollContainer = container.querySelector('div[style*="overflow"]');
            expect(scrollContainer).not.toBeNull();
        });

        it('should not activate virtual scroll when virtualScroll prop is false', () => {
            const props = defaultProps({
                virtualScroll: false,
                data: defaultData(200),
                totalRows: 200,
            });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            const scrollContainer = container.querySelector('div[style*="overflow"]');
            expect(scrollContainer).toBeNull();

            // All 200 rows should be rendered
            const rows = container.querySelectorAll('tbody tr');
            expect(rows).toHaveLength(200);
        });

        it('should render fewer rows than total when virtual scrolling', () => {
            const props = defaultProps({
                virtualScroll: true,
                data: defaultData(500),
                totalRows: 500,
            });
            const renderer = new DataTableRenderer(container, eventBridge, objectId, props);
            renderer.render();

            const rows = container.querySelectorAll('tbody tr');
            expect(rows.length).toBeLessThan(500);
        });
    });

    // ========================================================================
    // Update and destroy
    // ========================================================================

    describe('update', () => {
        it('should re-render with new props', () => {
            const renderer = new DataTableRenderer(container, eventBridge, objectId, defaultProps());
            renderer.render();

            expect(container.querySelectorAll('tbody tr')).toHaveLength(5);

            renderer.update(defaultProps({ data: defaultData(3), totalRows: 3 }));

            expect(container.querySelectorAll('tbody tr')).toHaveLength(3);
        });
    });

    describe('destroy', () => {
        it('should clean up the container', () => {
            const renderer = new DataTableRenderer(container, eventBridge, objectId, defaultProps());
            renderer.render();

            renderer.destroy();

            expect(container.innerHTML).toBe('');
        });
    });
});
