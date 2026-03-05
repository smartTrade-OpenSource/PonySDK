/**
 * Web Component framework adapter for PComponent.
 *
 * Requirements: 7.4 - THE WebComponent_Adapter SHALL mount custom elements and update them via property setters
 * Requirements: 9.3, 9.4 - Supports event dispatch through EventBridge
 */
import type { Operation } from 'fast-json-patch';
import { BaseFrameworkAdapter } from './FrameworkAdapter.js';
import type { ComponentFactory, SlotOperation } from '../types.js';
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
    private eventForwarder;
    private overlayController;
    constructor(factory: ComponentFactory<TProps>, initialProps: TProps, eventBridge: EventBridge, objectId: number);
    mount(): void;
    private performMount;
    unmount(): void;
    setProps(props: TProps): void;
    applyPatches(patches: Operation[]): void;
    applyBinary(data: ArrayBuffer): void;
    isMounted(): boolean;
    /**
     * Get the underlying DOM element.
     * Returns null if the component is not mounted.
     */
    getElement(): HTMLElement | null;
    /**
     * Handle a slot operation (add or remove a child element).
     * Requirements: 7.2 - Insert child into the corresponding Web Component slot
     * Requirements: 7.3 - Remove child from the slot DOM
     * Requirements: 7.4 - Default slot (null slotName) mounts without slot attribute
     *
     * @param slotOp - The slot operation descriptor
     * @param childElement - The child DOM element to add or remove
     */
    handleSlotOperation(slotOp: SlotOperation, childElement: HTMLElement): void;
    private applyPropsToElement;
    private decodeBinary;
}
//# sourceMappingURL=WebComponentAdapter.d.ts.map