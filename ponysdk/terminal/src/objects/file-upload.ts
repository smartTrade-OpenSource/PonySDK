/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, HandlerModel as H, ServerToClientModel as K } from '#model';
import { about, FLAG } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import { log } from '../runtime/log.ts';
import { CONTEXT_KEY_HEADER, type Context } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';

/**
 * PFileUpload: a file input in a label. A change sends the file name and size; HANDLER_EMBEDDED_STREAM_REQUEST posts
 * the file to the server's stream (multipart, as a form would), then reports HANDLER_SUBMIT_COMPLETE.
 */
export class FileUpload extends Widget {
    private readonly input: HTMLInputElement;
    private readonly caption: HTMLSpanElement;

    static create(context: Context, id: number, type: number): FileUpload {
        return new FileUpload(context, id, type);
    }

    private constructor(context: Context, id: number, type: number) {
        const input = document.createElement('input');
        input.type = 'file';
        input.name = 'file';
        const caption = document.createElement('span');
        const label = document.createElement('label');
        label.append(caption, input);
        super(context, id, type, label, 'pony-FileUpload');
        this.input = input;
        this.caption = caption;
    }

    override get focusTarget(): HTMLElement {
        return this.input;
    }

    override update(reader: Reader): boolean {
        switch (reader.key) {
            case K.NAME:
                this.input.name = reader.str;
                return true;
            case K.ENABLED:
                this.input.disabled = !reader.bool;
                return true;
            case K.TEXT:
                this.caption.textContent = reader.str;
                return true;
            case K.CLEAR:
                this.input.value = '';
                return true;
            default:
                return super.update(reader);
        }
    }

    override addHandler(reader: Reader, handler: number): void {
        if (handler === H.HANDLER_CHANGE) {
            this.listen('change', () => {
                const message = about(this.id);
                message[C.HANDLER_CHANGE] = this.input.value;
                message[C.SIZE] = this.input.files?.[0]?.size ?? 0;
                this.context.send(message);
            }, this.input);
        } else if (handler === H.HANDLER_EMBEDDED_STREAM_REQUEST) {
            reader.read(); // STREAM_REQUEST_ID
            this.submit(reader.int);
        } else {
            super.addHandler(reader, handler);
        }
    }

    private submit(streamRequestId: number): void {
        const session = this.context.session;
        const data = new FormData();
        for (const file of this.input.files!) data.append(this.input.name, file);
        // The application's origin: the host's in a page, another in a <pony-app> element (ADR-0008), whose context's
        // key proves it when the session cookie does not come (ADR-0009)
        const headers: Record<string, string> = session.contextKey === null ? {} : { [CONTEXT_KEY_HEADER]: session.contextKey };
        fetch(session.streamUrl(streamRequestId), { method: 'POST', body: data, credentials: 'include', headers })
            .then(response => response.arrayBuffer()) // read to its end: Chrome reports a body left unread as aborted
            .catch(e => log.error(`${this}: upload failed`, e))
            .finally(() => this.context.guard(() => {
                const message = about(this.id);
                message[C.HANDLER_SUBMIT_COMPLETE] = FLAG;
                this.context.send(message);
            }));
    }
}
