/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import { ClientToServerModel as C } from '#model';
import type { Message } from '../src/protocol/messages.ts';
import { listenByName, readFields } from '../src/runtime/named-events.ts';
import type { Context } from '../src/runtime/object.ts';

/** The messages a listener sends, through a context that only sends. */
function listening(json: string): { element: EventTarget; sent: Message[]; controller: AbortController } {
    const sent: Message[] = [];
    const context = { send: (message: Message) => sent.push(message), guard: (action: () => void) => action() } as unknown as Context;
    const element = new EventTarget();
    const [, controller] = listenByName(context, 7, element, json);
    return { element, sent, controller };
}

function fire(element: EventTarget, detail: number): Event {
    const event = new CustomEvent('pick', { detail, cancelable: true });
    element.dispatchEvent(event);
    return event;
}

const details = (sent: Message[]): unknown[] => sent.map(m => (m[C.EVENT_FIELDS] as Record<string, unknown>)['detail']);

test('reads the fields at their paths, as the server reads them', () => {
    const event = new CustomEvent('pick', { detail: { day: 12, tags: ['a'] } });
    Object.defineProperty(event, 'target', { value: { value: 'abc', checked: true, size: Number.NaN } });
    assert.deepEqual(readFields(event, ['type', 'detail', 'target.value', 'target.checked', 'target.size', 'missing.path']), {
        type: 'pick',
        detail: '{"day":12,"tags":["a"]}', // an object as JSON text
        'target.value': 'abc',
        'target.checked': true,
        'target.size': null, // not a JSON number
        'missing.path': null,
    });
});

test('reads a value JSON cannot write as its text', () => {
    const cycle: Record<string, unknown> = {};
    cycle['self'] = cycle;
    const event = new CustomEvent('pick', { detail: cycle });
    assert.deepEqual(readFields(event, ['detail']), { detail: '[object Object]' });
});

test('sends each event with its fields, prevented when asked, until unlistened', () => {
    const { element, sent, controller } = listening('{"name":"pick","fields":["detail"],"prevent":true}');
    const event = fire(element, 1);
    assert.equal(event.defaultPrevented, true);
    fire(element, 2);
    assert.deepEqual(sent, [
        { [C.OBJECT_ID]: 7, [C.EVENT]: 'pick', [C.EVENT_FIELDS]: { detail: 1 } },
        { [C.OBJECT_ID]: 7, [C.EVENT]: 'pick', [C.EVENT_FIELDS]: { detail: 2 } },
    ]);
    controller.abort();
    fire(element, 3);
    assert.equal(sent.length, 2);
});

test('sends an event without fields as the name alone', () => {
    const { element, sent } = listening('{"name":"pick"}');
    assert.equal(fire(element, 1).defaultPrevented, false);
    assert.deepEqual(sent, [{ [C.OBJECT_ID]: 7, [C.EVENT]: 'pick' }]);
});

test('debounce sends the last event once none came for the delay', t => {
    t.mock.timers.enable({ apis: ['setTimeout'] });
    const { element, sent } = listening('{"name":"pick","fields":["detail"],"debounce":100}');
    fire(element, 1);
    t.mock.timers.tick(50);
    fire(element, 2);
    t.mock.timers.tick(99);
    assert.deepEqual(details(sent), []);
    t.mock.timers.tick(1);
    assert.deepEqual(details(sent), [2]);
});

test('throttle sends the first event at once, then the last of each period at its end', t => {
    t.mock.timers.enable({ apis: ['setTimeout'] });
    const { element, sent, controller } = listening('{"name":"pick","fields":["detail"],"throttle":100}');
    fire(element, 1);
    fire(element, 2);
    fire(element, 3);
    assert.deepEqual(details(sent), [1]);
    t.mock.timers.tick(100);
    assert.deepEqual(details(sent), [1, 3]);
    t.mock.timers.tick(100); // a period without events closes the throttle
    fire(element, 4);
    assert.deepEqual(details(sent), [1, 3, 4]);
    fire(element, 5);
    controller.abort(); // the pending event goes with the listener
    t.mock.timers.tick(200);
    assert.deepEqual(details(sent), [1, 3, 4]);
});
