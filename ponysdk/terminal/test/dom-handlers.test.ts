/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import { HandlerModel as H } from '#model';
import type { Message } from '../src/protocol/messages.ts';
import type { Reader } from '../src/protocol/reader.ts';
import type { Context } from '../src/runtime/object.ts';
import { Widget } from '../src/runtime/widget.ts';

// The DOM handlers of a widget the server adds and removes, on a fake element: the real Widget, a mere EventTarget for the DOM

class FakeElement extends EventTarget {
    readonly classList = { add: () => undefined };
    draggable = false;
}

function widget(): { widget: Widget; element: EventTarget; sent: Message[] } {
    const sent: Message[] = [];
    const context = { send: (message: Message) => sent.push(message), guard: (action: () => void) => action() } as unknown as Context;
    const element = new FakeElement();
    return { widget: new Widget(context, 1, 0, element as unknown as HTMLElement), element, sent };
}

const reader = undefined as unknown as Reader; // a click and a drop read nothing more

function fire(element: EventTarget, type: string): Event {
    const event = new Event(type, { cancelable: true });
    element.dispatchEvent(event);
    return event;
}

test('a DOM handler removed stops the events, and added again listens once', () => {
    const { widget: w, element, sent } = widget();
    w.addHandler(reader, H.HANDLER_DOM_CONTEXT_MENU);
    fire(element, 'contextmenu');
    assert.equal(sent.length, 1);

    w.removeHandler(reader, H.HANDLER_DOM_CONTEXT_MENU);
    fire(element, 'contextmenu');
    assert.equal(sent.length, 1, 'removed: nothing sent');

    w.addHandler(reader, H.HANDLER_DOM_CONTEXT_MENU);
    fire(element, 'contextmenu');
    assert.equal(sent.length, 2, 'one listener, not two');
});

test('a drop handler removed lets the element refuse a drag again', () => {
    const { widget: w, element } = widget();
    w.addHandler(reader, H.HANDLER_DOM_DROP);
    assert.equal(fire(element, 'dragover').defaultPrevented, true);

    w.removeHandler(reader, H.HANDLER_DOM_DROP);
    assert.equal(fire(element, 'dragover').defaultPrevented, false);
});

test('a drag start handler removed makes the element not draggable', () => {
    const { widget: w, element, sent } = widget();
    w.addHandler(reader, H.HANDLER_DOM_DRAG_END);
    w.addHandler(reader, H.HANDLER_DOM_DRAG_END); // added already: one listener
    fire(element, 'dragend');
    assert.equal(sent.length, 1);

    w.addHandler(reader, H.HANDLER_DOM_DRAG_START);
    assert.equal((element as FakeElement).draggable, true);
    w.removeHandler(reader, H.HANDLER_DOM_DRAG_START);
    assert.equal((element as FakeElement).draggable, false);
});

test('removing a DOM handler leaves the other types listening', () => {
    const { widget: w, element, sent } = widget();
    w.addHandler(reader, H.HANDLER_DOM_CONTEXT_MENU);
    w.addHandler(reader, H.HANDLER_DOM_DRAG_END);
    w.removeHandler(reader, H.HANDLER_DOM_DRAG_END);
    w.removeHandler(reader, H.HANDLER_DOM_DRAG_LEAVE); // never added: nothing to do
    fire(element, 'contextmenu');
    fire(element, 'dragend');
    assert.equal(sent.length, 1);

    w.destroy();
    fire(element, 'contextmenu');
    assert.equal(sent.length, 1, 'destroyed: nothing sent');
});
