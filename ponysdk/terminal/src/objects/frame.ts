/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, ServerToClientModel as K } from '#model';
import { about } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import type { ChildTerminal, Context } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';

/**
 * PFrame: an iframe running a terminal of its own, on the same context. This terminal hands it the blocks of the
 * frame (FRAME_ID); the frame's terminal says when it is ready, and sends its messages through this one.
 */
export class Frame extends Widget implements ChildTerminal {
    readonly url: string;
    private isReady = false;

    static create(context: Context, id: number, type: number, reader: Reader): Frame {
        reader.read();
        let url = '';
        if (reader.key === K.URL) url = reader.str ?? '';
        else reader.rewind();
        const session = context.session;
        url += `${url.includes('?') ? '&' : '?'}${C.UI_CONTEXT_ID}=${session.contextId}&${C.FRAME_ID}=${id}`;
        if (session.tabindexOnlyFormField) url += `&${C.OPTION_TABINDEX_ACTIVATED}=true`;
        // In a <pony-app> element, relative to the application, not to the host page (ADR-0008)
        if (session.embedding !== null) url = new URL(url, session.hostPageBaseURL).href;
        return new Frame(context, id, type, url);
    }

    constructor(context: Context, id: number, type: number, url: string) {
        const iframe = document.createElement('iframe');
        iframe.setAttribute('src', url);
        super(context, id, type, iframe);
        this.url = url;
    }

    get ready(): boolean {
        return this.isReady;
    }

    postMessage(bytes: Uint8Array): void {
        // A copy of the slice only: posting a view would clone the whole buffer it is cut from
        (this.element as HTMLIFrameElement).contentWindow?.postMessage(bytes.slice(), '*');
    }

    setReady(): void {
        this.isReady = true;
        const message = about(this.id);
        message[C.HANDLER_OPEN] = this.url;
        this.context.send(message);
    }
}
