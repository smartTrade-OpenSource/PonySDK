/**
 * Main terminal for handling PComponent messages.
 * 
 * Requirements: 6.1 - THE Component_Terminal SHALL parse binary messages from the existing PonySDK protocol
 * Requirements: 6.3 - WHEN a component creation message is received, THE Component_Terminal SHALL instantiate the appropriate Framework_Adapter
 * Requirements: 6.4 - WHEN a props update message is received, THE Component_Terminal SHALL apply the JSON Patch to the current props
 * Requirements: 6.5 - WHEN a destroy message is received, THE Component_Terminal SHALL unmount the component and clean up resources
 * Requirements: 6.6 - THE Component_Terminal SHALL coexist with the existing GWT terminal without conflicts
 * Requirements: 9.3, 9.4 - EventBridge is passed to registry for adapter event dispatch
 * 
 * ## PComponent Container Element Architecture
 * 
 * Since PComponent extends PWidget, it requires a DOM container element to support PWidget functionality
 * (visibility, styles, dimensions) while hosting framework-specific components (React/Vue/Svelte).
 * 
 * ### Container Element Creation (Requirement 5.1)
 * 
 * When a COMPONENT widget is created, the Terminal must:
 * 1. Create a container DOM element (typically a `<div>`)
 * 2. Assign a unique ID: `pony-${widgetId}`
 * 3. Add CSS class: `pony-component-container`
 * 4. Apply initial PWidget properties (styles, visibility, dimensions)
 * 
 * Example:
 * ```typescript
 * const container = document.createElement('div');
 * container.id = `pony-${widgetId}`;
 * container.className = 'pony-component-container';
 * ```
 * 
 * ### PWidget Property Application (Requirements 5.2, 5.3, 6.1-6.4)
 * 
 * The container element receives all PWidget properties:
 * 
 * **CSS Classes**: Applied via `addStyleName()`, `removeStyleName()`, `setStyleName()`
 * - Classes are added to the container's `classList`
 * - Multiple classes can coexist with the base `pony-component-container` class
 * 
 * **Inline Styles**: Applied via `setWidth()`, `setHeight()`, or direct style properties
 * - Styles are applied to the container's `style` attribute
 * - Example: `container.style.width = '300px'`
 * 
 * **Visibility**: Controlled via `setVisible(true/false)`
 * - Hidden state: `container.style.display = 'none'` or `container.style.visibility = 'hidden'`
 * - Visible state: Restore original display value
 * - Framework component remains mounted when hidden (Requirement 6.5)
 * 
 * **Dimensions**: Applied via `setWidth()` and `setHeight()`
 * - Set as inline styles on the container element
 * - Framework component inherits container dimensions
 * 
 * ### Framework Component Mounting (Requirements 5.4, 10.1, 10.2)
 * 
 * The framework component (React/Vue/Svelte) mounts **inside** the container element:
 * 
 * 1. **Create Framework Adapter**: Use ComponentRegistry to create the appropriate adapter
 *    ```typescript
 *    const adapter = ComponentRegistry.create(
 *      widgetData.signature,
 *      widgetData.framework,
 *      widgetData.props
 *    );
 *    ```
 * 
 * 2. **Mount Inside Container**: The adapter mounts its framework component inside the container
 *    ```typescript
 *    adapter.mount(container);
 *    ```
 * 
 * 3. **Isolation**: The framework component renders only inside the container (Requirement 10.5)
 *    - React components use `ReactDOM.render(component, container)`
 *    - Vue components use `app.mount(container)`
 *    - Svelte components use `new Component({ target: container })`
 * 
 * ### DOM Tree Structure (Requirement 5.5)
 * 
 * When a PComponent is added to a parent container, the container element is inserted into the
 * parent's DOM tree:
 * 
 * ```
 * <div class="parent-container">
 *   <div id="pony-123" class="pony-component-container custom-style">
 *     <!-- Framework component renders here -->
 *     <wa-button>Click Me</wa-button>
 *   </div>
 * </div>
 * ```
 * 
 * ### Props Updates vs Widget State (Requirements 3.1-3.5)
 * 
 * Props updates and widget state changes are independent:
 * 
 * **Props Updates**: Handled by the framework adapter
 * - `adapter.applyPatches(patches)` - Apply JSON Patch to props
 * - `adapter.setProps(props)` - Replace props entirely
 * - Framework component re-renders with new props
 * 
 * **Widget State Updates**: Handled by the container element
 * - Visibility changes affect container display
 * - Style changes affect container classes/styles
 * - Dimension changes affect container size
 * - Props remain unchanged
 * 
 * ### Event Systems (Requirements 7.1-7.5)
 * 
 * Two independent event systems coexist:
 * 
 * **DOM Events**: Registered on the container element via `addDomHandler()`
 * - Standard DOM events: click, focus, blur, keypress, etc.
 * - Handlers attached to the container element
 * - Bubble up from framework component through container
 * 
 * **Custom Events**: Registered via `onEvent()` for framework-specific events
 * - Dispatched by framework components to server
 * - Handled by EventBridge
 * - Independent of DOM event system
 * 
 * ### Lifecycle (Requirements 4.1-4.5, 10.4)
 * 
 * **Creation**:
 * 1. Create container element
 * 2. Apply initial PWidget properties
 * 3. Create framework adapter
 * 4. Mount framework component inside container
 * 5. Insert container into parent DOM
 * 
 * **Updates**:
 * - Props updates → framework adapter → component re-render
 * - Widget state updates → container element properties
 * 
 * **Destruction**:
 * 1. Unmount framework component
 * 2. Remove container element from DOM
 * 3. Clean up event handlers and resources
 * 
 * ### Advantages of Container Element Approach
 * 
 * - **Consistency**: PComponent behaves like any other PWidget in the DOM tree
 * - **Isolation**: Framework component is isolated inside the container
 * - **Styling**: Container can receive PWidget styles without affecting framework component
 * - **Visibility**: Container can be hidden while keeping framework component mounted
 * - **Parent Management**: Parent containers can manage PComponent like any other widget
 */

import { ComponentRegistry } from './ComponentRegistry.js';
import { EventBridge } from './EventBridge.js';
import { WebComponentAdapter } from './adapters/WebComponentAdapter.js';
import { FormHandler } from './form/FormHandler.js';
import type { ComponentMessage, ComponentFactory, SlotOperation } from './types.js';

/**
 * Component terminal for handling PComponent lifecycle messages.
 * Coexists with the existing GWT terminal without conflicts.
 */
export class ComponentTerminal {
    private registry: ComponentRegistry;
    private eventBridge: EventBridge;
    private formHandlers = new Map<number, FormHandler>();

    constructor(websocket: WebSocket) {
        this.eventBridge = new EventBridge(websocket);
        this.registry = new ComponentRegistry(this.eventBridge);
    }

    /**
     * Register a component factory for a given signature.
     * @param signature - Unique component signature
     * @param factory - Factory for creating component instances
     */
    registerFactory(signature: string, factory: ComponentFactory): void {
        this.registry.registerFactory(signature, factory);
    }

    /**
     * Handle an incoming component message.
     * @param message - The component message to handle
     */
    handleMessage(message: ComponentMessage): void {
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
    getEventBridge(): EventBridge {
        return this.eventBridge;
    }

    /**
     * Get the component registry.
     */
    getRegistry(): ComponentRegistry {
        return this.registry;
    }

    /**
     * Get the FormHandler for a given form objectId, if one exists.
     */
    getFormHandler(objectId: number): FormHandler | undefined {
        return this.formHandlers.get(objectId);
    }

    private handleCreate(message: ComponentMessage): void {
        if (!message.framework || !message.signature) {
            console.warn('Invalid create message: missing framework or signature', message);
            return;
        }

        try {
            const adapter = this.registry.createAdapter(
                message.objectId,
                message.framework,
                message.signature,
                message.props
            );
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
        } catch (error) {
            console.error('Failed to create component:', error);
        }
    }

    private handleUpdate(message: ComponentMessage): void {
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
        } else if (message.binaryData) {
            adapter.applyBinary(message.binaryData);
        } else if (message.props !== undefined) {
            adapter.setProps(message.props);
        }
    }

    private handleDestroy(message: ComponentMessage): void {
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
    private handleServerErrors(message: ComponentMessage): void {
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
    private handleSlot(message: ComponentMessage): void {
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
