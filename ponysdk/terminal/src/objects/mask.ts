/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

/** A slot of the mask: a digit (0), a letter (A), or a character of the mask itself, always set. */
interface Slot {
    readonly kind: 'digit' | 'letter' | 'constant';
    char: string | null;
}

const DIGIT = /^\p{Nd}$/u;
const LETTER = /^\p{L}$/u;
const GROUP = /\{\{([A0]+)\}\}/g;

function accepts(slot: Slot, char: string): boolean {
    if (slot.kind === 'digit') return DIGIT.test(char);
    if (slot.kind === 'letter') return LETTER.test(char);
    return char === slot.char;
}

/**
 * The value of a masked text box, as the GWT terminal kept it: a mask such as
 * ({{000}}) {{000}}.{{0000}}, whose groups take digits (0) or letters (A). Characters go in at a position, pushing the
 * next ones; removing them pulls the next ones back.
 */
export class MaskValue {
    private readonly slots: Slot[] = [];
    private readonly showMask: boolean;
    private readonly free: string;

    /** @param free what an empty slot shows; @param showMask whether the mask's characters show past the input */
    constructor(mask: string, showMask: boolean, free: string) {
        this.showMask = showMask;
        this.free = free.charAt(0) || ' ';
        let current = 0;
        for (const match of mask.matchAll(GROUP)) {
            for (; current < match.index; current++) this.slots.push({ kind: 'constant', char: mask[current]! });
            for (const c of match[1]!) this.slots.push({ kind: c === '0' ? 'digit' : 'letter', char: null });
            current = match.index + match[0].length;
        }
        for (; current < mask.length; current++) this.slots.push({ kind: 'constant', char: mask[current]! });
    }

    get length(): number {
        return this.slots.length;
    }

    /** The text: the mask's characters show up to the first empty slot, and after it when showMask. */
    get text(): string {
        let filling = true;
        let text = '';
        for (const slot of this.slots) {
            if (slot.kind === 'constant') text += filling || this.showMask ? slot.char : this.free;
            else text += slot.char ?? this.free;
            if (slot.char === null) filling = false;
        }
        return text;
    }

    /** The position of the first empty slot: where typing goes on. */
    get insertPosition(): number {
        const index = this.slots.findIndex(slot => slot.char === null);
        return index === -1 ? this.slots.length : index;
    }

    /** Whether the character goes in at the position: in the next slot that is not one of the mask's characters. */
    fits(position: number, char: string): boolean {
        for (let next = position; next < this.slots.length; next++) {
            const slot = this.slots[next]!;
            if (slot.kind !== 'constant') return accepts(slot, char);
            if (accepts(slot, char)) return false;
        }
        return false;
    }

    /** Whether the slot at the position is the mask's own character. */
    isConstant(position: number, char: string): boolean {
        const slot = this.slots[position];
        return slot !== undefined && slot.kind === 'constant' && slot.char === char;
    }

    /**
     * Puts a character at a position, or at the next slot that takes it after the mask's characters; a slot taken
     * pushes its character to the next. The position after it, or -1 when the character does not go in.
     */
    insert(position: number, char: string): number {
        let next = position;
        for (; next < this.slots.length; next++) {
            const slot = this.slots[next]!;
            if (slot.kind !== 'constant') break;
            if (accepts(slot, char)) return -1; // typed over the mask's own character
        }
        const slot = this.slots[next];
        if (slot === undefined || !accepts(slot, char)) return -1;
        if (slot.char !== null) this.insert(position + 1, slot.char);
        slot.char = char;
        return next + 1;
    }

    /** Empties the slots from position to end, then pulls the next characters back. False before the start. */
    remove(from: number, to: number): boolean {
        if (from < 0) return false;
        for (let i = from; i < to && i < this.slots.length; i++) {
            const slot = this.slots[i]!;
            if (slot.kind !== 'constant') slot.char = null;
        }
        this.shift(from);
        return true;
    }

    private shift(position: number): void {
        if (position >= this.slots.length) return;
        let next = position;
        let empty: Slot | undefined;
        for (; next < this.slots.length; next++) {
            empty = this.slots[next]!;
            if (empty.kind !== 'constant' && empty.char === null) break;
        }
        if (empty === undefined) return;
        next++;
        for (let i = next; i < this.slots.length; i++) {
            const slot = this.slots[i]!;
            if (slot.kind === 'constant' || slot.char === null) continue;
            if (accepts(empty, slot.char)) {
                empty.char = slot.char;
                slot.char = null;
            }
            break;
        }
        this.shift(next);
    }
}

/**
 * A text box's mask (MASK): the field shows the value's text and takes no other; each change goes to the given
 * function, as the GWT terminal fires a value change at each key. Typing overwrites at the cursor, Backspace and
 * Delete pull the next characters back; a pasted or dropped text goes in character by character.
 */
export class Mask {
    private value: MaskValue;
    private config: [string, boolean, string] = ['', true, ' '];
    private readonly field: HTMLInputElement;
    private readonly changed: (text: string) => void;
    /** Focused: the cursor goes to the first empty slot. */
    private cursorPending = false;

    constructor(field: HTMLInputElement, changed: (text: string) => void, signal: AbortSignal) {
        this.field = field;
        this.changed = changed;
        this.value = new MaskValue('', true, ' ');
        field.addEventListener('beforeinput', e => this.onInput(e), { signal });
        // On focus the cursor goes where typing goes on: after the click that gave the focus, which places it too,
        // else at once; before a key typed meanwhile in any case
        let pressed = false;
        field.addEventListener('pointerdown', () => {
            pressed = document.activeElement !== field;
        }, { signal });
        field.addEventListener('focus', () => {
            this.cursorPending = true;
            if (!pressed) this.placeCursor();
        }, { signal });
        field.addEventListener('click', () => {
            pressed = false;
            this.placeCursor();
        }, { signal });
    }

    private placeCursor(): void {
        if (!this.cursorPending || document.activeElement !== this.field) return;
        this.cursorPending = false;
        this.field.value = this.value.text;
        this.field.setSelectionRange(this.value.insertPosition, this.value.insertPosition);
    }

    /** A new mask, which takes the field's current text. */
    set(mask: string, showMask: boolean, free: string): void {
        this.config = [mask, showMask, free];
        this.load(this.field.value);
        // the mask keeps the length; at its maxlength, WebKit sends no beforeinput
        this.field.removeAttribute('maxlength');
    }

    /** A text from the server, through the mask. */
    load(text: string): void {
        this.value = new MaskValue(...this.config);
        for (let i = 0; i < text.length; i++) if (this.value.insert(i, text[i]!) === -1) break;
        this.field.value = this.value.text;
    }

    private onInput(event: InputEvent): void {
        event.preventDefault();
        this.placeCursor();
        const before = this.value.text;
        const start = this.field.selectionStart ?? 0;
        const end = this.field.selectionEnd ?? start;
        const type = event.inputType;
        if (type.startsWith('delete')) {
            if (end > start) this.cut(start, end);
            else if (type.endsWith('Backward')) this.removeAt(start - 1, Math.max(start - 1, 0));
            else this.removeAt(start, start);
        } else if (type === 'insertText' || type === 'insertReplacementText' || type === 'insertFromPaste' || type === 'insertFromDrop') {
            const text = event.data ?? event.dataTransfer?.getData('text/plain') ?? '';
            let position = start;
            if (end > start) this.cut(start, end);
            for (const char of text) {
                if (this.value.isConstant(position, char)) {
                    position++; // the mask's own character: typed over
                    continue;
                }
                if (!this.value.fits(position, char)) continue; // refused
                this.value.remove(position, position + 1); // typing overwrites, as GWT
                position = this.value.insert(position, char);
            }
            this.show(position);
        }
        if (this.value.text !== before) this.changed(this.value.text);
    }

    private cut(start: number, end: number): void {
        this.value.remove(start, end);
        this.show(start);
    }

    private removeAt(position: number, cursor: number): void {
        if (position < this.value.length && this.value.remove(position, position + 1)) this.show(cursor);
        else this.field.setSelectionRange(cursor, cursor);
    }

    private show(cursor: number): void {
        this.field.value = this.value.text;
        this.field.setSelectionRange(cursor, cursor);
    }
}
