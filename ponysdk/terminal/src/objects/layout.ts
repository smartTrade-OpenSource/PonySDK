/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import {
    ClientToServerModel as C,
    HandlerModel as H,
    PAlignment as A,
    PDirection as D,
    PUnitValue,
    ServerToClientModel as K,
} from '#model';
import { about } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import type { Context, TerminalObject } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';

/** A CSS length from a value and a PUnit. */
function length(value: number, unit: number): string {
    return `${value}${PUnitValue[unit] ?? 'px'}`;
}

/** The styles a layer gives to its child, removed when the child leaves. */
const CHILD_STYLES = ['position', 'left', 'right', 'top', 'bottom', 'width', 'height'] as const;

function fill(child: HTMLElement): void {
    Object.assign(child.style, { position: 'absolute', left: '0', right: '0', top: '0', bottom: '0' });
}

function release(child: HTMLElement): void {
    for (const property of CHILD_STYLES) child.style.removeProperty(property);
}

/**
 * The layout panels (the GWT terminal's Layout): each child is in a layer, absolutely positioned in the panel by
 * constraints (left, right, width; top, bottom, height) in a unit; the child fills its layer, or keeps its own size
 * at the beginning or the end of it (PAlignment). Layout is CSS: nothing is measured, the browser resizes the layers
 * with the panel, which replaces GWT's resize propagation.
 *
 * Constraints set by an update take effect at the end of the message, or with a transition of ANIMATE's duration.
 */
export class LayoutPanel extends Widget {
    protected readonly layers = new Map<Widget, HTMLDivElement>();
    private pending: (() => void)[] = [];
    private flushQueued = false;

    static create(context: Context, id: number, type: number, _reader: Reader): LayoutPanel {
        return new LayoutPanel(context, id, type, document.createElement('div'), 'pony-LayoutPanel');
    }

    protected override nodeFor(child: Widget): Element {
        const layer = document.createElement('div');
        layer.className = 'pony-Layer';
        Object.assign(layer.style, { left: '0', right: '0', top: '0', bottom: '0' });
        fill(child.element);
        layer.append(child.element);
        this.layers.set(child, layer);
        return layer;
    }

    protected override nodeOf(child: Widget): Element {
        const layer = this.layers.get(child);
        this.layers.delete(child);
        release(child.element);
        if (layer !== undefined) child.element.remove();
        return layer ?? child.element;
    }

    override update(reader: Reader): boolean {
        switch (reader.key) {
            case K.UNIT:
                this.constrain(reader);
                return true;
            case K.WIDGET_HORIZONTAL_ALIGNMENT:
            case K.WIDGET_VERTICAL_ALIGNMENT: {
                const horizontal = reader.key === K.WIDGET_HORIZONTAL_ALIGNMENT;
                const alignment = reader.int;
                reader.read(); // WIDGET_ID
                const child = this.context.object(reader.int);
                if (child instanceof Widget) this.later(() => align(child.element, horizontal, alignment));
                return true;
            }
            case K.ANIMATE:
                this.flush(reader.int);
                return true;
            case K.WIDGET_HIDDEN: {
                const hidden = reader.bool;
                reader.read(); // WIDGET_ID
                const child = this.context.object(reader.int);
                const layer = child instanceof Widget ? this.layers.get(child) : undefined;
                if (layer !== undefined) layer.hidden = hidden;
                return true;
            }
            default:
                return super.update(reader);
        }
    }

    /** UNIT, WIDGET_ID, then two of LEFT, RIGHT, WIDTH, or two of TOP, BOTTOM, HEIGHT. */
    private constrain(reader: Reader): void {
        const unit = reader.int;
        reader.read(); // WIDGET_ID
        const child = this.context.object(reader.int);
        const values = new Map<number, number>();
        for (let i = 0; i < 2; i++) {
            reader.read();
            values.set(reader.key, reader.num);
        }
        const layer = child instanceof Widget ? this.layers.get(child) : undefined;
        if (layer === undefined) return;
        const set = (property: 'left' | 'right' | 'width' | 'top' | 'bottom' | 'height', key: number): string =>
            values.has(key) ? length(values.get(key)!, unit) : '';
        const horizontal = values.has(K.LEFT) || values.has(K.RIGHT) || values.has(K.WIDTH);
        this.later(() => {
            if (horizontal) {
                layer.style.left = set('left', K.LEFT);
                layer.style.right = set('right', K.RIGHT);
                layer.style.width = set('width', K.WIDTH);
            } else {
                layer.style.top = set('top', K.TOP);
                layer.style.bottom = set('bottom', K.BOTTOM);
                layer.style.height = set('height', K.HEIGHT);
            }
        });
    }

    /** A change for the end of the message, unless an ANIMATE comes first. */
    protected later(change: () => void): void {
        this.pending.push(change);
        if (this.flushQueued) return;
        this.flushQueued = true;
        queueMicrotask(() => this.context.guard(() => this.flush(0)));
    }

    protected flush(duration: number): void {
        this.flushQueued = false;
        const changes = this.pending;
        this.pending = [];
        const layers = [...this.layers.values()];
        for (const layer of layers) layer.style.transition = duration > 0 ? `all ${duration}ms` : '';
        for (const change of changes) change();
        if (duration > 0) setTimeout(() => layers.forEach(layer => layer.style.removeProperty('transition')), duration);
    }
}

/** A child at the beginning, the end, or across its layer. */
function align(child: HTMLElement, horizontal: boolean, alignment: number): void {
    const [start, end, size] = horizontal ? ['left', 'right', 'width'] as const : ['top', 'bottom', 'height'] as const;
    child.style[start] = alignment === A.END ? '' : '0';
    child.style[end] = alignment === A.BEGIN ? '' : '0';
    if (alignment !== A.STRETCH) child.style[size] = '';
}

/** PRootLayoutPanel: a layout panel over the whole window, in the document from the start. */
export class RootLayoutPanel extends LayoutPanel {
    static override create(context: Context, id: number, type: number, _reader: Reader): RootLayoutPanel {
        const element = document.createElement('div');
        document.body.append(element);
        return new RootLayoutPanel(context, id, type, element, 'pony-RootLayoutPanel');
    }

    constructor(context: Context, id: number, type: number, element: HTMLElement, typeClass: string) {
        super(context, id, type, element, typeClass);
        this.attach();
    }
}

/** PSimpleLayoutPanel: one child, filling the panel; a new one replaces it. */
export class SimpleLayoutPanel extends LayoutPanel {
    static override create(context: Context, id: number, type: number, _reader: Reader): SimpleLayoutPanel {
        return new SimpleLayoutPanel(context, id, type, document.createElement('div'), 'pony-SimpleLayoutPanel');
    }

    override add(reader: Reader, child: TerminalObject): void {
        for (const widget of [...this.children]) if (widget !== child) this.removeChild(widget);
        super.add(reader, child);
    }
}

interface Dock {
    widget: Widget;
    direction: number;
    size: number;
    hidden: boolean;
    /** The splitter that follows an edge widget, in a split layout panel. */
    splitter?: Splitter;
}

interface Splitter {
    element: HTMLDivElement;
    minSize: number;
    snapClosedSize: number;
    toggleAllowed: boolean;
    sizeBeforeToggle: number;
}

/**
 * PDockLayoutPanel, as GWT's: each edge child (NORTH, SOUTH, WEST, EAST, LINE_START, LINE_END) takes its SIZE on its
 * side of what the children before it left, in the order they were added; the CENTER child takes the rest.
 */
export class DockLayoutPanel extends LayoutPanel {
    protected readonly docks: Dock[] = [];
    protected readonly unit: number;

    static override create(context: Context, id: number, type: number, reader: Reader): DockLayoutPanel {
        reader.read(); // UNIT
        return new DockLayoutPanel(context, id, type, document.createElement('div'), 'pony-DockLayoutPanel', reader.int);
    }

    constructor(context: Context, id: number, type: number, element: HTMLElement, typeClass: string, unit: number) {
        super(context, id, type, element, typeClass);
        this.unit = unit;
    }

    /** DIRECTION, then SIZE. */
    override add(reader: Reader, child: TerminalObject): void {
        reader.read(); // DIRECTION
        const direction = reader.int;
        reader.read(); // SIZE
        const size = reader.num;
        if (!(child instanceof Widget)) return;
        this.append(child);
        this.docks.push({ widget: child, direction, size, hidden: false });
        this.layout();
    }

    override removeChild(child: Widget): void {
        const index = this.docks.findIndex(d => d.widget === child);
        if (index >= 0) {
            this.docks[index]!.splitter?.element.remove();
            this.docks.splice(index, 1);
        }
        super.removeChild(child);
        this.layout();
    }

    override update(reader: Reader): boolean {
        switch (reader.key) {
            case K.WIDGET_SIZE: {
                const size = reader.num;
                const dock = this.dockOf(reader);
                if (dock !== undefined) dock.size = size;
                this.layout();
                return true;
            }
            case K.WIDGET_HIDDEN: {
                const hidden = reader.bool;
                const dock = this.dockOf(reader);
                if (dock !== undefined) dock.hidden = hidden;
                this.layout();
                return true;
            }
            default:
                return super.update(reader);
        }
    }

    /** The dock of the widget whose id is the next model (WIDGET_ID). */
    protected dockOf(reader: Reader): Dock | undefined {
        reader.read();
        const child = this.context.object(reader.int);
        return this.docks.find(d => d.widget === child);
    }

    private layoutQueued = false;

    /** Lays the children out at the end of the message, or with the transition of an ANIMATE in it. */
    protected layout(): void {
        if (this.layoutQueued) return;
        this.layoutQueued = true;
        this.later(() => {
            this.layoutQueued = false;
            this.doLayout();
        });
    }

    /** Places every child, as GWT's DockLayoutPanel.doLayout; splitters, if any, after their widget. */
    private doLayout(): void {
        const offset = { left: 0, top: 0, right: 0, bottom: 0 };
        const place = (layer: HTMLElement, direction: number, size: number): void => {
            const s = layer.style;
            const u = (value: number): string => length(value, this.unit);
            const horizontal = () => {
                s.left = u(offset.left);
                s.right = u(offset.right);
                s.width = '';
            };
            const vertical = () => {
                s.top = u(offset.top);
                s.bottom = u(offset.bottom);
                s.height = '';
            };
            switch (direction) {
                case D.NORTH:
                    horizontal();
                    Object.assign(s, { top: u(offset.top), height: u(size), bottom: '' });
                    offset.top += size;
                    break;
                case D.SOUTH:
                    horizontal();
                    Object.assign(s, { bottom: u(offset.bottom), height: u(size), top: '' });
                    offset.bottom += size;
                    break;
                case D.WEST:
                case D.LINE_START:
                    vertical();
                    Object.assign(s, { left: u(offset.left), width: u(size), right: '' });
                    offset.left += size;
                    break;
                case D.EAST:
                case D.LINE_END:
                    vertical();
                    Object.assign(s, { right: u(offset.right), width: u(size), left: '' });
                    offset.right += size;
                    break;
                default: // CENTER
                    horizontal();
                    vertical();
            }
        };
        for (const dock of this.docks) {
            const layer = this.layers.get(dock.widget);
            if (layer === undefined) continue;
            layer.hidden = dock.hidden;
            if (dock.splitter !== undefined) dock.splitter.element.hidden = dock.hidden;
            if (dock.hidden) continue;
            place(layer, dock.direction, dock.size);
            if (dock.splitter !== undefined) place(dock.splitter.element, dock.direction, SPLITTER_SIZE);
        }
    }
}

const SPLITTER_SIZE = 8;

/**
 * PSplitLayoutPanel: a dock layout panel in pixels with a splitter after each edge child. Dragging a splitter sizes its
 * child, not under MIN_SIZE; under SNAP_CLOSED_SIZE it closes; a double click toggles it when TOGGLE_DISPLAY_ALLOWED.
 * With HANDLER_RESIZE, the size of every child goes to the server 500 ms after the last change: a drag, or the panel
 * resized.
 */
export class SplitLayoutPanel extends DockLayoutPanel {
    private reportResize = false;
    private resizeTimer: ReturnType<typeof setTimeout> | undefined;
    private observer: ResizeObserver | undefined;

    static override create(context: Context, id: number, type: number, reader: Reader): SplitLayoutPanel {
        reader.read(); // UNIT
        return new SplitLayoutPanel(context, id, type, document.createElement('div'), 'pony-SplitLayoutPanel', reader.int);
    }

    override add(reader: Reader, child: TerminalObject): void {
        super.add(reader, child);
        const dock = this.docks[this.docks.length - 1];
        if (dock === undefined || dock.widget !== child || dock.direction === D.CENTER) return;
        const element = document.createElement('div');
        const vertical = dock.direction === D.NORTH || dock.direction === D.SOUTH;
        element.className = `pony-Splitter ${vertical ? 'pony-Splitter-vertical' : 'pony-Splitter-horizontal'}`;
        element.setAttribute('role', 'separator');
        element.setAttribute('aria-orientation', vertical ? 'horizontal' : 'vertical');
        this.element.append(element);
        dock.splitter = { element, minSize: 0, snapClosedSize: -1, toggleAllowed: false, sizeBeforeToggle: dock.size };
        this.drag(dock);
        this.layout();
    }

    /** As GWT's splitters: the size the drag started from, plus the distance moved toward the center. */
    private drag(dock: Dock): void {
        const splitter = dock.splitter!;
        let start: { x: number; y: number; size: number } | null = null;
        this.listen<PointerEvent>('pointerdown', e => {
            start = { x: e.clientX, y: e.clientY, size: dock.size };
            splitter.element.setPointerCapture(e.pointerId);
            e.preventDefault();
        }, splitter.element);
        this.listen<PointerEvent>('pointermove', e => {
            if (start === null) return;
            const panel = this.element.getBoundingClientRect();
            const vertical = dock.direction === D.NORTH || dock.direction === D.SOUTH;
            const moved = vertical ? e.clientY - start.y : e.clientX - start.x;
            const towardCenter = dock.direction === D.NORTH || dock.direction === D.WEST || dock.direction === D.LINE_START;
            let size = start.size + (towardCenter ? moved : -moved);
            size = Math.max(splitter.minSize, Math.min(Math.round(size), (vertical ? panel.height : panel.width) - SPLITTER_SIZE));
            if (size < splitter.snapClosedSize) size = 0;
            if (size !== dock.size) {
                dock.size = size;
                this.layout();
            }
        }, splitter.element);
        this.listen<PointerEvent>('pointerup', () => {
            if (start === null) return;
            start = null;
            this.resized();
        }, splitter.element);
        this.listen('dblclick', () => {
            if (!splitter.toggleAllowed) return;
            if (dock.size === 0) dock.size = splitter.sizeBeforeToggle;
            else {
                splitter.sizeBeforeToggle = dock.size;
                dock.size = 0;
            }
            this.layout();
            this.resized();
        }, splitter.element);
    }

    override update(reader: Reader): boolean {
        switch (reader.key) {
            case K.MIN_SIZE: {
                const value = reader.int;
                const splitter = this.dockOf(reader)?.splitter;
                if (splitter !== undefined) splitter.minSize = value;
                return true;
            }
            case K.SNAP_CLOSED_SIZE: {
                const value = reader.int;
                const splitter = this.dockOf(reader)?.splitter;
                if (splitter !== undefined) splitter.snapClosedSize = value;
                return true;
            }
            case K.TOGGLE_DISPLAY_ALLOWED: {
                const value = reader.bool;
                const splitter = this.dockOf(reader)?.splitter;
                if (splitter !== undefined) splitter.toggleAllowed = value;
                return true;
            }
            default:
                return super.update(reader);
        }
    }

    override addHandler(reader: Reader, handler: number): void {
        if (handler !== H.HANDLER_RESIZE) {
            super.addHandler(reader, handler);
            return;
        }
        this.reportResize = true;
        if (this.observer === undefined) {
            let first = true;
            this.observer = new ResizeObserver(() => {
                if (first) first = false; // the observation starts with the current size: not a change
                else this.resized();
            });
            this.observer.observe(this.element);
        }
    }

    override removeHandler(reader: Reader, handler: number): void {
        if (handler === H.HANDLER_RESIZE) this.reportResize = false;
        else super.removeHandler(reader, handler);
    }

    private resized(): void {
        if (!this.reportResize) return;
        clearTimeout(this.resizeTimer);
        this.resizeTimer = setTimeout(() => this.context.guard(() => {
            const message = about(this.id);
            message[C.HANDLER_RESIZE] = this.docks.map(dock => ({ [C.OBJECT_ID]: dock.widget.id, [C.SIZE]: dock.size }));
            this.context.send(message);
        }), 500);
    }

    override destroy(): void {
        clearTimeout(this.resizeTimer);
        this.observer?.disconnect();
        super.destroy();
    }
}

/**
 * PHeaderPanel: a header and a footer at their own height, the content area (INDEX 1) taking what is left; as in GWT,
 * the content widget keeps its own size in it.
 */
export class HeaderPanel extends Widget {
    private readonly slots: [HTMLDivElement, HTMLDivElement, HTMLDivElement];

    static create(context: Context, id: number, type: number): HeaderPanel {
        return new HeaderPanel(context, id, type);
    }

    private constructor(context: Context, id: number, type: number) {
        const slots: [HTMLDivElement, HTMLDivElement, HTMLDivElement] = [document.createElement('div'),
            document.createElement('div'), document.createElement('div')];
        slots[0].className = 'pony-HeaderPanel-header';
        slots[1].className = 'pony-HeaderPanel-content';
        slots[2].className = 'pony-HeaderPanel-footer';
        const element = document.createElement('div');
        element.append(...slots);
        super(context, id, type, element, 'pony-HeaderPanel');
        this.slots = slots;
    }

    /** INDEX: 0 header, 1 content, 2 footer; without it, the content. */
    override add(reader: Reader, child: TerminalObject): void {
        let index = 1;
        if (reader.read() === K.INDEX) index = reader.int;
        else reader.rewind();
        const slot = this.slots[index];
        if (!(child instanceof Widget) || slot === undefined) return;
        for (const widget of [...this.children]) if (slot.contains(widget.element)) this.removeChild(widget);
        this.appendInside(slot, child);
    }

    override update(reader: Reader): boolean {
        if (reader.key === K.RESIZE) return true; // the layout is CSS
        return super.update(reader);
    }
}
