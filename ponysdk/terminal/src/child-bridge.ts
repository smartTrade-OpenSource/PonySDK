/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { applyWindowOperation } from './objects/windows.ts';
import type { ToChild, ToParent } from './runtime/embedding.ts';
import { log } from './runtime/log.ts';
import type { ParentTerminal, Terminal } from './terminal.ts';

/**
 * A window or a frame of an embedded terminal (docs/decisions/0009-embedding-limits.md, decision 6): its parent runs
 * in a page of another origin, or in an element of a page whose window.pony is not that terminal. They talk by
 * postMessage only. The parent is accepted when its origin is this page's or one the server lists in the meta
 * pony-parent-origins (wsAllowedOrigins): any other page could open this one and feed it. Nothing goes to the parent
 * before it is accepted.
 */
export function bridgeToParent(terminal: Terminal, source: Window, windowId: string | null, frameId: string): void {
    const listed = document.querySelector<HTMLMetaElement>('meta[name="pony-parent-origins"]')?.content ?? '';
    const allowed = new Set([location.origin, ...listed.split(/\s+/).filter(origin => origin !== '')]);
    /** The parent's origin, once its init came from an allowed one. */
    let origin: string | null = null;
    const entries = new Map<number, string>();
    const queued: string[] = [];
    const post = (message: ToParent, target: string) => source.postMessage(message, target);

    const parent: ParentTerminal = {
        // "ready" says nothing, and goes before the parent is known: its init answers it
        setReadyWindow: () => post({ pony: 'ready' }, '*'),
        setReadyFrame: () => post({ pony: 'ready' }, '*'),
        sendDataToServerFromWindow: json => {
            if (origin === null) queued.push(json);
            else post({ pony: 'send', message: json }, origin);
        },
        dictionaryEntry: id => entries.get(id),
    };

    window.addEventListener('message', event => {
        if (event.source !== source || event.data instanceof Uint8Array) return; // the blocks: the terminal's
        const message = event.data as ToChild;
        if (message?.pony === 'init') {
            if (!allowed.has(event.origin)) {
                log.error(`A page of ${event.origin} opened this one: not a parent of this application`);
                return;
            }
            origin = event.origin;
            terminal.useKeys(message.keys);
            if (message.title !== null) document.title = message.title;
            for (const [id, value] of message.entries) entries.set(id, value);
            for (const json of queued.splice(0)) post({ pony: 'send', message: json }, origin);
            return;
        }
        if (event.origin !== origin) return;
        if (message?.pony === 'entries') for (const [id, value] of message.entries) entries.set(id, value);
        else if (message?.pony === 'window') applyWindowOperation(window, message.operation);
    });
    window.addEventListener('pagehide', () => {
        if (origin !== null) post({ pony: 'closing' }, origin);
    });
    terminal.attachToParent(parent, source, windowId, frameId, event => event.origin === origin);
}
