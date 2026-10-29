/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, HandlerModel as H, ServerToClientModel as K, WidgetType } from '#model';
import type { Message } from '../protocol/messages.ts';
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
    contextCreated(id: number, tabindexOnlyFormField: boolean, heartbeatSeconds: number, keys: string | null): boolean;
    /** RECONNECT_CONTEXT: the server resumed this context. */
    contextResumed(): void;
    /** DESTROY_CONTEXT: the server ended this context. */
    contextDestroyed(): void;
    /** HANDLER_STREAM_REQUEST: the server has a file for this page. */
    streamRequested(requestId: number): void;
    /** HANDLER_COOKIE_REQUEST: a cookie of an embedded application, set by the response to this request (ADR-0009). */
    cookieRequested(requestId: number): void;
}

/**
 * The addon factories, one table per page: ponysdk.js registers them on the page, and the terminals of the page's
 * <pony-app> elements create their addons from them (ADR-0008), whichever PonySDK version each terminal is: the
 * table is the page's, not this module's (ADR-0009, decision 10).
 */
const addOnFactories: Map<string, AddOnFactory> =
    (globalThis as { ponyAddOnFactories?: Map<string, AddOnFactory> }).ponyAddOnFactories ??= new Map();

/** Registers an addon factory for the terminals of the page (AbstractAddon.defineAddon, through window.pony). */
export function registerAddOnFactory(signature: string, factory: AddOnFactory): void {
    addOnFactories.set(signature, factory);
}

/** A block creates an object whose module is not loaded yet: the block runs again once it is. */
class Pending {
    readonly loading: Promise<void>;

    constructor(loading: Promise<void>) {
        this.loading = loading;
    }
}

const NOTHING = new Uint8Array(0);

/** What the inspector watches, in debug mode (src/inspector.ts); the dispatcher's watcher is null otherwise. */
export interface Watcher {
    /** A message of the server: its bytes. */
    frame(bytes: number): void;
    /** The bytes an instruction (TYPE_CREATE, TYPE_UPDATE...) spent on an object: an update run counts each object. */
    block(instruction: number, id: number, bytes: number): void;
    /** A message this terminal sends to the server. */
    sent(message: Message): void;
}

/** What happens from the first DEBUG_INFO until the inspector is loaded: it replays it. */
export class Recording implements Watcher {
    readonly events: (readonly [number, number, number] | Message)[] = [];

    frame(bytes: number): void {
        this.events.push([-1, 0, bytes]);
    }

    block(instruction: number, id: number, bytes: number): void {
        this.events.push([instruction, id, bytes]);
    }

    sent(message: Message): void {
        this.events.push(message);
    }
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
    private readonly host: Host;
    private readonly factory: ObjectFactory;
    /** The loading of a type's module, or null when it is loaded (docs/decisions/0004-terminal-chunks.md). */
    private readonly loader: (type: number) => Promise<void> | null;
    /** Set while a block waits for a module: the blocks that come meanwhile wait in the reader. */
    private paused = false;
    readonly session: Session;
    private readonly history: HistoryPort;
    /** The window the blocks being read belong to; -1 until the first WINDOW_ID. */
    private currentWindowId = -1;
    /** When the message being read arrived (performance.now()): a probe answers how long it waited here (ADR-0005). */
    private arrivedAt = 0;
    /** In debug mode, what the inspector watches; null otherwise: one test per block. */
    watcher: Watcher | null = null;
    /** Where the block being run starts: the inspector counts its bytes. */
    private blockStart = 0;
    /** What the inspector counts the next bytes of a create or an update block as: TYPE_CREATE, then TYPE_UPDATE. */
    private counted = 0;
    /** The bytes of the last message of the server: the one being read when the inspector starts counts. */
    private frameBytes = 0;

    constructor(host: Host, factory: ObjectFactory, session: Session, history: HistoryPort,
                dictionaryFallback?: (id: number) => string | undefined, loader: (type: number) => Promise<void> | null = () => null) {
        this.host = host;
        this.factory = factory;
        this.loader = loader;
        this.session = session;
        this.history = history;
        this.reader = new Reader(dictionaryFallback);
    }

    // ---- terminals ---------------------------------------------------------------------------------------------------

    /**
     * A message of the WebSocket: the main window's blocks, and the blocks of the windows and frames it feeds.
     * arrivedAt is when it arrived; while a module loads, the messages that come wait and count from the one that waits.
     */
    receive(message: Uint8Array, arrivedAt = performance.now()): void {
        if (message.byteLength > 0) {
            this.frameBytes = message.byteLength;
            if (this.watcher !== null) this.watcher.frame(message.byteLength);
        }
        const r = this.reader;
        r.feed(message);
        if (this.paused) return;
        this.arrivedAt = arrivedAt;
        while (r.hasKey()) {
            const next = r.blockEnd();
            if (next === INCOMPLETE) return;
            const start = r.offset;
            this.blockStart = start;
            try {
                if (!this.receiveBlock(next)) return;
            } catch (e) {
                if (!(e instanceof Pending)) throw e;
                this.pause(start, e.loading, () => this.receive(NOTHING, this.arrivedAt));
                return;
            }
        }
    }

    /** False when the rest of the message is not for this page: a context the server could not resume reloads it. */
    private receiveBlock(next: number): boolean {
        const r = this.reader;
        const key = r.read();
        if (key === K.ROUNDTRIP_LATENCY) {
            this.host.send({
                [C.TERMINAL_LATENCY]: r.num,
                [C.TERMINAL_TIME]: Math.max(0, Math.round(performance.now() - this.arrivedAt)),
            });
            r.read(); // END
        } else if (key === K.CREATE_CONTEXT) {
            const id = r.int;
            r.read();
            const tabindexOnlyFormField = r.bool;
            r.read();
            const heartbeatSeconds = r.int;
            r.read(); // END, or first an embedded terminal's keys (ADR-0009)
            let keys: string | null = null;
            if (r.key === K.CONTEXT_KEYS) {
                keys = r.str;
                r.read(); // END
            }
            if (!this.host.contextCreated(id, tabindexOnlyFormField, heartbeatSeconds, keys)) return false;
        } else if (key === K.RECONNECT_CONTEXT) {
            for (const object of this.objects.values()) object.contextResumed?.();
            this.host.contextResumed();
            r.read(); // END
        } else if (key === K.DESTROY_CONTEXT) {
            this.host.contextDestroyed();
            r.read(); // END
        } else {
            this.route(key, next);
        }
        return true;
    }

    /** A message from the opener, in a window: its blocks, and those of the frames it feeds. */
    receiveInWindow(message: Uint8Array): void {
        const r = this.reader;
        r.feed(message);
        if (this.paused) return;
        while (r.hasKey()) {
            const start = r.offset;
            this.blockStart = start;
            try {
                if (r.read() === K.FRAME_ID) this.forwardToFrame(r.int, r.blockEnd());
                else this.instruction();
            } catch (e) {
                if (!(e instanceof Pending)) throw e;
                this.pause(start, e.loading, () => this.receiveInWindow(NOTHING));
                return;
            }
        }
    }

    /** A message from the parent, in a frame: one block, or those that waited for a module. */
    receiveInFrame(message: Uint8Array): void {
        const r = this.reader;
        r.feed(message);
        if (this.paused) return;
        while (r.hasKey()) {
            const start = r.offset;
            this.blockStart = start;
            try {
                r.read();
                this.instruction();
            } catch (e) {
                if (!(e instanceof Pending)) throw e;
                this.pause(start, e.loading, () => this.receiveInFrame(NOTHING));
                return;
            }
        }
    }

    /** The block from start runs again once the module is loaded, then those that came meanwhile, in order. */
    private pause(start: number, loading: Promise<void>, resume: () => void): void {
        this.reader.offset = start;
        this.paused = true;
        loading.then(() => {
            this.paused = false;
            this.guard(resume);
        }, e => {
            this.paused = false;
            log.error('A module of the terminal did not load: its widgets are not created', e);
            this.guard(resume);
        });
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
        if (this.watcher !== null) this.watcher.block(K.WINDOW_ID, this.currentWindowId, end - this.blockStart);
        window.postMessage(r.slice(start, end));
    }

    private forwardToFrame(frameId: number, end: number): void {
        if (this.watcher !== null) this.watcher.block(K.FRAME_ID, frameId, end - this.blockStart);
        const frame = this.object(frameId) as Partial<ChildTerminal> | undefined;
        const bytes = this.reader.slice(this.reader.offset, end);
        if (frame?.postMessage) frame.postMessage(bytes);
    }

    // ---- instructions ------------------------------------------------------------------------------------------------

    /** Runs the instruction whose first model was just read. */
    private instruction(): void {
        const r = this.reader;
        const key = r.key;
        const id = r.int; // the object of every instruction but TYPE_HISTORY
        try {
            switch (key) {
                case K.TYPE_CREATE:
                    this.create(r.int);
                    break;
                case K.TYPE_UPDATE:
                    this.counted = K.TYPE_UPDATE;
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
                    this.historyItem(r.str);
                    break;
                default:
                    log.warn(`Unknown instruction ${Reader.keyName(key)}`);
                    if (key !== K.END) r.skipBlock();
            }
        } catch (e) {
            if (e instanceof Pending) throw e; // the block runs again once its module is loaded
            if (key !== K.END) r.skipBlock();
            this.error(e);
        }
        // a create or an update counted its objects one by one
        if (this.watcher !== null && key !== K.TYPE_CREATE && key !== K.TYPE_UPDATE && key !== K.TYPE_HISTORY) {
            this.watcher.block(key, id, r.offset - this.blockStart);
        }
    }

    private create(id: number): void {
        const r = this.reader;
        r.read(); // WIDGET_TYPE
        const type = r.int;
        const loading = this.loader(type);
        if (loading !== null) throw new Pending(loading); // before any effect of the block
        const object = this.factory(type, this, id, r);
        if (object === null) {
            log.warn(`Cannot create #${id} of type ${nameOf(WidgetType, type) ?? type}`);
            r.skipBlock();
            return;
        }
        this.objects.set(id, object);
        this.counted = K.TYPE_CREATE;
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
                    if (this.watcher !== null) this.count(id, false);
                    return;
                case K.UPDATE_NEXT:
                    if (this.watcher !== null) this.count(id, true);
                    id += 1;
                    object = this.updated(id);
                    break;
                case K.UPDATE_NEXT_DELTA:
                    if (this.watcher !== null) this.count(id, true);
                    id += 2 + (r.int & 0xff);
                    object = this.updated(id);
                    break;
                case K.UPDATE_NEXT_ID:
                    if (this.watcher !== null) this.count(id, true);
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

    /**
     * Debug mode: the bytes of an object in a create or an update block, from where its models start to the key just
     * read, which is the next object's when next is set (read again: the hot path keeps no offset).
     */
    private count(id: number, next: boolean): void {
        const r = this.reader;
        if (next) r.rewind();
        this.watcher!.block(this.counted, id, r.offset - this.blockStart);
        this.counted = K.TYPE_UPDATE;
        this.blockStart = r.offset;
        if (next) r.read();
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
        const parent = this.object(r.int);
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
        if (handler === H.HANDLER_STREAM_REQUEST || handler === H.HANDLER_COOKIE_REQUEST) {
            r.read(); // STREAM_REQUEST_ID
            if (handler === H.HANDLER_STREAM_REQUEST) this.host.streamRequested(r.int);
            else this.host.cookieRequested(r.int);
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

    /** The object with this id, or undefined, unreported: the inspector's. */
    peek(id: number): TerminalObject | undefined {
        return this.objects.get(id);
    }

    object(id: number): TerminalObject | undefined {
        const object = this.objects.get(id);
        if (object === undefined) this.warn(`PTObject #${id} not found`, id);
        return object;
    }

    registerAddOnFactory(signature: string, factory: AddOnFactory): void {
        registerAddOnFactory(signature, factory);
    }

    addOnFactory(signature: string): AddOnFactory | undefined {
        return addOnFactories.get(signature);
    }

    /** The terminal goes (its <pony-app> element left the page): every object releases what it holds on the page. */
    destroyAll(): void {
        for (const object of [...this.objects.values()].reverse()) {
            try {
                object.destroy();
            } catch (e) {
                log.warn(`Cannot destroy ${object}: ${describe(e)}`);
            }
        }
        this.objects.clear();
    }

    setReady(id: number, kind: 'window' | 'frame'): void {
        const child = kind === 'window' ? this.windows.get(id) : this.object(id) as Partial<ChildTerminal> | undefined;
        if (child?.setReady) child.setReady();
        else log.warn(`No ${kind} #${id}`);
    }

    // ---- messages ----------------------------------------------------------------------------------------------------

    send(message: Message): void {
        if (this.watcher !== null) this.watcher.sent(message);
        this.host.send({ [C.APPLICATION_INSTRUCTIONS]: [message] });
    }

    sendRaw(message: Message): void {
        if (this.watcher !== null) this.watcher.sent(message);
        this.host.send(message);
    }

    /** DEBUG_INFO: the server is in debug mode. The first time, the inspector loads; what happens meanwhile is kept. */
    debugging(): void {
        if (this.watcher !== null) return;
        const recording = new Recording();
        recording.frame(this.frameBytes); // the message being read, whose first blocks went uncounted
        this.watcher = recording;
        import('../inspector.ts').then(m => m.install(this, recording))
            .catch((e: unknown) => log.error('The inspector did not start', e));
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
