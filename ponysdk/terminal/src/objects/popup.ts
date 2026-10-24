/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, HandlerModel as H, ServerToClientModel as K } from '#model';
import { about, FLAG } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import type { Context, TerminalObject } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';

/** On every popup, whatever its type: the stylesheet resets the browser's dialog and popover styles on it. */
export const POPUP_CLASS = 'pony-Popup';

function dialog(): HTMLDialogElement {
    const element = document.createElement('dialog');
    element.setAttribute('popover', 'manual');
    return element;
}

/**
 * PPopupPanel and the popups made on it: a dialog element in the browser's top
 * layer, shown as a popover, or with showModal() when modal; no z-index to manage. Its page position is set by the
 * server (POSITION_LEFT, POSITION_TOP), or centered (CENTER).
 *
 * An auto-hide popup hides on a press outside it. A glass (POPUP_GLASS_ENABLED) covers the page under it, with the
 * class pony-Glass and the server's glass style name; a press on it hides an auto-hide popup. Hiding, by the user or
 * by the server, sends HANDLER_CLOSE, as GWT's CloseEvent does.
 */
export class PopupPanel extends Widget {
    protected readonly autoHide: boolean;
    /** Where the widget goes: the popup itself, or the content part of a dialog box. */
    protected container: HTMLElement;
    protected draggable = false;
    private showing = false;
    private modal = false;
    private glassEnabled = false;
    private glassStyleName = '';
    private glass: HTMLDivElement | null = null;
    /** The listeners that live while the popup shows. */
    private whileShown: AbortController | null = null;
    private dragHandle: HTMLElement | null = null;

    static create(context: Context, id: number, type: number, reader: Reader): PopupPanel {
        return new PopupPanel(context, id, type, dialog(), 'pony-PopupPanel', readAutoHide(reader));
    }

    constructor(context: Context, id: number, type: number, element: HTMLDialogElement, typeClass: string, autoHide: boolean) {
        super(context, id, type, element, typeClass);
        element.classList.add(POPUP_CLASS);
        element.style.left = '0px';
        element.style.top = '0px';
        this.autoHide = autoHide;
        this.container = element;
        this.onAttachChange(attached => {
            if (!attached) this.close(); // out of the page: shown no longer, nothing to tell the server
        });
    }

    get dialog(): HTMLDialogElement {
        return this.element as HTMLDialogElement;
    }

    /** The widget, which replaces the previous one. */
    override add(_reader: Reader, child: TerminalObject): void {
        if (!(child instanceof Widget)) return;
        for (const widget of [...this.children]) if (widget !== child) this.removeChild(widget);
        this.appendInside(this.container, child);
    }

    override update(reader: Reader): boolean {
        switch (reader.key) {
            case K.ANIMATION:
                return true;
            case K.CENTER:
                this.show();
                this.center();
                this.setVisible(true); // created hidden when attached before being shown
                return true;
            case K.OPEN:
                this.show();
                this.setVisible(true);
                return true;
            case K.POPUP_POSITION_AND_SHOW:
                this.setVisible(true);
                return true;
            case K.CLOSE:
                this.hide();
                return true;
            case K.POPUP_GLASS_ENABLED:
                this.glassEnabled = reader.bool;
                return true;
            case K.POPUP_GLASS_STYLE_NAME:
                this.glassStyleName = reader.str ?? '';
                return true;
            case K.MODAL:
                this.modal = reader.bool;
                return true;
            case K.POSITION_LEFT: {
                const left = reader.int;
                reader.read(); // POSITION_TOP
                this.setPosition(left, reader.int);
                return true;
            }
            case K.DRAGGABLE:
                this.setDraggable(reader.bool, this.element);
                return true;
            default:
                return super.update(reader);
        }
    }

    /** HANDLER_POPUP_POSITION: shown, then its size and the window's go to the server, which places it. */
    override addHandler(reader: Reader, handler: number): void {
        if (handler !== H.HANDLER_POPUP_POSITION) {
            super.addHandler(reader, handler);
            return;
        }
        this.setVisible(true);
        this.show();
        setTimeout(() => this.context.guard(() => {
            const message = about(this.id);
            const root = document.documentElement;
            message[C.POPUP_POSITION] = [this.element.offsetWidth, this.element.offsetHeight, root.clientWidth, root.clientHeight];
            this.context.send(message);
        }));
    }

    setPosition(left: number, top: number): void {
        this.element.style.left = `${left}px`;
        this.element.style.top = `${top}px`;
    }

    /** In the middle of the window, as GWT's PopupPanel.center. */
    center(): void {
        const root = document.documentElement;
        const left = (root.clientWidth - this.element.offsetWidth) >> 1;
        const top = (root.clientHeight - this.element.offsetHeight) >> 1;
        this.setPosition(Math.max(window.scrollX + left, 0), Math.max(window.scrollY + top, 0));
    }

    show(): void {
        if (this.showing) return;
        if (!this.element.isConnected) document.body.append(this.element);
        this.showing = true;
        this.whileShown = new AbortController(); // aborted when hidden, and by destroy() through close()
        const signal = this.whileShown.signal;
        if (this.glassEnabled) this.showGlass(signal);
        if (this.modal) {
            this.dialog.removeAttribute('popover');
            this.dialog.showModal();
            // Escape does not close a popup, as in GWT; a second Escape the browser forces is a close
            this.dialog.addEventListener('cancel', e => e.preventDefault(), { signal });
            this.dialog.addEventListener('close', () => this.context.guard(() => {
                if (this.showing) this.hide();
            }), { signal });
        } else {
            this.dialog.setAttribute('popover', 'manual');
            this.dialog.showPopover();
        }
        if (this.autoHide) {
            document.addEventListener('pointerdown', e => this.context.guard(() => {
                if (this.showing && this.isOutside(e)) this.hide();
            }), { capture: true, signal });
        }
    }

    private showGlass(signal: AbortSignal): void {
        const glass = document.createElement('div');
        glass.setAttribute('popover', 'manual');
        glass.className = `pony-Glass ${this.glassStyleName}`.trim();
        document.body.append(glass);
        glass.showPopover(); // before the popup: under it in the top layer
        glass.addEventListener('pointerdown', () => this.context.guard(() => {
            if (this.autoHide && this.showing) this.hide();
        }), { signal });
        this.glass = glass;
    }

    /** A press outside the popup; on a modal one, a press on its backdrop targets the dialog itself. */
    private isOutside(event: PointerEvent): boolean {
        const target = event.target as Node | null;
        if (target === this.element) {
            const r = this.element.getBoundingClientRect();
            return event.clientX < r.left || event.clientX > r.right || event.clientY < r.top || event.clientY > r.bottom;
        }
        return target === null || !this.element.contains(target);
    }

    /** Hides it and tells the server. */
    hide(): void {
        if (this.close()) this.closed();
    }

    /** False when it was not shown. */
    private close(): boolean {
        if (!this.showing) return false;
        this.showing = false;
        this.whileShown?.abort();
        this.whileShown = null;
        if (this.dialog.open) this.dialog.close();
        if (this.dialog.matches(':popover-open')) this.dialog.hidePopover();
        this.glass?.remove();
        this.glass = null;
        return true;
    }

    /** Hidden: the server is told. */
    private closed(): void {
        const message = about(this.id);
        message[C.HANDLER_CLOSE] = FLAG;
        this.context.send(message);
    }

    /** Dragged by a handle: the whole popup, or a dialog box's caption. */
    protected setDraggable(draggable: boolean, handle: HTMLElement): void {
        this.draggable = draggable;
        if (!draggable || this.dragHandle === handle) return;
        this.dragHandle = handle;
        let start: [number, number] | null = null;
        this.listen<PointerEvent>('pointerdown', e => {
            if (!this.draggable || e.button !== 0) return;
            const r = this.element.getBoundingClientRect();
            start = [e.clientX - r.left, e.clientY - r.top];
            handle.setPointerCapture(e.pointerId);
            e.preventDefault();
        }, handle);
        this.listen<PointerEvent>('pointermove', e => {
            if (start === null) return;
            const x = e.pageX;
            const y = e.pageY;
            // As GWT: not past the left, the top or the right of the window
            if (x < 0 || x >= document.documentElement.clientWidth + window.scrollX || y < 0) return;
            this.setPosition(x - start[0], y - start[1]);
        }, handle);
        this.listen<PointerEvent>('pointerup', () => {
            start = null;
        }, handle);
    }

    override destroy(): void {
        this.close();
        super.destroy();
    }
}

function readAutoHide(reader: Reader): boolean {
    if (reader.read() === K.POPUP_AUTO_HIDE) return reader.bool;
    reader.rewind();
    return false;
}

/** PDecoratedPopupPanel: a popup whose decoration is the stylesheet's. */
export const createDecoratedPopupPanel = (context: Context, id: number, type: number, reader: Reader): PopupPanel =>
    new PopupPanel(context, id, type, dialog(), 'pony-DecoratedPopupPanel', readAutoHide(reader));

let captionIds = 0;

/** PDialogBox: a popup with a caption (POPUP_CAPTION, HTML) by which it is dragged, unless DRAGGABLE is false. */
export class DialogBox extends PopupPanel {
    private readonly caption: HTMLDivElement;

    static override create(context: Context, id: number, type: number, reader: Reader): DialogBox {
        return new DialogBox(context, id, type, readAutoHide(reader));
    }

    private constructor(context: Context, id: number, type: number, autoHide: boolean) {
        const element = dialog();
        const caption = document.createElement('div');
        caption.className = 'pony-Caption';
        caption.id = `pony-caption-${++captionIds}`;
        const content = document.createElement('div');
        content.className = 'pony-Content';
        element.append(caption, content);
        element.setAttribute('aria-labelledby', caption.id);
        super(context, id, type, element, 'pony-DialogBox', autoHide);
        this.caption = caption;
        this.container = content;
        this.setDraggable(true, caption);
    }

    override update(reader: Reader): boolean {
        if (reader.key === K.POPUP_CAPTION) {
            this.caption.innerHTML = reader.str ?? '';
            return true;
        }
        if (reader.key === K.DRAGGABLE) {
            this.setDraggable(reader.bool, this.caption);
            return true;
        }
        return super.update(reader);
    }
}
