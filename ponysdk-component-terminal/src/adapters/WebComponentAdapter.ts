/**
 * Web Component framework adapter for PComponent.
 * 
 * Requirements: 7.4 - THE WebComponent_Adapter SHALL mount custom elements and update them via property setters
 * Requirements: 9.3, 9.4 - Supports event dispatch through EventBridge
 */

import type { Operation } from 'fast-json-patch';
import { applyPatch } from 'fast-json-patch';
import { BaseFrameworkAdapter } from './FrameworkAdapter.js';
import type { ComponentFactory, SlotOperation } from '../types.js';
import type { EventBridge } from '../EventBridge.js';
import { EventForwarder } from '../events/EventForwarder.js';
import { OverlayController } from '../overlay/OverlayController.js';
import { getWebAwesomeLoader } from '../WebAwesomeRegistry.js';

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
    private eventForwarder: EventForwarder | null = null;
    private overlayController: OverlayController | null = null;

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

        // For Web Awesome components (wa-*), ensure they're defined before mounting
        if (this.tagName.startsWith('wa-')) {
            const loader = getWebAwesomeLoader();

            // Check if component is ready
            if (!loader.isReady(this.tagName)) {
                // Show placeholder while loading
                const placeholder = loader.showPlaceholder(this.container);

                // Wait for component to be defined, then mount
                loader.ensureDefined(this.tagName)
                    .then(() => {
                        loader.removePlaceholder(placeholder);
                        this.performMount();
                    })
                    .catch((err: unknown) => {
                        console.error(`Failed to load Web Awesome component ${this.tagName}:`, err);
                        loader.removePlaceholder(placeholder);
                        // Show error placeholder
                        const errorDiv = document.createElement('div');
                        errorDiv.className = 'wa-load-error';
                        errorDiv.textContent = `Failed to load ${this.tagName}`;
                        errorDiv.style.color = 'red';
                        errorDiv.style.padding = '1rem';
                        errorDiv.style.border = '1px solid red';
                        this.container.appendChild(errorDiv);
                    });
                return;
            }
        }

        // Component is ready or not a Web Awesome component - mount immediately
        this.performMount();
    }

    private performMount(): void {
        this.element = document.createElement(this.tagName);
        this.applyPropsToElement();
        this.container.appendChild(this.element);
        this.mounted = true;

        // Attach event forwarding for all interactive components
        this.eventForwarder = new EventForwarder(this.element, this.eventBridge, this.objectId);
        this.eventForwarder.attach();

        // Attach overlay controller for wa-dialog and wa-drawer
        if (OverlayController.isOverlayElement(this.element)) {
            this.overlayController = new OverlayController(this.element);
            this.overlayController.attach();
            // Sync initial open state from props
            const propsObj = this.props as Record<string, unknown>;
            if (typeof propsObj['open'] === 'boolean') {
                this.overlayController.syncOpen(propsObj['open']);
            }
        }
    }

    unmount(): void {
        if (!this.mounted) {
            return; // Safe to call on unmounted component
        }

        // Detach event forwarder
        if (this.eventForwarder) {
            this.eventForwarder.detach();
            this.eventForwarder = null;
        }

        // Detach overlay controller
        if (this.overlayController) {
            this.overlayController.detach();
            this.overlayController = null;
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

    /**
     * Get the underlying DOM element.
     * Returns null if the component is not mounted.
     */
    getElement(): HTMLElement | null {
        return this.element;
    }

    /**
     * Handle a slot operation (add or remove a child element).
     * Requirements: 7.2 - Insert child into the corresponding Web Component slot
     * Requirements: 7.3 - Remove child from the slot DOM
     * Requirements: 7.4 - Default slot (null slotName) mounts without slot attribute
     *
     * @param slotOp - The slot operation descriptor
     * @param childElement - The child DOM element to add or remove
     */
    handleSlotOperation(slotOp: SlotOperation, childElement: HTMLElement): void {
        if (!this.mounted || !this.element) {
            console.warn('Cannot handle slot operation on unmounted component:', this.objectId);
            return;
        }

        if (slotOp.operation === 'add') {
            // Set slot attribute for named slots, remove it for default slot
            if (slotOp.slotName !== null) {
                childElement.setAttribute('slot', slotOp.slotName);
            } else {
                childElement.removeAttribute('slot');
            }
            this.element.appendChild(childElement);
        } else if (slotOp.operation === 'remove') {
            // Detach the child element if it's a child of this element
            if (childElement.parentNode === this.element) {
                this.element.removeChild(childElement);
            }
        }
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

        // Sync overlay open state if this is an overlay component
        if (this.overlayController && typeof propsObj['open'] === 'boolean') {
            this.overlayController.syncOpen(propsObj['open']);
        }
    }

    private decodeBinary(_data: ArrayBuffer): Partial<TProps> {
        // Binary decoding implementation for high-frequency updates
        return {} as Partial<TProps>;
    }
}
