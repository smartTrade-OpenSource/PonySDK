/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, ServerToClientModel as K } from '#model';
import { about, FLAG } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import type { Context, TerminalObject } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';

/**
 * PDisclosurePanel: details and its summary, the header text. Each change of state, by the user or by the server
 * (OPEN, CLOSE), sends HANDLER_OPEN or HANDLER_CLOSE, as GWT's DisclosurePanel fires its events for both.
 */
export class DisclosurePanel extends Widget {
    private content: HTMLDivElement;

    static create(context: Context, id: number, type: number, reader: Reader): DisclosurePanel {
        reader.read(); // the header text
        return new DisclosurePanel(context, id, type, reader.str ?? '');
    }

    private constructor(context: Context, id: number, type: number, header: string) {
        const details = document.createElement('details');
        const summary = document.createElement('summary');
        summary.textContent = header;
        const content = document.createElement('div');
        details.append(summary, content);
        super(context, id, type, details, 'pony-DisclosurePanel');
        this.content = content;
        this.listen('toggle', () => {
            const message = about(this.id);
            message[details.open ? C.HANDLER_OPEN : C.HANDLER_CLOSE] = FLAG;
            this.context.send(message);
        });
    }

    override update(reader: Reader): boolean {
        switch (reader.key) {
            case K.OPEN:
                (this.element as HTMLDetailsElement).open = true;
                return true;
            case K.CLOSE:
                (this.element as HTMLDetailsElement).open = false;
                return true;
            case K.ANIMATION:
                return true; // the browser's own
            default:
                return super.update(reader);
        }
    }

    /** The content, which replaces the previous one. */
    override add(_reader: Reader, child: TerminalObject): void {
        if (!(child instanceof Widget)) return;
        for (const widget of [...this.children]) if (widget !== child) this.removeChild(widget);
        this.appendInside(this.content, child);
    }
}
