/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { PHorizontalAlignment as HA, PVerticalAlignment as VA, ServerToClientModel as K } from '#model';
import type { Reader } from '../protocol/reader.ts';
import { apply, type Context, type Setters, type TerminalObject } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';

const TEXT_ALIGN: Readonly<Record<number, string>> = { [HA.ALIGN_LEFT]: 'left', [HA.ALIGN_CENTER]: 'center', [HA.ALIGN_RIGHT]: 'right' };
const VERTICAL_ALIGN: Readonly<Record<number, string>> = { [VA.ALIGN_TOP]: 'top', [VA.ALIGN_MIDDLE]: 'middle', [VA.ALIGN_BOTTOM]: 'bottom' };

/**
 * PFlexTable and PGrid: a table whose widgets go to a cell (ROW, COLUMN), which then
 * carries the class ptc. Rows, cells and columns take style names, alignment, spans and sizes. A cell a property
 * names is created with the rows and cells before it, when missing.
 */
export class HtmlTable extends Widget {
    protected readonly body: HTMLTableSectionElement;
    private columns: HTMLTableColElement[] = [];
    private padding: string | null = null;
    private cellBorder: string | null = null;

    constructor(context: Context, id: number, type: number, typeClass: string) {
        const table = document.createElement('table');
        const body = table.createTBody();
        super(context, id, type, table, typeClass);
        this.body = body;
    }

    get table(): HTMLTableElement {
        return this.element as HTMLTableElement;
    }

    override update(reader: Reader): boolean {
        return apply(HTML_TABLE, this, reader) || super.update(reader);
    }

    /** The widget replaces what the cell held. */
    override add(reader: Reader, child: TerminalObject): void {
        reader.read(); // ROW
        const row = reader.int;
        reader.read(); // COLUMN
        const cell = this.cell(row, reader.int);
        if (!(child instanceof Widget)) return;
        for (const widget of this.widgetsIn(cell)) if (widget !== child) this.removeChild(widget);
        cell.replaceChildren();
        cell.classList.add('ptc');
        this.appendInside(cell, child);
    }

    cell(row: number, column: number): HTMLTableCellElement {
        const tr = this.row(row);
        while (tr.cells.length <= column) this.decorate(tr.insertCell());
        return tr.cells[column]!;
    }

    row(row: number): HTMLTableRowElement {
        while (this.body.rows.length <= row) this.newRow(this.body.rows.length);
        return this.body.rows[row]!;
    }

    /** A new row at index; a grid's rows have their cells. */
    protected newRow(index: number): HTMLTableRowElement {
        return this.body.insertRow(index);
    }

    /** A new row at index; the rows before it are created when missing. */
    insertRow(index: number): void {
        while (this.body.rows.length < index) this.newRow(this.body.rows.length);
        this.newRow(index);
    }

    removeRow(index: number): void {
        const tr = this.body.rows[index];
        if (tr === undefined) return;
        for (const widget of this.widgetsIn(tr)) this.removeChild(widget);
        tr.remove();
    }

    /** The widgets, not the cells: those stay. */
    clear(): void {
        for (const widget of [...this.children]) this.removeChild(widget);
    }

    private widgetsIn(container: Element): Widget[] {
        return this.children.filter(widget => container.contains(widget.element));
    }

    column(index: number): HTMLTableColElement {
        if (this.columns.length <= index) {
            let group = this.table.querySelector(':scope > colgroup');
            if (group === null) group = this.table.insertBefore(document.createElement('colgroup'), this.body);
            while (this.columns.length <= index) this.columns.push(group.appendChild(document.createElement('col')));
        }
        return this.columns[index]!;
    }

    setPadding(pixels: number): void {
        this.padding = `${pixels}px`;
        for (const cell of this.table.querySelectorAll<HTMLTableCellElement>(':scope > tbody > tr > td')) this.decorate(cell);
    }

    setBorder(pixels: number): void {
        this.table.style.border = pixels > 0 ? `${pixels}px solid` : '';
        this.cellBorder = pixels > 0 ? '1px solid' : '';
        for (const cell of this.table.querySelectorAll<HTMLTableCellElement>(':scope > tbody > tr > td')) this.decorate(cell);
    }

    private decorate(cell: HTMLTableCellElement): void {
        if (this.padding !== null) cell.style.padding = this.padding;
        if (this.cellBorder !== null) cell.style.border = this.cellBorder;
    }
}

function names(value: string | null): string[] {
    return value === null ? [] : value.split(/\s+/).filter(name => name.length > 0);
}

/** The next models: ROW, then COLUMN. */
function rowColumn(reader: Reader): [number, number] {
    reader.read();
    const row = reader.int;
    reader.read();
    return [row, reader.int];
}

function nextInt(reader: Reader): number {
    reader.read();
    return reader.int;
}

const HTML_TABLE: Setters<HtmlTable> = new Map<number, (w: HtmlTable, r: Reader) => void>([
    [K.CLEAR, w => w.clear()],
    [K.CLEAR_ROW, (w, r) => w.removeRow(r.int)],
    [K.INSERT_ROW, (w, r) => w.insertRow(r.int)],
    [K.BORDER_WIDTH, (w, r) => w.setBorder(r.int)],
    [K.CELL_SPACING, (w, r) => {
        w.table.style.borderSpacing = `${r.int}px`;
    }],
    [K.CELL_PADDING, (w, r) => w.setPadding(r.int)],
    [K.ROW_FORMATTER_ADD_STYLE_NAME, (w, r) => {
        const style = names(r.str);
        w.row(nextInt(r)).classList.add(...style);
    }],
    [K.ROW_FORMATTER_SET_STYLE_NAME, (w, r) => {
        const style = r.str ?? '';
        w.row(nextInt(r)).className = style;
    }],
    [K.ROW_FORMATTER_REMOVE_STYLE_NAME, (w, r) => {
        const style = names(r.str);
        w.row(nextInt(r)).classList.remove(...style);
    }],
    [K.ROW_FORMATTER_SHOW_ROW, (w, r) => {
        w.row(r.int).style.display = '';
    }],
    [K.ROW_FORMATTER_HIDE_ROW, (w, r) => {
        w.row(r.int).style.display = 'none';
    }],
    [K.CELL_FORMATTER_ADD_STYLE_NAME, (w, r) => {
        const style = names(r.str);
        w.cell(...rowColumn(r)).classList.add(...style);
    }],
    [K.CELL_FORMATTER_REMOVE_STYLE_NAME, (w, r) => {
        const style = names(r.str);
        w.cell(...rowColumn(r)).classList.remove(...style);
    }],
    [K.CELL_FORMATTER_SET_STYLE_NAME, (w, r) => {
        const style = r.str ?? '';
        w.cell(...rowColumn(r)).className = style;
    }],
    [K.VERTICAL_ALIGNMENT, (w, r) => {
        const align = VERTICAL_ALIGN[r.int] ?? '';
        w.cell(...rowColumn(r)).style.verticalAlign = align;
    }],
    [K.HORIZONTAL_ALIGNMENT, (w, r) => {
        const align = TEXT_ALIGN[r.int] ?? '';
        w.cell(...rowColumn(r)).style.textAlign = align;
    }],
    [K.SET_COL_SPAN, (w, r) => {
        const span = r.int;
        w.cell(...rowColumn(r)).colSpan = span;
    }],
    [K.SET_ROW_SPAN, (w, r) => {
        const span = r.int;
        w.cell(...rowColumn(r)).rowSpan = span;
    }],
    [K.COLUMN_FORMATTER_ADD_STYLE_NAME, (w, r) => {
        const style = names(r.str);
        w.column(nextInt(r)).classList.add(...style);
    }],
    [K.COLUMN_FORMATTER_REMOVE_STYLE_NAME, (w, r) => {
        const style = names(r.str);
        w.column(nextInt(r)).classList.remove(...style);
    }],
    [K.COLUMN_FORMATTER_SET_STYLE_NAME, (w, r) => {
        const style = r.str ?? '';
        w.column(nextInt(r)).className = style;
    }],
    [K.COLUMN_FORMATTER_COLUMN_WIDTH, (w, r) => {
        const width = r.str ?? '';
        w.column(nextInt(r)).style.width = width;
    }],
]);

/** PFlexTable: rows of any length. */
export const createFlexTable = (context: Context, id: number, type: number): HtmlTable =>
    new HtmlTable(context, id, type, 'pony-FlexTable');

/** PGrid: ROW rows of COLUMN cells from the start, and its new rows as long. */
export class Grid extends HtmlTable {
    private columnCount = 0;

    static create(context: Context, id: number, type: number, reader: Reader): Grid {
        const grid = new Grid(context, id, type, 'pony-Grid');
        if (reader.read() === K.ROW) {
            const rows = reader.int;
            reader.read(); // COLUMN
            grid.columnCount = reader.int;
            for (let i = 0; i < rows; i++) grid.newRow(i);
        } else {
            reader.rewind();
        }
        return grid;
    }

    protected override newRow(index: number): HTMLTableRowElement {
        const tr = super.newRow(index);
        for (let i = 0; i < this.columnCount; i++) this.cell(index, i);
        return tr;
    }
}
