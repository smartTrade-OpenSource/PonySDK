(() => {
  // trading-grid/TradingGrid.tsx
  var { useMemo, useRef, useCallback } = React;
  var AgGridReact = agGrid.AgGridReact;
  function TradingGrid({ stocks, title, showStats = true, onRowClick }) {
    const gridRef = useRef(null);
    const columnDefs = useMemo(() => [
      { field: "id", headerName: "ID", width: 80, sort: "asc" },
      { field: "race", headerName: "Race", flex: 1, minWidth: 150 },
      {
        field: "price",
        headerName: "Price",
        width: 120,
        valueFormatter: (p) => "$" + p.value?.toFixed(2),
        cellClass: (p) => {
          const c = p.data?.change;
          if (c && c > 0)
            return "price-up";
          if (c && c < 0)
            return "price-down";
          return "";
        },
        enableCellChangeFlash: true
      },
      { field: "count", headerName: "Stock", width: 100, enableCellChangeFlash: true },
      {
        field: "change",
        headerName: "Change",
        width: 100,
        valueFormatter: (p) => !p.value ? "-" : (p.value > 0 ? "+" : "") + p.value.toFixed(2),
        cellClass: (p) => {
          if (p.value > 0)
            return "price-up";
          if (p.value < 0)
            return "price-down";
          return "";
        }
      }
    ], []);
    const defaultColDef = useMemo(() => ({
      sortable: true,
      resizable: true,
      enableCellChangeFlash: true
    }), []);
    const getRowId = useCallback((p) => String(p.data.id), []);
    const handleRowClicked = useCallback((e) => {
      if (onRowClick && e.data)
        onRowClick(e.data);
    }, [onRowClick]);
    return /* @__PURE__ */ React.createElement("div", { style: { height: "100%", display: "flex", flexDirection: "column" } }, title && /* @__PURE__ */ React.createElement("div", { className: "grid-title" }, title), /* @__PURE__ */ React.createElement("div", { className: "ag-theme-alpine-dark", style: { flex: 1 } }, /* @__PURE__ */ React.createElement(
      AgGridReact,
      {
        ref: gridRef,
        rowData: stocks,
        columnDefs,
        defaultColDef,
        getRowId,
        animateRows: true,
        onRowClicked: handleRowClicked
      }
    )), showStats && /* @__PURE__ */ React.createElement("div", { className: "grid-stats" }, "Rows: ", stocks.length));
  }

  // trading-grid/index.ts
  var SIGNATURE = "trading-grid";
  function createFactory(container) {
    return {
      getContainer: () => container,
      getReactComponent: () => TradingGrid,
      initialProps: { stocks: [] }
    };
  }

  // trading-grid/register.ts
  if (typeof window !== "undefined" && window.registerReactComponent) {
    window.registerReactComponent(SIGNATURE, createFactory);
    console.log(`Registered ${SIGNATURE} component`);
  } else {
    console.error("registerReactComponent not available - ensure component-bridge.js is loaded first");
  }
})();
