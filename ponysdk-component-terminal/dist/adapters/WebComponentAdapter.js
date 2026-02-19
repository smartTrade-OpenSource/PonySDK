/**
 * Web Component framework adapter for PComponent.
 *
 * Requirements: 7.4 - THE WebComponent_Adapter SHALL mount custom elements and update them via property setters
 * Requirements: 9.3, 9.4 - Supports event dispatch through EventBridge
 */
import { applyPatch } from 'fast-json-patch';
import { BaseFrameworkAdapter } from './FrameworkAdapter.js';
/**
 * Adapter for mounting and updating Web Components (Custom Elements).
 * Uses standard Custom Elements API with property setters.
 */
export class WebComponentAdapter extends BaseFrameworkAdapter {
    constructor(factory, initialProps, eventBridge, objectId) {
        super(eventBridge, objectId);
        this.element = null;
        this.mounted = false;
        this.tagName = factory.getTagName?.() ?? 'unknown-component';
        this.props = initialProps;
        this.container = factory.getContainer();
    }
    mount() {
        if (this.mounted) {
            return; // Idempotent - already mounted
        }
        this.element = document.createElement(this.tagName);
        this.applyPropsToElement();
        this.container.appendChild(this.element);
        this.mounted = true;
    }
    unmount() {
        if (!this.mounted) {
            return; // Safe to call on unmounted component
        }
        if (this.element && this.element.parentNode) {
            this.element.parentNode.removeChild(this.element);
        }
        this.element = null;
        this.mounted = false;
    }
    setProps(props) {
        this.props = props;
        if (this.mounted) {
            this.applyPropsToElement();
        }
    }
    applyPatches(patches) {
        const result = applyPatch(this.props, patches, false, false);
        this.props = result.newDocument;
        if (this.mounted) {
            this.applyPropsToElement();
        }
    }
    applyBinary(data) {
        const decoded = this.decodeBinary(data);
        this.props = { ...this.props, ...decoded };
        if (this.mounted) {
            this.applyPropsToElement();
        }
    }
    isMounted() {
        return this.mounted;
    }
    applyPropsToElement() {
        if (!this.element || this.props === null || this.props === undefined) {
            return;
        }
        // Apply props as properties on the custom element
        const propsObj = this.props;
        for (const [key, value] of Object.entries(propsObj)) {
            this.element[key] = value;
        }
    }
    decodeBinary(_data) {
        // Binary decoding implementation for high-frequency updates
        return {};
    }
}
//# sourceMappingURL=WebComponentAdapter.js.map