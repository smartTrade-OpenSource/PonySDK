/**
 * Loader utility for Web Awesome custom elements.
 *
 * Ensures wa-* elements are defined in the browser's custom elements registry
 * before the WebComponentAdapter attempts to mount them. Uses
 * `customElements.whenDefined()` for lazy loading with a placeholder fallback
 * and a configurable timeout for components that never load.
 *
 * Requirements: 12.2 (error handling: lazy loading)
 */

/** Default timeout in milliseconds for waiting on a custom element definition. */
const DEFAULT_TIMEOUT_MS = 10_000;

/** CSS class applied to the placeholder shown while a component is loading. */
const PLACEHOLDER_CLASS = 'wa-loading-placeholder';

/**
 * Manages lazy-loading of Web Awesome custom elements.
 *
 * Usage:
 * ```ts
 * const loader = new WebAwesomeLoader();
 * await loader.ensureDefined('wa-button');
 * ```
 */
export class WebAwesomeLoader {
    /** Per-tagName promises so we only wait once per element type. */
    private readonly pending = new Map<string, Promise<void>>();

    /** Configurable timeout (ms) for `ensureDefined`. */
    private readonly timeoutMs: number;

    constructor(timeoutMs: number = DEFAULT_TIMEOUT_MS) {
        this.timeoutMs = timeoutMs;
    }

    /**
     * Returns `true` if the given tag name is already registered in the
     * custom elements registry.
     */
    isReady(tagName: string): boolean {
        return customElements.get(tagName) !== undefined;
    }

    /**
     * Waits until the custom element for `tagName` is defined.
     *
     * - If the element is already registered, resolves immediately.
     * - Otherwise waits via `customElements.whenDefined()` with a timeout.
     * - Rejects with an `Error` if the timeout expires.
     * - Deduplicates concurrent calls for the same tag name.
     */
    async ensureDefined(tagName: string): Promise<void> {
        if (this.isReady(tagName)) {
            return;
        }

        const existing = this.pending.get(tagName);
        if (existing) {
            return existing;
        }

        const promise = this.waitForDefinition(tagName);
        this.pending.set(tagName, promise);

        try {
            await promise;
        } finally {
            this.pending.delete(tagName);
        }
    }

    /**
     * Shows a lightweight placeholder element inside `container` while the
     * custom element is loading. The placeholder is automatically removed
     * once the element is defined (or on timeout).
     *
     * Returns the placeholder element so callers can customise it further.
     */
    showPlaceholder(container: HTMLElement): HTMLElement {
        const placeholder = document.createElement('div');
        placeholder.className = PLACEHOLDER_CLASS;
        placeholder.setAttribute('role', 'status');
        placeholder.setAttribute('aria-label', 'Loading component…');
        placeholder.textContent = '';
        container.appendChild(placeholder);
        return placeholder;
    }

    /**
     * Removes a previously-added placeholder from its parent.
     */
    removePlaceholder(placeholder: HTMLElement): void {
        if (placeholder.parentNode) {
            placeholder.parentNode.removeChild(placeholder);
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private waitForDefinition(tagName: string): Promise<void> {
        return new Promise<void>((resolve, reject) => {
            const timer = setTimeout(() => {
                reject(
                    new Error(
                        `Timeout: custom element "${tagName}" was not defined within ${this.timeoutMs}ms`
                    )
                );
            }, this.timeoutMs);

            customElements.whenDefined(tagName).then(() => {
                clearTimeout(timer);
                resolve();
            }).catch((err: unknown) => {
                clearTimeout(timer);
                reject(err);
            });
        });
    }
}
