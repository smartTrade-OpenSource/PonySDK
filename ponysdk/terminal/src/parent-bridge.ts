/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import type { BridgedChild, ChildBridge, ToChild, ToParent } from './runtime/embedding.ts';
import type { WindowOperation } from './objects/windows.ts';
import type { Terminal } from './terminal.ts';

/**
 * The windows and frames of an embedded terminal, as their parent sees them (docs/decisions/0009-embedding-limits.md,
 * decision 6). Their pages are the application's, maybe of another origin than this page: they are talked to by
 * postMessage only, to the application's origin, never to any. A child is known by the window its messages come from.
 */
export class ParentBridge implements ChildBridge {
    private readonly terminal: Terminal;
    /** The application's origin: the child pages'. */
    private readonly origin: string;
    /** Each child, with the dictionary entries it has not received yet; null until it said it is ready. */
    private readonly children = new Map<BridgedChild, Set<number> | null>();
    /** The window operations asked before the child was ready: run once it is. */
    private readonly operations = new Map<BridgedChild, WindowOperation[]>();

    constructor(terminal: Terminal, origin: string, signal: AbortSignal) {
        this.terminal = terminal;
        this.origin = origin;
        window.addEventListener('message', event => this.received(event), { signal });
    }

    register(child: BridgedChild): void {
        this.children.set(child, null);
        this.terminal.dispatcher.reader.onEntry = id => {
            for (const pending of this.children.values()) pending?.add(id);
        };
    }

    unregister(child: BridgedChild): void {
        this.children.delete(child);
        this.operations.delete(child);
        if (this.children.size === 0) this.terminal.dispatcher.reader.onEntry = undefined;
    }

    post(child: BridgedChild, bytes: Uint8Array): void {
        const target = child.target;
        const pending = this.children.get(child);
        if (target === null || pending === undefined || pending === null) return;
        if (pending.size > 0) {
            this.send(target, { pony: 'entries', entries: this.entries(pending) });
            pending.clear();
        }
        target.postMessage(bytes.slice(), this.origin); // a copy of the slice, not of the buffer it is cut from
    }

    operate(child: BridgedChild, operation: WindowOperation): void {
        const target = child.target;
        if (this.children.get(child) === null || target === null) {
            const queued = this.operations.get(child) ?? [];
            queued.push(operation);
            this.operations.set(child, queued);
        } else {
            this.send(target, { pony: 'window', operation });
        }
    }

    private received(event: MessageEvent): void {
        if (event.origin !== this.origin) return;
        let child: BridgedChild | undefined;
        for (const candidate of this.children.keys()) if (candidate.target === event.source) child = candidate;
        if (child === undefined) return;
        const message = event.data as ToParent;
        const terminal = this.terminal;
        terminal.dispatcher.guard(() => {
            switch (message?.pony) {
                case 'ready':
                    this.ready(child);
                    break;
                case 'send':
                    terminal.sendDataToServerFromWindow(message.message);
                    break;
                case 'closing':
                    child.closing?.();
                    break;
            }
        });
    }

    /** The child's page is ready: it gets what it cannot ask for, then the server is told, which sends its blocks. */
    private ready(child: BridgedChild): void {
        const target = child.target;
        if (target === null) return;
        const reader = this.terminal.dispatcher.reader;
        this.send(target, { pony: 'init', keys: this.terminal.contextKeys, title: child.title, entries: reader.entries() });
        this.children.set(child, new Set());
        for (const operation of this.operations.get(child) ?? []) this.send(target, { pony: 'window', operation });
        this.operations.delete(child);
        if (child.kind === 'window') this.terminal.setReadyWindow(child.id);
        else this.terminal.setReadyFrame(child.id);
    }

    private entries(ids: Set<number>): [number, string][] {
        const reader = this.terminal.dispatcher.reader;
        return [...ids].map(id => [id, reader.entry(id)]);
    }

    private send(target: Window, message: ToChild): void {
        target.postMessage(message, this.origin);
    }
}
