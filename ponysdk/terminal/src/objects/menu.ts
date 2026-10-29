/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, HandlerModel as H, ServerToClientModel as K } from '#model';
import { about, FLAG } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import type { Context, TerminalObject } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';
import { POPUP_CLASS } from './popup.ts';

/**
 * PMenuBar, on the WAI-ARIA menubar pattern: a bar (role menubar), or the menu of an item (role menu), shown in a
 * popover under the item of a horizontal bar, beside the item of a vertical menu.
 *
 * As GWT's MenuBar: a click opens or closes an item's submenu; once a submenu shows, hovering another item of the bar
 * opens its own, and in a submenu hovering opens at once. A click on an item with a command closes every menu, then
 * sends HANDLER_COMMAND. A press outside the menus closes them. Keyboard: arrows, Home, End, Enter, Space, Escape.
 */
export class MenuBar extends Widget {
    readonly vertical: boolean;
    /** The item this bar is the submenu of. */
    parentItem: MenuItem | null = null;
    /** The item whose submenu shows. */
    openItem: MenuItem | null = null;
    private outside: AbortController | null = null;

    static create(context: Context, id: number, type: number, reader: Reader): MenuBar {
        reader.read(); // VERTICAL
        return new MenuBar(context, id, type, reader.bool);
    }

    private constructor(context: Context, id: number, type: number, vertical: boolean) {
        const element = document.createElement('div');
        element.setAttribute('role', 'menubar');
        element.setAttribute('aria-orientation', vertical ? 'vertical' : 'horizontal');
        super(context, id, type, element, 'pony-MenuBar');
        this.vertical = vertical;
        this.listen<KeyboardEvent>('keydown', e => this.onMenuKey(e));
    }

    /** An item or a separator, at BEFORE_INDEX when given. */
    override add(reader: Reader, child: TerminalObject): void {
        let index = this.children.length;
        if (reader.read() === K.BEFORE_INDEX) index = Math.min(reader.int, this.children.length);
        else reader.rewind();
        if (!(child instanceof Widget)) return;
        this.insert(child, index);
        this.refreshTabStop();
    }

    override update(reader: Reader): boolean {
        if (reader.key === K.CLEAR) {
            this.closeSubmenus();
            for (const child of [...this.children]) this.removeChild(child);
            return true;
        }
        if (reader.key === K.ANIMATION) return true;
        return super.update(reader);
    }

    get items(): MenuItem[] {
        return this.children.filter((c): c is MenuItem => c instanceof MenuItem);
    }

    /** The bar at the top of this menu's chain. */
    get root(): MenuBar {
        let bar: MenuBar = this;
        while (bar.parentItem !== null) bar = bar.parentItem.bar;
        return bar;
    }

    /** Becomes the submenu of an item. */
    nestUnder(item: MenuItem): void {
        this.parentItem = item;
        this.element.setAttribute('role', 'menu');
        this.element.setAttribute('aria-orientation', 'vertical');
    }

    openSubmenu(item: MenuItem): void {
        if (this.openItem === item) return;
        this.closeSubmenus();
        this.openItem = item;
        item.showSubmenu(this.vertical || this.parentItem !== null);
        const root = this.root;
        if (root.outside === null) {
            root.outside = new AbortController();
            document.addEventListener('pointerdown', e => this.context.guard(() => {
                if (!root.element.contains(e.composedPath()[0] as Node)) root.closeSubmenus(); // the target in a shadow root
            }), { capture: true, signal: root.outside.signal }); // aborted when the menus close, and by destroy()
        }
    }

    closeSubmenus(): void {
        const item = this.openItem;
        this.openItem = null;
        if (item !== null) {
            item.submenu?.closeSubmenus();
            item.hideSubmenu();
        }
        if (this.parentItem === null) {
            this.outside?.abort();
            this.outside = null;
        }
    }

    private refreshTabStop(): void {
        if (this.parentItem !== null) return;
        const items = this.items;
        items.forEach((item, i) => {
            item.element.tabIndex = i === 0 ? 0 : -1;
        });
    }

    private onMenuKey(event: KeyboardEvent): void {
        const items = this.items.filter(i => i.enabled);
        const current = items.findIndex(i => i.element === this.focused);
        if (current === -1) return;
        const item = items[current]!;
        const next = this.vertical || this.parentItem !== null ? ['ArrowDown', 'ArrowUp'] : ['ArrowRight', 'ArrowLeft'];
        let handled = true;
        if (event.key === next[0]) items[(current + 1) % items.length]!.element.focus();
        else if (event.key === next[1]) items[(current - 1 + items.length) % items.length]!.element.focus();
        else if (event.key === 'Home') items[0]!.element.focus();
        else if (event.key === 'End') items[items.length - 1]!.element.focus();
        else if (event.key === 'Enter' || event.key === ' ') item.activate(true);
        else if (event.key === 'ArrowDown' && item.submenu !== null) item.activate(true);
        else if (event.key === 'ArrowRight' && item.submenu !== null) item.activate(true);
        else if ((event.key === 'Escape' || event.key === 'ArrowLeft') && this.parentItem !== null) {
            this.parentItem.bar.closeSubmenus();
            this.parentItem.element.focus();
        } else handled = false;
        if (handled) {
            event.preventDefault();
            event.stopPropagation();
        }
    }

    override destroy(): void {
        this.outside?.abort();
        super.destroy();
    }
}

/** PMenuItem: text or HTML, a command (HANDLER_COMMAND) and/or a submenu (an added PMenuBar). */
export class MenuItem extends Widget {
    submenu: MenuBar | null = null;
    enabled = true;
    private command = false;
    private readonly label: HTMLSpanElement;
    private popup: HTMLDivElement | null = null;

    static create(context: Context, id: number, type: number): MenuItem {
        return new MenuItem(context, id, type);
    }

    private constructor(context: Context, id: number, type: number) {
        const element = document.createElement('div');
        element.setAttribute('role', 'menuitem');
        element.tabIndex = -1;
        const label = document.createElement('span');
        element.append(label);
        super(context, id, type, element, 'pony-MenuItem');
        this.label = label;
        this.listen<MouseEvent>('click', e => {
            if (!this.popup?.contains(e.target as Node)) this.activate(false);
        });
        this.listen<MouseEvent>('mouseover', e => {
            if (!this.popup?.contains(e.target as Node)) this.hover();
        });
    }

    /** The server adds an item to a bar only; a removed one leaves the page, and has no events. */
    get bar(): MenuBar {
        return this.parent as MenuBar;
    }

    /** The submenu, in a popover inside this item. */
    override add(_reader: Reader, child: TerminalObject): void {
        if (!(child instanceof MenuBar)) return;
        if (this.popup === null) {
            this.popup = document.createElement('div');
            this.popup.setAttribute('popover', 'manual');
            this.popup.className = `${POPUP_CLASS} pony-MenuPopup`;
            this.element.append(this.popup);
        }
        for (const widget of [...this.children]) this.removeChild(widget);
        child.nestUnder(this);
        this.submenu = child;
        this.appendInside(this.popup, child);
        this.element.setAttribute('aria-haspopup', 'menu');
        this.element.setAttribute('aria-expanded', 'false');
    }

    override update(reader: Reader): boolean {
        switch (reader.key) {
            case K.TEXT:
                this.label.textContent = reader.str;
                return true;
            case K.HTML:
                this.label.innerHTML = reader.str;
                return true;
            case K.ENABLED:
                this.enabled = reader.bool;
                if (this.enabled) this.element.removeAttribute('aria-disabled');
                else this.element.setAttribute('aria-disabled', 'true');
                return true;
            default:
                return super.update(reader);
        }
    }

    override addHandler(reader: Reader, handler: number): void {
        if (handler === H.HANDLER_COMMAND) this.command = true;
        else super.addHandler(reader, handler);
    }

    /** A click, Enter or Space: the submenu opens or closes, or the command runs. */
    activate(byKeyboard: boolean): void {
        const bar = this.bar;
        if (!this.enabled) return;
        if (this.submenu !== null) {
            if (bar.openItem === this && !byKeyboard) bar.closeSubmenus();
            else {
                bar.openSubmenu(this);
                if (byKeyboard) this.submenu.items.find(i => i.enabled)?.element.focus();
            }
            return;
        }
        bar.closeSubmenus();
        if (!this.command) return;
        bar.root.closeSubmenus();
        const message = about(this.id);
        message[C.HANDLER_COMMAND] = FLAG;
        this.context.send(message);
    }

    private hover(): void {
        const bar = this.bar;
        if (!this.enabled) return;
        const menuOpen = bar.openItem !== null || bar.parentItem !== null;
        if (!menuOpen || bar.openItem === this) return;
        if (this.submenu !== null) bar.openSubmenu(this);
        else bar.closeSubmenus();
    }

    /** Of an item with a submenu. */
    showSubmenu(beside: boolean): void {
        const popup = this.popup!;
        const r = this.element.getBoundingClientRect();
        popup.style.left = `${(beside ? r.right : r.left) + window.scrollX}px`;
        popup.style.top = `${(beside ? r.top : r.bottom) + window.scrollY}px`;
        popup.showPopover();
        this.element.setAttribute('aria-expanded', 'true');
    }

    hideSubmenu(): void {
        if (this.popup?.matches(':popover-open')) this.popup.hidePopover();
        if (this.submenu !== null) this.element.setAttribute('aria-expanded', 'false');
    }
}

/** PMenuItemSeparator. */
export const createMenuItemSeparator = (context: Context, id: number, type: number): Widget => {
    const element = document.createElement('div');
    element.setAttribute('role', 'separator');
    return new Widget(context, id, type, element, 'pony-MenuItemSeparator');
};
