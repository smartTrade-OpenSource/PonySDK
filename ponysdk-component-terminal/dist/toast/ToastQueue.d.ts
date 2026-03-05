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
/** Options for creating a toast notification. */
export interface ToastOptions {
    message: string;
    variant?: 'primary' | 'success' | 'neutral' | 'warning' | 'danger';
    duration?: number;
    icon?: string;
    closable?: boolean;
}
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
export declare class ToastQueue {
    private queue;
    private activeToast;
    private autoCloseTimer;
    private container;
    private closeListener;
    constructor(container?: HTMLElement);
    /**
     * Add a toast to the queue. If no toast is currently showing,
     * it displays immediately.
     */
    enqueue(toast: ToastOptions): void;
    /**
     * Remove all pending and active toasts.
     */
    clear(): void;
    /**
     * Number of toasts waiting in the queue (not including the active toast).
     */
    getQueueLength(): number;
    /**
     * Whether a toast is currently being displayed.
     */
    get isShowing(): boolean;
    private showNext;
    private createAlertElement;
    private onToastClosed;
    private dismissActive;
    private clearTimer;
    private removeActiveElement;
}
//# sourceMappingURL=ToastQueue.d.ts.map