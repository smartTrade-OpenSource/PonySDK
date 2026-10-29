/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, HandlerModel as H, PUnitValue, ServerToClientModel as K } from '#model';
import { about } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import type { Context, TerminalObject } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';

interface Stack {
    readonly widget: Widget;
    readonly header: HTMLButtonElement;
    readonly content: HTMLDivElement;
}

let stackIds = 0;

/**
 * PStackLayoutPanel: headers (buttons, aria-expanded) of a given height, the widget of the open one filling the rest.
 * As GWT's: the first added opens; a header click, or the server (WIDGET_ID), opens another and sends
 * HANDLER_BEFORE_SELECTION then HANDLER_SELECTION with its index when the server listens; removing the open one
 * opens the first.
 */
export class StackLayoutPanel extends Widget {
    private readonly stacks: Stack[] = [];
    private readonly unit: number;
    private selected = -1;
    private listensBeforeSelection = false;
    private listensSelection = false;

    static create(context: Context, id: number, type: number, reader: Reader): StackLayoutPanel {
        reader.read(); // UNIT
        return new StackLayoutPanel(context, id, type, reader.int);
    }

    private constructor(context: Context, id: number, type: number, unit: number) {
        super(context, id, type, document.createElement('div'), 'pony-StackLayoutPanel');
        this.unit = unit;
    }

    /** The header, HTML, then its size. */
    override add(reader: Reader, child: TerminalObject): void {
        reader.read();
        const html = reader.str;
        reader.read();
        const size = reader.num;
        if (!(child instanceof Widget)) return;
        const header = document.createElement('button');
        header.type = 'button';
        header.className = 'pony-StackLayoutPanel-header';
        header.innerHTML = html;
        header.style.height = `${size}${PUnitValue[this.unit] ?? 'px'}`;
        header.id = `pony-stack-${++stackIds}`;
        const content = document.createElement('div');
        content.className = 'pony-StackLayoutPanel-content';
        content.setAttribute('role', 'region');
        content.setAttribute('aria-labelledby', header.id);
        header.setAttribute('aria-controls', content.id = `${header.id}-content`);
        this.element.append(header, content);
        Object.assign(child.element.style, { position: 'absolute', left: '0', right: '0', top: '0', bottom: '0' });
        this.appendInside(content, child);
        this.stacks.push({ widget: child, header, content });
        this.listen('click', () => this.show(this.stacks.findIndex(s => s.header === header)), header);
        if (this.selected === -1) this.show(0);
        else this.refresh();
    }

    override remove(_reader: Reader, child: TerminalObject): void {
        const index = this.stacks.findIndex(s => s.widget === child); // a stack's, for the server
        const [removed] = this.stacks.splice(index, 1);
        this.removeChild(removed!.widget);
        for (const p of ['position', 'left', 'right', 'top', 'bottom']) removed!.widget.element.style.removeProperty(p);
        removed!.header.remove();
        removed!.content.remove();
        if (this.selected === index) {
            this.selected = -1;
            if (this.stacks.length > 0) this.show(0);
        } else {
            if (index <= this.selected) this.selected--;
            this.refresh();
        }
    }

    override update(reader: Reader): boolean {
        switch (reader.key) {
            case K.WIDGET_ID: {
                const child = this.context.object(reader.int);
                this.show(this.stacks.findIndex(s => s.widget === child));
                return true;
            }
            case K.ANIMATE:
            case K.ANIMATION_DURATION:
                return true;
            default:
                return super.update(reader);
        }
    }

    override addHandler(reader: Reader, handler: number): void {
        if (handler === H.HANDLER_SELECTION) this.listensSelection = true;
        else if (handler === H.HANDLER_BEFORE_SELECTION) this.listensBeforeSelection = true;
        else super.addHandler(reader, handler);
    }

    private show(index: number): void {
        if (index < 0 || index >= this.stacks.length || index === this.selected) return;
        if (this.listensBeforeSelection) this.send(C.HANDLER_BEFORE_SELECTION, index);
        this.selected = index;
        this.refresh();
        if (this.listensSelection) this.send(C.HANDLER_SELECTION, index);
    }

    private send(key: string, index: number): void {
        const message = about(this.id);
        message[key] = index;
        this.context.send(message);
    }

    private refresh(): void {
        this.stacks.forEach(({ header, content }, i) => {
            header.setAttribute('aria-expanded', String(i === this.selected));
            content.hidden = i !== this.selected;
        });
    }
}
