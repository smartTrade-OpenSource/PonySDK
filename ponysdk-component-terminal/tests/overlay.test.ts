/**
 * Unit tests for OverlayController.
 *
 * Requirements: 5.1 - Provide PDialog overlay component
 * Requirements: 5.2 - Show dialog with blocking overlay when open=true
 * Requirements: 3.3 - Animate drawer open/close transitions
 */

import { describe, it, expect, vi } from 'vitest';
import { OverlayController } from '../src/overlay/OverlayController.js';

// ============================================================================
// Test Helpers
// ============================================================================

/**
 * Create a mock wa-dialog or wa-drawer element with show/hide methods.
 */
function createOverlayElement(tagName: string, initialOpen = false): HTMLElement & {
    show: ReturnType<typeof vi.fn>;
    hide: ReturnType<typeof vi.fn>;
    open: boolean;
} {
    const el = document.createElement(tagName);
    const mock = el as HTMLElement & {
        show: ReturnType<typeof vi.fn>;
        hide: ReturnType<typeof vi.fn>;
        open: boolean;
    };
    mock.show = vi.fn(() => { mock.open = true; });
    mock.hide = vi.fn(() => { mock.open = false; });
    Object.defineProperty(mock, 'open', { value: initialOpen, writable: true, configurable: true });
    return mock;
}

/**
 * Create a plain element without show/hide methods (fallback scenario).
 */
function createPlainElement(tagName: string, initialOpen = false): HTMLElement & { open: boolean } {
    const el = document.createElement(tagName);
    const mock = el as HTMLElement & { open: boolean };
    Object.defineProperty(mock, 'open', { value: initialOpen, writable: true, configurable: true });
    return mock;
}

// ============================================================================
// Tests
// ============================================================================

describe('OverlayController', () => {

    describe('wa-dialog with show/hide', () => {
        it('should call show() when open changes to true', () => {
            const el = createOverlayElement('wa-dialog');
            const controller = new OverlayController(el);
            controller.attach();

            controller.syncOpen(true);

            expect(el.show).toHaveBeenCalledOnce();
            expect(el.hide).not.toHaveBeenCalled();
        });

        it('should call hide() when open changes to false', () => {
            const el = createOverlayElement('wa-dialog', true);
            const controller = new OverlayController(el);
            controller.attach();

            controller.syncOpen(false);

            expect(el.hide).toHaveBeenCalledOnce();
            expect(el.show).not.toHaveBeenCalled();
        });

        it('should not call show() twice for same open=true', () => {
            const el = createOverlayElement('wa-dialog');
            const controller = new OverlayController(el);
            controller.attach();

            controller.syncOpen(true);
            controller.syncOpen(true);

            expect(el.show).toHaveBeenCalledOnce();
        });

        it('should not call hide() twice for same open=false', () => {
            const el = createOverlayElement('wa-dialog', true);
            const controller = new OverlayController(el);
            controller.attach();

            controller.syncOpen(false);
            controller.syncOpen(false);

            expect(el.hide).toHaveBeenCalledOnce();
        });

        it('should handle open→close→open cycle', () => {
            const el = createOverlayElement('wa-dialog');
            const controller = new OverlayController(el);
            controller.attach();

            controller.syncOpen(true);
            controller.syncOpen(false);
            controller.syncOpen(true);

            expect(el.show).toHaveBeenCalledTimes(2);
            expect(el.hide).toHaveBeenCalledTimes(1);
        });
    });

    describe('wa-drawer with show/hide', () => {
        it('should call show() when open changes to true', () => {
            const el = createOverlayElement('wa-drawer');
            const controller = new OverlayController(el);
            controller.attach();

            controller.syncOpen(true);

            expect(el.show).toHaveBeenCalledOnce();
        });

        it('should call hide() when open changes to false', () => {
            const el = createOverlayElement('wa-drawer', true);
            const controller = new OverlayController(el);
            controller.attach();

            controller.syncOpen(false);

            expect(el.hide).toHaveBeenCalledOnce();
        });
    });

    describe('fallback (no show/hide methods)', () => {
        it('should set open=true when element lacks show()', () => {
            const el = createPlainElement('wa-dialog');
            const controller = new OverlayController(el);
            controller.attach();

            controller.syncOpen(true);

            expect(el.open).toBe(true);
        });

        it('should set open=false when element lacks hide()', () => {
            const el = createPlainElement('wa-dialog', true);
            const controller = new OverlayController(el);
            controller.attach();

            controller.syncOpen(false);

            expect(el.open).toBe(false);
        });
    });

    describe('lifecycle', () => {
        it('should not sync before attach', () => {
            const el = createOverlayElement('wa-dialog');
            const controller = new OverlayController(el);

            controller.syncOpen(true);

            expect(el.show).not.toHaveBeenCalled();
        });

        it('should not sync after detach', () => {
            const el = createOverlayElement('wa-dialog');
            const controller = new OverlayController(el);
            controller.attach();
            controller.detach();

            controller.syncOpen(true);

            expect(el.show).not.toHaveBeenCalled();
        });

        it('attach should be idempotent', () => {
            const el = createOverlayElement('wa-dialog');
            const controller = new OverlayController(el);
            controller.attach();
            controller.attach();

            controller.syncOpen(true);

            expect(el.show).toHaveBeenCalledOnce();
        });

        it('detach should be idempotent', () => {
            const el = createOverlayElement('wa-dialog');
            const controller = new OverlayController(el);
            controller.attach();
            controller.detach();
            controller.detach();

            expect(controller.isAttached()).toBe(false);
        });

        it('isAttached should reflect state', () => {
            const el = createOverlayElement('wa-dialog');
            const controller = new OverlayController(el);

            expect(controller.isAttached()).toBe(false);
            controller.attach();
            expect(controller.isAttached()).toBe(true);
            controller.detach();
            expect(controller.isAttached()).toBe(false);
        });

        it('should allow re-attach after detach', () => {
            const el = createOverlayElement('wa-dialog');
            const controller = new OverlayController(el);
            controller.attach();
            controller.syncOpen(true);
            controller.detach();

            controller.attach();
            // After re-attach, lastOpen is read from element (which is now true)
            // So syncing true again should be a no-op
            controller.syncOpen(true);
            expect(el.show).toHaveBeenCalledOnce();

            // But syncing false should work
            controller.syncOpen(false);
            expect(el.hide).toHaveBeenCalledOnce();
        });
    });

    describe('isOverlayElement', () => {
        it('should return true for wa-dialog', () => {
            const el = document.createElement('wa-dialog');
            expect(OverlayController.isOverlayElement(el)).toBe(true);
        });

        it('should return true for wa-drawer', () => {
            const el = document.createElement('wa-drawer');
            expect(OverlayController.isOverlayElement(el)).toBe(true);
        });

        it('should return false for wa-input', () => {
            const el = document.createElement('wa-input');
            expect(OverlayController.isOverlayElement(el)).toBe(false);
        });

        it('should return false for div', () => {
            const el = document.createElement('div');
            expect(OverlayController.isOverlayElement(el)).toBe(false);
        });
    });
});
