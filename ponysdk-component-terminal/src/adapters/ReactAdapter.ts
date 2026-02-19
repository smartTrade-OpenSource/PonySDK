/**
 * React framework adapter for PComponent.
 * 
 * Requirements: 7.1 - THE React_Adapter SHALL mount React components and update them via props changes
 * Requirements: 9.3, 9.4 - Supports event dispatch through EventBridge
 */

import type { Operation } from 'fast-json-patch';
import { applyPatch } from 'fast-json-patch';
import { BaseFrameworkAdapter } from './FrameworkAdapter.js';
import type { ComponentFactory } from '../types.js';
import type { EventBridge } from '../EventBridge.js';

// React types (will be available at runtime when React is loaded)
interface ReactRoot {
    render(element: unknown): void;
    unmount(): void;
}

interface ReactAPI {
    createElement(component: unknown, props: unknown): unknown;
}

interface ReactDOMClient {
    createRoot(container: HTMLElement): ReactRoot;
}

declare global {
    interface Window {
        React?: ReactAPI;
        ReactDOMClient?: ReactDOMClient;
    }
}

/**
 * Adapter for mounting and updating React components.
 * Uses React 18's createRoot API for concurrent rendering support.
 */
export class ReactAdapter<TProps> extends BaseFrameworkAdapter<TProps> {
    private _root: ReactRoot | null = null;
    private props: TProps;
    private _component: unknown;
    private _container: HTMLElement;
    private mounted = false;

    constructor(factory: ComponentFactory<TProps>, initialProps: TProps, eventBridge: EventBridge, objectId: number) {
        super(eventBridge, objectId);
        this._component = factory.getReactComponent?.();
        this.props = initialProps;
        this._container = factory.getContainer();
    }

    mount(): void {
        if (this.mounted) {
            return; // Idempotent - already mounted
        }

        // Support both ESM (ReactDOMClient) and UMD (ReactDOM.createRoot) builds
        const createRoot = window.ReactDOMClient?.createRoot || (window as any).ReactDOM?.createRoot;

        if (!createRoot) {
            console.error('ReactDOM.createRoot not available - ensure React 18+ is loaded');
            return;
        }

        this._root = createRoot(this._container);
        this.mounted = true;
        this.render();
    }

    unmount(): void {
        if (!this.mounted) {
            return; // Safe to call on unmounted component
        }

        if (this._root) {
            this._root.unmount();
            this._root = null;
        }
        this.mounted = false;
    }

    setProps(props: TProps): void {
        this.props = props;
        if (this.mounted) {
            this.render();
        }
    }

    applyPatches(patches: Operation[]): void {
        const result = applyPatch(this.props, patches, false, false);
        this.props = result.newDocument as TProps;
        if (this.mounted) {
            this.render();
        }
    }

    applyBinary(data: ArrayBuffer): void {
        const decoded = this.decodeBinary(data);
        this.props = { ...this.props, ...decoded };
        if (this.mounted) {
            this.render();
        }
    }

    isMounted(): boolean {
        return this.mounted;
    }

    private render(): void {
        if (!this._root || !window.React) {
            return;
        }

        // Inject common event handlers that will dispatch to server
        // Only handlers actually used by the React component will be called
        const propsWithHandlers = {
            ...this.props,
            onRowClick: (data: unknown) => {
                console.log('[ReactAdapter] onRowClick triggered, dispatching to server:', data);
                this.dispatchEvent('rowClick', data);
            },
            onCellClick: (data: unknown) => {
                this.dispatchEvent('cellClick', data);
            },
            onChange: (data: unknown) => {
                this.dispatchEvent('change', data);
            },
            onClick: (data: unknown) => {
                this.dispatchEvent('click', data);
            },
            onSelect: (data: unknown) => {
                this.dispatchEvent('select', data);
            }
        };

        const element = window.React.createElement(this._component, propsWithHandlers);
        this._root.render(element);
    }

    private decodeBinary(_data: ArrayBuffer): Partial<TProps> {
        // Binary decoding implementation for high-frequency updates
        // Format: [FieldID(1), Type(1), Length(2), Value(N)]
        return {} as Partial<TProps>;
    }
}
