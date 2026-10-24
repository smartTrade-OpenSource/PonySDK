/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { WidgetType } from '#model';
import { nameOf } from './names.ts';
import type { Message } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import type { Windows } from './windows.ts';

/** The JavaScript object an addon factory returns (AbstractAddon in ponysdk.js). */
export interface AddOn {
    onInit(): void;
    onAttached(): void;
    onDetached(): void;
    update(method: string, args: unknown): void;
    destroy(): void;
}

/** Registered by ponysdk.js for each AbstractAddon.defineAddon. */
export type AddOnFactory = (id: number, args: unknown, widgetId: string | null, element: HTMLElement | null) => AddOn;

/** What a terminal object knows of its terminal session. */
export interface Session {
    readonly contextId: number;
    readonly tabindexOnlyFormField: boolean;
    /** The URL of the page without its file, query and hash: where the server's paths start. */
    readonly hostPageBaseURL: string;
}

/** A window opened by the server, or a frame: a terminal of its own, fed by this one. */
export interface ChildTerminal {
    readonly ready: boolean;
    postMessage(bytes: Uint8Array): void;
    setReady(): void;
}

/** What terminal objects reach of the terminal. */
export interface Context {
    readonly session: Session;
    readonly windows: Windows;
    /** Sends an object's message, wrapped in APPLICATION_INSTRUCTIONS as the server expects events. */
    send(message: Message): void;
    /** Sends a message as is. */
    sendRaw(message: Message): void;
    /** The object with this id; a missing one is reported to the server. */
    object(id: number): TerminalObject | undefined;
    addOnFactory(signature: string): AddOnFactory | undefined;
    /** Logs and reports to the server. */
    warn(message: string, objectId?: number): void;
    /** Logs and reports an error to the server. */
    error(error: unknown): void;
    /** Runs a callback of the browser (an event, a timer), reporting what it throws. */
    guard(action: () => void): void;
}

/** An object the server creates, updates and collects: a widget, an addon, the cookies... */
export interface TerminalObject {
    readonly id: number;
    /** Applies the model just read; false when this object has no such property. */
    update(reader: Reader): boolean;
    add(reader: Reader, child: TerminalObject): void;
    remove(reader: Reader, child: TerminalObject): void;
    addHandler(reader: Reader, handler: number): void;
    removeHandler(reader: Reader, handler: number): void;
    /** Collected by the server: releases what it holds. */
    destroy(): void;
}

/** Creates the object of a TYPE_CREATE block, reading what the block carries before the properties. */
export type ObjectFactory = (type: number, context: Context, id: number, reader: Reader) => TerminalObject | null;

/** Properties by model key: how an object type applies each one. */
export type Setters<T> = ReadonlyMap<number, (target: T, reader: Reader) => void>;

/** Applies the model just read if the table has it. */
export function apply<T>(setters: Setters<T>, target: T, reader: Reader): boolean {
    const setter = setters.get(reader.key);
    if (setter === undefined) return false;
    setter(target, reader);
    return true;
}

/** An object with nothing to do but what it overrides (AbstractPTObject). */
export class BaseObject implements TerminalObject {
    readonly context: Context;
    readonly id: number;
    /** WidgetType, -1 when created otherwise; for messages. */
    readonly type: number;

    constructor(context: Context, id: number, type: number) {
        this.context = context;
        this.id = id;
        this.type = type;
    }

    update(_reader: Reader): boolean {
        return false;
    }

    add(_reader: Reader, _child: TerminalObject): void {
    }

    remove(_reader: Reader, _child: TerminalObject): void {
    }

    addHandler(_reader: Reader, _handler: number): void {
    }

    removeHandler(_reader: Reader, _handler: number): void {
    }

    destroy(): void {
    }

    toString(): string {
        return `${nameOf(WidgetType, this.type) ?? 'object'} #${this.id}`;
    }
}
