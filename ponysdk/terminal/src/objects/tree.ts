/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, ServerToClientModel as K } from '#model';
import { about } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import type { Context, TerminalObject } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';

/**
 * PTree, on the WAI-ARIA tree pattern: a tree (role tree) of items (role treeitem) in nested lists (role group).
 *
 * As GWT's Tree: a click on an item selects it and sends HANDLER_SELECTION, even if it was selected; a click on its
 * toggle opens or closes it and sends HANDLER_OPEN or HANDLER_CLOSE, with the item's id. What the server sets
 * (SELECTED_INDEX, OPEN, CLOSE) sends nothing, except the selection a closing moves. Keyboard: Up and Down select, Right opens or goes to the first child,
 * Left closes or goes to the parent, Home and End.
 */
export class Tree extends Widget {
    readonly list: HTMLUListElement;
    private selected: TreeItem | null = null;

    static create(context: Context, id: number, type: number): Tree {
        return new Tree(context, id, type);
    }

    private constructor(context: Context, id: number, type: number) {
        const element = document.createElement('div');
        element.setAttribute('role', 'tree');
        const list = document.createElement('ul');
        list.setAttribute('role', 'presentation');
        element.append(list);
        super(context, id, type, element, 'pony-Tree');
        this.list = list;
        this.listen<KeyboardEvent>('keydown', e => this.onTreeKey(e));
    }

    /** The invisible root item, whose children are the tree's top items (TREE_ROOT). */
    adoptRoot(root: TreeItem): void {
        this.children.push(root);
        this.adopt(root);
    }

    override remove(reader: Reader, child: TerminalObject): void {
        if (child instanceof Widget && child.parent instanceof TreeItem) child.parent.remove(reader, child);
    }

    override update(reader: Reader): boolean {
        switch (reader.key) {
            case K.ANIMATION:
                return true;
            case K.SELECTED_INDEX: {
                const item = reader.int === -1 ? null : this.context.object(reader.int);
                this.select(item instanceof TreeItem ? item : null, false);
                return true;
            }
            case K.CLEAR:
                for (const root of this.children) if (root instanceof TreeItem) root.clearItems();
                this.selected = null;
                return true;
            default:
                return super.update(reader);
        }
    }

    get selectedItem(): TreeItem | null {
        return this.selected;
    }

    select(item: TreeItem | null, fireEvents: boolean): void {
        this.selected?.setSelected(false);
        this.selected = item;
        if (item === null) return;
        item.setSelected(true);
        this.refreshTabStop();
        if (fireEvents) this.sendItem(C.HANDLER_SELECTION, item);
    }

    sendItem(key: string, item: TreeItem): void {
        const message = about(this.id);
        message[key] = item.id;
        this.context.send(message);
    }

    /** The items shown, in order: those of open items only. */
    private visibleItems(): TreeItem[] {
        const items: TreeItem[] = [];
        const walk = (item: TreeItem): void => {
            for (const child of item.items) {
                items.push(child);
                if (child.open) walk(child);
            }
        };
        for (const root of this.children) if (root instanceof TreeItem) walk(root);
        return items;
    }

    private refreshTabStop(): void {
        const items = this.visibleItems();
        const stop = this.selected !== null && items.includes(this.selected) ? this.selected : items[0];
        for (const item of items) item.element.tabIndex = item === stop ? 0 : -1;
    }

    private onTreeKey(event: KeyboardEvent): void {
        const items = this.visibleItems();
        const current = items.findIndex(i => i.element === document.activeElement);
        if (current === -1) return;
        const item = items[current]!;
        let target: TreeItem | undefined;
        switch (event.key) {
            case 'ArrowDown':
                target = items[current + 1];
                break;
            case 'ArrowUp':
                target = items[current - 1];
                break;
            case 'Home':
                target = items[0];
                break;
            case 'End':
                target = items[items.length - 1];
                break;
            case 'ArrowRight':
                if (item.items.length > 0 && !item.open) item.toggle();
                else target = item.items[0];
                break;
            case 'ArrowLeft':
                if (item.open) item.toggle();
                else if (item.parent instanceof TreeItem && !item.parent.isRoot) target = item.parent;
                break;
            default:
                return;
        }
        event.preventDefault();
        if (target !== undefined) {
            this.select(target, true);
            target.element.focus();
        }
    }
}

/**
 * PTreeItem: a list item with a row (a toggle and the content: TEXT, or a widget added with WIDGET) and the list of
 * its children. The tree's root item (TREE_ROOT) is never shown: its children are the tree's top items.
 */
export class TreeItem extends Widget {
    readonly isRoot: boolean;
    open = false;
    private readonly childList: HTMLUListElement;
    private readonly content: HTMLSpanElement;
    private readonly toggleElement: HTMLSpanElement;

    static create(context: Context, id: number, type: number, reader: Reader): TreeItem {
        let text: string | null = null;
        if (reader.read() === K.TEXT) text = reader.str;
        else reader.rewind();
        let tree: TerminalObject | undefined;
        if (reader.read() === K.TREE_ROOT) tree = context.object(reader.int);
        else reader.rewind();
        const item = new TreeItem(context, id, type, tree instanceof Tree ? tree : null);
        if (text !== null) item.content.textContent = text;
        return item;
    }

    private constructor(context: Context, id: number, type: number, tree: Tree | null) {
        const element = document.createElement('li');
        element.setAttribute('role', 'treeitem');
        element.tabIndex = -1;
        const row = document.createElement('div');
        row.className = 'pony-TreeItem-row';
        const toggle = document.createElement('span');
        toggle.className = 'pony-TreeItem-toggle';
        toggle.setAttribute('aria-hidden', 'true');
        const content = document.createElement('span');
        content.className = 'pony-TreeItem-content';
        row.append(toggle, content);
        const children = document.createElement('ul');
        children.setAttribute('role', 'group');
        children.hidden = true;
        element.append(row, children);
        super(context, id, type, element, 'pony-TreeItem');
        this.content = content;
        this.toggleElement = toggle;
        this.isRoot = tree !== null;
        this.childList = tree !== null ? tree.list : children;
        if (tree !== null) tree.adoptRoot(this);
        this.listen<MouseEvent>('click', e => {
            if (toggle.contains(e.target as Node)) this.toggle();
            else if (row.contains(e.target as Node)) this.tree?.select(this, true);
        }, row);
    }

    get items(): TreeItem[] {
        return this.children.filter((c): c is TreeItem => c instanceof TreeItem);
    }

    get tree(): Tree | null {
        let parent = this.parent;
        while (parent instanceof TreeItem) parent = parent.parent;
        return parent instanceof Tree ? parent : null;
    }

    /** A widget as content (WIDGET), or a child item, at INDEX when given. */
    override add(reader: Reader, child: TerminalObject): void {
        const key = reader.read();
        if (key === K.WIDGET && child instanceof Widget) {
            this.content.replaceChildren();
            this.appendInside(this.content, child);
            return;
        }
        let index = this.items.length;
        if (key === K.INDEX) index = Math.min(reader.int, this.items.length);
        else reader.rewind();
        if (!(child instanceof TreeItem)) return;
        child.removeFromParent();
        this.childList.insertBefore(child.element, this.items[index]?.element ?? null);
        this.children.push(child);
        this.adopt(child);
        this.refresh();
    }

    override remove(_reader: Reader, child: TerminalObject): void {
        if (child instanceof Widget) this.removeChild(child);
        this.refresh();
    }

    clearItems(): void {
        for (const item of this.items) this.removeChild(item);
        this.refresh();
    }

    override update(reader: Reader): boolean {
        switch (reader.key) {
            case K.OPEN:
                this.setOpen(true);
                return true;
            case K.CLOSE:
                this.setOpen(false);
                return true;
            case K.TEXT:
                this.content.textContent = reader.str;
                return true;
            default:
                return super.update(reader);
        }
    }

    /** By the user: the server is told. */
    toggle(): void {
        if (this.items.length === 0) return;
        this.setOpen(!this.open);
        this.tree?.sendItem(this.open ? C.HANDLER_OPEN : C.HANDLER_CLOSE, this);
    }

    /**
     * Closing an item that holds the selection, or is it, selects it and tells the server, by the user or by the
     * server: as GWT's Tree.maybeUpdateSelection.
     */
    setOpen(open: boolean): void {
        this.open = open;
        this.refresh();
        const tree = this.tree;
        if (open || tree === null) return;
        for (let item: Widget | null = tree.selectedItem; item instanceof TreeItem; item = item.parent) {
            if (item === this) {
                tree.select(this, true);
                return;
            }
        }
    }

    setSelected(selected: boolean): void {
        this.element.setAttribute('aria-selected', String(selected));
    }

    private refresh(): void {
        if (this.isRoot) return;
        const parent = this.items.length > 0;
        if (parent) this.element.setAttribute('aria-expanded', String(this.open));
        else this.element.removeAttribute('aria-expanded');
        this.toggleElement.dataset['state'] = parent ? (this.open ? 'open' : 'closed') : 'leaf';
        this.childList.hidden = !this.open;
    }
}
