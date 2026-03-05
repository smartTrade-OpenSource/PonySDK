/**
 * Main terminal for handling PComponent messages.
 *
 * Requirements: 6.1 - THE Component_Terminal SHALL parse binary messages from the existing PonySDK protocol
 * Requirements: 6.3 - WHEN a component creation message is received, THE Component_Terminal SHALL instantiate the appropriate Framework_Adapter
 * Requirements: 6.4 - WHEN a props update message is received, THE Component_Terminal SHALL apply the JSON Patch to the current props
 * Requirements: 6.5 - WHEN a destroy message is received, THE Component_Terminal SHALL unmount the component and clean up resources
 * Requirements: 6.6 - THE Component_Terminal SHALL coexist with the existing GWT terminal without conflicts
 * Requirements: 9.3, 9.4 - EventBridge is passed to registry for adapter event dispatch
 */
import { ComponentRegistry } from './ComponentRegistry.js';
import { EventBridge } from './EventBridge.js';
import { WebComponentAdapter } from './adapters/WebComponentAdapter.js';
import { FormHandler } from './form/FormHandler.js';
/**
 * Component terminal for handling PComponent lifecycle messages.
 * Coexists with the existing GWT terminal without conflicts.
 */
export class ComponentTerminal {
    constructor(websocket) {
        this.formHandlers = new Map();
        this.eventBridge = new EventBridge(websocket);
        this.registry = new ComponentRegistry(this.eventBridge);
    }
    /**
     * Register a component factory for a given signature.
     * @param signature - Unique component signature
     * @param factory - Factory for creating component instances
     */
    registerFactory(signature, factory) {
        this.registry.registerFactory(signature, factory);
    }
    /**
     * Handle an incoming component message.
     * @param message - The component message to handle
     */
    handleMessage(message) {
        switch (message.type) {
            case 'create':
                this.handleCreate(message);
                break;
            case 'update':
                this.handleUpdate(message);
                break;
            case 'destroy':
                this.handleDestroy(message);
                break;
            case 'slot':
                this.handleSlot(message);
                break;
            case 'serverErrors':
                this.handleServerErrors(message);
                break;
        }
    }
    /**
     * Get the event bridge for dispatching events to the server.
     */
    getEventBridge() {
        return this.eventBridge;
    }
    /**
     * Get the component registry.
     */
    getRegistry() {
        return this.registry;
    }
    /**
     * Get the FormHandler for a given form objectId, if one exists.
     */
    getFormHandler(objectId) {
        return this.formHandlers.get(objectId);
    }
    handleCreate(message) {
        if (!message.framework || !message.signature) {
            console.warn('Invalid create message: missing framework or signature', message);
            return;
        }
        try {
            const adapter = this.registry.createAdapter(message.objectId, message.framework, message.signature, message.props);
            adapter.mount();
            // Attach a FormHandler for wa-form components
            if (message.signature === 'wa-form' && adapter instanceof WebComponentAdapter) {
                const element = adapter.getElement();
                if (element) {
                    const handler = new FormHandler(element, this.eventBridge, message.objectId);
                    handler.attach();
                    this.formHandlers.set(message.objectId, handler);
                }
            }
        }
        catch (error) {
            console.error('Failed to create component:', error);
        }
    }
    handleUpdate(message) {
        const adapter = this.registry.get(message.objectId);
        if (!adapter) {
            console.warn('Update for unknown component:', message.objectId);
            return;
        }
        // Prevent updates to unmounted components
        if (!adapter.isMounted()) {
            console.warn('Update for unmounted component:', message.objectId);
            return;
        }
        if (message.patches) {
            adapter.applyPatches(message.patches);
        }
        else if (message.binaryData) {
            adapter.applyBinary(message.binaryData);
        }
        else if (message.props !== undefined) {
            adapter.setProps(message.props);
        }
    }
    handleDestroy(message) {
        // Clean up FormHandler if this is a form component
        const formHandler = this.formHandlers.get(message.objectId);
        if (formHandler) {
            formHandler.detach();
            this.formHandlers.delete(message.objectId);
        }
        const adapter = this.registry.get(message.objectId);
        if (adapter) {
            adapter.unmount();
            this.registry.remove(message.objectId);
        }
    }
    /**
     * Handle server validation errors for a form component.
     * Requirements: 9.3 - Display server validation errors on matching input components
     */
    handleServerErrors(message) {
        const errors = message.serverErrors;
        if (!errors) {
            console.warn('serverErrors message missing errors:', message.objectId);
            return;
        }
        const formHandler = this.formHandlers.get(message.objectId);
        if (!formHandler) {
            console.warn('serverErrors for unknown form:', message.objectId);
            return;
        }
        formHandler.applyServerErrors(errors);
    }
    /**
     * Handle a slot operation message.
     * Requirements: 7.2, 7.3, 7.4 - Slot add/remove with named and default slots
     */
    handleSlot(message) {
        const slotOp = message.slotOperation;
        if (!slotOp) {
            console.warn('Slot message missing slotOperation:', message.objectId);
            return;
        }
        const parentAdapter = this.registry.get(message.objectId);
        if (!parentAdapter) {
            console.warn('Slot operation for unknown parent component:', message.objectId);
            return;
        }
        if (!(parentAdapter instanceof WebComponentAdapter)) {
            console.warn('Slot operations are only supported on WebComponentAdapter:', message.objectId);
            return;
        }
        const childAdapter = this.registry.get(slotOp.childObjectId);
        if (!childAdapter) {
            console.warn('Slot operation references unknown child component:', slotOp.childObjectId);
            return;
        }
        if (!(childAdapter instanceof WebComponentAdapter)) {
            console.warn('Slot child must be a WebComponentAdapter:', slotOp.childObjectId);
            return;
        }
        const childElement = childAdapter.getElement();
        if (!childElement) {
            console.warn('Slot child element is not mounted:', slotOp.childObjectId);
            return;
        }
        parentAdapter.handleSlotOperation(slotOp, childElement);
    }
}
//# sourceMappingURL=ComponentTerminal.js.map