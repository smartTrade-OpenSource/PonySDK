/**
 * Minimal AgGridReact wrapper for UMD/CDN usage
 */
(() => {
    const { useEffect, useRef, useImperativeHandle, forwardRef } = React;

    const AgGridReact = forwardRef((props, ref) => {
        const eGridDiv = useRef(null);
        const gridApi = useRef(null);
        const initialized = useRef(false);

        useImperativeHandle(ref, () => ({
            api: gridApi.current,
            columnApi: gridApi.current?.columnApi
        }));

        // Initialize grid once
        useEffect(() => {
            if (!eGridDiv.current || !window.agGrid || initialized.current) return;

            const gridOptions = {
                ...props,
                onGridReady: (params) => {
                    gridApi.current = params.api;
                    if (props.onGridReady) props.onGridReady(params);
                }
            };

            agGrid.createGrid(eGridDiv.current, gridOptions);
            initialized.current = true;

            return () => {
                if (gridApi.current) {
                    gridApi.current.destroy();
                }
            };
        }, []);

        // Update rowData only
        useEffect(() => {
            if (gridApi.current && props.rowData) {
                gridApi.current.setGridOption('rowData', props.rowData);
            }
        }, [props.rowData]);

        return React.createElement('div', {
            ref: eGridDiv,
            style: { width: '100%', height: '100%' }
        });
    });

    if (!window.agGrid) window.agGrid = {};
    window.agGrid.AgGridReact = AgGridReact;

    console.log('AgGridReact wrapper registered');
})();
