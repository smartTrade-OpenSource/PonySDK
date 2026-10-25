/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { HandlerModel as H, ServerToClientModel as K } from '#model';
import type { Reader } from '../protocol/reader.ts';
import type { Context } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';

/**
 * PImage: an img. With a clip (POSITION_LEFT, then top, width, height), the part of a sprite it shows. Its source may
 * be a stream of the server (HANDLER_EMBEDDED_STREAM_REQUEST).
 */
export class Image extends Widget {
    static create(context: Context, id: number, type: number, reader: Reader): Image {
        const img = document.createElement('img');
        if (reader.read() === K.URL) {
            img.src = reader.str ?? '';
            if (reader.read() === K.POSITION_LEFT) {
                const left = reader.int;
                reader.read();
                const top = reader.int;
                reader.read();
                const width = reader.int;
                reader.read();
                const height = reader.int;
                img.style.cssText += `object-fit:none;object-position:${-left}px ${-top}px;width:${width}px;height:${height}px`;
            } else {
                reader.rewind();
            }
        } else {
            reader.rewind();
        }
        return new Image(context, id, type, img, 'pony-Image');
    }

    override update(reader: Reader): boolean {
        if (reader.key === K.URL) {
            (this.element as HTMLImageElement).src = reader.str ?? '';
            return true;
        }
        return super.update(reader);
    }

    override addHandler(reader: Reader, handler: number): void {
        if (handler !== H.HANDLER_EMBEDDED_STREAM_REQUEST) {
            super.addHandler(reader, handler);
            return;
        }
        reader.read(); // STREAM_REQUEST_ID
        (this.element as HTMLImageElement).src = this.context.session.streamUrl(reader.int);
    }
}
