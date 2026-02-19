/**
 * Web Component framework adapter for PComponent.
 * 
 * Requirements: 7.4 - THE WebComponent_Adapter SHALL mount custom elements and update them via property setters
 * Requirements: 9.3, 9.4 - Supports event dispatch through EventBridge
 */

import type { Operation } from 'fast-json-patch';
import { applyPatch } from 'fast-json-patch';
import { BaseFrameworkAdapter } from './FrameworkAdapter.js';
import type { ComponentFactory } from '../types.js';
import type { EventBridge } from '../EventBridge.js';

/**
 * Adapter for mounting and updating Web Components (Custom Elements).
 * Uses standard Custom Elements API with property setters.
 */
export class WebComponentAdapter<TProps> extends BaseFrameworkAdapter<TProps> {
    private element: HTMLElement | null = null;
    private props: TProps;
    private tagName: string;
    private container: HTMLElement;
    private mounted = false;

    constructor(factory: ComponentFactory<TProps>, initialProps: TProps, eventBridge: EventBridge, objectId: number) {
        super(eventBridge, objectId);
        this.tagName = factory.getTagName?.() ?? 'unknown-component';
        this.props = initialProps;
        this.container = factory.getContainer();
    }

    mount(): void {
        if (this.mounted) {
            return; // Idempotent - already mounted
        }

        this.element = document.createElement(this.tagName);
        this.applyPropsToElement();
        this.container.appendChild(this.element);
        this.mounted = true;
    }

    unmount(): void {
        if (!this.mounted) {
            return; // Safe to call on unmounted component
        }

        if (this.element && this.element.parentNode) {
            this.element.parentNode.removeChild(this.element);
        }
        this.element = null;
        this.mounted = false;
    }

    setProps(props: TProps): void {
        this.props = props;
        if (this.mounted) {
            this.applyPropsToElement();
        }
    }

    applyPatches(patches: Operation[]): void {
        const result = applyPatch(this.props, patches, false, false);
        this.props = result.newDocument as TProps;
        if (this.mounted) {
            this.applyPropsToElement();
        }
    }

    applyBinary(data: ArrayBuffer): void {
        const decoded = this.decodeBinary(data);
        this.props = { ...this.props, ...decoded };
        if (this.mounted) {
            this.applyPropsToElement();
        }
    }

    isMounted(): boolean {
        return this.mounted;
    }

    private applyPropsToElement(): void {
        if (!this.element || this.props === null || this.props === undefined) {
            return;
        }

        // Apply props as properties on the custom element
        const propsObj = this.props as Record<string, unknown>;
        for (const [key, value] of Object.entries(propsObj)) {
            (this.element as unknown as Record<string, unknown>)[key] = value;
        }
    }

    private decodeBinary(_data: ArrayBuffer): Partial<TProps> {
        // Binary decoding implementation for high-frequency updates
        return {} as Partial<TProps>;
    }
}
