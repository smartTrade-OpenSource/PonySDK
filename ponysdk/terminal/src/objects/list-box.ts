/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, ServerToClientModel as K } from '#model';
import { about } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import { apply, type Context, type Setters } from '../runtime/object.ts';
import { FocusWidget } from './focus-widget.ts';

/**
 * PListBox: a select. A change sends the selected indexes, the first selected first: "1", "1,3", or "-1" for none.
 * Item indexes count the options of every group.
 */
export class ListBox extends FocusWidget {
    static create(context: Context, id: number, type: number): ListBox {
        return new ListBox(context, id, type, document.createElement('select'), 'pony-ListBox');
    }

    constructor(context: Context, id: number, type: number, element: HTMLSelectElement, typeClass: string) {
        super(context, id, type, element, typeClass);
        this.listen('change', () => {
            const message = about(this.id);
            message[C.HANDLER_CHANGE] = this.selection();
            this.context.send(message);
        });
    }

    get select(): HTMLSelectElement {
        return this.element as HTMLSelectElement;
    }

    override update(reader: Reader): boolean {
        return apply(LIST_BOX, this, reader) || super.update(reader);
    }

    private selection(): string {
        const first = this.select.selectedIndex;
        if (first === -1) return '-1';
        const indexes = [first];
        for (let i = 0; i < this.select.options.length; i++) if (i !== first && this.select.options[i]!.selected) indexes.push(i);
        return indexes.join(',');
    }

    insertItem(text: string, index: number): void {
        const option = new Option(text);
        const before = index === -1 ? null : this.select.options[index] ?? null;
        this.select.add(option, before);
    }
}

const LIST_BOX: Setters<ListBox> = new Map<number, (w: ListBox, r: Reader) => void>([
    [K.CLEAR, w => w.select.replaceChildren()],
    [K.ITEM_INSERTED, (w, r) => {
        const text = r.str;
        if (r.read() === K.INDEX) {
            w.insertItem(text, r.int);
        } else {
            r.rewind();
            w.insertItem(text, -1);
        }
    }],
    [K.ITEM_ADD, (w, r) => {
        const items = r.array;
        r.read(); // ITEM_GROUP
        const group = document.createElement('optgroup');
        group.label = r.str;
        for (const item of items) group.append(new Option(String(item)));
        w.select.append(group);
    }],
    [K.ITEM_UPDATED, (w, r) => {
        const text = r.str;
        r.read(); // INDEX
        const option = w.select.options[r.int];
        if (option !== undefined) option.text = text;
    }],
    [K.ITEM_REMOVED, (w, r) => w.select.options[r.int]?.remove()],
    [K.SELECTED, (w, r) => {
        const selected = r.bool;
        r.read(); // INDEX
        if (r.int === -1) w.select.selectedIndex = -1;
        else {
            const option = w.select.options[r.int];
            if (option !== undefined) option.selected = selected;
        }
    }],
    [K.VISIBLE_ITEM_COUNT, (w, r) => {
        w.select.size = r.int;
    }],
    [K.MULTISELECT, (w, r) => {
        w.select.multiple = r.bool;
    }],
]);
