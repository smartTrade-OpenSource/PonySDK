/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, ServerToClientModel as K } from '#model';
import { about } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import type { Context } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';

/**
 * PRootPanel: the body, or the element of ROOT_ID; the widgets of a window are added to it. It is in the document
 * from the start, and reports whether the document is visible. In a <pony-app> element, the body is the element's
 * container and ROOT_ID is looked up in its shadow root (ADR-0008).
 */
export class RootPanel extends Widget {
    static create(context: Context, id: number, type: number, reader: Reader): RootPanel {
        reader.read();
        let element: HTMLElement | null = context.session.body;
        if (reader.key === K.ROOT_ID) {
            element = context.session.byId(reader.str);
            if (element === null) throw new Error(`No element #${reader.str} for root panel #${id}`);
        } else {
            reader.rewind();
        }
        return new RootPanel(context, id, type, element);
    }

    constructor(context: Context, id: number, type: number, element: HTMLElement) {
        super(context, id, type, element);
        this.attach();
        this.listen('visibilitychange', () => this.sendVisibility(), document);
        this.sendVisibility();
    }

    private sendVisibility(): void {
        const message = about(this.id);
        message[C.HANDLER_DOCUMENT_VISIBILITY] = !document.hidden;
        this.context.send(message);
    }
}
