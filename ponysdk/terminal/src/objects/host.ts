/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, ServerToClientModel as K } from '#model';
import { about } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import { hostAttributes } from '../runtime/embedding.ts';
import { BaseObject, type Context } from '../runtime/object.ts';
import { customEventMessage } from './web-component.ts';

/**
 * PHost: the <pony-app> element this terminal runs in, as its application sees it
 * (docs/decisions/0008-embedded-application.md). It reports the changes of the element's attributes, sends the
 * host's events the application listens to, and dispatches the application's events on the element.
 */
export class HostObject extends BaseObject {
    private readonly element: HTMLElement;
    /** The attributes as last sent: the server knows them. */
    private sent: Record<string, string>;
    /** Every attribute name sent: after a reconnection, those gone are sent as removed. */
    private readonly names: Set<string>;
    private readonly observer: MutationObserver;
    private readonly listeners = new Map<string, AbortController>();
    private readonly lifetime = new AbortController();
    /** The events the application dispatched: not delivered back to it. */
    private readonly own = new WeakSet<Event>();

    static create(context: Context, id: number, type: number): HostObject {
        const embedding = context.session.embedding!; // the server has a PHost for an embedded application only
        return new HostObject(context, id, type, embedding.element, embedding.attributes);
    }

    private constructor(context: Context, id: number, type: number, element: HTMLElement,
                        attributes: Readonly<Record<string, string>>) {
        super(context, id, type);
        this.element = element;
        this.sent = { ...attributes };
        this.names = new Set(Object.keys(attributes));
        this.observer = new MutationObserver(() => context.guard(() => this.sendChanges(false)));
        this.observer.observe(element, { attributes: true });
        // A change made while the connection was lost went nowhere: all of them again
        element.addEventListener('pony-reconnected', () => context.guard(() => this.sendChanges(true)),
            { signal: this.lifetime.signal });
        this.sendChanges(false); // those made since the handshake
    }

    /** The attributes changed since the last sent, or all of them with those gone as removed. */
    private sendChanges(all: boolean): void {
        const current = hostAttributes(this.element);
        const changes: Record<string, string | null> = {};
        let changed = false;
        for (const [name, value] of Object.entries(current)) {
            this.names.add(name);
            if (all || this.sent[name] !== value) {
                changes[name] = value;
                changed = true;
            }
        }
        for (const name of all ? this.names : Object.keys(this.sent)) {
            if (!(name in current)) {
                changes[name] = null;
                changed = true;
            }
        }
        this.sent = current;
        if (!changed) return;
        const message = about(this.id);
        message[C.HOST_ATTRIBUTES] = changes;
        this.context.send(message);
    }

    override update(reader: Reader): boolean {
        switch (reader.key) {
            case K.WC_LISTEN_EVENT:
                this.listenTo(reader.str);
                return true;
            case K.WC_UNLISTEN_EVENT: {
                const name = reader.str;
                this.listeners.get(name)?.abort();
                this.listeners.delete(name);
                return true;
            }
            case K.DISPATCH_EVENT: {
                const [type, detail] = reader.array as [string, unknown];
                // An object or an array comes parsed (reader.ts, json()), another value as its JSON text, none as null
                this.dispatch(type, typeof detail === 'string' ? JSON.parse(detail) : detail);
                return true;
            }
            default:
                return super.update(reader);
        }
    }

    private listenTo(name: string): void {
        const controller = new AbortController(); // aborted by WC_UNLISTEN_EVENT and destroy()
        this.listeners.set(name, controller);
        this.element.addEventListener(name, event => this.context.guard(() => {
            if (!this.own.has(event)) this.context.send(customEventMessage(this.id, name, event));
        }), { signal: controller.signal });
    }

    private dispatch(type: string, detail: unknown): void {
        const event = new CustomEvent(type, { detail, bubbles: true, composed: true });
        this.own.add(event);
        this.element.dispatchEvent(event);
    }

    override destroy(): void {
        this.observer.disconnect();
        this.lifetime.abort();
        for (const controller of this.listeners.values()) controller.abort();
        this.listeners.clear();
    }
}
