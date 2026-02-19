/**
 * React framework adapter for PComponent.
 *
 * Requirements: 7.1 - THE React_Adapter SHALL mount React components and update them via props changes
 * Requirements: 9.3, 9.4 - Supports event dispatch through EventBridge
 */
import { applyPatch } from 'fast-json-patch';
import { BaseFrameworkAdapter } from './FrameworkAdapter.js';
/**
 * Adapter for mounting and updating React components.
 * Uses React 18's createRoot API for concurrent rendering support.
 */
export class ReactAdapter extends BaseFrameworkAdapter {
    constructor(factory, initialProps, eventBridge, objectId) {
        super(eventBridge, objectId);
        this._root = null;
        this.mounted = false;
        this._component = factory.getReactComponent?.();
        this.props = initialProps;
        this._container = factory.getContainer();
    }
    mount() {
        if (this.mounted) {
            return; // Idempotent - already mounted
        }
        // Support both ESM (ReactDOMClient) and UMD (ReactDOM.createRoot) builds
        const createRoot = window.ReactDOMClient?.createRoot || window.ReactDOM?.createRoot;
        if (!createRoot) {
            console.error('ReactDOM.createRoot not available - ensure React 18+ is loaded');
            return;
        }
        this._root = createRoot(this._container);
        this.mounted = true;
        this.render();
    }
    unmount() {
        if (!this.mounted) {
            return; // Safe to call on unmounted component
        }
        if (this._root) {
            this._root.unmount();
            this._root = null;
        }
        this.mounted = false;
    }
    setProps(props) {
        this.props = props;
        if (this.mounted) {
            this.render();
        }
    }
    applyPatches(patches) {
        const result = applyPatch(this.props, patches, false, false);
        this.props = result.newDocument;
        if (this.mounted) {
            this.render();
        }
    }
    applyBinary(data) {
        const decoded = this.decodeBinary(data);
        this.props = { ...this.props, ...decoded };
        if (this.mounted) {
            this.render();
        }
    }
    isMounted() {
        return this.mounted;
    }
    render() {
        if (!this._root || !window.React) {
            return;
        }
        // Inject common event handlers that will dispatch to server
        // Only handlers actually used by the React component will be called
        const propsWithHandlers = {
            ...this.props,
            onRowClick: (data) => {
                console.log('[ReactAdapter] onRowClick triggered, dispatching to server:', data);
                this.dispatchEvent('rowClick', data);
            },
            onCellClick: (data) => {
                this.dispatchEvent('cellClick', data);
            },
            onChange: (data) => {
                this.dispatchEvent('change', data);
            },
            onClick: (data) => {
                this.dispatchEvent('click', data);
            },
            onSelect: (data) => {
                this.dispatchEvent('select', data);
            }
        };
        const element = window.React.createElement(this._component, propsWithHandlers);
        this._root.render(element);
    }
    decodeBinary(_data) {
        // Binary decoding implementation for high-frequency updates
        // Format: [FieldID(1), Type(1), Length(2), Value(N)]
        return {};
    }
}
//# sourceMappingURL=ReactAdapter.js.map