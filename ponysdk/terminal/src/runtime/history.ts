/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import type { HistoryPort } from './dispatcher.ts';

/**
 * The history token: the URL fragment, decoded. As GWT's History, the token changes first, then the hash, so that
 * the hashchange this causes fires nothing; only the user's navigation and a newItem asked to do so fire.
 */
export class BrowserHistory implements HistoryPort {
    private current = decode(location.hash);
    private readonly listeners: ((token: string) => void)[] = [];

    constructor() {
        window.addEventListener('hashchange', () => {
            const token = decode(location.hash);
            if (token === this.current) return;
            this.current = token;
            this.fire(token);
        });
    }

    get token(): string {
        return this.current;
    }

    newItem(token: string, fireEvents: boolean): void {
        if (token === this.current) return;
        this.current = token;
        location.hash = encodeURI(token).replaceAll('#', '%23');
        if (fireEvents) this.fire(token);
    }

    fireCurrent(): void {
        this.fire(this.current);
    }

    onChange(listener: (token: string) => void): void {
        this.listeners.push(listener);
    }

    private fire(token: string): void {
        for (const listener of this.listeners) listener(token);
    }
}

function decode(hash: string): string {
    const token = hash.startsWith('#') ? hash.substring(1) : hash;
    try {
        return decodeURI(token.replaceAll('%23', '#'));
    } catch {
        return token;
    }
}
