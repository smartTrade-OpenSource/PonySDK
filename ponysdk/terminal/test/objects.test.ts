/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test, type TestContext } from 'node:test';

import { ClientToServerModel as C, ServerToClientModel as K, WidgetType as W } from '#model';
import { AddOnObject } from '../src/objects/addon.ts';
import { Script } from '../src/objects/script.ts';
import type { Message } from '../src/protocol/messages.ts';
import { Dispatcher, registerAddOnFactory, type HistoryPort, type Host } from '../src/runtime/dispatcher.ts';
import type { AddOn, Session } from '../src/runtime/object.ts';
import { encode, type Model } from './frames.ts';

// The objects that need no document, driven by the server's blocks: what they do with a malformed or unexpected one

const SESSION: Session = {
    contextId: 1, tabindexOnlyFormField: false, hostPageBaseURL: '',
    body: null as unknown as HTMLElement, byId: () => null, embedding: null, contextKey: null, streamUrl: () => '', legacy: false,
};

interface Run {
    dispatcher: Dispatcher;
    sent: Message[];
    console: string[];
}

function terminal(t: TestContext): Run {
    const sent: Message[] = [];
    const printed: string[] = [];
    t.mock.method(console, 'warn', (_format: string, message: string) => printed.push(`warn ${message}`));
    t.mock.method(console, 'error', (_format: string, message: string, error?: unknown) =>
        printed.push(`error ${message}${error instanceof Error ? `: ${error.message}` : ''}`));
    const host: Host = {
        send: message => sent.push(message),
        contextCreated: () => true,
        contextResumed: () => undefined,
        contextDestroyed: () => undefined,
        streamRequested: () => undefined,
        cookieRequested: () => undefined,
    };
    const history: HistoryPort = { token: '', newItem: () => undefined, fireCurrent: () => undefined };
    const dispatcher = new Dispatcher(host, (type, context, id, reader) => {
        if (type === W.SCRIPT) return new Script(context, id, type);
        if (type === W.ADDON) return AddOnObject.create(context, id, type, reader);
        return null;
    }, SESSION, history);
    return { dispatcher, sent, console: printed };
}

const create = (id: number, type: number, ...models: Model[]): Uint8Array =>
    encode([K.TYPE_CREATE, id], [K.WIDGET_TYPE, type], ...models, [K.END]);
const update = (id: number, ...models: Model[]): Uint8Array => encode([K.TYPE_UPDATE, id], ...models, [K.END]);

/** An addon that records its calls; "fail" throws. */
function recordingAddOn(calls: string[], failOnInit = false): (id: number, args: unknown) => AddOn {
    return (id, args) => ({
        onInit: () => {
            if (failOnInit) throw new Error('no init');
            calls.push(`init #${id} ${JSON.stringify(args)}`);
        },
        onAttached: () => calls.push('attached'),
        onDetached: () => calls.push('detached'),
        update: (method, a) => {
            if (method === 'fail') throw new Error('failed');
            calls.push(`${method} ${JSON.stringify(a)}`);
        },
        destroy: () => calls.push('destroy'),
    });
}

test('a script: a property other than a function name is refused, and its block skipped', t => {
    const run = terminal(t);
    run.dispatcher.receive(create(5, W.SCRIPT));
    run.dispatcher.receive(update(5, [K.TEXT, 'x'], [K.FUNCTION_NAME, 'ignored']));
    assert.deepEqual(run.console, ['warn SCRIPT #5 has no property TEXT: the rest of its block is skipped']);
});

test('a script whose function throws: logged, and the error goes back with its command id', t => {
    const run = terminal(t);
    (globalThis as Record<string, unknown>).ponyTestThrows = () => {
        throw new Error('broken');
    };
    (globalThis as Record<string, unknown>).ponyTestThrowsText = () => {
        throw 'plain';
    };
    try {
        run.dispatcher.receive(create(5, W.SCRIPT));
        run.dispatcher.receive(update(5, [K.FUNCTION_NAME, 'ponyTestThrows'], [K.COMMAND_ID, 7]));
        run.dispatcher.receive(update(5, [K.FUNCTION_NAME, 'ponyTestThrowsText'], [K.COMMAND_ID, 8]));
        run.dispatcher.receive(update(5, [K.FUNCTION_NAME, 'ponyTestThrows']));
    } finally {
        delete (globalThis as Record<string, unknown>).ponyTestThrows;
        delete (globalThis as Record<string, unknown>).ponyTestThrowsText;
    }
    assert.deepEqual(run.console, [
        'error Script exception for ponyTestThrows: broken',
        'error Script exception for ponyTestThrowsText',
        'error Script exception for ponyTestThrows: broken',
    ]);
    const answers = run.sent.map(m => (m[C.APPLICATION_INSTRUCTIONS] as Message[] | undefined)?.[0] ?? m);
    assert.deepEqual(answers.map(m => [m[C.COMMAND_ID], m[C.ERROR_MSG]]), [[7, 'broken'], [8, 'plain']]);
});

test('an addon whose factory is not in the page: the error goes to the server, the next block runs', t => {
    const run = terminal(t);
    run.dispatcher.receive(create(5, W.ADDON, [K.FACTORY, 'ponyTest.missing']));
    run.dispatcher.receive(create(6, W.SCRIPT));
    assert.equal(run.sent.length, 1);
    assert.match(run.sent[0]![C.ERROR_MSG] as string, /AddOn factory not found for signature: ponyTest\.missing/);
});

test('an addon that fails to start, to answer a call, or is called once destroyed: logged, the next calls still run', t => {
    const run = terminal(t);
    const calls: string[] = [];
    registerAddOnFactory('ponyTest.broken', recordingAddOn(calls, true));
    registerAddOnFactory('ponyTest.recording', recordingAddOn(calls));
    run.dispatcher.receive(create(5, W.ADDON, [K.FACTORY, 'ponyTest.broken']));
    run.dispatcher.receive(create(6, W.ADDON, [K.FACTORY, 'ponyTest.recording'], [K.PADDON_CREATION_ARGS, ['a', 1]]));
    run.dispatcher.receive(update(6, [K.PADDON_METHOD, 'fail'], [K.PADDON_ARGUMENTS, []]));
    run.dispatcher.receive(update(6, [K.PADDON_METHOD, 'ping']));
    run.dispatcher.receive(update(6, [K.DESTROY]));
    run.dispatcher.receive(update(6, [K.DESTROY], [K.PADDON_METHOD, 'late']));
    run.dispatcher.receive(update(6, [K.TEXT, 'x']));
    assert.deepEqual(calls, ['init #6 ["a",1]', 'ping null', 'destroy']);
    assert.deepEqual(run.console, [
        'error ADDON #5 could not be created: no init',
        'error ADDON #6.fail: failed',
        'warn ADDON #6 is destroyed: late is discarded',
        'warn ADDON #6 has no property TEXT: the rest of its block is skipped',
    ]);
});
