/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ServerToClientModel as K } from '#model';
import type { Reader } from '../protocol/reader.ts';
import { apply, type Context, type Setters } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';
import { JsFunction } from './script.ts';

/**
 * The text of an element. When it already holds a single text node (a price, a counter being updated), the node's
 * data changes in place: nothing is removed nor created.
 */
export function setText(element: HTMLElement, text: string): void {
    const node = element.firstChild;
    if (node !== null && node.nodeType === Node.TEXT_NODE && node.nextSibling === null && text) {
        if ((node as Text).data !== text) (node as Text).data = text;
    } else {
        element.textContent = text;
    }
}

/** PLabel: text in a div. ATTRIBUTE_LINKED_TO_VALUE names an attribute that follows the text. */
export class Label extends Widget {
    private linkedAttribute = '';

    static create(context: Context, id: number, type: number): Label {
        return new Label(context, id, type, document.createElement('div'), 'pony-Label');
    }

    override update(reader: Reader): boolean {
        return apply(LABEL, this, reader) || super.update(reader);
    }

    text(value: string): void {
        setText(this.element, value);
        this.syncLinkedAttribute(value);
    }

    linkAttribute(name: string): void {
        this.linkedAttribute = name;
        this.syncLinkedAttribute(this.element.textContent);
    }

    private syncLinkedAttribute(value: string | null): void {
        if (this.linkedAttribute === '') return;
        if (value) this.element.setAttribute(this.linkedAttribute, value);
        else this.element.removeAttribute(this.linkedAttribute);
    }
}

const LABEL: Setters<Label> = new Map<number, (w: Label, r: Reader) => void>([
    [K.TEXT, (w, r) => w.text(r.str)],
    [K.ATTRIBUTE_LINKED_TO_VALUE, (w, r) => w.linkAttribute(r.str)],
]);

/** PHTML: HTML in a div, wrapping or not. */
export class Html extends Label {
    static override create(context: Context, id: number, type: number): Html {
        return new Html(context, id, type, document.createElement('div'), 'pony-HTML');
    }

    override update(reader: Reader): boolean {
        return apply(HTML, this, reader) || super.update(reader);
    }
}

const HTML: Setters<Html> = new Map<number, (w: Html, r: Reader) => void>([
    [K.HTML, (w, r) => {
        w.element.innerHTML = r.str;
    }],
    [K.WORD_WRAP, (w, r) => {
        w.element.style.whiteSpace = r.bool ? 'normal' : 'nowrap';
    }],
]);

/** PFunctionalLabel: its text is a function (FUNCTION_ID) applied to arguments (FUNCTION_ARGS) in the browser. */
export class FunctionalLabel extends Label {
    private fn: JsFunction | undefined;

    static override create(context: Context, id: number, type: number): FunctionalLabel {
        return new FunctionalLabel(context, id, type, document.createElement('div'), 'pony-FunctionalLabel');
    }

    override update(reader: Reader): boolean {
        if (reader.key === K.FUNCTION_ID) {
            this.fn = this.context.object(reader.int) as JsFunction;
            return true;
        }
        if (reader.key === K.FUNCTION_ARGS) {
            setText(this.element, String(this.fn!.fn(reader.array))); // after its function
            return true;
        }
        return super.update(reader);
    }
}
