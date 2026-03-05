/**
 * End-to-end integration tests for the props update flow.
 *
 * Tests the full pipeline:
 *   Server setProps() → PropsDiffer → JSON Patch → WebSocket → ComponentTerminal
 *   → ComponentRegistry → WebComponentAdapter → wa-* element property update
 *
 * Covers representative components: wa-input, wa-button, wa-dialog, wa-select, wa-data-table
 *
 * Validates: Requirements 1.2, 2.2, 12.1, 12.4, 12.5
 */

import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ComponentTerminal } from '../src/ComponentTerminal.js';
import { WebComponentAdapter } from '../src/adapters/WebComponentAdapter.js';
import type { ComponentFactory, ComponentMessage } from '../src/types.js';

// ============================================================================
// Test Helpers
// ============================================================================

function createMockWebSocket(): WebSocket {
    return {
        send: () => { },
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

function getElementProp(adapter: WebComponentAdapter<unknown>, prop: string): unknown {
    const el = adapter.getElement();
    return el ? (el as unknown as Record<string, unknown>)[prop] : undefined;
}

// ============================================================================
// End-to-end props update flow
// ============================================================================

describe('End-to-end props update flow', () => {
    let terminal: ComponentTerminal;
    const factories = new Map<string, ComponentFactory>();

    const TAGS = ['wa-input', 'wa-button', 'wa-dialog', 'wa-select', 'wa-data-table'];

    beforeEach(() => {
        terminal = new ComponentTerminal(createMockWebSocket());
        for (const tag of TAGS) {
            const factory = createMockFactory(tag);
            factories.set(tag, factory);
            terminal.registerFactory(tag, factory);
        }
    });

    afterEach(() => {
        // Destroy all components
        for (const tag of TAGS) {
            const factory = factories.get(tag)!;
            cleanupContainer(factory);
        }
        factories.clear();
    });

    // Helper: create a component via terminal message and return its adapter
    function createComponent(objectId: number, signature: string, props: Record<string, unknown>): WebComponentAdapter<unknown> {
        terminal.handleMessage({
            objectId,
            type: 'create',
            framework: 'webcomponent',
            signature,
            props,
        });
        return terminal.getRegistry().get(objectId) as WebComponentAdapter<unknown>;
    }

    // ========================================================================
    // 1. wa-input: create with initial props, update via setProps, patch
    // ========================================================================
    describe('wa-input', () => {
        const OID = 100;

        afterEach(() => {
            terminal.handleMessage({ objectId: OID, type: 'destroy' });
        });

        it('should create element with initial props', () => {
            const adapter = createComponent(OID, 'wa-input', {
                value: 'hello',
                disabled: false,
                label: 'Email',
            });

            expect(adapter).toBeDefined();
            expect(adapter.isMounted()).toBe(true);
            expect(getElementProp(adapter, 'value')).toBe('hello');
            expect(getElementProp(adapter, 'disabled')).toBe(false);
            expect(getElementProp(adapter, 'label')).toBe('Email');
        });

        it('should update props via full update message', () => {
            const adapter = createComponent(OID, 'wa-input', {
                value: 'initial',
                disabled: false,
                label: 'Name',
            });

            terminal.handleMessage({
                objectId: OID,
                type: 'update',
                props: { value: 'updated', disabled: true, label: 'Full Name' },
            });

            expect(getElementProp(adapter, 'value')).toBe('updated');
            expect(getElementProp(adapter, 'disabled')).toBe(true);
            expect(getElementProp(adapter, 'label')).toBe('Full Name');
        });

        it('should apply JSON Patch to update only changed properties', () => {
            const adapter = createComponent(OID, 'wa-input', {
                value: 'old',
                disabled: false,
                label: 'Search',
            });

            terminal.handleMessage({
                objectId: OID,
                type: 'update',
                patches: [
                    { op: 'replace', path: '/value', value: 'new-value' },
                    { op: 'replace', path: '/disabled', value: true },
                ],
            });

            expect(getElementProp(adapter, 'value')).toBe('new-value');
            expect(getElementProp(adapter, 'disabled')).toBe(true);
            // label should remain unchanged
            expect(getElementProp(adapter, 'label')).toBe('Search');
        });
    });

    // ========================================================================
    // 2. wa-button: variant and disabled updates
    // ========================================================================
    describe('wa-button', () => {
        const OID = 200;

        afterEach(() => {
            terminal.handleMessage({ objectId: OID, type: 'destroy' });
        });

        it('should create element with initial props', () => {
            const adapter = createComponent(OID, 'wa-button', {
                variant: 'primary',
                disabled: false,
            });

            expect(adapter.isMounted()).toBe(true);
            expect(getElementProp(adapter, 'variant')).toBe('primary');
            expect(getElementProp(adapter, 'disabled')).toBe(false);
        });

        it('should update variant via full props update', () => {
            const adapter = createComponent(OID, 'wa-button', {
                variant: 'primary',
                disabled: false,
            });

            terminal.handleMessage({
                objectId: OID,
                type: 'update',
                props: { variant: 'danger', disabled: true },
            });

            expect(getElementProp(adapter, 'variant')).toBe('danger');
            expect(getElementProp(adapter, 'disabled')).toBe(true);
        });

        it('should apply JSON Patch for variant change only', () => {
            const adapter = createComponent(OID, 'wa-button', {
                variant: 'neutral',
                disabled: false,
            });

            terminal.handleMessage({
                objectId: OID,
                type: 'update',
                patches: [{ op: 'replace', path: '/variant', value: 'success' }],
            });

            expect(getElementProp(adapter, 'variant')).toBe('success');
            expect(getElementProp(adapter, 'disabled')).toBe(false);
        });
    });

    // ========================================================================
    // 3. wa-dialog: open state toggle
    // ========================================================================
    describe('wa-dialog', () => {
        const OID = 300;

        afterEach(() => {
            terminal.handleMessage({ objectId: OID, type: 'destroy' });
        });

        it('should create dialog with open=false', () => {
            const adapter = createComponent(OID, 'wa-dialog', {
                open: false,
                label: 'Confirm',
            });

            expect(adapter.isMounted()).toBe(true);
            expect(getElementProp(adapter, 'open')).toBe(false);
        });

        it('should open dialog via full props update', () => {
            const adapter = createComponent(OID, 'wa-dialog', {
                open: false,
                label: 'Confirm',
            });

            terminal.handleMessage({
                objectId: OID,
                type: 'update',
                props: { open: true, label: 'Confirm' },
            });

            expect(getElementProp(adapter, 'open')).toBe(true);
        });

        it('should toggle open via JSON Patch', () => {
            const adapter = createComponent(OID, 'wa-dialog', {
                open: false,
                label: 'Delete?',
            });

            // Open
            terminal.handleMessage({
                objectId: OID,
                type: 'update',
                patches: [{ op: 'replace', path: '/open', value: true }],
            });
            expect(getElementProp(adapter, 'open')).toBe(true);

            // Close
            terminal.handleMessage({
                objectId: OID,
                type: 'update',
                patches: [{ op: 'replace', path: '/open', value: false }],
            });
            expect(getElementProp(adapter, 'open')).toBe(false);
            // label unchanged
            expect(getElementProp(adapter, 'label')).toBe('Delete?');
        });
    });

    // ========================================================================
    // 4. wa-select: value update
    // ========================================================================
    describe('wa-select', () => {
        const OID = 400;

        afterEach(() => {
            terminal.handleMessage({ objectId: OID, type: 'destroy' });
        });

        it('should create select with initial value', () => {
            const adapter = createComponent(OID, 'wa-select', {
                value: 'opt1',
                placeholder: 'Choose...',
                disabled: false,
            });

            expect(adapter.isMounted()).toBe(true);
            expect(getElementProp(adapter, 'value')).toBe('opt1');
            expect(getElementProp(adapter, 'placeholder')).toBe('Choose...');
        });

        it('should update value via full props', () => {
            const adapter = createComponent(OID, 'wa-select', {
                value: 'opt1',
                placeholder: 'Choose...',
                disabled: false,
            });

            terminal.handleMessage({
                objectId: OID,
                type: 'update',
                props: { value: 'opt3', placeholder: 'Choose...', disabled: false },
            });

            expect(getElementProp(adapter, 'value')).toBe('opt3');
        });

        it('should patch value only via JSON Patch', () => {
            const adapter = createComponent(OID, 'wa-select', {
                value: 'a',
                placeholder: 'Pick',
                disabled: false,
            });

            terminal.handleMessage({
                objectId: OID,
                type: 'update',
                patches: [{ op: 'replace', path: '/value', value: 'b' }],
            });

            expect(getElementProp(adapter, 'value')).toBe('b');
            expect(getElementProp(adapter, 'placeholder')).toBe('Pick');
            expect(getElementProp(adapter, 'disabled')).toBe(false);
        });
    });

    // ========================================================================
    // 5. wa-data-table: columns and data updates
    // ========================================================================
    describe('wa-data-table', () => {
        const OID = 500;

        afterEach(() => {
            terminal.handleMessage({ objectId: OID, type: 'destroy' });
        });

        it('should create data-table with initial columns and data', () => {
            const initialProps = {
                columns: [
                    { field: 'name', header: 'Name', sortable: true },
                    { field: 'age', header: 'Age', sortable: false },
                ],
                data: [
                    { name: 'Alice', age: 30 },
                    { name: 'Bob', age: 25 },
                ],
                page: 1,
                pageSize: 10,
            };

            const adapter = createComponent(OID, 'wa-data-table', initialProps);

            expect(adapter.isMounted()).toBe(true);
            const data = getElementProp(adapter, 'data') as Array<Record<string, unknown>>;
            expect(data).toHaveLength(2);
            expect(data[0].name).toBe('Alice');
        });

        it('should update data rows via full props', () => {
            const adapter = createComponent(OID, 'wa-data-table', {
                columns: [{ field: 'id', header: 'ID' }],
                data: [{ id: 1 }, { id: 2 }],
                page: 1,
                pageSize: 10,
            });

            terminal.handleMessage({
                objectId: OID,
                type: 'update',
                props: {
                    columns: [{ field: 'id', header: 'ID' }],
                    data: [{ id: 1 }, { id: 2 }, { id: 3 }],
                    page: 1,
                    pageSize: 10,
                },
            });

            const data = getElementProp(adapter, 'data') as Array<Record<string, unknown>>;
            expect(data).toHaveLength(3);
            expect(data[2].id).toBe(3);
        });

        it('should patch individual row data via JSON Patch', () => {
            const adapter = createComponent(OID, 'wa-data-table', {
                columns: [{ field: 'name', header: 'Name' }],
                data: [{ name: 'Alice' }, { name: 'Bob' }],
                page: 1,
                pageSize: 10,
            });

            terminal.handleMessage({
                objectId: OID,
                type: 'update',
                patches: [
                    { op: 'replace', path: '/data/1/name', value: 'Charlie' },
                ],
            });

            const data = getElementProp(adapter, 'data') as Array<Record<string, unknown>>;
            expect(data[0].name).toBe('Alice'); // unchanged
            expect(data[1].name).toBe('Charlie'); // patched
        });

        it('should update page via JSON Patch', () => {
            const adapter = createComponent(OID, 'wa-data-table', {
                columns: [],
                data: [],
                page: 1,
                pageSize: 10,
            });

            terminal.handleMessage({
                objectId: OID,
                type: 'update',
                patches: [{ op: 'replace', path: '/page', value: 3 }],
            });

            expect(getElementProp(adapter, 'page')).toBe(3);
            expect(getElementProp(adapter, 'pageSize')).toBe(10);
        });
    });

    // ========================================================================
    // 6. Cross-component: verify registry tracks all components
    // ========================================================================
    describe('registry integration', () => {
        const ids = [601, 602, 603, 604, 605];

        afterEach(() => {
            for (const id of ids) {
                terminal.handleMessage({ objectId: id, type: 'destroy' });
            }
        });

        it('should track multiple components simultaneously', () => {
            createComponent(601, 'wa-input', { value: '' });
            createComponent(602, 'wa-button', { variant: 'primary' });
            createComponent(603, 'wa-dialog', { open: false });
            createComponent(604, 'wa-select', { value: '' });
            createComponent(605, 'wa-data-table', { columns: [], data: [] });

            const registry = terminal.getRegistry();
            expect(registry.size).toBe(5);

            for (const id of ids) {
                expect(registry.has(id)).toBe(true);
                const adapter = registry.get(id) as WebComponentAdapter<unknown>;
                expect(adapter.isMounted()).toBe(true);
            }
        });

        it('should update each component independently via patches', () => {
            const inputAdapter = createComponent(601, 'wa-input', { value: 'a', label: 'X' });
            const buttonAdapter = createComponent(602, 'wa-button', { variant: 'primary', disabled: false });

            // Patch input
            terminal.handleMessage({
                objectId: 601,
                type: 'update',
                patches: [{ op: 'replace', path: '/value', value: 'b' }],
            });

            // Patch button
            terminal.handleMessage({
                objectId: 602,
                type: 'update',
                patches: [{ op: 'replace', path: '/disabled', value: true }],
            });

            expect(getElementProp(inputAdapter, 'value')).toBe('b');
            expect(getElementProp(inputAdapter, 'label')).toBe('X'); // unchanged
            expect(getElementProp(buttonAdapter, 'variant')).toBe('primary'); // unchanged
            expect(getElementProp(buttonAdapter, 'disabled')).toBe(true);
        });
    });

    // ========================================================================
    // 7. Edge cases: update on unknown component, update on destroyed component
    // ========================================================================
    describe('edge cases', () => {
        it('should ignore update for unknown objectId', () => {
            expect(() => {
                terminal.handleMessage({
                    objectId: 9999,
                    type: 'update',
                    props: { value: 'ghost' },
                });
            }).not.toThrow();
        });

        it('should ignore update after component is destroyed', () => {
            createComponent(700, 'wa-input', { value: 'alive' });
            terminal.handleMessage({ objectId: 700, type: 'destroy' });

            expect(() => {
                terminal.handleMessage({
                    objectId: 700,
                    type: 'update',
                    props: { value: 'dead' },
                });
            }).not.toThrow();
        });

        it('should handle add property via JSON Patch', () => {
            const adapter = createComponent(701, 'wa-input', { value: 'x' });

            terminal.handleMessage({
                objectId: 701,
                type: 'update',
                patches: [{ op: 'add', path: '/placeholder', value: 'Type here...' }],
            });

            expect(getElementProp(adapter, 'placeholder')).toBe('Type here...');
            expect(getElementProp(adapter, 'value')).toBe('x');

            terminal.handleMessage({ objectId: 701, type: 'destroy' });
        });
    });
});
