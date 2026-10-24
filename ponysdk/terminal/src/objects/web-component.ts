/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, ServerToClientModel as K } from '#model';
import { about } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import { apply, type Context, type Setters, type TerminalObject } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';

/**
 * PWebComponent: a custom element driven by its properties, attributes, methods and events. Children go to a named
 * slot (WC_SLOT_NAME), to an index, or at the end.
 */
export class WebComponent extends Widget {
    private readonly listeners = new Map<string, AbortController>();

    static create(context: Context, id: number, type: number, reader: Reader): WebComponent {
        reader.read(); // TAG
        return new WebComponent(context, id, type, document.createElement(reader.str ?? 'div'));
    }

    override update(reader: Reader): boolean {
        return apply(WEB_COMPONENT, this, reader) || super.update(reader);
    }

    override add(reader: Reader, child: TerminalObject): void {
        reader.read();
        if (reader.key === K.WC_SLOT_NAME && child instanceof Widget) {
            child.element.setAttribute('slot', reader.str ?? '');
            this.insert(child, this.children.length);
        } else if (reader.key === K.INDEX && child instanceof Widget) {
            this.insert(child, reader.int);
        } else {
            if (reader.key !== K.WC_SLOT_NAME && reader.key !== K.INDEX) reader.rewind();
            super.add(reader, child);
        }
    }

    /** A custom event, sent with its detail as JSON. */
    listenTo(name: string): void {
        if (this.listeners.has(name)) return;
        const controller = new AbortController(); // aborted by stopListening and destroy()
        this.listeners.set(name, controller);
        const signal = controller.signal;
        this.element.addEventListener(name, event => this.context.guard(() => {
            const message = about(this.id);
            message[C.WC_EVENT_NAME] = name;
            const detail = (event as CustomEvent).detail as unknown;
            if (detail !== undefined && detail !== null) message[C.WC_EVENT_DETAIL] = toJson(detail);
            this.context.send(message);
        }), { signal });
    }

    override destroy(): void {
        for (const controller of this.listeners.values()) controller.abort();
        this.listeners.clear();
        super.destroy();
    }

    stopListening(name: string): void {
        this.listeners.get(name)?.abort();
        this.listeners.delete(name);
    }

    property(name: string, json: string | null): void {
        const element = this.element as unknown as Record<string, unknown>;
        try {
            element[name] = JSON.parse(json ?? 'null');
        } catch {
            element[name] = json; // not JSON: a plain string
        }
    }

    /**
     * A JSON merge patch (RFC 7396) of an object property, applied to a shallow copy that is then assigned:
     * components that detect changes by identity (Lit's hasChanged is !==) would miss a change made in place.
     */
    patch(name: string, json: string | null): void {
        const element = this.element as unknown as Record<string, unknown>;
        const current = element[name];
        const next: Record<string, unknown> =
            typeof current === 'object' && current !== null && !Array.isArray(current) ? { ...current } : {};
        for (const [key, value] of Object.entries(JSON.parse(json ?? '{}') as Record<string, unknown>)) {
            if (value === null) delete next[key];
            else next[key] = value;
        }
        element[name] = next;
    }

    call(method: string, args: unknown[] | null): void {
        const fn = (this.element as unknown as Record<string, unknown>)[method];
        if (typeof fn === 'function') fn.apply(this.element, args ?? []);
    }
}

function toJson(detail: unknown): string {
    try {
        return JSON.stringify(detail);
    } catch {
        return String(detail);
    }
}

function next(reader: Reader): Reader {
    reader.read();
    return reader;
}

const WEB_COMPONENT: Setters<WebComponent> = new Map<number, (w: WebComponent, r: Reader) => void>([
    [K.WC_SET_PROPERTY, (w, r) => {
        const name = r.str ?? '';
        w.property(name, next(r).str);
    }],
    [K.WC_PATCH_PROPERTY, (w, r) => {
        const name = r.str ?? '';
        w.patch(name, next(r).str);
    }],
    [K.WC_REMOVE_PROPERTY, (w, r) => {
        delete (w.element as unknown as Record<string, unknown>)[r.str ?? ''];
    }],
    [K.WC_SET_ATTRIBUTE, (w, r) => {
        const name = r.str ?? '';
        w.element.setAttribute(name, next(r).str ?? '');
    }],
    [K.WC_REMOVE_ATTRIBUTE, (w, r) => w.element.removeAttribute(r.str ?? '')],
    [K.WC_LISTEN_EVENT, (w, r) => w.listenTo(r.str ?? '')],
    [K.WC_UNLISTEN_EVENT, (w, r) => w.stopListening(r.str ?? '')],
    [K.WC_CALL_METHOD, (w, r) => {
        const method = r.str ?? '';
        r.read();
        if (r.key === K.WC_METHOD_ARGS) {
            w.call(method, r.array);
        } else {
            r.rewind();
            w.call(method, null);
        }
    }],
]);
