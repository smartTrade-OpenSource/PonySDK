/**
 * Toast queue system for displaying notifications in FIFO order.
 *
 * Creates `wa-alert` elements to display toasts using Web Awesome's
 * toast pattern. Each toast with a defined duration auto-closes after
 * that duration. When a toast closes, the next queued toast is displayed.
 *
 * Requirements: 5.4 - Toast queue displays notifications sequentially
 * Requirements: 5.5 - Auto-close toast after defined duration
 */
/**
 * Manages a FIFO queue of toast notifications.
 *
 * Usage:
 * ```ts
 * const queue = new ToastQueue();
 * queue.enqueue({ message: 'Saved!', variant: 'success', duration: 3000 });
 * queue.enqueue({ message: 'Error', variant: 'danger', closable: true });
 * ```
 */
export class ToastQueue {
    constructor(container) {
        this.queue = [];
        this.activeToast = null;
        this.autoCloseTimer = null;
        this.closeListener = null;
        this.container = container ?? document.body;
    }
    /**
     * Add a toast to the queue. If no toast is currently showing,
     * it displays immediately.
     */
    enqueue(toast) {
        this.queue.push(toast);
        if (!this.activeToast) {
            this.showNext();
        }
    }
    /**
     * Remove all pending and active toasts.
     */
    clear() {
        this.queue = [];
        this.dismissActive();
    }
    /**
     * Number of toasts waiting in the queue (not including the active toast).
     */
    getQueueLength() {
        return this.queue.length;
    }
    /**
     * Whether a toast is currently being displayed.
     */
    get isShowing() {
        return this.activeToast !== null;
    }
    // ========== Internal ==========
    showNext() {
        if (this.queue.length === 0) {
            return;
        }
        const toast = this.queue.shift();
        const el = this.createAlertElement(toast);
        this.activeToast = el;
        this.container.appendChild(el);
        // Listen for manual close (wa-close or wa-after-hide)
        this.closeListener = () => this.onToastClosed();
        el.addEventListener('wa-after-hide', this.closeListener);
        // Auto-close after duration
        if (toast.duration && toast.duration > 0) {
            this.autoCloseTimer = setTimeout(() => {
                this.dismissActive();
                this.showNext();
            }, toast.duration);
        }
    }
    createAlertElement(toast) {
        const el = document.createElement('wa-alert');
        if (toast.variant) {
            el.setAttribute('variant', toast.variant);
        }
        if (toast.icon) {
            el.setAttribute('icon', toast.icon);
        }
        if (toast.closable) {
            el.setAttribute('closable', '');
        }
        el.setAttribute('open', '');
        el.textContent = toast.message;
        return el;
    }
    onToastClosed() {
        this.clearTimer();
        this.removeActiveElement();
        this.showNext();
    }
    dismissActive() {
        this.clearTimer();
        this.removeActiveElement();
    }
    clearTimer() {
        if (this.autoCloseTimer !== null) {
            clearTimeout(this.autoCloseTimer);
            this.autoCloseTimer = null;
        }
    }
    removeActiveElement() {
        if (this.activeToast) {
            if (this.closeListener) {
                this.activeToast.removeEventListener('wa-after-hide', this.closeListener);
                this.closeListener = null;
            }
            if (this.activeToast.parentNode) {
                this.activeToast.parentNode.removeChild(this.activeToast);
            }
            this.activeToast = null;
        }
    }
}
//# sourceMappingURL=ToastQueue.js.map