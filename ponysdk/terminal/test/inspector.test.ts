/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import { ClientToServerModel as C, ServerToClientModel as K, WidgetType as W } from '#model';
import { replay, size, Stats } from '../src/inspector.ts';
import { Dispatcher, Recording, type HistoryPort, type Host } from '../src/runtime/dispatcher.ts';
import { BaseObject, type Session, type TerminalObject } from '../src/runtime/object.ts';
import type { Reader } from '../src/protocol/reader.ts';
import { encode } from './frames.ts';

// What the inspector counts (src/inspector.ts), driven by a dispatcher fed as the server feeds it

const SESSION: Session = {
    contextId: 1, tabindexOnlyFormField: false, hostPageBaseURL: '',
    body: null as unknown as HTMLElement, byId: () => null, embedding: null, contextKey: null, streamUrl: () => '', legacy: false,
};

/** An object that takes any property and any child. */
class Plain extends BaseObject {
    override update(reader: Reader): boolean {
        return reader.key === K.TEXT;
    }

    override add(): void {}
}

function watched(): { dispatcher: Dispatcher; stats: Stats } {
    const host: Host = {
        send: () => undefined, contextCreated: () => true, contextResumed: () => undefined, contextDestroyed: () => undefined,
        streamRequested: () => undefined, cookieRequested: () => undefined,
    };
    const history: HistoryPort = { token: '', newItem: () => undefined, fireCurrent: () => undefined };
    const dispatcher = new Dispatcher(host, (type, context, id): TerminalObject => new Plain(context, id, type), SESSION, history);
    const stats = new Stats(() => 0);
    dispatcher.watcher = stats;
    return { dispatcher, stats };
}

test('each byte of the server goes to the object it was for: an update run counts each object', () => {
    const { dispatcher, stats } = watched();
    const frames = [
        encode([K.TYPE_CREATE, 5], [K.WIDGET_TYPE, W.LABEL], [K.TEXT, 'five'], [K.END],
            [K.TYPE_CREATE, 6], [K.WIDGET_TYPE, W.LABEL], [K.END],
            [K.TYPE_CREATE, 9], [K.WIDGET_TYPE, W.FLOW_PANEL], [K.END]),
        encode([K.TYPE_UPDATE, 5], [K.TEXT, 'a much longer text'], [K.UPDATE_NEXT], [K.TEXT, 'x'],
            [K.UPDATE_NEXT_ID, 9], [K.TEXT, 'y'], [K.END]),
        encode([K.TYPE_ADD, 6], [K.PARENT_OBJECT_ID, 9], [K.END], [K.TYPE_GC, 5], [K.END]),
    ];
    for (const frame of frames) dispatcher.receive(frame);

    assert.equal(stats.framesIn, 3);
    const total = [...stats.objects.values()].reduce((sum, o) => sum + o.bytes, 0);
    assert.equal(total, stats.bytesIn, 'nothing counted twice, nothing lost');
    const five = stats.objects.get(5)!;
    assert.deepEqual([five.creates, five.updates, five.collected], [1, 1, true]);
    assert.ok(five.bytes > stats.objects.get(6)!.bytes, 'the long text is five\'s');
    assert.deepEqual([stats.objects.get(6)!.updates, stats.objects.get(9)!.updates], [1, 1]);
});

test('what the terminal sends is counted, by object when it names one', () => {
    const { dispatcher, stats } = watched();
    dispatcher.send({ [C.OBJECT_ID]: 5, [C.DOM_HANDLER_TYPE]: 1 });
    dispatcher.sendRaw({ [C.ERROR_MSG]: 'x' });
    assert.equal(stats.messagesOut, 2);
    assert.ok(stats.bytesOut > 0);
    assert.equal(stats.objects.get(5)!.events, 1);
});

test('by class, busiest first, and the rate over five seconds', () => {
    let now = 0;
    const stats = new Stats(() => now);
    stats.block(K.TYPE_CREATE, 1, 100);
    stats.block(K.TYPE_CREATE, 2, 50);
    stats.block(K.TYPE_UPDATE, 2, 30);
    stats.block(K.TYPE_CREATE, 3, 500);
    const names = new Map([[1, 'PLabel'], [2, 'PLabel'], [3, 'PButton']]);
    assert.deepEqual(stats.byClass(id => names.get(id)!), [
        { name: 'PButton', objects: 1, bytes: 500, updates: 0 },
        { name: 'PLabel', objects: 2, bytes: 180, updates: 1 },
    ]);
    assert.deepEqual(stats.busiest(2).map(o => o.id), [3, 1]);

    stats.frame(1000);
    now = 3000;
    stats.frame(1500);
    assert.equal(stats.rate(), 500);
    now = 7000; // the first frame is over five seconds old
    stats.frame(0);
    assert.equal(stats.rate(), 300);
});

test('sizes read as people write them', () => {
    assert.deepEqual([size(12), size(2048), size(3 * 1024 * 1024)], ['12 B', '2.0 KB', '3.0 MB']);
});

test('debug mode found in the middle of a message: that message counts', t => {
    const { dispatcher } = watched();
    dispatcher.watcher = null;
    t.mock.method(console, 'error', () => undefined); // no document here: the inspector does not start
    const frame = encode([K.TYPE_CREATE, 5], [K.WIDGET_TYPE, W.LABEL], [K.END]);
    dispatcher.receive(frame);
    dispatcher.debugging();
    const recording = dispatcher.watcher as unknown as Recording;
    assert.deepEqual(recording.events[0], [-1, 0, frame.byteLength]);
    dispatcher.debugging(); // once
    assert.equal(dispatcher.watcher, recording);
});

test('until the inspector is loaded, the dispatcher records what happens', () => {
    const recording = new Recording();
    recording.frame(10);
    recording.block(K.TYPE_CREATE, 4, 10);
    recording.sent({ [C.OBJECT_ID]: 4 });
    assert.deepEqual(recording.events, [[-1, 0, 10], [K.TYPE_CREATE, 4, 10], { [C.OBJECT_ID]: 4 }]);
    const stats = new Stats(() => 0);
    replay(recording, stats);
    assert.deepEqual([stats.framesIn, stats.bytesIn, stats.messagesOut], [1, 10, 1]);
    assert.deepEqual(stats.objects.get(4), { id: 4, bytes: 10, creates: 1, updates: 0, events: 1, collected: false });
});
