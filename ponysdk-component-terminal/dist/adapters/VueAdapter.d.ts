/**
 * Vue framework adapter for PComponent.
 *
 * Requirements: 7.2 - THE Vue_Adapter SHALL mount Vue components and update them via reactive props
 * Requirements: 9.3, 9.4 - Supports event dispatch through EventBridge
 */
import type { Operation } from 'fast-json-patch';
import { BaseFrameworkAdapter } from './FrameworkAdapter.js';
import type { ComponentFactory } from '../types.js';
import type { EventBridge } from '../EventBridge.js';
/**
 * Adapter for mounting and updating Vue components.
 * Uses Vue 3's createApp API with reactive props.
 */
export declare class VueAdapter<TProps> extends BaseFrameworkAdapter<TProps> {
    private _app;
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
    private updateReactiveProps;
    private decodeBinary;
}
//# sourceMappingURL=VueAdapter.d.ts.map