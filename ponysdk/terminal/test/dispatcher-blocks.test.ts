/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test, type TestContext } from 'node:test';

import {
    ClientToServerModel as C,
    HandlerModel as H,
    HandlerModelNames,
    ServerToClientModel as K,
    WidgetType as W,
    WidgetTypeNames,
} from '#model';
import type { Message } from '../src/protocol/messages.ts';
import { ProtocolError, Reader } from '../src/protocol/reader.ts';
import { Dispatcher, type HistoryPort, type Host } from '../src/runtime/dispatcher.ts';
import { BaseObject, type Context, type Session, type TerminalObject } from '../src/runtime/object.ts';
import { Windows } from '../src/runtime/windows.ts';
import { encode, type Model } from './frames.ts';

const SESSION: Session = {
    contextId: 1, tabindexOnlyFormField: false, hostPageBaseURL: '',
    body: null as unknown as HTMLElement, byId: () => null, embedding: null, contextKey: null, streamUrl: () => '', legacy: false,
};

/** Logs what the dispatcher asks of it; IMAGE objects fail, BUTTON objects have no property (BaseObject). */
class Recorded extends BaseObject {
    private readonly log: string[];

    constructor(context: Context, id: number, type: number, log: string[]) {
        super(context, id, type);
        this.log = log;
    }

    override update(reader: Reader): boolean {
        if (this.type === W.IMAGE) throw new Error(`#${this.id} cannot take ${Reader.keyName(reader.key)}`);
        if (this.type === W.BUTTON) return super.update(reader);
        this.log.push(`#${this.id} ${Reader.keyName(reader.key)}=${reader.str ?? reader.int}`);
        return true;
    }

    override add(_reader: Reader, child: TerminalObject): void {
        this.log.push(`add #${child.id} to #${this.id}`);
    }

    override remove(_reader: Reader, child: TerminalObject): void {
        this.log.push(`remove #${child.id} from #${this.id}`);
    }

    override addHandler(_reader: Reader, handler: number): void {
        this.log.push(`#${this.id} handler ${HandlerModelNames[handler]}`);
    }

    override removeHandler(_reader: Reader, handler: number): void {
        this.log.push(`#${this.id} no handler ${HandlerModelNames[handler]}`);
    }

    override destroy(): void {
        if (this.type === W.IMAGE) throw new Error(`#${this.id} holds on`);
        this.log.push(`destroy #${this.id}`);
    }
}

interface Run {
    dispatcher: Dispatcher;
    /** What the objects did. */
    log: string[];
    /** What went to the server. */
    sent: Message[];
    /** What went to the console. */
    console: string[];
}

function terminal(t: TestContext, loader?: (type: number) => Promise<void> | null): Run {
    const log: string[] = [];
    const sent: Message[] = [];
    const printed: string[] = [];
    t.mock.method(console, 'warn', (_format: string, message: string) => printed.push(`warn ${message}`));
    t.mock.method(console, 'error', (_format: string, message: string) => printed.push(`error ${message}`));
    const host: Host = {
        send: message => sent.push(message),
        contextCreated: () => true,
        contextResumed: () => undefined,
        contextDestroyed: () => undefined,
        stateCarried: () => undefined,
        streamRequested: id => log.push(`stream ${id}`),
        cookieRequested: id => log.push(`cookie ${id}`),
    };
    const history: HistoryPort = { token: '', newItem: () => undefined, fireCurrent: () => undefined };
    const dispatcher = new Dispatcher(host, (type, context, id) => {
        if (type === W.ROOT_PANEL) return null;
        log.push(`create ${WidgetTypeNames[type]} #${id}`);
        return new Recorded(context, id, type, log);
    }, SESSION, history, undefined, loader);
    return { dispatcher, log, sent, console: printed };
}

const create = (id: number, type: number, ...models: Model[]): Uint8Array =>
    encode([K.TYPE_CREATE, id], [K.WIDGET_TYPE, type], ...models, [K.END]);
const update = (id: number, ...models: Model[]): Uint8Array => encode([K.TYPE_UPDATE, id], ...models, [K.END]);
const warnings = (sent: Message[]): Message[] => sent.filter(m => C.WARN_MSG in m);

test('an unknown instruction is reported and skipped, the next block runs', t => {
    const run = terminal(t);
    run.dispatcher.receive(encode([K.TEXT, 'stray'], [K.END], [K.END]));
    run.dispatcher.receive(create(5, W.LABEL, [K.TEXT, 'after']));
    assert.deepEqual(run.console, ['warn Unknown instruction TEXT', 'warn Unknown instruction END']);
    assert.deepEqual(run.log, ['create LABEL #5', '#5 TEXT=after']);
});

test('a type the factory cannot create skips its block', t => {
    const run = terminal(t);
    run.dispatcher.receive(create(5, W.ROOT_PANEL, [K.TEXT, 'lost']));
    run.dispatcher.receive(create(6, W.LABEL));
    assert.deepEqual(run.console, ['warn Cannot create #5 of type ROOT_PANEL']);
    assert.deepEqual(run.log, ['create LABEL #6']);
});

test('a property the object does not have skips the rest of its block', t => {
    const run = terminal(t);
    run.dispatcher.receive(create(5, W.BUTTON, [K.TEXT, 'one'], [K.HTML, 'two']));
    run.dispatcher.receive(create(6, W.LABEL));
    assert.deepEqual(run.console, ['warn BUTTON #5 has no property TEXT: the rest of its block is skipped']);
    assert.deepEqual(run.log, ['create BUTTON #5', 'create LABEL #6']);
});

test('an object that fails reports the error, and the next block runs', t => {
    const run = terminal(t);
    run.dispatcher.receive(create(5, W.IMAGE, [K.TEXT, 'one']));
    run.dispatcher.receive(create(6, W.LABEL));
    assert.equal(run.sent.length, 1);
    assert.match(run.sent[0]![C.ERROR_MSG] as string, /^Error : #5 cannot take TEXT : /);
    assert.deepEqual(run.console, ['error PonySDK has encountered an internal error']);
    assert.deepEqual(run.log, ['create IMAGE #5', 'create LABEL #6']);
});

test('an unknown object or parent is reported to the server, and its block skipped', t => {
    const run = terminal(t);
    const d = run.dispatcher;
    d.receive(create(5, W.LABEL, [K.PARENT_OBJECT_ID, 99]));
    d.receive(encode([K.TYPE_ADD, 98], [K.PARENT_OBJECT_ID, 5], [K.END]));
    d.receive(encode([K.TYPE_ADD, 5], [K.PARENT_OBJECT_ID, 97], [K.END]));
    d.receive(encode([K.TYPE_REMOVE, 96], [K.PARENT_OBJECT_ID, 5], [K.END]));
    d.receive(encode([K.TYPE_REMOVE, 5], [K.PARENT_OBJECT_ID, 95], [K.END]));
    d.receive(encode([K.TYPE_ADD_HANDLER, 94], [K.HANDLER_TYPE, H.HANDLER_DOM_CLICK], [K.END]));
    d.receive(encode([K.TYPE_REMOVE_HANDLER], [K.WIDGET_ID, 93], [K.HANDLER_TYPE, H.HANDLER_DOM_CLICK], [K.END]));
    d.receive(update(5, [K.TEXT, 'still read']));
    assert.deepEqual(warnings(run.sent).map(m => [m[C.WARN_MSG], m[C.OBJECT_ID]]),
        [99, 98, 97, 96, 95, 94, 93].map(id => [`PTObject #${id} not found`, id]));
    assert.deepEqual(run.console.filter(line => line.startsWith('warn Cannot')), [
        'warn Cannot add #5 to the unknown parent #99',
        'warn Cannot add LABEL #5 to the unknown #97',
        'warn Cannot remove LABEL #5 from the unknown #95',
    ]);
    assert.deepEqual(run.log, ['create LABEL #5', '#5 TEXT=still read']);
});

test('a handler is removed from the object named, or from the last one read below protocol 4', t => {
    const run = terminal(t);
    const d = run.dispatcher;
    d.receive(create(5, W.LABEL));
    d.receive(create(6, W.LABEL));
    d.receive(encode([K.TYPE_REMOVE_HANDLER], [K.WIDGET_ID, 5], [K.HANDLER_TYPE, H.HANDLER_DOM_CLICK], [K.END]));
    d.receive(update(6, [K.TEXT, 'x'])); // the last int read: 6
    d.receive(encode([K.TYPE_REMOVE_HANDLER], [K.HANDLER_TYPE, H.HANDLER_DOM_BLUR], [K.END]));
    assert.deepEqual(run.log.slice(2), ['#5 no handler HANDLER_DOM_CLICK', '#6 TEXT=x', '#6 no handler HANDLER_DOM_BLUR']);
});

test('a stream or cookie request goes to the host, whatever the object', t => {
    const run = terminal(t);
    run.dispatcher.receive(encode([K.TYPE_ADD_HANDLER, 0], [K.HANDLER_TYPE, H.HANDLER_STREAM_REQUEST], [K.STREAM_REQUEST_ID, 3], [K.END]));
    run.dispatcher.receive(encode([K.TYPE_ADD_HANDLER, 0], [K.HANDLER_TYPE, H.HANDLER_COOKIE_REQUEST], [K.STREAM_REQUEST_ID, 4], [K.END]));
    assert.deepEqual(run.log, ['stream 3', 'cookie 4']);
    assert.deepEqual(run.sent, []);
});

test('a collected object is destroyed and forgotten; an unknown one is reported', t => {
    const run = terminal(t);
    const d = run.dispatcher;
    d.receive(create(5, W.LABEL));
    d.receive(encode([K.TYPE_GC, 5], [K.END]));
    d.receive(encode([K.TYPE_GC, 5], [K.END]));
    d.receive(update(5, [K.TEXT, 'gone']));
    d.receive(create(6, W.LABEL));
    assert.deepEqual(run.log, ['create LABEL #5', 'destroy #5', 'create LABEL #6']);
    assert.deepEqual(run.console.filter(line => !line.includes('not found')), [
        'warn Cannot collect the unknown #5',
        'warn Update of the unknown #5: its properties are skipped',
    ]);
});

test('destroyAll destroys every object, the last created first, past one that fails', t => {
    const run = terminal(t);
    const d = run.dispatcher;
    d.receive(create(5, W.LABEL));
    d.receive(create(6, W.IMAGE));
    d.receive(create(7, W.LABEL));
    d.destroyAll();
    assert.deepEqual(run.log.slice(3), ['destroy #7', 'destroy #5']);
    assert.equal(run.console.length, 1);
    assert.match(run.console[0]!, /^warn Cannot destroy IMAGE #6: Error : #6 holds on : /);
    d.receive(update(5, [K.TEXT, 'gone']));
    assert.equal(run.log.length, 5);
});

test('a warning without an object goes to the server without an id', t => {
    const run = terminal(t);
    run.dispatcher.warn('careful');
    assert.deepEqual(run.sent, [{ [C.WARN_MSG]: 'careful' }]);
    assert.deepEqual(run.console, ['warn careful']);
});

test('a module that fails to load is reported, then its block runs without it', async t => {
    let failed = false;
    const run = terminal(t, type => {
        if (type !== W.ROOT_PANEL || failed) return null;
        return Promise.reject(new Error('offline')).catch(e => {
            failed = true;
            throw e;
        });
    });
    run.dispatcher.receive(create(5, W.ROOT_PANEL));
    run.dispatcher.receive(create(6, W.LABEL));
    assert.deepEqual(run.log, [], 'the block after it waits');
    await new Promise(resolve => setTimeout(resolve));
    assert.deepEqual(run.console, ['error A module of the terminal did not load: its widgets are not created',
        'warn Cannot create #5 of type ROOT_PANEL']);
    assert.deepEqual(run.log, ['create LABEL #6']);
});

for (const receiver of ['receiveInWindow', 'receiveInFrame'] as const) {
    test(`${receiver}: a block waits for its module, and the blocks after it`, async t => {
        let loaded!: () => void;
        const loading = new Promise<void>(resolve => {
            loaded = resolve;
        });
        let ready = false;
        const run = terminal(t, type => type === W.LABEL && !ready ? loading.then(() => {
            ready = true;
        }) : null);
        const d = run.dispatcher;
        d[receiver](create(5, W.LABEL));
        d[receiver](create(6, W.BUTTON));
        assert.deepEqual(run.log, []);
        loaded();
        await new Promise(resolve => setTimeout(resolve));
        assert.deepEqual(run.log, ['create LABEL #5', 'create BUTTON #6']);
    });

    test(`${receiver}: a message that breaks the protocol is thrown to the caller`, t => {
        const run = terminal(t);
        assert.throws(() => run.dispatcher[receiver](Uint8Array.of(255)), ProtocolError);
    });
}

test('a frame of a window is forwarded to the frame object', t => {
    const run = terminal(t);
    const received: number[][] = [];
    const frame = Object.assign(new BaseObject(run.dispatcher, 9, W.FRAME), {
        ready: true,
        postMessage: (bytes: Uint8Array) => received.push([...bytes]),
        setReady: () => received.push([]),
    });
    run.dispatcher.register(frame);
    const forFrame = update(4, [K.TEXT, 'in frame']);
    run.dispatcher.receiveInWindow(encode([K.FRAME_ID, 9], [K.TYPE_UPDATE, 4], [K.TEXT, 'in frame'], [K.END]));
    run.dispatcher.setReady(9, 'frame');
    assert.deepEqual(received, [[...forFrame], []]);
    run.dispatcher.setReady(10, 'window');
    assert.deepEqual(run.console, ['warn No window #10']);
});

test('the windows registry closes every window, the last opened first, past one that fails', t => {
    const printed: string[] = [];
    t.mock.method(console, 'warn', (_format: string, message: string) => printed.push(message));
    const windows = new Windows();
    const closed: number[] = [];
    const window = (id: number) => ({
        ready: true, closed: false, onClose: () => undefined, postMessage: () => undefined, setReady: () => undefined,
        close: () => {
            if (id === 2) throw new Error('blocked');
            closed.push(id);
        },
    });
    for (const id of [1, 2, 3, 4]) windows.register(id, window(id));
    windows.unregister(4);
    windows.closeAll();
    assert.deepEqual(closed, [3, 1]);
    assert.equal(printed.length, 1);
    assert.match(printed[0]!, /^Cannot close a window: Error : blocked : /);
});

test('a plain object has no property and does nothing else', () => {
    const object = new BaseObject(null as unknown as Context, 3, -1);
    const reader = new Reader();
    assert.equal(object.update(reader), false);
    object.add(reader, object);
    object.remove(reader, object);
    object.addHandler(reader, H.HANDLER_DOM_CLICK);
    object.removeHandler(reader, H.HANDLER_DOM_CLICK);
    object.destroy();
    assert.equal(String(object), 'object #3');
    assert.equal(String(new BaseObject(null as unknown as Context, 4, W.LABEL)), 'LABEL #4');
});
