/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import { ClientToServerModel as C } from '#model';
import type { Message } from '../src/protocol/messages.ts';
import { listenByName } from '../src/runtime/named-events.ts';
import type { Context, TerminalObject } from '../src/runtime/object.ts';
import { Widget } from '../src/runtime/widget.ts';

// Client commands (docs/decisions/0027-client-commands.md) on a fake element: the real Widget, a mere EventTarget for the DOM

/** The part of an HTMLElement the commands touch. */
class FakeElement extends EventTarget {
    readonly style = { display: '' };
    readonly attributes = new Map<string, string>();
    readonly classes = new Set<string>();
    focused = 0;
    readonly classList = {
        add: (name: string) => {
            if (/\s/.test(name)) throw new Error('InvalidCharacterError');
            this.classes.add(name);
        },
        remove: (...names: string[]) => names.forEach(name => this.classes.delete(name)),
        toggle: (name: string) => (this.classes.delete(name) ? false : this.classes.add(name).has(name)),
    };
    setAttribute(name: string, value: string): void {
        this.attributes.set(name, value);
    }
    removeAttribute(name: string): void {
        this.attributes.delete(name);
    }
    focus(): void {
        this.focused++;
    }
}

interface Run {
    context: Context;
    sent: Message[];
    warnings: string[];
    errors: unknown[];
    objects: Map<number, TerminalObject>;
    widget(id: number, typeClass?: string): Widget;
}

function run(): Run {
    const sent: Message[] = [];
    const warnings: string[] = [];
    const errors: unknown[] = [];
    const objects = new Map<number, TerminalObject>();
    const context = {
        send: (message: Message) => sent.push(message),
        guard: (action: () => void) => action(),
        peek: (id: number) => objects.get(id),
        warn: (message: string) => warnings.push(message),
        error: (error: unknown) => errors.push(error),
        session: { legacy: false },
    } as unknown as Context;
    const widget = (id: number, typeClass?: string): Widget => {
        const created = new Widget(context, id, 0, new FakeElement() as unknown as HTMLElement, typeClass ?? null);
        objects.set(id, created);
        return created;
    };
    return { context, sent, warnings, errors, objects, widget };
}

const fake = (widget: Widget): FakeElement => widget.element as unknown as FakeElement;

function listen(r: Run, element: EventTarget, options: object): void {
    listenByName(r.context, 1, element, JSON.stringify({ name: 'click', ...options }));
}

const click = (element: EventTarget): void => {
    element.dispatchEvent(new CustomEvent('click'));
};

test('every command, in the order declared, before the event is sent', () => {
    const r = run();
    const source = r.widget(1);
    const panel = r.widget(2, 'pony-Panel');
    const input = r.widget(3);
    // the order shows in what the sending sees: the server event goes out after the last command
    const seen: string[] = [];
    r.context.send = message => {
        seen.push(`sent after ${[...fake(panel).classes].join(',')} ${fake(panel).style.display}`);
        r.sent.push(message);
    };
    listen(r, source.element, {
        commands: [
            ['show', 2], ['hide', 2], ['toggle', 2], ['addClass', 2, 'open'], ['addClass', 2, 'wide'], ['removeClass', 2, 'wide'],
            ['toggleClass', 2, 'busy'], ['toggleClass', 2, 'busy'], ['toggleClass', 2, 'busy'],
            ['setAttribute', 2, 'aria-expanded', 'true'], ['setAttribute', 3, 'data-x', ''], ['removeAttribute', 3, 'data-x'],
            ['focus', 3],
        ],
    });
    click(source.element);
    assert.equal(fake(panel).style.display, ''); // show, hide, toggle: shown at the end
    assert.deepEqual([...fake(panel).classes].sort(), ['busy', 'open', 'pony-Panel']);
    assert.equal(fake(panel).attributes.get('aria-expanded'), 'true');
    assert.equal(fake(input).attributes.size, 0);
    assert.equal(fake(input).focused, 1);
    assert.deepEqual(seen, ['sent after pony-Panel,open,busy ']);
    assert.equal(r.sent.length, 1);
    assert.deepEqual(r.warnings, []);
    assert.deepEqual(r.errors, []);
});

test('hide and show render what the server renders: display and aria-hidden', () => {
    const r = run();
    const source = r.widget(1);
    const panel = r.widget(2);
    listen(r, source.element, { commands: [['hide', 2]], send: false });
    click(source.element);
    assert.equal(fake(panel).style.display, 'none');
    assert.equal(fake(panel).attributes.get('aria-hidden'), 'true');
    listen(r, source.element, { commands: [['toggle', 2]], send: false });
    click(source.element);
    assert.equal(fake(panel).style.display, '');
    assert.equal(fake(panel).attributes.has('aria-hidden'), false);
    listen(r, source.element, { commands: [['toggle', 2]], send: false });
    click(source.element);
    assert.equal(fake(panel).style.display, 'none');
});

test('removeClass keeps the type class of the widget, as the server\'s removal does', () => {
    const r = run();
    const source = r.widget(1);
    const panel = r.widget(2, 'pony-Panel');
    fake(panel).classes.add('open');
    listen(r, source.element, { commands: [['removeClass', 2, 'pony-Panel'], ['removeClass', 2, 'open']], send: false });
    click(source.element);
    assert.deepEqual([...fake(panel).classes], ['pony-Panel']);
});

test('a client-only listener sends nothing, and the event still goes out when it is asked to', () => {
    const r = run();
    const source = r.widget(1);
    r.widget(2);
    listen(r, source.element, { commands: [['toggle', 2]], send: false });
    click(source.element);
    assert.deepEqual(r.sent, []);

    const other = r.widget(3);
    listen(r, other.element, { fields: ['type'], commands: [['toggle', 2]] });
    click(other.element);
    assert.deepEqual(r.sent, [{ [C.OBJECT_ID]: 1, [C.EVENT]: 'click', [C.EVENT_FIELDS]: { type: 'click' } }]);
});

test('commands are not rate limited, only the sending is', t => {
    t.mock.timers.enable({ apis: ['setTimeout'] });
    const r = run();
    const source = r.widget(1);
    const panel = r.widget(2);
    listen(r, source.element, { commands: [['toggleClass', 2, 'on']], debounce: 100 });
    click(source.element);
    assert.equal(fake(panel).classes.has('on'), true); // at once
    click(source.element);
    assert.equal(fake(panel).classes.has('on'), false);
    assert.equal(r.sent.length, 0);
    t.mock.timers.tick(100);
    assert.equal(r.sent.length, 1);
});

test('a target destroyed or not created is skipped without a word, the others run', () => {
    const r = run();
    const source = r.widget(1);
    const alive = r.widget(2);
    const gone = r.widget(3);
    listen(r, source.element, { commands: [['addClass', 3, 'x'], ['addClass', 99, 'x'], ['addClass', 2, 'x']] });
    r.objects.delete(3); // destroyed after the listener was set
    r.objects.set(4, { id: 4 } as TerminalObject); // an object without element
    click(source.element);
    assert.equal(fake(alive).classes.has('x'), true);
    assert.equal(fake(gone).classes.has('x'), false);
    assert.deepEqual(r.warnings, []);
    assert.equal(r.sent.length, 1, 'the event is sent all the same');
});

test('an object that is not a widget is no target', () => {
    const r = run();
    const source = r.widget(1);
    r.objects.set(4, { id: 4 } as TerminalObject);
    listen(r, source.element, { commands: [['show', 4]], send: false });
    click(source.element);
    assert.deepEqual(r.errors, []);
});

test('an unknown command or an event handler attribute is reported once at listening, and left out', () => {
    const r = run();
    const source = r.widget(1);
    const panel = r.widget(2);
    listen(r, source.element, {
        commands: [['eval', 2, 'alert(1)'], ['toString', 2], ['setAttribute', 2, 'onclick', 'alert(1)'], ['removeAttribute', 2, 'OnLoad'], ['addClass', 2, 'ok']],
        send: false,
    });
    click(source.element);
    click(source.element);
    assert.deepEqual(r.warnings, [
        "Unknown client command 'eval': skipped",
        "Unknown client command 'toString': skipped",
        "Client command setAttribute refuses the event handler attribute 'onclick'",
        "Client command removeAttribute refuses the event handler attribute 'OnLoad'",
    ]);
    assert.deepEqual([...fake(panel).classes], ['ok']);
    assert.equal(fake(panel).attributes.size, 0);
});

test('a command the DOM refuses is reported, the next ones run, the event is sent', () => {
    const r = run();
    const source = r.widget(1);
    const panel = r.widget(2);
    listen(r, source.element, { commands: [['addClass', 2, 'two words'], ['addClass', 2, 'next']] });
    click(source.element);
    assert.equal(r.errors.length, 1);
    assert.equal((r.errors[0] as Error).message, 'InvalidCharacterError');
    assert.equal(fake(panel).classes.has('next'), true);
    assert.equal(r.sent.length, 1);
});

test('a command that dispatches an event reaches the listeners by name of the target, once per listener', () => {
    const r = run();
    const source = r.widget(1);
    const target = r.widget(2);
    listenByName(r.context, 2, target.element, '{"name":"open","fields":["type","bubbles"],"commands":[["addClass",2,"seen"]]}');
    listen(r, source.element, { commands: [['dispatch', 2, 'open']], send: false });
    click(source.element);
    assert.deepEqual(r.sent, [{ [C.OBJECT_ID]: 2, [C.EVENT]: 'open', [C.EVENT_FIELDS]: { type: 'open', bubbles: true } }]);
    assert.equal(fake(target).classes.has('seen'), true);
});

test('a command that dispatches the event of its own listener does not run its commands again', () => {
    const r = run();
    const source = r.widget(1);
    const panel = r.widget(2);
    listen(r, source.element, { commands: [['toggleClass', 2, 'on'], ['dispatch', 1, 'click']] });
    click(source.element);
    assert.equal(fake(panel).classes.has('on'), true, 'toggled once, not twice, not for ever');
    assert.equal(r.sent.length, 2, 'both events are sent: the nested one, then the first');
    click(source.element);
    assert.equal(fake(panel).classes.has('on'), false, 'the guard is released');
});

test('a listener without commands is the one it was', () => {
    const r = run();
    const source = r.widget(1);
    listen(r, source.element, {});
    click(source.element);
    assert.deepEqual(r.sent, [{ [C.OBJECT_ID]: 1, [C.EVENT]: 'click' }]);
});
