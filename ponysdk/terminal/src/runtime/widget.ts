/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import {
    ClientToServerModel as C,
    DomHandlerType as D,
    HandlerModel as H,
    HandlerModelDomHandler,
    HandlerModelToDomHandlerType,
    ServerToClientModel as K,
} from '#model';
import { about, type Message } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import { log } from './log.ts';
import { apply, BaseObject, type Context, type Setters, type TerminalObject } from './object.ts';

/**
 * Event type bits of PREVENT_EVENT and STOP_EVENT: the server's PEventType codes, which are GWT's Event.ONxxx
 * constants. A widget prevents or stops only the events it listens to for the server.
 */
const EVENT_BITS: Readonly<Record<string, number>> = {
    click: 0x00001, dblclick: 0x00002, mousedown: 0x00004, mouseup: 0x00008, mouseover: 0x00010, mouseout: 0x00020,
    mousemove: 0x00040, keydown: 0x00080, keypress: 0x00100, keyup: 0x00200, change: 0x00400, focus: 0x00800,
    blur: 0x01000, scroll: 0x04000, load: 0x08000, error: 0x10000, wheel: 0x20000, contextmenu: 0x40000,
    paste: 0x80000, touchstart: 0x100000, touchmove: 0x200000, touchend: 0x400000, touchcancel: 0x800000,
};

/** The object a BIND names: a global function's result, which receives the NATIVE messages of the widget. */
interface NativeBinding {
    update(data: unknown): void;
}

/**
 * A terminal object with an element (the GWT terminal's PTUIObject and PTWidget): style, attributes, visibility,
 * the DOM events the server listens to, and the attachment to the document. A container keeps its children here.
 *
 * Everything the widget registers on the browser goes through {@link lifetime}: collecting the widget releases it.
 */
export class Widget extends BaseObject {
    /** Replaced only by a composite, which takes the element of the widget it wraps. */
    element: HTMLElement;
    /** pony-<Type> (pony-Button...): always on the element, whatever the server's style names. Null for raw elements. */
    protected readonly typeClass: string | null;
    parent: Widget | null = null;
    /** The widgets inside this one, in order; empty for a widget that holds none. */
    readonly children: Widget[] = [];
    protected readonly lifetime = new AbortController();
    private isAttached = false;
    private attachListeners: ((attached: boolean) => void)[] | undefined;
    private prevented: Set<number> | undefined;
    private stopped: Set<number> | undefined;
    private visibility: IntersectionObserver | undefined;
    private binding: NativeBinding | undefined;

    constructor(context: Context, id: number, type: number, element: HTMLElement, typeClass: string | null = null) {
        super(context, id, type);
        this.element = element;
        this.typeClass = typeClass;
        if (typeClass !== null) element.classList.add(typeClass);
    }

    /** What FOCUS focuses: the element, or the control inside it (the input of a check box). */
    get focusTarget(): HTMLElement {
        return this.element;
    }

    override update(reader: Reader): boolean {
        return apply(WIDGET, this, reader);
    }

    override destroy(): void {
        this.lifetime.abort();
        this.visibility?.disconnect();
        this.attachListeners = undefined;
    }

    // ---- attachment --------------------------------------------------------------------------------------------------

    /** In the document, through its parents up to a root. */
    get attached(): boolean {
        return this.isAttached;
    }

    /** Called with true when the widget enters the document, false when it leaves it. */
    onAttachChange(listener: (attached: boolean) => void): void {
        (this.attachListeners ??= []).push(listener);
    }

    /** Children are attached before their parent's listeners run, and detached after: as in GWT. */
    protected attach(): void {
        if (this.isAttached) return;
        this.isAttached = true;
        for (const child of this.children) child.attach();
        this.notifyAttach(true);
    }

    protected detach(): void {
        if (!this.isAttached) return;
        try {
            this.notifyAttach(false);
        } finally {
            for (const child of this.children) child.detach();
            this.isAttached = false;
        }
    }

    private notifyAttach(attached: boolean): void {
        if (this.attachListeners === undefined) return;
        for (const listener of this.attachListeners) this.context.guard(() => listener(attached));
    }

    // ---- children ----------------------------------------------------------------------------------------------------

    /** A child at the end of this widget's element. */
    protected append(child: Widget): void {
        child.removeFromParent();
        this.children.push(child);
        this.element.appendChild(this.nodeFor(child));
        this.adopt(child);
    }

    /**
     * A child at an index: of the children for the list, of the child elements for the DOM, as GWT's
     * ComplexPanel.insert does.
     */
    protected insert(child: Widget, index: number): void {
        if (child.parent === this && this.children.indexOf(child) < index) index--;
        child.removeFromParent();
        this.children.splice(index, 0, child);
        this.element.insertBefore(this.nodeFor(child), this.element.children[index] ?? null);
        this.adopt(child);
    }

    /** A child at the end of an element inside this widget: a table cell, a tab's panel. */
    protected appendInside(container: Element, child: Widget): void {
        child.removeFromParent();
        this.children.push(child);
        container.appendChild(child.element);
        this.adopt(child);
    }

    /** The node this widget's element holds for a child: its element, or a cell around it. */
    protected nodeFor(child: Widget): Element {
        return child.element;
    }

    /** The node to take out when a child leaves. */
    protected nodeOf(child: Widget): Element {
        return child.element;
    }

    /** The child is this widget's: it is attached when this widget is. */
    protected adopt(child: Widget): void {
        child.parent = this;
        if (this.isAttached) child.attach();
    }

    /** Detaches the child, then takes its element out. */
    removeChild(child: Widget): void {
        if (child.parent !== this) return;
        try {
            child.detach();
        } finally {
            child.parent = null;
            this.nodeOf(child).remove();
            const index = this.children.indexOf(child);
            if (index >= 0) this.children.splice(index, 1);
        }
    }

    removeFromParent(): void {
        this.parent?.removeChild(this);
    }

    override add(reader: Reader, child: TerminalObject): void {
        if (child instanceof Widget) this.append(child);
        else log.warn(`${child} has no element to add to ${this}`);
    }

    override remove(_reader: Reader, child: TerminalObject): void {
        if (child instanceof Widget) this.removeChild(child);
    }

    // ---- handlers ----------------------------------------------------------------------------------------------------

    override addHandler(reader: Reader, handler: number): void {
        if (HandlerModelDomHandler[handler]) this.addDomHandler(reader, HandlerModelToDomHandlerType[handler]!);
        else if (handler === H.HANDLER_VISIBILITY) this.observeVisibility();
        else super.addHandler(reader, handler);
    }

    override removeHandler(reader: Reader, handler: number): void {
        if (handler === H.HANDLER_VISIBILITY) this.visibility?.unobserve(this.element);
        else super.removeHandler(reader, handler);
    }

    /** Listens to a DOM event of the element until the widget is collected. */
    protected listen<E extends Event>(type: string, listener: (event: E) => void, target: EventTarget = this.element): void {
        target.addEventListener(type, event => this.context.guard(() => listener(event as E)), { signal: this.lifetime.signal });
    }

    private addDomHandler(reader: Reader, type: number): void {
        switch (type) {
            case D.CLICK:
                this.listen<MouseEvent>('click', e => this.onClick(type, e));
                break;
            case D.DOUBLE_CLICK:
                this.listen<MouseEvent>('dblclick', e => this.onClick(type, e));
                break;
            case D.MOUSE_OVER:
                this.listen<MouseEvent>('mouseover', e => this.sendMouseEvent(type, e));
                break;
            case D.MOUSE_OUT:
                this.listen<MouseEvent>('mouseout', e => this.sendMouseEvent(type, e));
                break;
            case D.MOUSE_DOWN:
                this.listen<MouseEvent>('mousedown', e => this.sendMouseEvent(type, e));
                break;
            case D.MOUSE_UP:
                this.listen<MouseEvent>('mouseup', e => this.sendMouseEvent(type, e));
                break;
            case D.MOUSE_WHELL:
                this.listen<WheelEvent>('wheel', e => this.sendWheelEvent(type, e));
                break;
            case D.BLUR:
                this.listen('blur', e => this.sendDomEvent(type, e));
                break;
            case D.FOCUS:
                this.listen('focus', e => this.sendDomEvent(type, e));
                break;
            case D.KEY_PRESS:
            case D.KEY_DOWN:
            case D.KEY_UP: {
                const filter = keyFilter(reader);
                const name = type === D.KEY_PRESS ? 'keypress' : type === D.KEY_DOWN ? 'keydown' : 'keyup';
                this.listen<KeyboardEvent>(name, e => this.onKey(type, e, filter));
                break;
            }
            case D.DRAG_START:
                this.element.draggable = true;
                this.listen<DragEvent>('dragstart', e => {
                    e.dataTransfer?.setData('text', String(this.id));
                    e.dataTransfer?.setDragImage(this.element, 10, 10);
                    this.sendDomEvent(type, e);
                });
                break;
            case D.DRAG_END:
                this.listen('dragend', e => this.sendDomEvent(type, e));
                break;
            case D.DRAG_ENTER:
                // As in GWT: a drag over an element is refused unless dragenter and dragover are prevented
                this.listen('dragenter', e => {
                    e.preventDefault();
                    this.sendDomEvent(type, e);
                });
                break;
            case D.DRAG_LEAVE:
                this.listen('dragleave', e => this.sendDomEvent(type, e));
                break;
            case D.DROP:
                this.listen('dragover', e => e.preventDefault());
                this.listen<DragEvent>('drop', e => {
                    e.preventDefault();
                    const message = this.domEvent(type);
                    const source = e.dataTransfer?.getData('text');
                    if (source) message[C.DRAG_SRC] = Number(source);
                    this.context.send(message);
                });
                break;
            case D.CONTEXT_MENU:
                this.listen('contextmenu', e => this.sendDomEvent(type, e));
                break;
            default:
                log.info(`${this} does not handle the DOM event #${type}`);
        }
    }

    protected domEvent(type: number): Message {
        const message = about(this.id);
        message[C.DOM_HANDLER_TYPE] = type;
        return message;
    }

    protected sendDomEvent(type: number, event: Event, details?: (message: Message) => void): void {
        const message = this.domEvent(type);
        details?.(message);
        this.context.send(message);
        this.preventOrStop(event);
    }

    /** A click or a double click: a widget that can be disabled filters it (P2). */
    protected onClick(type: number, event: MouseEvent): void {
        this.sendMouseEvent(type, event);
    }

    protected sendMouseEvent(type: number, event: MouseEvent): void {
        this.sendDomEvent(type, event, message => {
            message[C.EVENT_INFO] = [...this.pointer(event), event.ctrlKey, event.altKey, event.shiftKey, event.metaKey];
            message[C.WIDGET_POSITION] = this.position();
        });
    }

    private sendWheelEvent(type: number, event: WheelEvent): void {
        this.sendDomEvent(type, event, message => {
            message[C.EVENT_INFO] = [...this.pointer(event), Math.sign(event.deltaY)];
            message[C.WIDGET_POSITION] = this.position();
        });
    }

    protected onKey(type: number, event: KeyboardEvent, filter: number[] | null): void {
        const keyCode = event.keyCode | 0;
        if (filter === null || filter.includes(keyCode)) {
            const message = this.domEvent(type);
            message[C.VALUE_KEY] = keyCode;
            this.context.send(message);
        }
        this.preventOrStop(event);
    }

    /** clientX, clientY, x and y relative to the element, button (1 left, 4 middle, 2 right): GWT's MouseEvent. */
    private pointer(event: MouseEvent): number[] {
        const clientX = Math.trunc(event.clientX);
        const clientY = Math.trunc(event.clientY);
        const [left, top] = this.position();
        return [clientX, clientY, clientX - left! + this.element.scrollLeft + window.scrollX,
            clientY - top! + this.element.scrollTop + window.scrollY, event.button === 1 ? 4 : event.button === 2 ? 2 : 1];
    }

    /** Absolute left and top in the document, offset height and width. */
    private position(): number[] {
        const rect = this.element.getBoundingClientRect();
        return [Math.trunc(rect.left + window.scrollX), Math.trunc(rect.top + window.scrollY), this.element.offsetHeight,
            this.element.offsetWidth];
    }

    protected preventOrStop(event: Event): void {
        const bit = EVENT_BITS[event.type];
        if (bit === undefined) return;
        if (this.prevented?.has(bit)) event.preventDefault();
        if (this.stopped?.has(bit)) event.stopPropagation();
    }

    private observeVisibility(): void {
        this.visibility ??= new IntersectionObserver(entries => {
            const entry = entries[0];
            if (entry === undefined) return;
            const message = about(this.id);
            message[C.HANDLER_WIDGET_VISIBILITY] = entry.isIntersecting;
            this.context.send(message);
        });
        const observer = this.visibility;
        setTimeout(() => observer.observe(this.element)); // after the element is added, as GWT's deferred command
    }

    // ---- properties --------------------------------------------------------------------------------------------------

    setVisible(visible: boolean): void {
        this.element.style.display = visible ? '' : 'none';
        if (visible) this.element.removeAttribute('aria-hidden');
        else this.element.setAttribute('aria-hidden', 'true');
    }

    /**
     * STYLE_NAME replaces the class attribute, except the type class. With the legacy layer (legacy.ts), the first name
     * becomes the GWT primary name, as GWT's setStyleName made it.
     */
    setStyleName(names: string): void {
        this.element.className = this.typeClass === null ? names : `${this.typeClass} ${names}`.trim();
        if (document.documentElement.hasAttribute('data-pony-legacy')) {
            this.element.setAttribute('data-gwt-primary', names.trim().split(/\s+/)[0] ?? '');
        }
    }

    removeStyleNames(names: string[]): void {
        this.element.classList.remove(...names.filter(name => name !== this.typeClass));
    }

    /** BIND: a global function makes the object that receives this widget's NATIVE messages. */
    bind(functionName: string): void {
        const factory = (window as unknown as Record<string, unknown>)[functionName];
        if (typeof factory !== 'function') throw new Error(`No function ${functionName} to bind ${this}`);
        this.binding = factory(this.id, this.element) as NativeBinding;
    }

    sendToBinding(json: string): void {
        if (this.binding === undefined) throw new Error(`NATIVE message for ${this}, which is not bound`);
        this.binding.update(JSON.parse(json));
    }

    preventEvent(bit: number): void {
        (this.prevented ??= new Set()).add(bit);
    }

    stopEvent(bit: number): void {
        (this.stopped ??= new Set()).add(bit);
    }
}

/** KEY_FILTER, when the server gave one: only these key codes are sent. */
function keyFilter(reader: Reader): number[] | null {
    reader.read();
    if (reader.key === K.KEY_FILTER) return reader.array.map(Number);
    reader.rewind();
    return null;
}

/** A style property as GWT's Style.setProperty takes it: camelCase, or a hyphenated or custom property. */
export function setStyle(element: HTMLElement, name: string, value: string): void {
    if (name.includes('-')) element.style.setProperty(name, value);
    else (element.style as unknown as Record<string, string>)[name] = value;
}

/** Splits names on spaces: the server may send several in one. */
function names(value: string | null): string[] {
    return value === null ? [] : value.split(/\s+/).filter(name => name.length > 0);
}

function next(reader: Reader): string | null {
    reader.read();
    return reader.str;
}

const WIDGET: Setters<Widget> = new Map<number, (w: Widget, r: Reader) => void>([
    [K.ADD_STYLE_NAME, (w, r) => w.element.classList.add(...names(r.str))],
    [K.REMOVE_STYLE_NAME, (w, r) => w.removeStyleNames(names(r.str))],
    [K.STYLE_NAME, (w, r) => w.setStyleName(r.str ?? '')],
    [K.WIDGET_VISIBLE, (w, r) => w.setVisible(r.bool)],
    [K.WIDGET_TITLE, (w, r) => {
        if (r.str) w.element.title = r.str;
        else w.element.removeAttribute('title');
    }],
    [K.PUT_PROPERTY_KEY, (w, r) => {
        const name = r.str ?? '';
        (w.element as unknown as Record<string, unknown>)[name] = next(r);
    }],
    [K.PUT_ATTRIBUTE_KEY, (w, r) => {
        const name = r.str ?? '';
        w.element.setAttribute(name, next(r) ?? '');
    }],
    [K.PUT_STYLE_KEY, (w, r) => {
        const name = r.str ?? '';
        setStyle(w.element, name, next(r) ?? '');
    }],
    [K.REMOVE_ATTRIBUTE_KEY, (w, r) => w.element.removeAttribute(r.str ?? '')],
    [K.REMOVE_STYLE_KEY, (w, r) => setStyle(w.element, r.str ?? '', '')],
    [K.FORCE_DOM_ID, w => {
        w.element.id = String(w.id);
    }],
    [K.FOCUS, (w, r) => {
        if (r.bool) w.focusTarget.focus();
        else w.focusTarget.blur();
    }],
    [K.FOCUS_PREVENT_SCROLL, w => w.focusTarget.focus({ preventScroll: true })],
    [K.WIDGET_WIDTH, (w, r) => {
        w.element.style.width = r.str ?? '';
    }],
    [K.WIDGET_HEIGHT, (w, r) => {
        w.element.style.height = r.str ?? '';
    }],
    [K.WIDGET_FULL_SIZE, w => {
        w.element.style.width = '100%';
        w.element.style.height = '100%';
    }],
    [K.TABINDEX, (w, r) => {
        w.focusTarget.tabIndex = r.int;
    }],
    [K.ENSURE_DEBUG_ID, (w, r) => w.element.setAttribute('pid', r.str ?? '')],
    [K.EVAL, (w, r) => {
        new Function('element', `return ${r.str ?? ''}`)(w.element);
    }],
    [K.BIND, (w, r) => w.bind(r.str ?? '')],
    [K.NATIVE, (w, r) => w.sendToBinding(r.str ?? 'null')],
    [K.PREVENT_EVENT, (w, r) => w.preventEvent(r.int)],
    [K.STOP_EVENT, (w, r) => w.stopEvent(r.int)],
]);
