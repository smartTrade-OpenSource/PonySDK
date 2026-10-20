/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, ServerToClientModel as K } from '#model';
import { about } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import type { Context } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';
import { FocusWidget } from './focus-widget.ts';

/** PFontSize, by ordinal: the size execCommand takes, 1 (xx-small) to 7 (xx-large). */
const FONT_SIZES = [5, 4, 3, 6, 2, 7, 1];
/** PJustification, by ordinal. */
const JUSTIFICATIONS = ['justifyCenter', 'justifyFull', 'justifyLeft', 'justifyRight'];

/** The server's commands that take no value. */
const COMMANDS = new Map<number, string>([
    [K.INSERT_HORIZONTAL_RULE, 'insertHorizontalRule'],
    [K.ORDERED, 'insertOrderedList'],
    [K.UNORDERED, 'insertUnorderedList'],
    [K.TOGGLE_BOLD, 'bold'],
    [K.TOGGLE_ITALIC, 'italic'],
    [K.TOGGLE_SUBSCRIPT, 'subscript'],
    [K.TOGGLE_UNDERLINE, 'underline'],
    [K.LEFT_INDENT, 'outdent'],
    [K.TOGGLE_RIGHT_INDENT, 'indent'],
    [K.REDO, 'redo'],
    [K.REMOVE_FORMAT, 'removeFormat'],
    [K.REMOVE_LINK, 'unlink'],
]);

/** The server's commands that take a string. */
const VALUE_COMMANDS = new Map<number, string>([
    [K.URL, 'createLink'],
    [K.INSERT_HTML, 'insertHTML'],
    [K.IMAGE_URL, 'insertImage'],
    [K.BACK_COLOR, 'hiliteColor'],
    [K.FONT_COLOR, 'foreColor'],
    [K.FONT_NAME, 'fontName'],
]);

let areaIds = 0;

/**
 * PRichTextArea: an editable element (role textbox), formatted by the browser's editing commands, as GWT's
 * RichTextArea in its frame. A command acts on the selection the area had last. Its HTML goes to the server when it
 * loses the focus (HANDLER_STRING_VALUE_CHANGE).
 */
export class RichTextArea extends FocusWidget {
    private selection: Range | null = null;

    static create(context: Context, id: number, type: number): RichTextArea {
        return new RichTextArea(context, id, type);
    }

    private constructor(context: Context, id: number, type: number) {
        const element = document.createElement('div');
        element.contentEditable = 'true';
        element.setAttribute('role', 'textbox');
        element.setAttribute('aria-multiline', 'true');
        element.id = `pony-richtext-${++areaIds}`;
        super(context, id, type, element, 'pony-RichTextArea');
        this.listen('selectionchange', () => {
            const selection = document.getSelection();
            if (selection !== null && selection.rangeCount > 0 && element.contains(selection.anchorNode)) {
                this.selection = selection.getRangeAt(0).cloneRange();
            }
        }, document);
        this.listen('blur', () => {
            const message = about(this.id);
            message[C.HANDLER_STRING_VALUE_CHANGE] = element.innerHTML;
            this.context.send(message);
        });
    }

    override setEnabled(enabled: boolean): void {
        this.element.contentEditable = String(enabled);
        if (enabled) this.element.removeAttribute('aria-disabled');
        else this.element.setAttribute('aria-disabled', 'true');
    }

    override update(reader: Reader): boolean {
        const command = COMMANDS.get(reader.key);
        if (command !== undefined) {
            this.exec(command);
            return true;
        }
        const valueCommand = VALUE_COMMANDS.get(reader.key);
        if (valueCommand !== undefined) {
            this.exec(valueCommand, reader.str ?? '');
            return true;
        }
        switch (reader.key) {
            case K.HTML:
                this.element.innerHTML = reader.str ?? '';
                this.selection = null;
                return true;
            case K.TEXT:
                this.element.textContent = reader.str ?? '';
                this.selection = null;
                return true;
            case K.FONT_SIZE:
                this.exec('fontSize', String(FONT_SIZES[reader.int] ?? 3));
                return true;
            case K.JUSTIFICATION:
                this.exec(JUSTIFICATIONS[reader.int] ?? 'justifyLeft');
                return true;
            case K.SELECT_ALL: {
                this.element.focus();
                const range = document.createRange();
                range.selectNodeContents(this.element);
                document.getSelection()?.removeAllRanges();
                document.getSelection()?.addRange(range);
                return true;
            }
            default:
                return super.update(reader);
        }
    }

    /** A command on the area's last selection, which gets the focus back, as GWT's formatters. */
    exec(command: string, value?: string): void {
        this.element.focus({ preventScroll: true });
        const selection = document.getSelection();
        if (selection !== null && this.selection !== null && this.element.contains(this.selection.startContainer)) {
            selection.removeAllRanges();
            selection.addRange(this.selection);
        }
        document.execCommand(command, false, value);
    }

    /** Whether the selection is bold, italic...: the state of a toggle of the toolbar. */
    state(command: string): boolean {
        return document.queryCommandState(command);
    }

    /** Whether the area holds the selection. */
    get selected(): boolean {
        const selection = document.getSelection();
        return selection !== null && this.element.contains(selection.anchorNode);
    }
}

/** The toolbar's buttons: command, label (GWT's), text; toggles show their state. */
const TOGGLES: [string, string, string][] = [
    ['bold', 'Toggle Bold', 'B'],
    ['italic', 'Toggle Italic', 'I'],
    ['underline', 'Toggle Underline', 'U'],
    ['subscript', 'Toggle Subscript', 'x₂'],
    ['superscript', 'Toggle Superscript', 'x²'],
    ['strikeThrough', 'Toggle Strikethrough', 'S'],
];
const PUSHES: [string, string, string][] = [
    ['justifyLeft', 'Left Justify', '⇤'],
    ['justifyCenter', 'Center', '↔'],
    ['justifyRight', 'Right Justify', '⇥'],
    ['indent', 'Indent Right', '→'],
    ['outdent', 'Indent Left', '←'],
    ['insertHorizontalRule', 'Insert Horizontal Rule', '―'],
    ['insertOrderedList', 'Insert Ordered List', '1.'],
    ['insertUnorderedList', 'Insert Unordered List', '•'],
    ['insertImage', 'Insert Image', 'Img'],
    ['createLink', 'Create Link', 'Link'],
    ['unlink', 'Remove Link', 'Unlink'],
    ['removeFormat', 'Remove Formatting', 'Tx'],
];
const COLORS: [string, string][] = [['White', 'white'], ['Black', 'black'], ['Red', 'red'], ['Green', 'green'],
    ['Yellow', 'yellow'], ['Blue', 'blue']];
const FONTS: [string, string][] = [['Normal', ''], ['Times New Roman', 'Times New Roman'], ['Arial', 'Arial'],
    ['Courier New', 'Courier New'], ['Georgia', 'Georgia'], ['Trebuchet', 'Trebuchet'], ['Verdana', 'Verdana']];
const SIZES: [string, string][] = [['XX-Small', '1'], ['X-Small', '2'], ['Small', '3'], ['Medium', '4'], ['Large', '5'],
    ['X-Large', '6'], ['XX-Large', '7']];

/**
 * PRichTextToolbar, on the WAI-ARIA toolbar pattern: the buttons and lists of GWT's RichTextToolbar, acting on its area
 * (WIDGET_ID) in the browser only; the area sends its HTML when it loses the focus. A press on a button keeps the
 * focus in the area. The toggles show the state of the selection (aria-pressed).
 */
export class RichTextToolbar extends Widget {
    private readonly area: RichTextArea;
    private readonly toggles = new Map<string, HTMLButtonElement>();

    static create(context: Context, id: number, type: number, reader: Reader): RichTextToolbar {
        reader.read(); // WIDGET_ID
        const area = context.object(reader.int);
        if (!(area instanceof RichTextArea)) throw new Error(`Rich text toolbar #${id}: no rich text area`);
        return new RichTextToolbar(context, id, type, area);
    }

    private constructor(context: Context, id: number, type: number, area: RichTextArea) {
        const element = document.createElement('div');
        element.setAttribute('role', 'toolbar');
        element.setAttribute('aria-label', 'Formatting');
        element.setAttribute('aria-controls', area.element.id);
        super(context, id, type, element, 'pony-RichTextToolbar');
        this.area = area;
        area.element.classList.add('pony-hasRichTextToolbar');

        const top = document.createElement('div');
        top.className = 'pony-RichTextToolbar-row';
        for (const [command, label, text] of TOGGLES) {
            const b = this.button(command, label, text);
            b.setAttribute('aria-pressed', 'false');
            this.toggles.set(command, b);
            top.append(b);
        }
        for (const [command, label, text] of PUSHES) top.append(this.button(command, label, text));
        const bottom = document.createElement('div');
        bottom.className = 'pony-RichTextToolbar-row';
        bottom.append(
            this.list('Background', COLORS, value => area.exec('hiliteColor', value)),
            this.list('Foreground', COLORS, value => area.exec('foreColor', value)),
            this.list('Font', FONTS, value => area.exec('fontName', value)),
            this.list('Size', SIZES, value => area.exec('fontSize', value)),
        );
        element.append(top, bottom);
        this.listen('selectionchange', () => {
            if (area.selected) this.refresh();
        }, document);
    }

    private button(command: string, label: string, text: string): HTMLButtonElement {
        const b = document.createElement('button');
        b.type = 'button';
        b.className = `pony-RichTextToolbar-${command}`;
        b.title = label;
        b.setAttribute('aria-label', label);
        b.textContent = text;
        this.listen<MouseEvent>('mousedown', e => e.preventDefault(), b); // the area keeps the focus and the selection
        this.listen('click', () => this.run(command), b);
        return b;
    }

    /** A list whose first option is its caption: an option chosen acts, then the list shows the caption again. */
    private list(caption: string, options: [string, string][], act: (value: string) => void): HTMLSelectElement {
        const select = document.createElement('select');
        select.setAttribute('aria-label', caption);
        select.add(new Option(caption, ''));
        for (const [text, value] of options) select.add(new Option(text, value));
        this.listen('change', () => {
            if (select.selectedIndex > 0) act(select.value);
            select.selectedIndex = 0;
        }, select);
        return select;
    }

    private run(command: string): void {
        if (command === 'insertImage' || command === 'createLink') {
            const url = window.prompt(command === 'insertImage' ? 'Enter an image URL:' : 'Enter a link URL:', 'http://');
            if (url !== null) this.area.exec(command, url);
            return;
        }
        this.area.exec(command);
        this.refresh();
    }

    private refresh(): void {
        for (const [command, b] of this.toggles) b.setAttribute('aria-pressed', String(this.area.state(command)));
    }
}
