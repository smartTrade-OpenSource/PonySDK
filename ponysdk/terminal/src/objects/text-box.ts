/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, DomHandlerType as D, HandlerModel as H, ServerToClientModel as K } from '#model';
import { about } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import { apply, type Context, type Setters } from '../runtime/object.ts';
import { FocusWidget } from './focus-widget.ts';
import { Mask } from './mask.ts';

type Field = HTMLInputElement | HTMLTextAreaElement;

/**
 * A text field: its value goes to the server on change, and, when the server
 * listens to key up, before each key up whose key changed it.
 */
export class TextBoxBase extends FocusWidget {
    /** The value the server knows. */
    private lastValue: string | null = null;
    private pasteHandled = false;
    private sendsPaste = false;
    private filter: RegExp | undefined;
    private mask: Mask | undefined;

    constructor(context: Context, id: number, type: number, element: Field, typeClass: string) {
        super(context, id, type, element, typeClass);
        this.listen('change', () => {
            this.lastValue = this.field.value;
            this.sendValue();
        });
    }

    protected get field(): Field {
        return this.element as Field;
    }

    override update(reader: Reader): boolean {
        return apply(TEXT_BOX, this, reader) || super.update(reader);
    }

    text(value: string | null): void {
        this.lastValue = value;
        if (this.mask !== undefined) this.mask.load(value ?? '');
        else this.field.value = value ?? '';
    }

    /** MASK: the field takes the mask's characters only, and sends its value at each change. */
    setMask(mask: string, showMask: boolean, free: string): void {
        this.mask ??= new Mask(this.field as HTMLInputElement, text => {
            this.lastValue = text;
            this.sendValue();
        }, this.lifetime.signal);
        this.mask.set(mask, showMask, free);
    }

    private sendValue(): void {
        const message = about(this.id);
        message[C.HANDLER_STRING_VALUE_CHANGE] = this.lastValue;
        this.context.send(message);
    }

    /** Before a key up, the value if the key changed it: the server reads the value with the key. */
    protected override onKey(type: number, event: KeyboardEvent, filter: number[] | null): void {
        if (type !== D.KEY_UP) {
            super.onKey(type, event, filter);
            return;
        }
        if (!this.enabled) return;
        const keyCode = event.keyCode | 0;
        if (filter === null || filter.includes(keyCode)) {
            if (this.field.value !== this.lastValue) {
                this.lastValue = this.field.value;
                this.sendValue();
            }
            const message = this.domEvent(type);
            message[C.VALUE_KEY] = keyCode;
            this.context.send(message);
        }
        this.preventOrStop(event);
    }

    override addHandler(reader: Reader, handler: number): void {
        if (handler === H.HANDLER_PASTE) {
            this.sendsPaste = true;
            if (this.pasteHandled) return;
            this.pasteHandled = true;
            this.listen('paste', () => {
                if (!this.enabled || !this.sendsPaste) return;
                setTimeout(() => this.context.guard(() => {
                    const message = about(this.id);
                    message[C.HANDLER_PASTE] = this.field.value;
                    this.context.send(message);
                })); // once the pasted text is in the field
            });
        } else {
            super.addHandler(reader, handler);
        }
    }

    override removeHandler(reader: Reader, handler: number): void {
        if (handler === H.HANDLER_PASTE) this.sendsPaste = false;
        else super.removeHandler(reader, handler);
    }

    /** REGEX_FILTER: characters that do not match are refused, typed, pasted or dropped. */
    setFilter(pattern: string): void {
        const first = this.filter === undefined;
        this.filter = new RegExp(pattern);
        if (!first) return;
        this.listen<InputEvent>('beforeinput', e => {
            if (e.inputType === 'insertText' && e.data !== null && !this.matches(e.data)) e.preventDefault();
        });
        this.listen<InputEvent>('input', e => {
            if (e.inputType === 'insertFromPaste' || e.inputType === 'insertFromDrop') {
                this.field.value = [...this.field.value].filter(c => this.matches(c)).join('');
            }
        });
    }

    private matches(text: string): boolean {
        return [...text].every(c => this.filter === undefined || this.filter.test(c));
    }

    select(start: number, length: number): void {
        this.field.setSelectionRange(start, start + length);
    }
}

const TEXT_BOX: Setters<TextBoxBase> = new Map<number, (w: TextBoxBase, r: Reader) => void>([
    [K.TEXT, (w, r) => w.text(r.str)],
    [K.PLACEHOLDER, (w, r) => w.element.setAttribute('placeholder', r.str ?? '')],
    [K.SELECT_ALL, w => (w.element as Field).select()],
    [K.CURSOR_POSITION, (w, r) => w.select(r.int, 0)],
    [K.SELECTION_RANGE_START, (w, r) => {
        const start = r.int;
        r.read(); // SELECTION_RANGE_LENGTH
        w.select(start, r.int);
    }],
    [K.REGEX_FILTER, (w, r) => w.setFilter(r.str ?? '')],
    [K.MASK, (w, r) => {
        const mask = r.str ?? '';
        r.read(); // VISIBILITY
        const showMask = r.bool;
        r.read(); // REPLACEMENT_STRING
        w.setMask(mask, showMask, r.str ?? ' ');
    }],
    [K.VISIBLE_LENGTH, (w, r) => {
        (w.element as HTMLInputElement).size = r.int;
    }],
    [K.MAX_LENGTH, (w, r) => {
        (w.element as Field).maxLength = r.int;
    }],
    [K.VISIBLE_LINES, (w, r) => {
        (w.element as HTMLTextAreaElement).rows = r.int;
    }],
    [K.CHARACTER_WIDTH, (w, r) => {
        (w.element as HTMLTextAreaElement).cols = r.int;
    }],
]);

function input(type: string): HTMLInputElement {
    const element = document.createElement('input');
    element.type = type;
    return element;
}

export const createTextBox = (context: Context, id: number, type: number): TextBoxBase =>
    new TextBoxBase(context, id, type, input('text'), 'pony-TextBox');

export const createPasswordTextBox = (context: Context, id: number, type: number): TextBoxBase =>
    new TextBoxBase(context, id, type, input('password'), 'pony-PasswordTextBox');

export const createTextArea = (context: Context, id: number, type: number): TextBoxBase =>
    new TextBoxBase(context, id, type, document.createElement('textarea'), 'pony-TextArea');
