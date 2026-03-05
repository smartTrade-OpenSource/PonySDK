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
/**
 * Manages lazy-loading of Web Awesome custom elements.
 *
 * Usage:
 * ```ts
 * const loader = new WebAwesomeLoader();
 * await loader.ensureDefined('wa-button');
 * ```
 */
export declare class WebAwesomeLoader {
    /** Per-tagName promises so we only wait once per element type. */
    private readonly pending;
    /** Configurable timeout (ms) for `ensureDefined`. */
    private readonly timeoutMs;
    constructor(timeoutMs?: number);
    /**
     * Returns `true` if the given tag name is already registered in the
     * custom elements registry.
     */
    isReady(tagName: string): boolean;
    /**
     * Waits until the custom element for `tagName` is defined.
     *
     * - If the element is already registered, resolves immediately.
     * - Otherwise waits via `customElements.whenDefined()` with a timeout.
     * - Rejects with an `Error` if the timeout expires.
     * - Deduplicates concurrent calls for the same tag name.
     */
    ensureDefined(tagName: string): Promise<void>;
    /**
     * Shows a lightweight placeholder element inside `container` while the
     * custom element is loading. The placeholder is automatically removed
     * once the element is defined (or on timeout).
     *
     * Returns the placeholder element so callers can customise it further.
     */
    showPlaceholder(container: HTMLElement): HTMLElement;
    /**
     * Removes a previously-added placeholder from its parent.
     */
    removePlaceholder(placeholder: HTMLElement): void;
    private waitForDefinition;
}
//# sourceMappingURL=WebAwesomeLoader.d.ts.map