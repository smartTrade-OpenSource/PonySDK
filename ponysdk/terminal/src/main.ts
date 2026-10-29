/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

// The TypeScript terminal (docs/decisions/0002-javascript-terminal.md).
import { ClientToServerModel as C } from '#model';
import { log } from './runtime/log.ts';
import { Terminal } from './terminal.ts';

/**
 * The page is the application's own: the bootstrap page (its early socket), a window or a frame the server opened
 * (UI_CONTEXT_ID), or a custom page under the application's path. A host page of <pony-app> elements is none of these.
 */
function applicationPage(): boolean {
    return 'ponyEarlySocket' in window
        || new URLSearchParams(location.search).has(C.UI_CONTEXT_ID)
        || location.href.startsWith(new URL('..', import.meta.url).href);
}

// A host page of <pony-app> elements (docs/decisions/0008-embedded-application.md) loads the element at once; the
// application's page with the modules loaded on demand (loadAll)
if (!applicationPage()) {
    import('./pony-app.ts').then(m => m.definePonyApp(), e => log.error('<pony-app> did not load', e));
} else {
    const terminal = new Terminal();
    const loaded = (window as unknown as { onPonySDKModuleLoaded?: (terminal: Terminal) => void }).onPonySDKModuleLoaded;
    if (typeof loaded === 'function') {
        // ponysdk.js: it publishes window.pony, runs the onPonyLoaded listeners, then starts the terminal
        loaded(terminal);
    } else {
        log.info('No ponysdk.js in the page: the terminal starts itself');
        (window as unknown as { pony: Terminal }).pony = terminal;
        terminal.start();
    }
}

/**
 * Runs the application of src in a <pony-app> element that another PonySDK version defined: the contract of mount 1
 * (docs/decisions/0009-embedding-limits.md, decision 10). Frozen: its name, its arguments and its result stay.
 */
export function mountPonyApp(element: HTMLElement, src: string): Promise<{ stop(): void }> {
    return import('./pony-app.ts').then(m => m.mountPonyApp(element, src));
}

export * from './core-api.ts';
