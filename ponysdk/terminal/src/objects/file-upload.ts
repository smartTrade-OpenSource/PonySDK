/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, HandlerModel as H, MappingPath, ServerToClientModel as K } from '#model';
import { about, FLAG } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import { log } from '../runtime/log.ts';
import type { Context } from '../runtime/object.ts';
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
                this.input.name = reader.str ?? '';
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
        const url = `${session.hostPageBaseURL}${MappingPath.STREAM}?${C.UI_CONTEXT_ID}=${session.contextId}`
            + `&${C.STREAM_REQUEST_ID}=${streamRequestId}`;
        const data = new FormData();
        for (const file of this.input.files ?? []) data.append(this.input.name, file);
        fetch(url, { method: 'POST', body: data, credentials: 'same-origin' })
            .catch(e => log.error(`${this}: upload failed`, e))
            .finally(() => this.context.guard(() => {
                const message = about(this.id);
                message[C.HANDLER_SUBMIT_COMPLETE] = FLAG;
                this.context.send(message);
            }));
    }
}
