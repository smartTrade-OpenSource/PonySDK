/**
 * Overlay controller for wa-dialog and wa-drawer components.
 *
 * Bridges the `open` prop from the server to the native Web Awesome
 * show()/hide() methods, which trigger the built-in open/close animations.
 *
 * When the element doesn't support show()/hide(), falls back to setting
 * the `open` attribute directly.
 *
 * Requirements: 5.1 - Provide PDialog overlay component
 * Requirements: 5.2 - Show dialog with blocking overlay when open=true
 * Requirements: 3.3 - Animate drawer open/close transitions
 */
/** Tags that the OverlayController manages */
const OVERLAY_TAGS = new Set(['wa-dialog', 'wa-drawer']);
/**
 * Controls open/close behavior for wa-dialog and wa-drawer elements.
 *
 * Usage:
 * ```ts
 * const controller = new OverlayController(element);
 * controller.attach();
 * // When props change, call:
 * controller.syncOpen(true);  // triggers show()
 * controller.syncOpen(false); // triggers hide()
 * // Cleanup:
 * controller.detach();
 * ```
 */
export class OverlayController {
    constructor(element) {
        this.attached = false;
        this.lastOpen = null;
        this.element = element;
    }
    /**
     * Attach the controller. Reads the current `open` state from the element.
     * Safe to call multiple times — only attaches once.
     */
    attach() {
        if (this.attached) {
            return;
        }
        this.lastOpen = this.isElementOpen();
        this.attached = true;
    }
    /**
     * Detach the controller and reset internal state.
     * Safe to call multiple times.
     */
    detach() {
        if (!this.attached) {
            return;
        }
        this.lastOpen = null;
        this.attached = false;
    }
    /**
     * Whether the controller is currently attached.
     */
    isAttached() {
        return this.attached;
    }
    /**
     * Sync the open state from server props to the native element.
     *
     * When `open` changes to `true`, calls `element.show()` to trigger
     * the native Web Awesome open animation with blocking overlay.
     * When `open` changes to `false`, calls `element.hide()` to trigger
     * the native close animation.
     *
     * If the element doesn't have show/hide methods, falls back to
     * setting the `open` property directly.
     *
     * @param open - The desired open state from server props
     */
    syncOpen(open) {
        if (!this.attached) {
            return;
        }
        // Skip if state hasn't changed
        if (this.lastOpen === open) {
            return;
        }
        this.lastOpen = open;
        const el = this.element;
        if (open) {
            if (typeof el.show === 'function') {
                el.show();
            }
            else {
                // Graceful fallback: set the open property/attribute directly
                el.open = true;
            }
        }
        else {
            if (typeof el.hide === 'function') {
                el.hide();
            }
            else {
                el.open = false;
            }
        }
    }
    /**
     * Check if this element is an overlay-type component (wa-dialog or wa-drawer).
     */
    static isOverlayElement(element) {
        return OVERLAY_TAGS.has(element.tagName.toLowerCase());
    }
    // ========== Internal ==========
    isElementOpen() {
        const el = this.element;
        return el.open === true;
    }
}
//# sourceMappingURL=OverlayController.js.map