/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

// The TypeScript terminal (docs/decisions/0002-javascript-terminal.md).
import { log } from './runtime/log.ts';
import { Terminal } from './terminal.ts';

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

export * from './core-api.ts';
