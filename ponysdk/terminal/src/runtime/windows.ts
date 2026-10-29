/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { describe, log } from './log.ts';
import type { ChildTerminal } from './object.ts';

/** A window opened by the server. */
export interface ChildWindow extends ChildTerminal {
    /** Its page said it is ready: the blocks for it are forwarded, not skipped. */
    readonly ready: boolean;
    /** Closed by its user, or never opened. */
    readonly closed: boolean;
    close(): void;
    /** Closed without an unload event: the server is told. */
    onClose(): void;
}

/** The windows opened by the server; the main window is the PBrowser object. */
export class Windows {
    mainWindowId = 0;
    private readonly windows = new Map<number, ChildWindow>();

    get(id: number): ChildWindow | undefined {
        return this.windows.get(id);
    }

    register(id: number, window: ChildWindow): void {
        this.windows.set(id, window);
    }

    unregister(id: number): void {
        this.windows.delete(id);
    }

    all(): ChildWindow[] {
        return [...this.windows.values()];
    }

    closeAll(): void {
        for (const window of this.all().reverse()) {
            try {
                window.close();
            } catch (e) {
                log.warn(`Cannot close a window: ${describe(e)}`);
            }
        }
    }
}

