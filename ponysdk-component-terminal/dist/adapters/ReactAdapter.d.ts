/**
 * React framework adapter for PComponent.
 *
 * Requirements: 7.1 - THE React_Adapter SHALL mount React components and update them via props changes
 * Requirements: 9.3, 9.4 - Supports event dispatch through EventBridge
 */
import type { Operation } from 'fast-json-patch';
import { BaseFrameworkAdapter } from './FrameworkAdapter.js';
import type { ComponentFactory } from '../types.js';
import type { EventBridge } from '../EventBridge.js';
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
export declare class ReactAdapter<TProps> extends BaseFrameworkAdapter<TProps> {
    private _root;
    private props;
    private _component;
    private _container;
    private mounted;
    constructor(factory: ComponentFactory<TProps>, initialProps: TProps, eventBridge: EventBridge, objectId: number);
    mount(): void;
    unmount(): void;
    setProps(props: TProps): void;
    applyPatches(patches: Operation[]): void;
    applyBinary(data: ArrayBuffer): void;
    isMounted(): boolean;
    private render;
    private decodeBinary;
}
export {};
//# sourceMappingURL=ReactAdapter.d.ts.map