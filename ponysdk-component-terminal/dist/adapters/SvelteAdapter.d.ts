/**
 * Svelte framework adapter for PComponent.
 *
 * Requirements: 7.3 - THE Svelte_Adapter SHALL mount Svelte components and update them via store updates
 * Requirements: 9.3, 9.4 - Supports event dispatch through EventBridge
 */
import type { Operation } from 'fast-json-patch';
import { BaseFrameworkAdapter } from './FrameworkAdapter.js';
import type { ComponentFactory } from '../types.js';
import type { EventBridge } from '../EventBridge.js';
/**
 * Adapter for mounting and updating Svelte components.
 * Uses Svelte's component API with store-based updates.
 */
export declare class SvelteAdapter<TProps> extends BaseFrameworkAdapter<TProps> {
    private _instance;
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
    private updateSvelteProps;
    private decodeBinary;
}
//# sourceMappingURL=SvelteAdapter.d.ts.map