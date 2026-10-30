/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import type { Context } from './object.ts';
import type { Widget } from './widget.ts';

/**
 * The client commands of an event listener (docs/decisions/0027-client-commands.md): named operations on widgets, run
 * in the browser when the event happens. Each entry of LISTEN_EVENT's "commands" is [operation, target id, arguments...].
 * Never code: an operation the table does not have is refused, so the page needs no 'unsafe-eval'.
 */
type Operation = (target: Widget, a: string, b: string) => void;

/** removeClass is removeStyleNames: the widget's own type class stays, as for the server's removal. */
const OPERATIONS: Record<string, Operation> = {
    show: target => target.setVisible(true),
    hide: target => target.setVisible(false),
    // what setVisible renders: the server's visibility and the browser's are the same state, set on the same element
    toggle: target => target.setVisible(target.element.style.display === 'none'),
    addClass: (target, name) => target.element.classList.add(name),
    removeClass: (target, name) => target.removeStyleNames([name]),
    toggleClass: (target, name) => target.element.classList.toggle(name),
    setAttribute: (target, name, value) => target.element.setAttribute(name, value),
    removeAttribute: (target, name) => target.element.removeAttribute(name),
    focus: target => target.focusTarget.focus(),
    dispatch: (target, name) => {
        target.element.dispatchEvent(new CustomEvent(name, { bubbles: true }));
    },
};

/** An attribute that would run code: the server refuses these names, and so does the terminal that receives them. */
const HANDLER = /^on/i;

type Run = () => void;

/**
 * The commands of a listener, ready to run. A command the terminal does not know, or with an argument it refuses, is
 * reported once, here, and left out: the others run. A target is looked up when the event happens: one destroyed
 * since (or not created yet) is skipped without a word.
 */
export function compileCommands(context: Context, listenerId: number, entries: readonly unknown[]): Run[] {
    const runs: Run[] = [];
    for (const entry of entries) {
        const [name, id, a, b] = entry as [string, number, string | undefined, string | undefined];
        const operation = Object.hasOwn(OPERATIONS, name) ? OPERATIONS[name] : undefined;
        if (operation === undefined) {
            context.warn(`Unknown client command '${name}': skipped`, listenerId);
        } else if ((name === 'setAttribute' || name === 'removeAttribute') && HANDLER.test(a ?? '')) {
            context.warn(`Client command ${name} refuses the event handler attribute '${a}'`, listenerId);
        } else {
            runs.push(() => {
                const target = widget(context, id);
                if (target !== undefined) operation(target, a ?? '', b ?? '');
            });
        }
    }
    return runs;
}

/** The widget with this id, unreported when it is gone: a destroyed target is no error. */
function widget(context: Context, id: number): Widget | undefined {
    const object = context.peek(id) as Partial<Widget> | undefined;
    return object?.element === undefined ? undefined : (object as Widget);
}

/** Runs the commands in order: one that throws (an invalid name for the DOM) is reported, the next ones run. */
export function runCommands(context: Context, runs: readonly Run[]): void {
    for (const run of runs) {
        try {
            run();
        } catch (error) {
            context.error(error);
        }
    }
}
