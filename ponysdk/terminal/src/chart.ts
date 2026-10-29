/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

// PChart's addon (docs/decisions/0026-chart.md): a chunk of its own, loaded when the first chart is created
// (docs/decisions/0004-terminal-chunks.md). It keeps the points the server sends, draws them on a canvas and puts the
// legend, the tooltip and what a screen reader needs beside it, in a shadow root styled by a constructed sheet: no
// inline style, under the page's policy (docs/decisions/0015-content-security-policy.md).

import {
    extent, fixed, formatTime, formatValue, linear, nearest, Rows, meanGap, Store, summary, tableRows, timeDomain,
    timeTicks, valueDomain, valueTicks,
} from './chart-model.ts';
import type { AddOn } from './runtime/object.ts';

type Kind = 'line' | 'area' | 'bar';

/** The colours when the page defines no --pony-chart-1..8: distinct in light and in dark. */
const COLORS = ['#4e79a7', '#f28e2b', '#e15759', '#76b7b2', '#59a14f', '#edc948', '#b07aa1', '#ff9da7'];
/** The label and the table a screen reader reads change at most this often, whatever the data's rate. */
const A11Y_DELAY = 1000;

const RULES = `
:host{display:block;position:relative;height:260px;min-width:160px;font-size:12px;line-height:1.3;color-scheme:light dark}
.frame{display:flex;flex-direction:column;height:100%}
.plot{position:relative;flex:1 1 0;min-height:0}
canvas{position:absolute;inset:0;width:100%;height:100%}
.tip{position:absolute;top:0;left:0;pointer-events:none;padding:4px 8px;white-space:nowrap;font-variant-numeric:tabular-nums;
background:var(--pony-surface,Canvas);color:var(--pony-text,CanvasText);border:1px solid var(--pony-border,rgba(128,128,128,.4));border-radius:4px;box-shadow:0 2px 8px rgba(0,0,0,.2)}
.tip[hidden]{display:none}
.tip div{display:flex;align-items:center;gap:6px}
.tip .time{opacity:.7}
.legend{display:flex;flex-wrap:wrap;gap:2px 14px;margin:6px 0 0;padding:0;list-style:none}
.legend li{display:flex;align-items:center;gap:5px;font-variant-numeric:tabular-nums}
.swatch{flex:none;width:10px;height:10px;border-radius:2px}
.value{opacity:.75}
.sr{position:absolute;width:1px;height:1px;overflow:hidden;clip-path:inset(50%);white-space:nowrap}
`;

let sheet: CSSStyleSheet | undefined;

interface Palette {
    text: string;
    border: string;
    font: string;
    series: string[];
}

function readPalette(host: HTMLElement): Palette {
    const style = getComputedStyle(host);
    const get = (name: string, fallback: string): string => style.getPropertyValue(name).trim() || fallback;
    return {
        text: get('--pony-text', style.color),
        border: get('--pony-border', 'rgba(128,128,128,.35)'),
        font: `${style.fontSize} ${style.fontFamily}`,
        series: COLORS.map((color, i) => get(`--pony-chart-${i + 1}`, color)),
    };
}

/** Where the plot is on the canvas, and how data maps to it: what the pointer needs. */
interface Geometry {
    left: number;
    right: number;
    top: number;
    bottom: number;
    x: (time: number) => number;
    y: (value: number) => number;
    /** The time at a position: the inverse of x. */
    time: (x: number) => number;
}

function make<K extends keyof HTMLElementTagNameMap>(tag: K, className?: string, parent?: HTMLElement): HTMLElementTagNameMap[K] {
    const e = document.createElement(tag);
    if (className !== undefined) e.className = className;
    parent?.append(e);
    return e;
}

class Chart implements AddOn {
    private readonly kind: Kind;
    private readonly decimals: number;
    private readonly windowMs: number;
    private readonly maxPoints: number;
    private readonly title: string;
    private readonly tableLength: number;
    private readonly names: string[];
    private readonly store: Store;
    private readonly rows: Rows;
    private readonly row: Float64Array;

    private readonly plot: HTMLElement;
    private readonly data: HTMLCanvasElement;
    private readonly over: HTMLCanvasElement;
    private readonly tip: HTMLElement;
    private readonly tipTime: HTMLElement;
    private readonly tipValues: HTMLElement[] = [];
    private readonly legendValues: HTMLElement[] = [];
    /** The colour chips of the legend and the tooltip, with their series. */
    private readonly swatches: [HTMLElement, number][] = [];
    private readonly table: HTMLTableElement | null;
    private readonly host: HTMLElement;
    private readonly observers: (() => void)[] = [];

    private palette: Palette;
    private geometry: Geometry | null = null;
    private width = 0;
    private height = 0;
    private ratio = 1;
    private measureDue = true;
    private hover: number | null = null;
    private frame = 0;
    private a11yTimer: ReturnType<typeof setTimeout> | undefined;

    constructor(host: HTMLElement, args: unknown[]) {
        const [kind, decimals, windowMs, maxPoints, title, tableLength, ...names] = args;
        this.kind = kind as Kind;
        this.decimals = decimals as number;
        this.windowMs = windowMs as number;
        this.maxPoints = maxPoints as number;
        this.title = String(title);
        this.tableLength = tableLength as number;
        this.names = names.map(String);
        this.store = new Store(this.names.length);
        this.rows = new Rows(this.names.length, this.decimals);
        this.row = new Float64Array(this.names.length);
        this.host = host;

        if (sheet === undefined) {
            sheet = new CSSStyleSheet();
            sheet.replaceSync(RULES);
        }
        const root = host.attachShadow({ mode: 'open' });
        root.adoptedStyleSheets = [sheet];
        const frame = make('div', 'frame');
        root.append(frame);
        this.plot = make('div', 'plot', frame);
        this.plot.setAttribute('role', 'img');
        this.data = make('canvas', undefined, this.plot);
        this.over = make('canvas', undefined, this.plot);
        this.tip = make('div', 'tip', this.plot);
        this.tip.hidden = true;
        this.tipTime = make('div', 'time', this.tip);
        const legend = make('ul', 'legend', frame);
        this.palette = readPalette(host);
        this.names.forEach((name, s) => {
            const tipRow = make('div', undefined, this.tip);
            const item = make('li', undefined, legend);
            for (const [row, values] of [[tipRow, this.tipValues], [item, this.legendValues]] as const) {
                this.swatches.push([make('span', 'swatch', row), s]);
                make('span', undefined, row).textContent = name;
                row.append(' '); // a screen reader says "EURUSD 1.0851", not "EURUSD1.0851"
                values.push(make('span', 'value', row));
            }
        });
        if (this.tableLength > 0) {
            this.table = make('table', undefined, make('div', 'sr', frame));
            make('caption', undefined, this.table).textContent = this.title;
        } else {
            this.table = null;
        }
        this.colorSwatches();
        this.paint();
        this.updateAccessible();
    }

    onInit(): void {
    }

    /** In the document its custom properties and its colour are known: the palette read before was the fallback. */
    onAttached(): void {
        this.theme();
        this.watch();
    }

    /** Out of the document nothing of the page is watched: a chart the server has not collected yet costs nothing. */
    onDetached(): void {
        this.unwatch();
    }

    update(method: string, args: unknown): void {
        if (method === 'append') {
            const time = this.rows.read(args as unknown[], this.row);
            this.store.push(time, this.row);
            this.store.trim(this.windowMs, this.maxPoints);
            this.changed();
        } else if (method === 'clear') {
            this.store.clear();
            this.rows.reset();
            this.changed();
        }
    }

    destroy(): void {
        cancelAnimationFrame(this.frame); // 0 when none is pending: nothing to cancel
        clearTimeout(this.a11yTimer);
        this.unwatch();
    }

    // ---- scheduling ----------------------------------------------------------------------------------------------

    private changed(): void {
        if (this.frame === 0) this.frame = requestAnimationFrame(() => this.render());
        if (this.a11yTimer === undefined) this.a11yTimer = setTimeout(() => this.updateAccessible(), A11Y_DELAY);
    }

    private render(): void {
        this.frame = 0;
        this.paint();
    }

    private unwatch(): void {
        for (const stop of this.observers.splice(0)) stop();
    }

    /** The size, the theme and the pointer: what redraws the chart besides its data. */
    private watch(): void {
        this.unwatch();
        // Measured and resized in the next frame, not here: a size changed inside the callback is reported by the browsers as a loop
        const resize = new ResizeObserver(() => {
            this.measureDue = true;
            this.changed();
        });
        resize.observe(this.plot);
        this.observers.push(() => resize.disconnect());
        const theme = (): void => this.theme();
        const dark = matchMedia('(prefers-color-scheme: dark)');
        dark.addEventListener('change', theme);
        this.observers.push(() => dark.removeEventListener('change', theme));
        const mutations = new MutationObserver(theme);
        mutations.observe(document.documentElement, { attributes: true });
        this.observers.push(() => mutations.disconnect());
        const move = (event: PointerEvent): void => {
            this.hover = event.clientX - this.plot.getBoundingClientRect().left;
            this.drawOver();
        };
        const leave = (): void => {
            this.hover = null;
            this.drawOver();
        };
        this.plot.addEventListener('pointermove', move);
        this.plot.addEventListener('pointerleave', leave);
        this.observers.push(() => {
            this.plot.removeEventListener('pointermove', move);
            this.plot.removeEventListener('pointerleave', leave);
        });
    }

    private theme(): void {
        this.palette = readPalette(this.host);
        this.colorSwatches();
        this.changed();
    }

    private measure(): void {
        this.measureDue = false;
        this.width = this.plot.clientWidth;
        this.height = this.plot.clientHeight;
        this.ratio = devicePixelRatio;
        for (const canvas of [this.data, this.over]) {
            canvas.width = Math.round(this.width * this.ratio);
            canvas.height = Math.round(this.height * this.ratio);
        }
    }

    // ---- drawing -------------------------------------------------------------------------------------------------

    private paint(): void {
        if (this.measureDue || this.ratio !== devicePixelRatio) this.measure();
        const g = this.data.getContext('2d')!;
        g.setTransform(this.ratio, 0, 0, this.ratio, 0, 0);
        g.clearRect(0, 0, this.width, this.height);
        this.geometry = this.store.length === 0 ? null : this.drawData(g);
        this.drawOver();
        this.names.forEach((_, s) => this.show(this.legendValues[s]!, this.store.length === 0 ? NaN : this.store.columns[s]![this.store.end - 1]!));
    }

    private colorSwatches(): void {
        for (const [swatch, series] of this.swatches) swatch.style.background = this.color(series);
    }

    private show(target: HTMLElement, value: number): void {
        const text = fixed(value, this.decimals);
        if (target.textContent !== text) target.textContent = text;
    }

    private drawData(g: CanvasRenderingContext2D): Geometry {
        const { store, palette } = this;
        const bars = this.kind === 'bar';
        const first = store.start;
        const end = store.end;
        // a bar's slot in time: the mean gap between two points, else a twentieth of the window (a second at least)
        const gap = bars ? meanGap(store.times, first, end, Math.max(this.windowMs / 20, 1000)) : 0;
        const [time0, time1] = timeDomain(store.times[first]!, store.times[end - 1]!, this.windowMs, gap / 2);
        const [low, high] = extent(store.columns, first, end);
        const [value0, value1] = valueDomain(low, high, bars);
        const offset = -new Date(time0).getTimezoneOffset() * 60000;
        g.font = palette.font;

        const top = 8;
        const bottom = this.height - 22;
        const rows = valueTicks(value0, value1, Math.max(2, Math.floor((bottom - top) / 36)), this.decimals);
        const labels = rows.ticks.map(v => formatValue(v, rows.step, this.decimals));
        const left = Math.ceil(Math.max(0, ...labels.map(l => g.measureText(l).width))) + 10;
        const right = this.width - 14;
        const x = linear(time0, time1, left, right);
        const y = linear(value0, value1, bottom, top);

        g.lineWidth = 1;
        g.strokeStyle = palette.border;
        g.fillStyle = palette.text;
        g.textBaseline = 'middle';
        g.textAlign = 'right';
        g.beginPath();
        rows.ticks.forEach((v, i) => {
            const py = Math.round(y(v)) + 0.5;
            g.moveTo(left, py);
            g.lineTo(right, py);
            g.fillText(labels[i]!, left - 6, py);
        });
        g.moveTo(left, bottom + 0.5);
        g.lineTo(right, bottom + 0.5);
        g.stroke();
        const columns = timeTicks(time0, time1, Math.max(2, Math.floor((right - left) / 90)), offset);
        g.textAlign = 'center';
        g.textBaseline = 'top';
        for (const t of columns.ticks) {
            const label = formatTime(t, columns.step, offset);
            const half = g.measureText(label).width / 2;
            g.fillText(label, Math.min(Math.max(x(t), half), this.width - half), bottom + 6); // not cut at the edges
        }

        g.save();
        g.beginPath();
        g.rect(left, top, right - left, bottom - top + 1);
        g.clip();
        if (bars) this.drawBars(g, x, y, gap);
        else this.drawLines(g, x, y, bottom);
        g.restore();
        return { left, right, top, bottom, x, y, time: linear(left, right, time0, time1) };
    }

    private drawLines(g: CanvasRenderingContext2D, x: (t: number) => number, y: (v: number) => number, bottom: number): void {
        const { store } = this;
        const area = this.kind === 'area';
        g.lineWidth = 1.5;
        g.lineJoin = 'round';
        this.names.forEach((_, s) => {
            const line = new Path2D();
            const fill = area ? new Path2D() : null;
            const column = store.columns[s]!;
            let open = false;
            let runStart = 0;
            let runEnd = 0;
            const close = (): void => {
                if (fill !== null && open) {
                    fill.lineTo(runEnd, bottom);
                    fill.lineTo(runStart, bottom);
                    fill.closePath();
                }
                open = false;
            };
            for (let i = store.start; i < store.end; i++) {
                const v = column[i]!;
                if (Number.isNaN(v)) {
                    close();
                    continue;
                }
                const px = x(store.times[i]!);
                const py = y(v);
                if (open) {
                    line.lineTo(px, py);
                    fill?.lineTo(px, py);
                } else {
                    line.moveTo(px, py);
                    fill?.moveTo(px, py);
                    runStart = px;
                    open = true;
                }
                runEnd = px;
            }
            close();
            const color = this.color(s);
            if (fill !== null) {
                g.globalAlpha = 0.18;
                g.fillStyle = color;
                g.fill(fill);
                g.globalAlpha = 1;
            }
            g.strokeStyle = color;
            g.stroke(line);
        });
    }

    private drawBars(g: CanvasRenderingContext2D, x: (t: number) => number, y: (v: number) => number, gap: number): void {
        const { store } = this;
        const group = (x(gap) - x(0)) * 0.8;
        const width = group / this.names.length;
        const base = y(0);
        this.names.forEach((_, s) => {
            g.fillStyle = this.color(s);
            const column = store.columns[s]!;
            for (let i = store.start; i < store.end; i++) {
                const top = y(column[i]!); // NaN, a series with no value: fillRect draws nothing for it
                g.fillRect(x(store.times[i]!) - group / 2 + s * width, Math.min(base, top), Math.max(1, width - 1), Math.abs(base - top));
            }
        });
    }

    private color(series: number): string {
        return this.palette.series[series % this.palette.series.length]!;
    }

    /** The crosshair and the tooltip, on a canvas of their own: the pointer does not redraw the data. */
    private drawOver(): void {
        const g = this.over.getContext('2d')!;
        g.setTransform(this.ratio, 0, 0, this.ratio, 0, 0);
        g.clearRect(0, 0, this.width, this.height);
        const geometry = this.geometry;
        const { store } = this;
        this.tip.hidden = true;
        if (geometry === null || this.hover === null || this.hover < geometry.left || this.hover > geometry.right) return;
        const first = store.start;
        const at = nearest(store.times, first, store.end, geometry.time(this.hover));
        const px = geometry.x(store.times[at]!);
        g.strokeStyle = this.palette.text;
        g.globalAlpha = 0.5;
        g.beginPath();
        g.moveTo(Math.round(px) + 0.5, geometry.top);
        g.lineTo(Math.round(px) + 0.5, geometry.bottom);
        g.stroke();
        g.globalAlpha = 1;
        const offset = -new Date(store.times[at]!).getTimezoneOffset() * 60000;
        this.tipTime.textContent = formatTime(store.times[at]!, 1000, offset);
        this.names.forEach((_, s) => {
            const v = store.columns[s]![at]!;
            this.tipValues[s]!.textContent = fixed(v, this.decimals);
            if (Number.isNaN(v)) return;
            g.fillStyle = this.color(s);
            g.beginPath();
            g.arc(px, geometry.y(v), 3.5, 0, 2 * Math.PI);
            g.fill();
        });
        this.tip.hidden = false;
        const room = this.width - this.tip.offsetWidth;
        const tipX = px + 12 + this.tip.offsetWidth > this.width ? px - 12 - this.tip.offsetWidth : px + 12;
        this.tip.style.transform = `translate(${Math.max(0, Math.min(tipX, room))}px,${geometry.top}px)`;
    }

    // ---- accessibility -------------------------------------------------------------------------------------------

    /** The label and the table a screen reader reads, at most once a second whatever the data's rate. */
    private updateAccessible(): void {
        this.a11yTimer = undefined;
        const { store } = this;
        const offset = store.length === 0 ? 0 : -new Date(store.times[store.end - 1]!).getTimezoneOffset() * 60000;
        this.plot.setAttribute('aria-label', summary(this.title, this.names, store, offset, this.decimals));
        const table = this.table;
        if (table === null) return;
        const body = tableRows(this.names, store, this.tableLength, offset, this.decimals).map((cells, r) => {
            const tr = document.createElement('tr');
            for (const text of cells) {
                const cell = document.createElement(r === 0 ? 'th' : 'td');
                cell.textContent = text;
                tr.append(cell);
            }
            return tr;
        });
        table.replaceChildren(table.caption!, ...body);
    }
}

/** The factory of PChart's addon (com.ponysdk.core.ui.chart.PChart): its creation arguments, as a typed array. */
export function createChart(_id: number, args: unknown, _widgetId: string | null, element: HTMLElement | null): AddOn {
    if (element === null || !Array.isArray(args)) throw new Error('A chart needs its element and its creation arguments');
    return new Chart(element, args);
}
