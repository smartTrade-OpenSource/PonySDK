/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import {
    ClientToServerModel as C,
    HandlerModel as H,
    PHorizontalAlignment as HA,
    PVerticalAlignment as VA,
    ServerToClientModel as K,
} from '#model';
import { about, FLAG } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import { apply, type Context, type Setters, type TerminalObject } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';

function div(): HTMLDivElement {
    return document.createElement('div');
}

/** PFlowPanel: children one after the other, at INDEX when given. */
export class FlowPanel extends Widget {
    static create(context: Context, id: number, type: number): FlowPanel {
        return new FlowPanel(context, id, type, div(), 'pony-FlowPanel');
    }

    override add(reader: Reader, child: TerminalObject): void {
        if (reader.read() === K.INDEX && child instanceof Widget) {
            this.insert(child, reader.int);
        } else {
            if (reader.key !== K.INDEX) reader.rewind();
            super.add(reader, child);
        }
    }
}

/** PAbsolutePanel: children placed by their own position style. */
export const createAbsolutePanel = (context: Context, id: number, type: number): Widget =>
    new Widget(context, id, type, div(), 'pony-AbsolutePanel');

/** PSimplePanel and the panels made on it: one child, which a new one replaces. */
export class SimplePanel extends Widget {
    static create(context: Context, id: number, type: number): SimplePanel {
        return new SimplePanel(context, id, type, div(), 'pony-SimplePanel');
    }

    override add(reader: Reader, child: TerminalObject): void {
        const current = this.children[0];
        if (current !== undefined && current !== child) this.removeChild(current);
        super.add(reader, child);
    }
}

/** PDecoratorPanel: a simple panel whose decoration is left to the stylesheet. */
export class DecoratorPanel extends SimplePanel {
    static override create(context: Context, id: number, type: number): DecoratorPanel {
        return new DecoratorPanel(context, id, type, div(), 'pony-DecoratorPanel');
    }
}

/** PFocusPanel: a simple panel that takes the focus. */
export class FocusPanel extends SimplePanel {
    static override create(context: Context, id: number, type: number): FocusPanel {
        const element = div();
        element.tabIndex = 0;
        return new FocusPanel(context, id, type, element, 'pony-FocusPanel');
    }
}

/**
 * PScrollPanel: a simple panel that scrolls. Its scroll handler reports the position once a drag of the scroll bar
 * ends, not during it.
 */
export class ScrollPanel extends SimplePanel {
    private dragging = false;
    /** Undefined until HANDLER_SCROLL: then whether the scroll goes to the server. */
    private sendsScroll: boolean | undefined;

    static override create(context: Context, id: number, type: number): ScrollPanel {
        return new ScrollPanel(context, id, type, div(), 'pony-ScrollPanel');
    }

    override update(reader: Reader): boolean {
        const e = this.element;
        switch (reader.key) {
            case K.HORIZONTAL_SCROLL_POSITION:
                e.scrollLeft = reader.int;
                return true;
            case K.VERTICAL_SCROLL_POSITION:
                e.scrollTop = reader.int;
                return true;
            case K.SCROLL_TO: {
                const to = reader.num; // 0 bottom, 1 left, 2 right, 3 top
                if (to === 0) e.scrollTop = e.scrollHeight;
                else if (to === 1) e.scrollLeft = 0;
                else if (to === 2) e.scrollLeft = e.scrollWidth;
                else if (to === 3) e.scrollTop = 0;
                return true;
            }
            default:
                return super.update(reader);
        }
    }

    override addHandler(reader: Reader, handler: number): void {
        if (handler !== H.HANDLER_SCROLL) {
            super.addHandler(reader, handler);
            return;
        }
        const listening = this.sendsScroll !== undefined;
        this.sendsScroll = true;
        if (listening) return;
        this.listen('scroll', () => {
            if (!this.dragging) this.sendScroll();
        });
        this.listen('mousedown', () => {
            this.dragging = true;
        });
        this.listen('mouseup', () => {
            if (!this.dragging) return;
            this.dragging = false;
            this.sendScroll();
        }, window);
    }

    /** Protocol 4 names the object: the scroll stops going to the server. */
    override removeHandler(reader: Reader, handler: number): void {
        if (handler === H.HANDLER_SCROLL) this.sendsScroll = false;
        else super.removeHandler(reader, handler);
    }

    private sendScroll(): void {
        if (this.sendsScroll !== true) return;
        const e = this.element;
        const message = about(this.id);
        message[C.HANDLER_SCROLL] = FLAG;
        message[C.HANDLER_SCROLL_HEIGHT] = e.scrollHeight - e.clientHeight;
        message[C.HANDLER_SCROLL_WIDTH] = e.scrollWidth - e.clientWidth;
        message[C.HANDLER_SCROLL_VERTICAL] = Math.round(e.scrollTop);
        message[C.HANDLER_SCROLL_HORIZONTAL] = Math.round(e.scrollLeft);
        this.context.send(message);
    }
}

/**
 * PComposite: the widget it wraps (WIDGET_ID) is what the page shows. The composite takes that widget's element, so
 * that styles and events the server sets on either land on the same element.
 */
export class Composite extends Widget {
    static create(context: Context, id: number, type: number): Composite {
        return new Composite(context, id, type, div());
    }

    override update(reader: Reader): boolean {
        if (reader.key !== K.WIDGET_ID) return super.update(reader);
        const inner = this.context.object(reader.int);
        if (!(inner instanceof Widget)) throw new Error(`${this}: #${reader.int} is not a widget`);
        inner.removeFromParent();
        this.element = inner.element;
        this.children.push(inner);
        this.adopt(inner);
        return true;
    }
}

/** Alignment of a cell's content: flex values on the cell, text alignment for inline content. */
const HORIZONTAL: Readonly<Record<number, [string, string]>> = {
    [HA.ALIGN_LEFT]: ['stretch', 'left'],
    [HA.ALIGN_CENTER]: ['center', 'center'],
    [HA.ALIGN_RIGHT]: ['flex-end', 'right'],
};
const VERTICAL: Readonly<Record<number, string>> = {
    [VA.ALIGN_TOP]: 'flex-start',
    [VA.ALIGN_MIDDLE]: 'center',
    [VA.ALIGN_BOTTOM]: 'flex-end',
};

/**
 * PHorizontalPanel and PVerticalPanel: children in a row or a column, each in a cell (pony-Cell) that carries the
 * cell properties: alignment, width, height. The panel's alignments apply to the cells added after them.
 */
export class CellPanel extends Widget {
    private readonly cells = new Map<Widget, HTMLDivElement>();
    private horizontal: number = HA.ALIGN_LEFT;
    private vertical: number = VA.ALIGN_TOP;

    static createHorizontal(context: Context, id: number, type: number): CellPanel {
        return new CellPanel(context, id, type, div(), 'pony-HorizontalPanel');
    }

    static createVertical(context: Context, id: number, type: number): CellPanel {
        return new CellPanel(context, id, type, div(), 'pony-VerticalPanel');
    }

    override add(reader: Reader, child: TerminalObject): void {
        if (reader.read() === K.INDEX && child instanceof Widget) {
            this.insert(child, reader.int);
        } else {
            if (reader.key !== K.INDEX) reader.rewind();
            super.add(reader, child);
        }
    }

    protected override nodeFor(child: Widget): Element {
        const cell = div();
        cell.className = 'pony-Cell';
        cell.append(child.element);
        this.cells.set(child, cell);
        alignHorizontally(cell, this.horizontal);
        alignVertically(cell, this.vertical);
        return cell;
    }

    protected override nodeOf(child: Widget): Element {
        const cell = this.cells.get(child);
        this.cells.delete(child);
        return cell ?? child.element;
    }

    override update(reader: Reader): boolean {
        return apply(CELL_PANEL, this, reader) || super.update(reader);
    }

    /** The cell of the widget whose id is the next model (WIDGET_ID). */
    cellOf(reader: Reader): HTMLDivElement | undefined {
        reader.read(); // WIDGET_ID
        const child = this.context.object(reader.int);
        return child instanceof Widget ? this.cells.get(child) : undefined;
    }

    setDefaultAlignment(horizontal: number | null, vertical: number | null): void {
        if (horizontal !== null) this.horizontal = horizontal;
        if (vertical !== null) this.vertical = vertical;
    }
}

function alignHorizontally(cell: HTMLElement, alignment: number): void {
    const [items, text] = HORIZONTAL[alignment] ?? ['stretch', 'left'];
    cell.style.alignItems = items;
    cell.style.textAlign = text;
}

function alignVertically(cell: HTMLElement, alignment: number): void {
    cell.style.justifyContent = VERTICAL[alignment] ?? 'flex-start';
}

const CELL_PANEL: Setters<CellPanel> = new Map<number, (w: CellPanel, r: Reader) => void>([
    [K.WIDGET_HORIZONTAL_ALIGNMENT, (w, r) => {
        const alignment = r.int;
        const cell = w.cellOf(r);
        if (cell) alignHorizontally(cell, alignment);
    }],
    [K.WIDGET_VERTICAL_ALIGNMENT, (w, r) => {
        const alignment = r.int;
        const cell = w.cellOf(r);
        if (cell) alignVertically(cell, alignment);
    }],
    [K.CELL_WIDTH, (w, r) => {
        const width = r.str ?? '';
        const cell = w.cellOf(r);
        if (cell) cell.style.width = width;
    }],
    [K.CELL_HEIGHT, (w, r) => {
        const height = r.str ?? '';
        const cell = w.cellOf(r);
        if (cell) cell.style.height = height;
    }],
    [K.HORIZONTAL_ALIGNMENT, (w, r) => w.setDefaultAlignment(r.int, null)],
    [K.VERTICAL_ALIGNMENT, (w, r) => w.setDefaultAlignment(null, r.int)],
    [K.SPACING, (w, r) => {
        w.element.style.gap = `${r.int}px`;
    }],
    [K.BORDER_WIDTH, (w, r) => {
        w.element.style.border = r.int > 0 ? `${r.int}px solid` : '';
    }],
]);
