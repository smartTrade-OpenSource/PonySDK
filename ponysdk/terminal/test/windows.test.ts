/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import { ClientToServerModel as C, WidgetType as W } from '#model';
import { bridgeToParent } from '../src/child-bridge.ts';
import type { Message } from '../src/protocol/messages.ts';
import type { Reader } from '../src/protocol/reader.ts';
import { PopupWindow, watchWindows } from '../src/objects/windows.ts';
import type { Context, Session } from '../src/runtime/object.ts';
import { Widget } from '../src/runtime/widget.ts';
import { Windows } from '../src/runtime/windows.ts';
import type { Terminal } from '../src/terminal.ts';

// The windows a terminal opens and the bridge of a terminal opened by another page, apart from a browser

test('every second, a window closed without an unload event is noticed', t => {
    t.mock.timers.enable({ apis: ['setInterval'] });
    const windows = new Windows();
    const noticed: number[] = [];
    const window = (id: number, closed: boolean) => ({
        ready: true, closed, onClose: () => noticed.push(id), postMessage: () => undefined, setReady: () => undefined,
        close: () => undefined,
    });
    windows.register(1, window(1, true));
    windows.register(2, window(2, false));
    const stop = new AbortController();
    watchWindows(windows, stop.signal);

    t.mock.timers.tick(1000);
    assert.deepEqual(noticed, [1]);
    stop.abort();
    t.mock.timers.tick(5000);
    assert.deepEqual(noticed, [1], 'stopped with its terminal');
});

test('a block for a ready window found closed: the window is closed, the block goes nowhere', () => {
    const sent: Message[] = [];
    const context = {
        session: { contextId: 1, hostPageBaseURL: '', tabindexOnlyFormField: false, embedding: null } as unknown as Session,
        windows: new Windows(),
        send: (message: Message) => sent.push(message),
    } as unknown as Context;
    const values = [false, 'page.html', 'name', ''];
    let next = 0;
    const reader = {
        read: () => 0,
        get bool() { return values[next++] as boolean; },
        get str() { return values[next++] as string; },
    } as unknown as Reader;
    const popup = PopupWindow.create(context, 7, W.WINDOW, reader);
    popup.setReady(); // never opened: no window to close or post to
    popup.postMessage(new Uint8Array([1]));
    assert.equal(popup.closed, true);
    assert.deepEqual(sent.map(m => Object.keys(m).filter(k => k !== C.OBJECT_ID)), [[C.HANDLER_OPEN]]);
});

test('a terminal opened by another page, with no parent origins listed, accepts its own origin alone', t => {
    const globals = globalThis as Record<string, unknown>;
    const saved = { document: globals['document'], location: globals['location'], window: globals['window'] };
    globals['document'] = { querySelector: () => null };
    globals['location'] = { origin: 'https://app.example' };
    globals['window'] = new EventTarget();
    t.after(() => Object.assign(globals, saved));
    const attached: string[] = [];
    const terminal = {
        attachToParent: (_parent: unknown, _source: unknown, windowId: string | null, frameId: string) =>
            attached.push(`${windowId} ${frameId}`),
    } as unknown as Terminal;

    bridgeToParent(terminal, { postMessage: () => undefined } as unknown as Window, '3', '');
    assert.deepEqual(attached, ['3 ']);
});

test('a resumed context unlocks the events a widget listens to with lock', () => {
    const sent: Message[] = [];
    const context = { send: (m: Message) => sent.push(m), guard: (action: () => void) => action() } as unknown as Context;
    const attributes = new Map<string, string>();
    const element = Object.assign(new EventTarget(), {
        setAttribute: (name: string, value: string) => attributes.set(name, value),
        removeAttribute: (name: string) => attributes.delete(name),
    }) as unknown as HTMLElement;
    const widget = new Widget(context, 4, W.LABEL, element);
    widget.contextResumed(); // nothing listened to yet
    widget.listenEvent('{"name":"pick","lock":true}');
    element.dispatchEvent(new Event('pick'));
    assert.equal(attributes.get('aria-busy'), 'true');

    widget.contextResumed(); // the event may never have reached the server
    assert.equal(attributes.has('aria-busy'), false);
    element.dispatchEvent(new Event('pick'));
    assert.equal(sent.length, 2, 'sent again');
});
