/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C } from '#model';
import { about } from '../protocol/messages.ts';
import type { Context } from './object.ts';

/** LISTEN_EVENT's JSON (docs/decisions/0003-protocol-v4-listeners.md). */
interface Options {
    name: string;
    fields?: string[];
    throttle?: number;
    debounce?: number;
    prevent?: boolean;
    stop?: boolean;
    capture?: boolean;
}

type Fields = Record<string, unknown>;

/** A value as the server reads it: a string, a number, a boolean or null; anything else as JSON text. */
function plain(value: unknown): unknown {
    if (value === undefined || value === null) return null;
    if (typeof value === 'number') return Number.isFinite(value) ? value : null;
    if (typeof value === 'string' || typeof value === 'boolean') return value;
    try {
        return JSON.stringify(value) ?? null;
    } catch {
        return String(value); // a DOM node, a cycle
    }
}

/** The fields at their paths on the event (target.value, clientX, detail...), read when it happens. */
export function readFields(event: Event, paths: readonly string[]): Fields {
    const fields: Fields = {};
    for (const path of paths) {
        let value: unknown = event;
        for (const part of path.split('.')) {
            if (value === null || value === undefined) break;
            value = (value as Record<string, unknown>)[part];
        }
        fields[path] = plain(value);
    }
    return fields;
}

/**
 * Sends at the rate the options set: with debounce, the last event once none came for the delay; with throttle, the
 * first event at once and the last of each period at its end; else every event.
 */
function rateLimited(send: (fields: Fields | null) => void, options: Options, signal: AbortSignal): (fields: Fields | null) => void {
    const debounce = options.debounce ?? 0;
    const throttle = options.throttle ?? 0;
    let timer: ReturnType<typeof setTimeout> | undefined;
    signal.addEventListener('abort', () => clearTimeout(timer));
    if (debounce > 0) {
        return fields => {
            clearTimeout(timer);
            timer = setTimeout(() => send(fields), debounce);
        };
    }
    if (throttle > 0) {
        let pending: { fields: Fields | null } | null = null;
        const close = (): void => {
            timer = undefined;
            if (pending === null) return;
            const last = pending;
            pending = null;
            send(last.fields);
            timer = setTimeout(close, throttle);
        };
        return fields => {
            if (timer !== undefined) {
                pending = { fields };
                return;
            }
            send(fields);
            timer = setTimeout(close, throttle);
        };
    }
    return send;
}

/** Listens to a DOM event by name for an object (LISTEN_EVENT); the returned controller stops it (UNLISTEN_EVENT). */
export function listenByName(context: Context, objectId: number, element: EventTarget, json: string): [string, AbortController] {
    const options = JSON.parse(json) as Options;
    const controller = new AbortController();
    const signal = controller.signal;
    const deliver = rateLimited(fields => {
        const message = about(objectId);
        message[C.EVENT] = options.name;
        if (fields !== null) message[C.EVENT_FIELDS] = fields;
        context.send(message);
    }, options, signal);
    const paths = options.fields ?? [];
    element.addEventListener(options.name, event => context.guard(() => {
        if (options.prevent) event.preventDefault();
        if (options.stop) event.stopPropagation();
        deliver(paths.length > 0 ? readFields(event, paths) : null);
    }), { signal, capture: options.capture === true, passive: options.prevent !== true });
    return [options.name, controller];
}
