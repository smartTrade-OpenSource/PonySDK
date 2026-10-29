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
 *
 * The server writes the page's `lang` and `dir` on it (docs/decisions/0022-locale.md): the body's are the page's, so the
 * document element, where the browser reads the direction of its scrollbars and of the page, follows them.
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
        if (element === document.body) this.mirrorToDocument(element);
        this.listen('visibilitychange', () => this.sendVisibility(), document);
        this.sendVisibility();
    }

    private mirrorToDocument(body: HTMLElement): void {
        // The body is as long-lived as the page: nothing to disconnect
        new MutationObserver(records => {
            for (const record of records) {
                const name = record.attributeName!;
                const value = body.getAttribute(name);
                if (value === null) document.documentElement.removeAttribute(name);
                else document.documentElement.setAttribute(name, value);
            }
        }).observe(body, { attributeFilter: ['lang', 'dir'] });
    }

    private sendVisibility(): void {
        const message = about(this.id);
        message[C.HANDLER_DOCUMENT_VISIBILITY] = !document.hidden;
        this.context.send(message);
    }
}
