/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, PCheckBoxState as S, ServerToClientModel as K } from '#model';
import { about } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import { apply, type Context, type Setters } from '../runtime/object.ts';
import { FocusWidget } from './focus-widget.ts';

/**
 * PCheckBox: a label around its input and its text. A change sends the new value. The input is what takes the focus,
 * the tab index, the disabled state and INPUT attributes.
 */
export class CheckBox extends FocusWidget {
    protected readonly input: HTMLInputElement;
    private readonly caption: HTMLSpanElement;

    static create(context: Context, id: number, type: number): CheckBox {
        return new CheckBox(context, id, type, 'checkbox', 'pony-CheckBox');
    }

    protected constructor(context: Context, id: number, type: number, inputType: string, typeClass: string) {
        const input = document.createElement('input');
        input.type = inputType;
        const caption = document.createElement('span');
        const label = document.createElement('label');
        label.append(input, caption);
        super(context, id, type, label, typeClass);
        this.input = input;
        this.caption = caption;
        this.listen('change', () => this.sendValue(), input);
    }

    override get focusTarget(): HTMLElement {
        return this.input;
    }

    override update(reader: Reader): boolean {
        return apply(CheckBox.CHECK_BOX, this, reader) || super.update(reader);
    }

    protected sendValue(): void {
        const message = about(this.id);
        message[C.HANDLER_BOOLEAN_VALUE_CHANGE] = this.input.checked;
        this.context.send(message);
    }

    state(state: number): void {
        const indeterminate = state === S.INDETERMINATE;
        if (!indeterminate) this.input.checked = state === S.CHECKED;
        this.input.indeterminate = indeterminate;
    }

    private static readonly CHECK_BOX: Setters<CheckBox> = new Map<number, (w: CheckBox, r: Reader) => void>([
        [K.VALUE_CHECKBOX, (w, r) => w.state(r.int)],
        [K.TEXT, (w, r) => {
            w.caption.textContent = r.str;
        }],
        [K.HTML, (w, r) => {
            w.caption.innerHTML = r.str ?? '';
        }],
        [K.PUT_INPUT_ATTRIBUTE_KEY, (w, r) => {
            const name = r.str ?? '';
            r.read(); // INPUT_ATTRIBUTE_VALUE
            w.input.setAttribute(name, r.str ?? '');
        }],
        [K.REMOVE_ATTRIBUTE_KEY, (w, r) => w.input.removeAttribute(r.str ?? '')],
    ]);
}


/** PRadioButton: a check box in a group (NAME). A value set by the server that changes the button is sent back. */
export class RadioButton extends CheckBox {
    static override create(context: Context, id: number, type: number): RadioButton {
        return new RadioButton(context, id, type, 'radio', 'pony-RadioButton');
    }

    override update(reader: Reader): boolean {
        if (reader.key === K.NAME) {
            this.input.name = reader.str ?? '';
            return true;
        }
        if (reader.key === K.VALUE_CHECKBOX) {
            const checked = reader.int === S.CHECKED;
            if (this.input.checked !== checked) {
                this.input.checked = checked;
                this.sendValue();
            }
            return true;
        }
        return super.update(reader);
    }
}
