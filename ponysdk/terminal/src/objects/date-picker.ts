/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, ServerToClientModel as K } from '#model';
import { about, FLAG } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import { DateFormat } from '../runtime/date-format.ts';
import { isRightToLeft, localeOf, type Names, namesOf, type Week, weekOf } from '../runtime/locale.ts';
import type { Context, TerminalObject } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';
import { POPUP_CLASS } from './popup.ts';

const WEEKS = 6;
const DAY_FORMAT = new DateFormat('yyyy-MM-dd');

function day(year: number, month: number, date: number): Date {
    const d = new Date(year, month, date);
    if (year >= 0 && year < 100) d.setFullYear(year, month, date);
    return d;
}

function addDays(date: Date, days: number): Date {
    return day(date.getFullYear(), date.getMonth(), date.getDate() + days);
}

function key(date: Date): string {
    return `${date.getFullYear()}/${date.getMonth()}/${date.getDate()}`;
}

/**
 * A server date: the day it is in UTC, at that time here. The server sends midnight UTC; the GWT terminal moves it by
 * the time zone offset (decodeAndTransformToLocal). -1 is no date.
 */
function fromServer(timestamp: number): Date | null {
    if (timestamp === -1) return null;
    const d = new Date(timestamp);
    const local = day(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate());
    local.setHours(d.getUTCHours(), d.getUTCMinutes(), d.getUTCSeconds(), d.getUTCMilliseconds());
    return local;
}

function button(label: string, text: string, className: string): HTMLButtonElement {
    const element = document.createElement('button');
    element.type = 'button';
    element.className = className;
    element.setAttribute('aria-label', label);
    element.textContent = text;
    return element;
}

let gridIds = 0;

/**
 * PDatePicker, on the WAI-ARIA date grid pattern: the month (previous and next buttons, and the year's when
 * YEAR_ARROWS_VISIBLE) over a grid of six weeks from Monday, as GWT's DatePicker. A month starting on a Monday shows
 * the week before it. In a page with a `lang` (docs/decisions/0022-locale.md), the names are that language's, the week
 * starts on its first day (where the browser tells it), and Left and Right follow the page's direction.
 *
 * A click on a day, or Enter or Space on it, makes it the value and sends HANDLER_DATE_VALUE_CHANGE (yyyy-MM-dd), when it
 * changed; a day of the month before or after shows that month. Each month shown sends HANDLER_SHOW_RANGE with the
 * first and last days of the grid, once the picker is in the page. Arrows, Home, End, Page Up and Page Down move
 * through the days, Shift with the pages through the years.
 *
 * The server's styles of dates (ADD_DATE_STYLE) stay with the dates; disabled dates (DATE_ENABLED) last until another
 * month shows. The value has aria-selected, today aria-current, a disabled day aria-disabled, a day of another month
 * data-filler and a weekend day data-weekend.
 */
export class DatePicker extends Widget {
    private value: Date | null = null;
    /** The first of the month shown. */
    private month: Date;
    private first: Date;
    private readonly styles = new Map<string, Set<string>>();
    private readonly disabled = new Set<string>();
    private readonly title: HTMLDivElement;
    private readonly yearButtons: HTMLButtonElement[];
    private readonly names: Names;
    private readonly week: Week;
    private readonly monthFormat: DateFormat;
    private readonly cells: HTMLTableCellElement[] = [];
    /** The day that has the tab stop. */
    private active: Date | null = null;
    /** In a date box, shown with its popup only. */
    private host: { isShowing: () => boolean; picked: (date: Date) => void } | null = null;

    static create(context: Context, id: number, type: number): DatePicker {
        return new DatePicker(context, id, type);
    }

    private constructor(context: Context, id: number, type: number) {
        const element = document.createElement('div');
        super(context, id, type, element, 'pony-DatePicker');
        // From the page's root, not the element: it is not in the page yet
        const locale = localeOf(context.session.body);
        this.names = namesOf(locale);
        this.week = weekOf(locale);
        this.monthFormat = new DateFormat('MMMM yyyy', locale);
        const today = new Date();
        this.month = day(today.getFullYear(), today.getMonth(), 1);
        this.first = this.month;

        const header = document.createElement('div');
        header.className = 'pony-DatePicker-header';
        const previousYear = button('Previous year', '«', 'pony-DatePicker-previousYear');
        const previous = button('Previous month', '‹', 'pony-DatePicker-previous');
        const next = button('Next month', '›', 'pony-DatePicker-next');
        const nextYear = button('Next year', '»', 'pony-DatePicker-nextYear');
        this.title = document.createElement('div');
        this.title.className = 'pony-DatePicker-month';
        this.title.id = `pony-month-${++gridIds}`;
        this.title.setAttribute('aria-live', 'polite');
        header.append(previousYear, previous, this.title, next, nextYear);
        this.yearButtons = [previousYear, nextYear];
        for (const b of this.yearButtons) b.hidden = true;

        const grid = document.createElement('table');
        grid.setAttribute('role', 'grid');
        grid.setAttribute('aria-labelledby', this.title.id);
        grid.className = 'pony-DatePicker-days';
        const head = grid.createTHead().insertRow();
        for (let i = 0; i < 7; i++) {
            const weekday = (i + this.week.first) % 7;
            const th = document.createElement('th');
            th.scope = 'col';
            th.abbr = this.names.weekdays[weekday]!;
            th.textContent = this.names.weekdaysNarrow[weekday]!;
            head.append(th);
        }
        const body = grid.createTBody();
        for (let w = 0; w < WEEKS; w++) {
            const row = body.insertRow();
            for (let d = 0; d < 7; d++) {
                const cell = row.insertCell();
                cell.setAttribute('role', 'gridcell');
                cell.className = 'pony-DatePicker-day';
                cell.dataset['index'] = String(this.cells.length);
                if (this.week.weekend.includes((d + this.week.first) % 7)) cell.dataset['weekend'] = '';
                this.cells.push(cell);
            }
        }
        element.append(header, grid);

        this.listen('click', () => this.shiftMonths(-1), previous);
        this.listen('click', () => this.shiftMonths(1), next);
        this.listen('click', () => this.shiftMonths(-12), previousYear);
        this.listen('click', () => this.shiftMonths(12), nextYear);
        this.listen<MouseEvent>('click', e => {
            const cell = (e.target as Element).closest<HTMLElement>('[role=gridcell]');
            if (cell !== null) this.pick(this.dateAt(Number(cell.dataset['index'])));
        }, body);
        this.listen<KeyboardEvent>('keydown', e => this.onGridKey(e), body);
        this.onAttachChange(attached => {
            if (attached && this.host === null) this.sendRange();
        });
        this.refresh();
    }

    /** Taken by a date box: shown in its popup, and told the box of the user's picks. */
    hostIn(isShowing: () => boolean, picked: (date: Date) => void): void {
        this.host = { isShowing, picked };
    }

    private get shown(): boolean {
        return this.host !== null ? this.host.isShowing() : this.attached;
    }

    override update(reader: Reader): boolean {
        switch (reader.key) {
            case K.DATE:
                this.setValue(fromServer(reader.number));
                return true;
            case K.TIME: {
                const month = fromServer(reader.number);
                if (month !== null) this.setCurrentMonth(month);
                return true;
            }
            case K.DATE_ENABLED: {
                const dates = reader.array;
                reader.read(); // ENABLED
                const enabled = reader.bool;
                for (const timestamp of dates) {
                    const date = fromServer(Number(timestamp));
                    if (date === null || !this.isVisible(date)) continue;
                    if (enabled) this.disabled.delete(key(date));
                    else this.disabled.add(key(date));
                }
                this.render();
                return true;
            }
            case K.ADD_DATE_STYLE:
            case K.REMOVE_DATE_STYLE: {
                const add = reader.key === K.ADD_DATE_STYLE;
                const dates = reader.array;
                reader.read(); // STYLE_NAME
                const names = (reader.str).split(/\s+/).filter(n => n.length > 0);
                for (const timestamp of dates) {
                    const date = fromServer(Number(timestamp));
                    if (date !== null) this.styleDate(key(date), names, add);
                }
                this.render();
                return true;
            }
            case K.YEAR_ARROWS_VISIBLE:
                for (const b of this.yearButtons) b.hidden = !reader.bool;
                return true;
            default:
                return super.update(reader);
        }
    }

    /** The value, without telling the server; it does not change the month shown. */
    setValue(date: Date | null): void {
        this.value = date;
        this.render();
    }

    /** Shows the month of the date: the server learns the days shown, when the picker is. */
    setCurrentMonth(date: Date): void {
        this.month = day(date.getFullYear(), date.getMonth(), 1);
        this.active = null;
        this.refresh();
        if (this.shown) this.sendRange();
    }

    /** The picker comes into sight: the server learns the days shown, as GWT's DatePicker.onLoad. */
    showing(): void {
        this.sendRange();
    }

    private shiftMonths(months: number): void {
        this.setCurrentMonth(day(this.month.getFullYear(), this.month.getMonth() + months, 1));
    }

    private refresh(): void {
        this.disabled.clear();
        let first = day(this.month.getFullYear(), this.month.getMonth(), 1);
        first = addDays(first, -this.daysSinceWeekStart(first));
        if (first.getDate() === 1) first = addDays(first, -7); // a month starting on the first day of the week shows the week before
        this.first = first;
        this.title.textContent = this.monthFormat.format(this.month);
        this.render();
    }

    /** 0 on the first day of the week. */
    private daysSinceWeekStart(date: Date): number {
        return (date.getDay() - this.week.first + 7) % 7;
    }

    private dateAt(index: number): Date {
        return addDays(this.first, index);
    }

    private isVisible(date: Date): boolean {
        const index = this.indexOf(date);
        return index >= 0 && index < this.cells.length;
    }

    private indexOf(date: Date): number {
        const from = Date.UTC(this.first.getFullYear(), this.first.getMonth(), this.first.getDate());
        return Math.round((Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) - from) / 86_400_000);
    }

    private inMonth(date: Date): boolean {
        return date.getMonth() === this.month.getMonth() && date.getFullYear() === this.month.getFullYear();
    }

    private styleDate(dateKey: string, names: string[], add: boolean): void {
        let set = this.styles.get(dateKey);
        if (add) {
            if (set === undefined) this.styles.set(dateKey, set = new Set());
            for (const name of names) set.add(name);
        } else if (set !== undefined) {
            for (const name of names) set.delete(name);
            if (set.size === 0) this.styles.delete(dateKey);
        }
    }

    /** The tab stop: the day last moved to, else the value, today or the first of the month, when shown. */
    private tabStop(): Date {
        const today = new Date();
        for (const candidate of [this.active, this.value, today]) {
            if (candidate !== null && this.inMonth(candidate)) return candidate;
        }
        return this.month;
    }

    private render(): void {
        const today = key(new Date());
        const value = this.value === null ? null : key(this.value);
        const stop = key(this.tabStop());
        this.cells.forEach((cell, index) => {
            const date = this.dateAt(index);
            const k = key(date);
            const filler = !this.inMonth(date);
            cell.textContent = String(date.getDate());
            cell.setAttribute('aria-label', `${this.names.weekdays[date.getDay()]} ${date.getDate()} ${this.names.months[date.getMonth()]} ${date.getFullYear()}`);
            cell.setAttribute('aria-selected', String(k === value));
            cell.tabIndex = k === stop ? 0 : -1;
            toggle(cell, 'filler', filler);
            if (k === today) cell.setAttribute('aria-current', 'date');
            else cell.removeAttribute('aria-current');
            if (this.disabled.has(k)) cell.setAttribute('aria-disabled', 'true');
            else cell.removeAttribute('aria-disabled');
            // the server's styles: on the days of the month, as GWT
            cell.className = 'pony-DatePicker-day';
            const styles = filler ? undefined : this.styles.get(k);
            if (styles !== undefined) cell.classList.add(...styles);
        });
    }

    /** By the user: the value, then the month of a day of another month. */
    private pick(date: Date): void {
        if (this.disabled.has(key(date))) return;
        const changed = this.value === null || this.value.getTime() !== date.getTime();
        this.active = date;
        this.setValue(date);
        if (changed) {
            const message = about(this.id);
            message[C.HANDLER_DATE_VALUE_CHANGE] = DAY_FORMAT.format(date);
            this.context.send(message);
            this.host?.picked(date);
        }
        if (!this.inMonth(date)) this.setCurrentMonth(date);
    }

    private onGridKey(event: KeyboardEvent): void {
        const cell = (event.target as Element).closest<HTMLElement>('[role=gridcell]')!; // the grid's focusable elements
        const current = this.dateAt(Number(cell.dataset['index']));
        let target: Date;
        switch (event.key) {
            case 'ArrowLeft':
                target = addDays(current, isRightToLeft(cell) ? 1 : -1); // the next day is on the left in a right-to-left page
                break;
            case 'ArrowRight':
                target = addDays(current, isRightToLeft(cell) ? -1 : 1);
                break;
            case 'ArrowUp':
                target = addDays(current, -7);
                break;
            case 'ArrowDown':
                target = addDays(current, 7);
                break;
            case 'Home':
                target = addDays(current, -this.daysSinceWeekStart(current));
                break;
            case 'End':
                target = addDays(current, 6 - this.daysSinceWeekStart(current));
                break;
            case 'PageUp':
            case 'PageDown': {
                const months = (event.key === 'PageUp' ? -1 : 1) * (event.shiftKey ? 12 : 1);
                const last = day(current.getFullYear(), current.getMonth() + months + 1, 0).getDate();
                target = day(current.getFullYear(), current.getMonth() + months, Math.min(current.getDate(), last));
                break;
            }
            case 'Enter':
            case ' ':
                event.preventDefault();
                this.pick(current);
                return;
            default:
                return;
        }
        event.preventDefault();
        this.moveTo(target);
    }

    private moveTo(date: Date): void {
        if (!this.inMonth(date)) this.setCurrentMonth(date);
        this.active = date;
        this.render();
        this.cells[this.indexOf(date)]?.focus();
    }

    /** Puts the focus on the tab stop. */
    focusDay(): void {
        this.cells[this.indexOf(this.tabStop())]?.focus();
    }

    private sendRange(): void {
        const message = about(this.id);
        message[C.HANDLER_SHOW_RANGE] = FLAG;
        message[C.START_DATE] = DAY_FORMAT.format(this.first);
        message[C.END_DATE] = DAY_FORMAT.format(this.dateAt(this.cells.length - 1));
        this.context.send(message);
    }
}

function toggle(element: HTMLElement, name: string, on: boolean): void {
    if (on) element.dataset[name] = '';
    else delete element.dataset[name];
}

let popupIds = 0;

/**
 * PDateBox: a text field (role combobox) with its date picker (PICKER) in a popup under it, as GWT's DateBox. The
 * picker shows when the field gets the focus or a click, or on Down, at the month of the field's date; Escape and Up
 * hide it, as a press outside it; Enter and Tab take the typed date. A date picked puts its text in the field, which
 * keeps the focus.
 *
 * The text goes to the server (HANDLER_STRING_VALUE_CHANGE) when the user changed it, or picked a date: after the
 * picker's own message. The pattern (DATE_FORMAT_PATTERN) is Java's SimpleDateFormat's. A text that is not a date is
 * marked aria-invalid. With KEEP_DAY_TIME_NEEDED, a date picked keeps the time of the field's date.
 */
export class DateBox extends Widget {
    private readonly picker: DatePicker;
    private readonly input: HTMLInputElement;
    private readonly popup: HTMLDivElement;
    private format: DateFormat;
    private readonly keepDayTime: boolean;
    /** The date of the server's value, or of the text the user typed: its time is kept. */
    private lastDate: Date | null = null;
    private showingPicker = false;
    private allowShow = true;
    private whileShown: AbortController | null = null;

    static create(context: Context, id: number, type: number, reader: Reader): DateBox {
        reader.read(); // PICKER
        const picker = context.object(reader.int);
        reader.read(); // DATE_FORMAT_PATTERN
        const format = new DateFormat(reader.str, localeOf(context.session.body));
        let value: string | null = null;
        if (reader.read() === K.VALUE) value = reader.str;
        else reader.rewind();
        let keepDayTime = false;
        if (reader.read() === K.KEEP_DAY_TIME_NEEDED) keepDayTime = true;
        else reader.rewind();
        if (!(picker instanceof DatePicker)) throw new Error(`Date box #${id}: no date picker`);
        const box = new DateBox(context, id, type, picker, format, keepDayTime);
        if (value) box.setDate(format.parse(value));
        return box;
    }

    private constructor(context: Context, id: number, type: number, picker: DatePicker, format: DateFormat, keepDayTime: boolean) {
        const input = document.createElement('input');
        input.type = 'text';
        super(context, id, type, input, 'pony-DateBox');
        this.input = input;
        this.picker = picker;
        this.format = format;
        this.keepDayTime = keepDayTime;
        if (context.session.tabindexOnlyFormField) input.tabIndex = -1;

        this.popup = document.createElement('div');
        this.popup.setAttribute('popover', 'manual');
        this.popup.setAttribute('role', 'dialog');
        this.popup.setAttribute('aria-label', 'Choose a date');
        this.popup.id = `pony-datebox-${++popupIds}`;
        this.popup.className = `${POPUP_CLASS} pony-DateBoxPopup`;
        this.popup.append(picker.element);
        input.setAttribute('role', 'combobox');
        input.setAttribute('aria-haspopup', 'dialog');
        input.setAttribute('aria-expanded', 'false');
        input.setAttribute('aria-controls', this.popup.id);
        picker.hostIn(() => this.showingPicker, date => this.onPicked(date));

        this.listen('focus', () => {
            if (this.allowShow && !this.showingPicker) this.show();
        });
        this.listen('click', () => this.show());
        this.listen('blur', () => {
            if (!this.showingPicker) this.updateFromText();
        });
        this.listen<KeyboardEvent>('keydown', e => this.onKeyDown(e));
        this.listen<KeyboardEvent>('keydown', e => {
            if (e.key !== 'Escape') return;
            this.hide();
            this.focusWithoutPicker();
        }, this.popup);
        this.listen('change', () => {
            if (this.keepDayTime) this.lastDate = this.parse(true);
            this.sendText();
        });
        this.onAttachChange(attached => {
            if (!attached) this.hide();
        });
    }

    /** The date picker, which the server adds to the box: it is in the popup already. */
    override add(_reader: Reader, _child: TerminalObject): void {
    }

    override update(reader: Reader): boolean {
        switch (reader.key) {
            case K.VALUE:
                this.setDate(this.format.parse(reader.str));
                return true;
            case K.DATE_FORMAT_PATTERN: {
                const date = this.parse(true);
                this.format = new DateFormat(reader.str, localeOf(this.context.session.body));
                this.setDate(date);
                return true;
            }
            case K.ENABLED:
                this.input.disabled = !reader.bool;
                return true;
            default:
                return super.update(reader);
        }
    }

    /** The server's date, or the box's in a new format: in the field and the picker. */
    private setDate(date: Date | null): void {
        this.setValue(date, true);
        if (this.keepDayTime) this.lastDate = date;
    }

    private setValue(date: Date | null, updateText: boolean): void {
        if (date !== null) this.picker.setCurrentMonth(date);
        this.picker.setValue(date);
        if (updateText) {
            this.input.removeAttribute('aria-invalid');
            this.input.value = date === null ? '' : this.format.format(date);
        }
    }

    /** The field's date; a text that is not one is marked when reportError. */
    private parse(reportError: boolean): Date | null {
        if (reportError) this.input.removeAttribute('aria-invalid');
        const text = this.input.value.trim();
        if (text.length === 0) return null;
        let date = this.format.parse(text);
        if (date === null) {
            // as GWT's DateBox.DefaultFormat: the browser's reading of the text, in the pattern
            const loose = new Date(text);
            if (!Number.isNaN(loose.getTime())) date = this.format.parse(this.format.format(loose));
        }
        if (date === null && reportError) this.input.setAttribute('aria-invalid', 'true');
        return date;
    }

    private updateFromText(): void {
        const date = this.parse(true);
        if (date !== null) this.setValue(date, false);
    }

    /** Shows the picker at the month of the field's date; shown already, it shows that month again, as GWT. */
    private show(): void {
        this.picker.setCurrentMonth(this.parse(false) ?? new Date());
        const r = this.input.getBoundingClientRect();
        this.popup.style.left = `${r.left + window.scrollX}px`;
        this.popup.style.top = `${r.bottom + window.scrollY}px`;
        if (this.showingPicker) {
            this.alignToEnd(r);
            return;
        }
        if (!this.popup.isConnected) this.context.session.body.append(this.popup);
        this.popup.showPopover();
        this.alignToEnd(r);
        const below = this.popup.getBoundingClientRect();
        if (below.bottom > document.documentElement.clientHeight && r.top >= below.height) {
            this.popup.style.top = `${r.top + window.scrollY - below.height}px`; // above, as GWT's showRelativeTo
        }
        this.showingPicker = true;
        this.input.setAttribute('aria-expanded', 'true');
        this.whileShown = new AbortController();
        document.addEventListener('pointerdown', e => this.context.guard(() => {
            const target = e.composedPath()[0] as Node; // the target in a shadow root, not its host
            if (!this.popup.contains(target)) this.hide();
        }), { capture: true, signal: this.whileShown.signal });
        this.picker.showing();
    }

    /** Right to left: under the field's right edge, where its text starts, and not out of the window on the left. */
    private alignToEnd(field: DOMRect): void {
        if (!isRightToLeft(this.input)) return;
        this.popup.style.left = `${Math.max(window.scrollX, field.right + window.scrollX - this.popup.getBoundingClientRect().width)}px`;
    }

    /** Hides the picker; the field's date goes to the picker, as when GWT's popup closes. */
    private hide(): void {
        if (!this.showingPicker) return;
        this.showingPicker = false;
        this.whileShown?.abort();
        this.whileShown = null;
        if (this.popup.matches(':popover-open')) this.popup.hidePopover();
        this.input.setAttribute('aria-expanded', 'false');
        if (this.allowShow) this.updateFromText();
    }

    private onKeyDown(event: KeyboardEvent): void {
        switch (event.key) {
            case 'Enter':
            case 'Tab':
                this.updateFromText();
                this.hide();
                break;
            case 'Escape':
            case 'ArrowUp':
                this.hide();
                break;
            case 'ArrowDown':
                event.preventDefault();
                if (this.showingPicker) this.picker.focusDay();
                else this.show();
                break;
        }
    }

    /** A date picked: in the field, which keeps the focus without showing the picker again. */
    private onPicked(date: Date): void {
        const normalized = this.format.parse(this.format.format(date));
        this.setValue(normalized, true);
        this.hide();
        this.focusWithoutPicker();
        if (this.keepDayTime && this.lastDate !== null && normalized !== null) {
            const time = new Date(normalized);
            time.setHours(this.lastDate.getHours(), this.lastDate.getMinutes(), this.lastDate.getSeconds(), this.lastDate.getMilliseconds());
            this.input.value = this.format.format(time);
        }
        this.sendText();
    }

    private focusWithoutPicker(): void {
        this.allowShow = false;
        this.input.focus();
        setTimeout(() => {
            this.allowShow = true;
        });
    }

    private sendText(): void {
        const message = about(this.id);
        message[C.HANDLER_STRING_VALUE_CHANGE] = this.input.value;
        this.context.send(message);
    }

    override destroy(): void {
        this.whileShown?.abort();
        this.popup.remove();
        super.destroy();
    }
}
