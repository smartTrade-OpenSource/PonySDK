/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, ServerToClientModel as K } from '#model';
import { about, FLAG } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import type { Windows } from '../runtime/windows.ts';
import { log } from '../runtime/log.ts';
import { apply, BaseObject, type ChildTerminal, type Context, type Setters } from '../runtime/object.ts';

/** What the server does to a browser window: print, reload, resize, move, focus, title. */
abstract class AbstractWindow extends BaseObject {
    protected window: Window | null = null;

    override update(reader: Reader): boolean {
        return (this.window !== null && apply(WINDOW, this.window, reader)) || super.update(reader);
    }
}

function nextNumber(reader: Reader): number {
    reader.read();
    return reader.number;
}

const WINDOW: Setters<Window> = new Map<number, (w: Window, r: Reader) => void>([
    [K.PRINT, w => w.print()],
    [K.RELOAD, w => w.location.reload()],
    [K.WINDOW_LOCATION_REPLACE, (w, r) => w.location.replace(r.str ?? '')],
    [K.RESIZE_BY_X, (w, r) => w.resizeBy(Math.trunc(r.number), Math.trunc(nextNumber(r)))],
    [K.RESIZE_TO_WIDTH, (w, r) => w.resizeTo(r.number, nextNumber(r))],
    [K.MOVE_BY_X, (w, r) => w.moveBy(Math.trunc(r.number), Math.trunc(nextNumber(r)))],
    [K.MOVE_TO_X, (w, r) => w.moveTo(Math.trunc(r.number), Math.trunc(nextNumber(r)))],
    [K.FOCUS, (w, r) => {
        if (r.bool) w.focus();
        else w.blur();
    }],
    [K.WINDOW_TITLE, (w, r) => {
        w.document.title = r.str ?? '';
    }],
]);

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
                embedding.notify('pony-title', reader.str ?? '');
                return true;
            }
            if (reader.key === K.WINDOW_LOCATION_REPLACE) {
                embedding.notify('pony-location', new URL(reader.str ?? '', this.context.session.hostPageBaseURL).href);
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
export class PopupWindow extends AbstractWindow implements ChildTerminal {
    private readonly url: string;
    private readonly name: string;
    private readonly features: string;
    private readonly windows: Windows;
    private isReady = false;

    static create(context: Context, id: number, type: number, reader: Reader): PopupWindow {
        reader.read();
        const relative = reader.bool;
        reader.read();
        let url = reader.str ?? '';
        reader.read();
        const name = reader.str ?? '';
        reader.read();
        const features = reader.str ?? '';
        if (relative) {
            const session = context.session;
            url = `${session.hostPageBaseURL}${url}?${C.WINDOW_ID}=${id}&${C.UI_CONTEXT_ID}=${session.contextId}`;
            if (session.tabindexOnlyFormField) url += `&${C.OPTION_TABINDEX_ACTIVATED}=true`;
        }
        return new PopupWindow(context, id, type, url, name, features);
    }

    private constructor(context: Context, id: number, type: number, url: string,
                        name: string, features: string) {
        super(context, id, type);
        this.url = url;
        this.name = name;
        this.features = features;
        this.windows = context.windows;
        this.windows.register(id, this);
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
        // In a <pony-app> element, the window's terminal could not reach this one from another origin (ADR-0008)
        this.window = this.context.session.embedding === null ? window.open(this.url, this.name, this.features) : null;
        if (this.window === null) {
            // The browser blocked the popup
            const message = about(this.id);
            message[C.HANDLER_DESTROY] = FLAG;
            this.context.send(message);
            this.context.warn(this.context.session.embedding === null ? `Can't open PTWindow #${this.id}. Check the browser's settings`
                : `PTWindow #${this.id} refused: an embedded application opens no window (ADR-0008)`, this.id);
            return;
        }
        this.window.addEventListener('unload', () => this.onClose());
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
        if (this.isReady) this.window?.postMessage(bytes.slice(), '*');
    }

    setReady(): void {
        this.isReady = true;
        if (this.window !== null) this.window.document.title = this.name; // Chrome keeps the opener's title otherwise
        const message = about(this.id);
        message[C.HANDLER_OPEN] = this.url;
        this.context.send(message);
    }

    /** The window was closed by its user, or navigated away. */
    onClose(): void {
        if (!this.isReady || this.window === null) return;
        this.isReady = false;
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
            if (window instanceof PopupWindow && window.closed) window.onClose();
        }
    }, 1000);
    signal?.addEventListener('abort', () => clearInterval(timer));
}
