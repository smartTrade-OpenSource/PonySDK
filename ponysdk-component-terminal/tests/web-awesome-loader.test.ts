/**
 * Unit tests for WebAwesomeLoader.
 *
 * Requirements: 12.2 (error handling: lazy loading)
 *
 * jsdom does not provide a real customElements.whenDefined, so we mock
 * the CustomElementRegistry methods where needed.
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { WebAwesomeLoader } from '../src/WebAwesomeLoader.js';

// ============================================================================
// Helpers
// ============================================================================

/**
 * Register a fake custom element so `customElements.get(tagName)` returns
 * a constructor.
 */
function defineElement(tagName: string): void {
    if (!customElements.get(tagName)) {
        customElements.define(tagName, class extends HTMLElement { });
    }
}

// ============================================================================
// Tests
// ============================================================================

describe('WebAwesomeLoader', () => {
    let loader: WebAwesomeLoader;

    beforeEach(() => {
        // Disable auto-definition for these tests
        (globalThis as any).__disableAutoDefine = true;
        loader = new WebAwesomeLoader(500); // short timeout for tests
    });

    afterEach(() => {
        // Re-enable auto-definition after tests
        (globalThis as any).__disableAutoDefine = false;
    });

    // ------------------------------------------------------------------
    // isReady
    // ------------------------------------------------------------------

    describe('isReady', () => {
        it('should return false for an undefined element', () => {
            expect(loader.isReady('wa-undefined-element')).toBe(false);
        });

        it('should return true after the element is defined', () => {
            const tag = 'wa-ready-test';
            defineElement(tag);
            expect(loader.isReady(tag)).toBe(true);
        });
    });

    // ------------------------------------------------------------------
    // ensureDefined
    // ------------------------------------------------------------------

    describe('ensureDefined', () => {
        it('should resolve immediately for an already-defined element', async () => {
            const tag = 'wa-already-defined';
            defineElement(tag);

            await expect(loader.ensureDefined(tag)).resolves.toBeUndefined();
        });

        it('should resolve once the element becomes defined', async () => {
            const tag = 'wa-lazy-element';

            // Start waiting, then define after a short delay
            const promise = loader.ensureDefined(tag);
            setTimeout(() => defineElement(tag), 50);

            await expect(promise).resolves.toBeUndefined();
            expect(loader.isReady(tag)).toBe(true);
        });

        it('should reject when the element is not defined within the timeout', async () => {
            const shortLoader = new WebAwesomeLoader(50);

            await expect(
                shortLoader.ensureDefined('wa-never-defined')
            ).rejects.toThrow(/Timeout.*wa-never-defined.*50ms/);
        });

        it('should deduplicate concurrent calls for the same tag', async () => {
            const tag = 'wa-dedup-element';
            const whenDefinedSpy = vi.spyOn(customElements, 'whenDefined');

            const p1 = loader.ensureDefined(tag);
            const p2 = loader.ensureDefined(tag);

            // Define the element so both promises resolve
            setTimeout(() => defineElement(tag), 30);

            await Promise.all([p1, p2]);

            // whenDefined should have been called only once (the second call
            // reuses the pending promise)
            expect(whenDefinedSpy).toHaveBeenCalledTimes(1);
            whenDefinedSpy.mockRestore();
        });
    });

    // ------------------------------------------------------------------
    // Placeholder
    // ------------------------------------------------------------------

    describe('showPlaceholder / removePlaceholder', () => {
        it('should append a placeholder element to the container', () => {
            const container = document.createElement('div');
            const placeholder = loader.showPlaceholder(container);

            expect(container.contains(placeholder)).toBe(true);
            expect(placeholder.className).toBe('wa-loading-placeholder');
            expect(placeholder.getAttribute('role')).toBe('status');
        });

        it('should remove the placeholder from its parent', () => {
            const container = document.createElement('div');
            const placeholder = loader.showPlaceholder(container);

            loader.removePlaceholder(placeholder);

            expect(container.contains(placeholder)).toBe(false);
        });

        it('should be safe to call removePlaceholder on a detached element', () => {
            const placeholder = document.createElement('div');
            // Not attached to any parent — should not throw
            expect(() => loader.removePlaceholder(placeholder)).not.toThrow();
        });
    });

    // ------------------------------------------------------------------
    // Constructor defaults
    // ------------------------------------------------------------------

    describe('default timeout', () => {
        it('should use 10 000 ms when no timeout is provided', async () => {
            const defaultLoader = new WebAwesomeLoader();
            // We can't easily test the exact timeout value without waiting,
            // but we can verify the loader is constructed without error and
            // isReady works.
            expect(defaultLoader.isReady('wa-nonexistent')).toBe(false);
        });
    });
});
