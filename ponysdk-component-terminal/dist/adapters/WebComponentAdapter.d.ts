/**
 * Web Component framework adapter for PComponent.
 *
 * Requirements: 7.4 - THE WebComponent_Adapter SHALL mount custom elements and update them via property setters
 * Requirements: 9.3, 9.4 - Supports event dispatch through EventBridge
 */
import type { Operation } from 'fast-json-patch';
import { BaseFrameworkAdapter } from './FrameworkAdapter.js';
import type { ComponentFactory } from '../types.js';
import type { EventBridge } from '../EventBridge.js';
/**
 * Adapter for mounting and updating Web Components (Custom Elements).
 * Uses standard Custom Elements API with property setters.
 */
export declare class WebComponentAdapter<TProps> extends BaseFrameworkAdapter<TProps> {
    private element;
    private props;
    private tagName;
    private container;
    private mounted;
    constructor(factory: ComponentFactory<TProps>, initialProps: TProps, eventBridge: EventBridge, objectId: number);
    mount(): void;
    unmount(): void;
    setProps(props: TProps): void;
    applyPatches(patches: Operation[]): void;
    applyBinary(data: ArrayBuffer): void;
    isMounted(): boolean;
    private applyPropsToElement;
    private decodeBinary;
}
//# sourceMappingURL=WebComponentAdapter.d.ts.map