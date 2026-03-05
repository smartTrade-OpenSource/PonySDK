/**
 * Unit tests for client-side form submission and validation display.
 *
 * Requirements: 9.2 - Validate all child wa-* input elements on submit
 * Requirements: 9.3 - Display server validation errors on matching input components
 * Requirements: 9.5 - Dispatch single submission event with all field values when valid
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { FormHandler } from '../src/form/FormHandler.js';
import { ComponentTerminal } from '../src/ComponentTerminal.js';
import type { ComponentFactory, ComponentMessage } from '../src/types.js';
import type { EventBridge } from '../src/EventBridge.js';

// ============================================================================
// Test Helpers
// ============================================================================

function createMockEventBridge(): EventBridge {
    return {
        dispatch: vi.fn(),
        pendingCount: 0,
        flushNow: vi.fn(),
    } as unknown as EventBridge;
}

function createMockWebSocket(): WebSocket {
    return {
        send: vi.fn(),
        readyState: WebSocket.OPEN,
        addEventListener: () => { },
        removeEventListener: () => { },
    } as unknown as WebSocket;
}

function createMockFactory(tagName: string): ComponentFactory {
    const container = document.createElement('div');
    document.body.appendChild(container);
    return {
        getContainer: () => container,
        getTagName: () => tagName,
    };
}

function cleanupContainer(factory: ComponentFactory): void {
    const container = factory.getContainer();
    if (container.parentNode) {
        container.parentNode.removeChild(container);
    }
}

/**
 * Create a mock wa-input element with validation support.
 */
function createMockInput(name: string, value: string = '', valid: boolean = true): HTMLElement {
    const el = document.createElement('wa-input');
    el.setAttribute('name', name);

    // Simulate Web Awesome input properties
    Object.defineProperty(el, 'value', { value, writable: true, configurable: true });
    Object.defineProperty(el, 'checkValidity', {
        value: vi.fn(() => valid),
        writable: true,
        configurable: true,
    });
    Object.defineProperty(el, 'reportValidity', {
        value: vi.fn(() => valid),
        writable: true,
        configurable: true,
    });
    Object.defineProperty(el, 'setCustomValidity', {
        value: vi.fn(),
        writable: true,
        configurable: true,
    });

    return el;
}

function createMockCheckbox(name: string, checked: boolean = false): HTMLElement {
    const el = document.createElement('wa-checkbox');
    el.setAttribute('name', name);
    Object.defineProperty(el, 'checked', { value: checked, writable: true, configurable: true });
    Object.defineProperty(el, 'checkValidity', { value: vi.fn(() => true), writable: true, configurable: true });
    Object.defineProperty(el, 'reportValidity', { value: vi.fn(() => true), writable: true, configurable: true });
    Object.defineProperty(el, 'setCustomValidity', { value: vi.fn(), writable: true, configurable: true });
    return el;
}

// ============================================================================
// FormHandler unit tests
// ============================================================================

describe('FormHandler', () => {
    let formElement: HTMLElement;
    let eventBridge: EventBridge;
    let handler: FormHandler;
    const objectId = 42;

    beforeEach(() => {
        formElement = document.createElement('wa-form');
        document.body.appendChild(formElement);
        eventBridge = createMockEventBridge();
        handler = new FormHandler(formElement, eventBridge, objectId);
    });

    afterEach(() => {
        handler.detach();
        if (formElement.parentNode) {
            formElement.parentNode.removeChild(formElement);
        }
    });

    describe('getInputElements', () => {
        it('should find wa-input children', () => {
            const input = createMockInput('email');
            formElement.appendChild(input);

            const inputs = handler.getInputElements();
            expect(inputs).toHaveLength(1);
            expect(inputs[0]).toBe(input);
        });

        it('should find multiple input types', () => {
            formElement.appendChild(createMockInput('name'));
            formElement.appendChild(document.createElement('wa-textarea'));
            formElement.appendChild(document.createElement('wa-select'));
            formElement.appendChild(document.createElement('wa-checkbox'));
            formElement.appendChild(document.createElement('wa-switch'));

            const inputs = handler.getInputElements();
            expect(inputs).toHaveLength(5);
        });

        it('should ignore non-input wa-* elements', () => {
            formElement.appendChild(createMockInput('name'));
            formElement.appendChild(document.createElement('wa-button'));
            formElement.appendChild(document.createElement('wa-icon'));

            const inputs = handler.getInputElements();
            expect(inputs).toHaveLength(1);
        });

        it('should find nested input elements', () => {
            const wrapper = document.createElement('div');
            wrapper.appendChild(createMockInput('nested'));
            formElement.appendChild(wrapper);

            const inputs = handler.getInputElements();
            expect(inputs).toHaveLength(1);
        });
    });

    describe('submit - all fields valid', () => {
        it('should dispatch a single submit event with all field values', () => {
            const input1 = createMockInput('email', 'test@example.com', true);
            const input2 = createMockInput('name', 'John', true);
            formElement.appendChild(input1);
            formElement.appendChild(input2);

            handler.submit();

            expect(eventBridge.dispatch).toHaveBeenCalledOnce();
            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'submit', {
                values: {
                    email: 'test@example.com',
                    name: 'John',
                },
            });
        });

        it('should collect checkbox checked state as value', () => {
            const checkbox = createMockCheckbox('agree', true);
            formElement.appendChild(checkbox);

            handler.submit();

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'submit', {
                values: { agree: true },
            });
        });
    });

    describe('submit - validation failure', () => {
        it('should not dispatch submit event when a field is invalid', () => {
            const validInput = createMockInput('name', 'John', true);
            const invalidInput = createMockInput('email', '', false);
            formElement.appendChild(validInput);
            formElement.appendChild(invalidInput);

            handler.submit();

            expect(eventBridge.dispatch).not.toHaveBeenCalled();
        });

        it('should call reportValidity on each input', () => {
            const input1 = createMockInput('name', 'John', true);
            const input2 = createMockInput('email', '', false);
            formElement.appendChild(input1);
            formElement.appendChild(input2);

            handler.submit();

            expect((input1 as any).reportValidity).toHaveBeenCalled();
            expect((input2 as any).reportValidity).toHaveBeenCalled();
        });
    });

    describe('submit via DOM event', () => {
        it('should handle native submit event and prevent default', () => {
            handler.attach();
            const input = createMockInput('name', 'Alice', true);
            formElement.appendChild(input);

            const event = new Event('submit', { bubbles: true, cancelable: true });
            const prevented = !formElement.dispatchEvent(event);

            expect(prevented).toBe(true);
            expect(eventBridge.dispatch).toHaveBeenCalledOnce();
        });
    });

    describe('applyServerErrors', () => {
        it('should set customValidity on matching fields', () => {
            const input1 = createMockInput('email', 'bad');
            const input2 = createMockInput('name', 'John');
            formElement.appendChild(input1);
            formElement.appendChild(input2);

            handler.applyServerErrors({
                email: ['Invalid email format'],
            });

            expect((input1 as any).setCustomValidity).toHaveBeenCalledWith('Invalid email format');
            // name field should be cleared since it's not in the error map
            expect((input2 as any).setCustomValidity).toHaveBeenCalledWith('');
        });

        it('should clear errors on fields not in the error map', () => {
            const input = createMockInput('email', 'test@test.com');
            formElement.appendChild(input);

            handler.applyServerErrors({});

            expect((input as any).setCustomValidity).toHaveBeenCalledWith('');
        });

        it('should use first error message when multiple errors exist', () => {
            const input = createMockInput('email', 'bad');
            formElement.appendChild(input);

            handler.applyServerErrors({
                email: ['Too short', 'Invalid format'],
            });

            expect((input as any).setCustomValidity).toHaveBeenCalledWith('Too short');
        });
    });

    describe('clearErrors', () => {
        it('should clear customValidity on all inputs', () => {
            const input1 = createMockInput('email');
            const input2 = createMockInput('name');
            formElement.appendChild(input1);
            formElement.appendChild(input2);

            handler.clearErrors();

            expect((input1 as any).setCustomValidity).toHaveBeenCalledWith('');
            expect((input2 as any).setCustomValidity).toHaveBeenCalledWith('');
        });
    });

    describe('detach', () => {
        it('should stop listening for submit events after detach', () => {
            handler.attach();
            handler.detach();

            const input = createMockInput('name', 'Alice', true);
            formElement.appendChild(input);

            formElement.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));

            expect(eventBridge.dispatch).not.toHaveBeenCalled();
        });
    });

    describe('field name resolution', () => {
        it('should use name attribute as field name', () => {
            const input = createMockInput('email', 'test@test.com', true);
            formElement.appendChild(input);

            handler.submit();

            expect(eventBridge.dispatch).toHaveBeenCalledWith(objectId, 'submit', {
                values: { email: 'test@test.com' },
            });
        });
    });
});

// ============================================================================
// ComponentTerminal form integration tests
// ============================================================================

describe('ComponentTerminal form integration', () => {
    let terminal: ComponentTerminal;
    let formFactory: ComponentFactory;

    beforeEach(() => {
        const ws = createMockWebSocket();
        terminal = new ComponentTerminal(ws);
        formFactory = createMockFactory('wa-form');
        terminal.registerFactory('wa-form', formFactory);
    });

    afterEach(() => {
        cleanupContainer(formFactory);
    });

    it('should create a FormHandler when creating a wa-form component', () => {
        terminal.handleMessage({
            objectId: 100,
            type: 'create',
            framework: 'webcomponent',
            signature: 'wa-form',
            props: {},
        });

        const handler = terminal.getFormHandler(100);
        expect(handler).toBeDefined();
    });

    it('should not create a FormHandler for non-form components', () => {
        const inputFactory = createMockFactory('wa-input');
        terminal.registerFactory('wa-input', inputFactory);

        terminal.handleMessage({
            objectId: 200,
            type: 'create',
            framework: 'webcomponent',
            signature: 'wa-input',
            props: {},
        });

        const handler = terminal.getFormHandler(200);
        expect(handler).toBeUndefined();

        cleanupContainer(inputFactory);
    });

    it('should clean up FormHandler on destroy', () => {
        terminal.handleMessage({
            objectId: 100,
            type: 'create',
            framework: 'webcomponent',
            signature: 'wa-form',
            props: {},
        });

        expect(terminal.getFormHandler(100)).toBeDefined();

        terminal.handleMessage({ objectId: 100, type: 'destroy' });

        expect(terminal.getFormHandler(100)).toBeUndefined();
    });

    it('should handle serverErrors message for a form', () => {
        terminal.handleMessage({
            objectId: 100,
            type: 'create',
            framework: 'webcomponent',
            signature: 'wa-form',
            props: {},
        });

        // Add a mock input to the form element
        const adapter = terminal.getRegistry().get(100) as any;
        const formEl = adapter.getElement() as HTMLElement;
        const input = createMockInput('email', 'bad');
        formEl.appendChild(input);

        // Send server errors
        const msg: ComponentMessage = {
            objectId: 100,
            type: 'serverErrors',
            serverErrors: { email: ['Invalid email'] },
        };
        terminal.handleMessage(msg);

        expect((input as any).setCustomValidity).toHaveBeenCalledWith('Invalid email');
    });

    it('should warn when serverErrors targets unknown form', () => {
        const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => { });

        terminal.handleMessage({
            objectId: 999,
            type: 'serverErrors',
            serverErrors: { email: ['Error'] },
        });

        expect(warnSpy).toHaveBeenCalledWith('serverErrors for unknown form:', 999);
        warnSpy.mockRestore();
    });

    it('should warn when serverErrors message has no errors', () => {
        const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => { });

        terminal.handleMessage({
            objectId: 100,
            type: 'serverErrors',
        } as ComponentMessage);

        expect(warnSpy).toHaveBeenCalledWith('serverErrors message missing errors:', 100);
        warnSpy.mockRestore();
    });
});
