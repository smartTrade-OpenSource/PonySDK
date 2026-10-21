/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, HandlerModel as H, ServerToClientModel as K, WidgetType } from '#model';
import { FLAG, type Message } from '../protocol/messages.ts';
import { INCOMPLETE, Reader } from '../protocol/reader.ts';
import { describe, log } from './log.ts';
import { nameOf } from './names.ts';
import type { AddOnFactory, ChildTerminal, Context, ObjectFactory, Session, TerminalObject } from './object.ts';
import { Windows } from './windows.ts';

/** The browser history, as the dispatcher uses it. */
export interface HistoryPort {
    readonly token: string;
    newItem(token: string, fireEvents: boolean): void;
    fireCurrent(): void;
}

/** What the dispatcher asks of the terminal around it. */
export interface Host {
    /** Where messages go: the WebSocket, or the terminal of the opener or the parent frame. */
    send(message: Message): void;
    /** CREATE_CONTEXT. False when nothing more must be read: the page reloads. */
    contextCreated(id: number, tabindexOnlyFormField: boolean, heartbeatSeconds: number): boolean;
    /** RECONNECT_CONTEXT: the server resumed this context. */
    contextResumed(): void;
    /** DESTROY_CONTEXT: the server ended this context. */
    contextDestroyed(): void;
    /** HANDLER_STREAM_REQUEST: the server has a file for this page. */
    streamRequested(requestId: number): void;
}

/**
 * Applies the server's instructions to the terminal objects and sends their messages back. An
 * instruction is a block: its first model says what to do (TYPE_CREATE, TYPE_UPDATE...) to which
 * object, the next models how, END closes it. A failing instruction is reported and skipped; the next one runs.
 */
export class Dispatcher implements Context {
    readonly reader: Reader;
    readonly windows = new Windows();
    private readonly objects = new Map<number, TerminalObject>();
    private readonly addOnFactories = new Map<string, AddOnFactory>();
    private readonly host: Host;
    private readonly factory: ObjectFactory;
    readonly session: Session;
    private readonly history: HistoryPort;
    /** The window the blocks being read belong to; -1 until the first WINDOW_ID. */
    private currentWindowId = -1;

    constructor(host: Host, factory: ObjectFactory, session: Session, history: HistoryPort,
                dictionaryFallback?: (id: number) => string | undefined) {
        this.host = host;
        this.factory = factory;
        this.session = session;
        this.history = history;
        this.reader = new Reader(dictionaryFallback);
    }

    // ---- terminals ---------------------------------------------------------------------------------------------------

    /** A message of the WebSocket: the main window's blocks, and the blocks of the windows and frames it feeds. */
    receive(message: Uint8Array): void {
        const started = Date.now();
        const r = this.reader;
        r.feed(message);
        while (r.hasKey()) {
            const next = r.blockEnd();
            if (next === INCOMPLETE) return;
            const key = r.read();
            if (key === K.ROUNDTRIP_LATENCY) {
                this.host.send({ [C.TERMINAL_LATENCY]: r.num });
                r.read(); // END
            } else if (key === K.CREATE_CONTEXT) {
                const id = r.int;
                r.read();
                const tabindexOnlyFormField = r.bool;
                r.read();
                const heartbeatSeconds = r.int;
                r.read(); // END
                if (!this.host.contextCreated(id, tabindexOnlyFormField, heartbeatSeconds)) return;
            } else if (key === K.RECONNECT_CONTEXT) {
                this.host.contextResumed();
                r.read(); // END
            } else if (key === K.DESTROY_CONTEXT) {
                this.host.contextDestroyed();
                r.read(); // END
            } else {
                this.route(key, next);
            }
        }
        // A long batch kept the event loop busy: tell the server this terminal is alive
        if (Date.now() - started > 1000) this.host.send({ [C.HEARTBEAT_REQUEST]: FLAG });
    }

    /** A message from the opener, in a window: its blocks, and those of the frames it feeds. */
    receiveInWindow(message: Uint8Array): void {
        const r = this.reader;
        r.feed(message);
        while (r.hasKey()) {
            if (r.read() === K.FRAME_ID) this.forwardToFrame(r.int, r.blockEnd());
            else this.instruction();
        }
    }

    /** A message from the parent, in a frame: one block. */
    receiveInFrame(message: Uint8Array): void {
        this.reader.feed(message);
        this.reader.read();
        this.instruction();
    }

    /** A block of the main window runs here; one of another window goes to it with the blocks that follow. */
    private route(key: number, next: number): void {
        const r = this.reader;
        const previousWindowId = this.currentWindowId;
        if (key === K.WINDOW_ID) this.currentWindowId = r.int;

        if (this.currentWindowId === this.windows.mainWindowId || previousWindowId === -1) {
            if (key === K.WINDOW_ID) r.read();
            if (r.key === K.FRAME_ID) this.forwardToFrame(r.int, next);
            else this.instruction();
            return;
        }

        if (key !== K.WINDOW_ID) r.rewind();
        const window = this.windows.get(this.currentWindowId);
        if (window === undefined || !window.ready) {
            r.skipBlock();
            return;
        }
        const start = r.offset;
        let end = next;
        r.offset = end;
        while (r.hasKey()) {
            const blockEnd = r.blockEnd();
            if (blockEnd === INCOMPLETE) break;
            const first = r.read();
            if (first === K.WINDOW_ID || first === K.ROUNDTRIP_LATENCY || first === K.CREATE_CONTEXT
                || first === K.RECONNECT_CONTEXT || first === K.DESTROY_CONTEXT) break;
            end = blockEnd;
            r.offset = end;
        }
        window.postMessage(r.slice(start, end));
    }

    private forwardToFrame(frameId: number, end: number): void {
        const frame = this.object(frameId) as Partial<ChildTerminal> | undefined;
        const bytes = this.reader.slice(this.reader.offset, end);
        if (frame?.postMessage) frame.postMessage(bytes);
    }

    // ---- instructions ------------------------------------------------------------------------------------------------

    /** Runs the instruction whose first model was just read. */
    private instruction(): void {
        const r = this.reader;
        const key = r.key;
        try {
            switch (key) {
                case K.TYPE_CREATE:
                    this.create(r.int);
                    break;
                case K.TYPE_UPDATE:
                    this.updateObjects(r.int);
                    break;
                case K.TYPE_ADD:
                    this.add(r.int);
                    break;
                case K.TYPE_REMOVE:
                    this.removeObject(r.int);
                    break;
                case K.TYPE_GC:
                    this.collect(r.int);
                    break;
                case K.TYPE_ADD_HANDLER:
                    this.addHandler(r.int);
                    break;
                case K.TYPE_REMOVE_HANDLER: {
                    // Protocol 4 names the object (WIDGET_ID). Below it, the NULL model carries none and the last int
                    // read stood for it, as in the GWT terminal (ADR-0003).
                    const previous = r.int;
                    if (r.read() === K.WIDGET_ID) this.removeHandler(r.int);
                    else {
                        r.rewind();
                        this.removeHandler(previous);
                    }
                    break;
                }
                case K.TYPE_HISTORY:
                    this.historyItem(r.str ?? '');
                    break;
                default:
                    log.warn(`Unknown instruction ${Reader.keyName(key)}`);
                    if (key !== K.END) r.skipBlock();
            }
        } catch (e) {
            if (key !== K.END) r.skipBlock();
            this.error(e);
        }
    }

    private create(id: number): void {
        const r = this.reader;
        r.read(); // WIDGET_TYPE
        const type = r.int;
        const object = this.factory(type, this, id, r);
        if (object === null) {
            log.warn(`Cannot create #${id} of type ${nameOf(WidgetType, type) ?? type}`);
            r.skipBlock();
            return;
        }
        this.objects.set(id, object);
        this.updateObjects(id);
    }

    /**
     * An update block. Since protocol 2 it may run over several objects: UPDATE_NEXT (+1), UPDATE_NEXT_DELTA
     * (+2 + byte) and UPDATE_NEXT_ID (absolute) switch to the next one. Since protocol 3 a create block ends with
     * PARENT_OBJECT_ID, which adds the new object to its parent. The properties of an unknown object are skipped.
     */
    private updateObjects(firstId: number): void {
        const r = this.reader;
        let id = firstId;
        let object = this.updated(id);
        while (r.hasKey()) {
            const key = r.read();
            switch (key) {
                case K.END:
                    return;
                case K.UPDATE_NEXT:
                    id += 1;
                    object = this.updated(id);
                    break;
                case K.UPDATE_NEXT_DELTA:
                    id += 2 + (r.int & 0xff);
                    object = this.updated(id);
                    break;
                case K.UPDATE_NEXT_ID:
                    id = r.int;
                    object = this.updated(id);
                    break;
                case K.PARENT_OBJECT_ID: {
                    const parent = this.object(r.int);
                    if (object !== undefined && parent !== undefined) parent.add(r, object);
                    else log.warn(`Cannot add #${id} to the unknown parent #${r.int}`);
                    break;
                }
                default:
                    if (object !== undefined && !object.update(r)) {
                        log.warn(`${object} has no property ${Reader.keyName(key)}: the rest of its block is skipped`);
                        r.skipBlock();
                        return;
                    }
            }
        }
    }

    private updated(id: number): TerminalObject | undefined {
        const object = this.object(id);
        if (object === undefined) log.warn(`Update of the unknown #${id}: its properties are skipped`);
        return object;
    }

    private add(id: number): void {
        const r = this.reader;
        const object = this.object(id);
        if (object === undefined) {
            r.skipBlock();
            return;
        }
        r.read(); // PARENT_OBJECT_ID
        const parent = this.object(r.int);
        if (parent === undefined) {
            log.warn(`Cannot add ${object} to the unknown #${r.int}`);
            r.skipBlock();
            return;
        }
        parent.add(r, object);
        r.read(); // END
    }

    private removeObject(id: number): void {
        const r = this.reader;
        const object = this.object(id);
        if (object === undefined) {
            r.skipBlock();
            return;
        }
        r.read(); // PARENT_OBJECT_ID
        const parent = r.int === -1 ? object : this.object(r.int);
        if (parent === undefined) {
            log.warn(`Cannot remove ${object} from the unknown #${r.int}`);
            r.skipBlock();
            return;
        }
        parent.remove(r, object);
        r.read(); // END
    }

    private addHandler(id: number): void {
        const r = this.reader;
        r.read(); // HANDLER_TYPE
        const handler = r.int;
        if (handler === H.HANDLER_STREAM_REQUEST) {
            r.read(); // STREAM_REQUEST_ID
            this.host.streamRequested(r.int);
            r.read(); // END
            return;
        }
        const object = this.object(id);
        if (object === undefined) {
            r.skipBlock();
            return;
        }
        object.addHandler(r, handler);
        r.read(); // END
    }

    private removeHandler(id: number): void {
        const r = this.reader;
        const object = this.object(id);
        if (object === undefined) {
            r.skipBlock();
            return;
        }
        r.read(); // HANDLER_TYPE
        object.removeHandler(r, r.int);
        r.read(); // END
    }

    private historyItem(token: string): void {
        const r = this.reader;
        r.read(); // HISTORY_FIRE_EVENTS
        const fireEvents = r.bool;
        if (token === this.history.token) {
            if (fireEvents) this.history.fireCurrent();
        } else {
            this.history.newItem(token, fireEvents);
        }
        r.read(); // END
    }

    private collect(id: number): void {
        const object = this.objects.get(id);
        if (object === undefined) {
            log.warn(`Cannot collect the unknown #${id}`);
            this.reader.skipBlock();
            return;
        }
        this.objects.delete(id);
        object.destroy();
        this.reader.read(); // END
    }

    // ---- objects -----------------------------------------------------------------------------------------------------

    /** Registers an object the terminal creates itself (the cookies, #0). */
    register(object: TerminalObject): void {
        this.objects.set(object.id, object);
    }

    object(id: number): TerminalObject | undefined {
        const object = this.objects.get(id);
        if (object === undefined) this.warn(`PTObject #${id} not found`, id);
        return object;
    }

    registerAddOnFactory(signature: string, factory: AddOnFactory): void {
        this.addOnFactories.set(signature, factory);
    }

    addOnFactory(signature: string): AddOnFactory | undefined {
        return this.addOnFactories.get(signature);
    }

    setReady(id: number, kind: 'window' | 'frame'): void {
        const child = kind === 'window' ? this.windows.get(id) : this.object(id) as Partial<ChildTerminal> | undefined;
        if (child?.setReady) child.setReady();
        else log.warn(`No ${kind} #${id}`);
    }

    // ---- messages ----------------------------------------------------------------------------------------------------

    send(message: Message): void {
        this.host.send({ [C.APPLICATION_INSTRUCTIONS]: [message] });
    }

    sendRaw(message: Message): void {
        this.host.send(message);
    }

    warn(message: string, objectId?: number): void {
        log.warn(message);
        const warning: Message = { [C.WARN_MSG]: message };
        if (objectId !== undefined) warning[C.OBJECT_ID] = objectId;
        this.host.send(warning);
    }

    error(error: unknown): void {
        log.error('PonySDK has encountered an internal error', error);
        this.host.send({ [C.ERROR_MSG]: describe(error) });
    }

    guard(action: () => void): void {
        try {
            action();
        } catch (e) {
            this.error(e);
        }
    }
}
