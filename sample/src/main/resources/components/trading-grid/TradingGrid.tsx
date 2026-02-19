// Use React and AG Grid from global window (loaded from CDN)
declare const React: typeof import('react');
declare const agGrid: any;
const { useMemo, useRef, useCallback } = React;
const AgGridReact = agGrid.AgGridReact;

import type { ColDef, CellClassParams, GetRowIdParams, RowClickedEvent } from 'ag-grid-community';

export interface StockData {
    id: number;
    race: string;
    price: number;
    count: number;
    change?: number;
}

export interface TradingGridProps {
    stocks: StockData[];
    title?: string;
    showStats?: boolean;
}

interface TradingGridInternalProps extends TradingGridProps {
    onRowClick?: (stock: StockData) => void;
}

export function TradingGrid({ stocks, title, showStats = true, onRowClick }: TradingGridInternalProps) {
    const gridRef = useRef<AgGridReact>(null);

    const columnDefs = useMemo<ColDef<StockData>[]>(() => [
        { field: 'id', headerName: 'ID', width: 80, sort: 'asc' },
        { field: 'race', headerName: 'Race', flex: 1, minWidth: 150 },
        {
            field: 'price',
            headerName: 'Price',
            width: 120,
            valueFormatter: (p) => '$' + p.value?.toFixed(2),
            cellClass: (p: CellClassParams<StockData>) => {
                const c = p.data?.change;
                if (c && c > 0) return 'price-up';
                if (c && c < 0) return 'price-down';
                return '';
            },
            enableCellChangeFlash: true
        },
        { field: 'count', headerName: 'Stock', width: 100, enableCellChangeFlash: true },
        {
            field: 'change',
            headerName: 'Change',
            width: 100,
            valueFormatter: (p) => !p.value ? '-' : (p.value > 0 ? '+' : '') + p.value.toFixed(2),
            cellClass: (p: CellClassParams<StockData>) => {
                if (p.value > 0) return 'price-up';
                if (p.value < 0) return 'price-down';
                return '';
            }
        }
    ], []);

    const defaultColDef = useMemo<ColDef>(() => ({
        sortable: true,
        resizable: true,
        enableCellChangeFlash: true
    }), []);
    const getRowId = useCallback((p: GetRowIdParams<StockData>) => String(p.data.id), []);
    const handleRowClicked = useCallback((e: RowClickedEvent<StockData>) => {
        if (onRowClick && e.data) onRowClick(e.data);
    }, [onRowClick]);

    return (
        <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
            {title && <div className="grid-title">{title}</div>}
            <div className="ag-theme-alpine-dark" style={{ flex: 1 }}>
                <AgGridReact
                    ref={gridRef}
                    rowData={stocks}
                    columnDefs={columnDefs}
                    defaultColDef={defaultColDef}
                    getRowId={getRowId}
                    animateRows={true}
                    onRowClicked={handleRowClicked}
                />
            </div>
            {showStats && <div className="grid-stats">Rows: {stocks.length}</div>}
        </div>
    );
}
