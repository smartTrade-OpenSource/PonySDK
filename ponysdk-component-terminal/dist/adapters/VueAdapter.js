/**
 * Vue framework adapter for PComponent.
 *
 * Requirements: 7.2 - THE Vue_Adapter SHALL mount Vue components and update them via reactive props
 * Requirements: 9.3, 9.4 - Supports event dispatch through EventBridge
 */
import { applyPatch } from 'fast-json-patch';
import { BaseFrameworkAdapter } from './FrameworkAdapter.js';
/**
 * Adapter for mounting and updating Vue components.
 * Uses Vue 3's createApp API with reactive props.
 */
export class VueAdapter extends BaseFrameworkAdapter {
    constructor(factory, initialProps, eventBridge, objectId) {
        super(eventBridge, objectId);
        this._app = null;
        this.mounted = false;
        this._component = factory.getVueComponent?.();
        this.props = initialProps;
        this._container = factory.getContainer();
    }
    mount() {
        if (this.mounted) {
            return; // Idempotent - already mounted
        }
        // Vue mounting will be implemented when Vue is available
        // Uses createApp and mount
        this.mounted = true;
    }
    unmount() {
        if (!this.mounted) {
            return; // Safe to call on unmounted component
        }
        // Vue unmounting will be implemented when Vue is available
        // Uses app.unmount()
        this._app = null;
        this.mounted = false;
    }
    setProps(props) {
        this.props = props;
        if (this.mounted) {
            this.updateReactiveProps();
        }
    }
    applyPatches(patches) {
        const result = applyPatch(this.props, patches, false, false);
        this.props = result.newDocument;
        if (this.mounted) {
            this.updateReactiveProps();
        }
    }
    applyBinary(data) {
        const decoded = this.decodeBinary(data);
        this.props = { ...this.props, ...decoded };
        if (this.mounted) {
            this.updateReactiveProps();
        }
    }
    isMounted() {
        return this.mounted;
    }
    updateReactiveProps() {
        // Vue reactive props update will be implemented when Vue is available
        // Updates reactive refs or uses provide/inject
    }
    decodeBinary(_data) {
        // Binary decoding implementation for high-frequency updates
        return {};
    }
}
//# sourceMappingURL=VueAdapter.js.map