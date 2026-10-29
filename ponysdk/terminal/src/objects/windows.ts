/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, ServerToClientModel as K } from '#model';
import { about, FLAG } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import type { BridgedChild, ChildBridge } from '../runtime/embedding.ts';
import type { Windows } from '../runtime/windows.ts';
import { log } from '../runtime/log.ts';
import { BaseObject, type ChildTerminal, type Context } from '../runtime/object.ts';

/** What the server asks of a browser window, as read: its key and its values. */
export type WindowOperation = [key: number, values: (string | number | boolean)[]];

function nextNumber(reader: Reader): number {
    reader.read();
    return reader.number;
}

/** The window operation of the model just read: print, reload, location, resize, move, focus, title; else null. */
function readWindowOperation(reader: Reader): WindowOperation | null {
    const key = reader.key;
    switch (key) {
        case K.PRINT:
        case K.RELOAD:
        case K.FOCUS: // true: the server only focuses a window
            return [key, []];
        case K.WINDOW_LOCATION_REPLACE:
        case K.WINDOW_TITLE:
            return [key, [reader.str]];
        case K.RESIZE_BY_X:
        case K.RESIZE_TO_WIDTH:
        case K.MOVE_BY_X:
        case K.MOVE_TO_X:
            return [key, [reader.number, nextNumber(reader)]]; // x, then y (or width, then height)
        default:
            return null;
    }
}

/**
 * Runs a window operation on a window: the opener's for its own windows, a window's own when its opener is another
 * origin's, which a cross-origin WindowProxy refuses most of them (ADR-0009).
 */
export function applyWindowOperation(w: Window, [key, values]: WindowOperation): void {
    const [a, b] = values as [number, number];
    switch (key) {
        case K.PRINT: w.print(); break;
        case K.RELOAD: w.location.reload(); break;
        case K.WINDOW_LOCATION_REPLACE: w.location.replace(values[0] as string); break;
        case K.RESIZE_BY_X: w.resizeBy(Math.trunc(a), Math.trunc(b)); break;
        case K.RESIZE_TO_WIDTH: w.resizeTo(a, b); break;
        case K.MOVE_BY_X: w.moveBy(Math.trunc(a), Math.trunc(b)); break;
        case K.MOVE_TO_X: w.moveTo(Math.trunc(a), Math.trunc(b)); break;
        case K.FOCUS: w.focus(); break;
        case K.WINDOW_TITLE: w.document.title = values[0] as string; break;
    }
}

/** What the server does to a browser window: print, reload, resize, move, focus, title. */
abstract class AbstractWindow extends BaseObject {
    protected window: Window | null = null;

    override update(reader: Reader): boolean {
        const operation = readWindowOperation(reader);
        if (operation === null) return super.update(reader);
        this.operate(operation);
        return true;
    }

    protected operate(operation: WindowOperation): void {
        if (this.window !== null) applyWindowOperation(this.window, operation);
    }
}

/**
 * PBrowser: the window of this page, whose blocks the dispatcher runs itself. In a <pony-app> element, the page is the
 * host's (ADR-0008): RELOAD starts the application again in the element; the title and the location are the host's to
 * decide, told by the events pony-title and pony-location (ADR-0009).
 */
export class Browser extends AbstractWindow {
    constructor(context: Context, id: number, type: number) {
        super(context, id, type);
        this.window = window;
        context.windows.mainWindowId = id;
    }

    override update(reader: Reader): boolean {
        const embedding = this.context.session.embedding;
        if (embedding !== null) {
            if (reader.key === K.RELOAD) {
                embedding.restart();
                return true;
            }
            if (reader.key === K.WINDOW_TITLE) {
                embedding.notify('pony-title', reader.str);
                return true;
            }
            if (reader.key === K.WINDOW_LOCATION_REPLACE) {
                embedding.notify('pony-location', new URL(reader.str, this.context.session.hostPageBaseURL).href);
                return true;
            }
        }
        return super.update(reader);
    }
}

/**
 * PWindow: a browser window opened by the server, running a terminal of its own on the same context. This terminal
 * hands it the blocks of the window; it says when it is ready, and sends its messages through this one.
 */
export class PopupWindow extends AbstractWindow implements ChildTerminal, BridgedChild {
    readonly kind = 'window';
    private readonly url: string;
    private readonly name: string;
    private readonly features: string;
    private readonly windows: Windows;
    /** An embedded terminal's window of the application: talked to by postMessage only (ADR-0009). */
    private readonly bridge: ChildBridge | null;
    private isReady = false;

    static create(context: Context, id: number, type: number, reader: Reader): PopupWindow {
        reader.read();
        const relative = reader.bool;
        reader.read();
        let url = reader.str;
        reader.read();
        const name = reader.str;
        reader.read();
        const features = reader.str;
        const session = context.session;
        const bridge = relative ? session.embedding?.children ?? null : null;
        if (relative) {
            url = `${session.hostPageBaseURL}${url}?${C.WINDOW_ID}=${id}&${C.UI_CONTEXT_ID}=${session.contextId}`;
            if (session.tabindexOnlyFormField) url += `&${C.OPTION_TABINDEX_ACTIVATED}=true`;
            if (bridge !== null) url += `&${C.BRIDGED}=1`;
        }
        return new PopupWindow(context, id, type, url, name, features, bridge);
    }

    private constructor(context: Context, id: number, type: number, url: string,
                        name: string, features: string, bridge: ChildBridge | null) {
        super(context, id, type);
        this.url = url;
        this.name = name;
        this.features = features;
        this.bridge = bridge;
        this.windows = context.windows;
        this.windows.register(id, this);
    }

    get title(): string {
        return this.name;
    }

    get target(): Window | null {
        return this.window;
    }

    closing(): void {
        this.onClose();
    }

    protected override operate(operation: WindowOperation): void {
        if (this.bridge !== null) this.bridge.operate(this, operation);
        else super.operate(operation);
    }

    override update(reader: Reader): boolean {
        if (reader.key === K.OPEN) {
            this.open();
            return true;
        }
        if (reader.key === K.CLOSE) {
            this.close();
            return true;
        }
        return super.update(reader);
    }

    private open(): void {
        this.window = window.open(this.url, this.name, this.features);
        if (this.window === null) {
            // The browser blocked the popup
            const message = about(this.id);
            message[C.HANDLER_DESTROY] = FLAG;
            this.context.send(message);
            this.context.warn(`Can't open PTWindow #${this.id}. Check the browser's settings`, this.id);
            return;
        }
        // A window of another origin is not listened to: it says when it goes (closing), and watchWindows sees it closed
        if (this.bridge !== null) this.bridge.register(this);
        else this.window.addEventListener('unload', () => this.onClose());
    }

    get ready(): boolean {
        return this.isReady;
    }

    get closed(): boolean {
        return this.window === null || this.window.closed;
    }

    close(): void {
        if (this.isReady && !this.closed) this.window?.close();
    }

    postMessage(bytes: Uint8Array): void {
        if (this.isReady && this.closed) this.onClose();
        if (!this.isReady) return;
        // To this page's origin, never to any: a window that navigated elsewhere gets nothing
        if (this.bridge !== null) this.bridge.post(this, bytes);
        else this.window?.postMessage(bytes.slice(), location.origin);
    }

    setReady(): void {
        this.isReady = true;
        // Chrome keeps the opener's title otherwise; a bridged window gets it with its init
        if (this.window !== null && this.bridge === null) this.window.document.title = this.name;
        const message = about(this.id);
        message[C.HANDLER_OPEN] = this.url;
        this.context.send(message);
    }

    /** The window was closed by its user, or navigated away. */
    onClose(): void {
        if (!this.isReady || this.window === null) return;
        this.isReady = false;
        this.bridge?.unregister(this);
        this.window = null;
        this.windows.unregister(this.id);
        const message = about(this.id);
        message[C.HANDLER_CLOSE] = FLAG;
        this.context.send(message);
        log.info(`Window #${this.id} closed`);
    }
}

/**
 * Every second, the windows closed without an unload event (a crash, a blocked event) are noticed; until signal is
 * aborted, when given (the terminal of a <pony-app> element goes).
 */
export function watchWindows(windows: Windows, signal?: AbortSignal): void {
    const timer = setInterval(() => {
        for (const window of windows.all()) {
            if (window.closed) window.onClose();
        }
    }, 1000);
    signal?.addEventListener('abort', () => clearInterval(timer));
}
