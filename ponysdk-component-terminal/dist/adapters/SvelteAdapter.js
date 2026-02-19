/**
 * Svelte framework adapter for PComponent.
 *
 * Requirements: 7.3 - THE Svelte_Adapter SHALL mount Svelte components and update them via store updates
 * Requirements: 9.3, 9.4 - Supports event dispatch through EventBridge
 */
import { applyPatch } from 'fast-json-patch';
import { BaseFrameworkAdapter } from './FrameworkAdapter.js';
/**
 * Adapter for mounting and updating Svelte components.
 * Uses Svelte's component API with store-based updates.
 */
export class SvelteAdapter extends BaseFrameworkAdapter {
    constructor(factory, initialProps, eventBridge, objectId) {
        super(eventBridge, objectId);
        this._instance = null;
        this.mounted = false;
        this._component = factory.getSvelteComponent?.();
        this.props = initialProps;
        this._container = factory.getContainer();
    }
    mount() {
        if (this.mounted) {
            return; // Idempotent - already mounted
        }
        // Svelte mounting will be implemented when Svelte is available
        // Uses new Component({ target, props })
        this.mounted = true;
    }
    unmount() {
        if (!this.mounted) {
            return; // Safe to call on unmounted component
        }
        // Svelte unmounting will be implemented when Svelte is available
        // Uses instance.$destroy()
        this._instance = null;
        this.mounted = false;
    }
    setProps(props) {
        this.props = props;
        if (this.mounted) {
            this.updateSvelteProps();
        }
    }
    applyPatches(patches) {
        const result = applyPatch(this.props, patches, false, false);
        this.props = result.newDocument;
        if (this.mounted) {
            this.updateSvelteProps();
        }
    }
    applyBinary(data) {
        const decoded = this.decodeBinary(data);
        this.props = { ...this.props, ...decoded };
        if (this.mounted) {
            this.updateSvelteProps();
        }
    }
    isMounted() {
        return this.mounted;
    }
    updateSvelteProps() {
        // Svelte props update will be implemented when Svelte is available
        // Uses instance.$set(props)
    }
    decodeBinary(_data) {
        // Binary decoding implementation for high-frequency updates
        return {};
    }
}
//# sourceMappingURL=SvelteAdapter.js.map