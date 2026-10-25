/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { decode, type History } from './history.ts';

/**
 * The history of an application embedded in another page (ADR-0008): the token stays in the element, the page's
 * fragment is the host's. Only the server's new items change it.
 */
export class MemoryHistory implements History {
    private current: string;
    private readonly listeners: ((token: string) => void)[] = [];

    /** fragment: that of the element's src, the first token. */
    constructor(fragment: string) {
        this.current = decode(fragment);
    }

    get token(): string {
        return this.current;
    }

    newItem(token: string, fireEvents: boolean): void {
        if (token === this.current) return;
        this.current = token;
        if (fireEvents) this.fireCurrent();
    }

    fireCurrent(): void {
        for (const listener of this.listeners) listener(this.current);
    }

    onChange(listener: (token: string) => void): void {
        this.listeners.push(listener);
    }
}
