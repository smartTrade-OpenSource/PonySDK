/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

// What the chart computes, apart from the page (docs/decisions/0026-chart.md): the rows the server sends decoded, the
// points kept, the scales and ticks, the labels. Pure functions and one store: the Node tests drive them; chart.ts
// only draws.

/** The points kept: a time column and one column per series, the oldest dropped from the front. */
export class Store {
    readonly series: number;
    times: Float64Array;
    columns: Float64Array[];
    /** The first point kept, and how many there are: points are start..start+length-1 of each column. */
    start = 0;
    length = 0;

    constructor(series: number, capacity = 256) {
        this.series = series;
        this.times = new Float64Array(capacity);
        this.columns = Array.from({ length: series }, () => new Float64Array(capacity));
    }

    get end(): number {
        return this.start + this.length;
    }

    /** Adds a point at the end: room is made by moving the points down when the front has freed half of the store, else by growing it. */
    push(time: number, values: ArrayLike<number>): void {
        if (this.end === this.times.length) {
            if (this.start >= this.times.length >> 1) this.moveDown();
            else this.grow();
        }
        const at = this.end;
        this.times[at] = time;
        for (let s = 0; s < this.series; s++) this.columns[s]![at] = values[s]!;
        this.length++;
    }

    /** Drops the oldest points: older than the window before the last point (0: no limit), then those beyond maxPoints. */
    trim(windowMs: number, maxPoints: number): void {
        if (this.length === 0) return;
        if (windowMs > 0) {
            const from = this.times[this.end - 1]! - windowMs;
            while (this.length > 1 && this.times[this.start]! < from) {
                this.start++;
                this.length--;
            }
        }
        if (this.length > maxPoints) {
            this.start += this.length - maxPoints;
            this.length = maxPoints;
        }
    }

    clear(): void {
        this.start = 0;
        this.length = 0;
    }

    private moveDown(): void {
        this.times.copyWithin(0, this.start, this.end);
        for (const column of this.columns) column.copyWithin(0, this.start, this.end);
        this.start = 0;
    }

    private grow(): void {
        const capacity = this.times.length * 2;
        const times = new Float64Array(capacity);
        times.set(this.times);
        this.times = times;
        this.columns = this.columns.map(column => {
            const grown = new Float64Array(capacity);
            grown.set(column);
            return grown;
        });
    }
}

/**
 * What the rows the server sent add up to: each one is the time since the previous row, then for each series its
 * change since the previous row, in units of 10^-decimals; null leaves the series where it was
 * (docs/decisions/0026-chart.md).
 */
export class Rows {
    readonly series: number;
    readonly scale: number;
    private time = 0;
    private readonly units: Float64Array;
    private readonly seen: Uint8Array;

    constructor(series: number, decimals: number) {
        this.series = series;
        this.scale = 10 ** decimals;
        this.units = new Float64Array(series);
        this.seen = new Uint8Array(series);
    }

    /** The row's time, its values written to out (NaN for a series with no value yet). */
    read(row: readonly unknown[], out: Float64Array): number {
        if (row.length !== this.series + 1) throw new Error(`A row of ${row.length} elements for ${this.series} series`);
        this.time += number(row[0]);
        for (let s = 0; s < this.series; s++) {
            const change = row[s + 1];
            if (change !== null) {
                this.units[s]! += number(change);
                this.seen[s] = 1;
            }
            out[s] = this.seen[s] === 1 ? this.units[s]! / this.scale : NaN;
        }
        return this.time;
    }

    reset(): void {
        this.time = 0;
        this.units.fill(0);
        this.seen.fill(0);
    }
}

function number(value: unknown): number {
    if (typeof value !== 'number') throw new Error(`Not a number in a row: ${String(value)}`);
    return value;
}

/** The step of a 1, 2, 5 progression that gives about count ticks over the range. */
export function niceStep(range: number, count: number): number {
    const raw = range / Math.max(1, count);
    const exponent = Math.floor(Math.log10(raw));
    const fraction = raw / 10 ** exponent;
    return (fraction < 1.5 ? 1 : fraction < 3 ? 2 : fraction < 7 ? 5 : 10) * 10 ** exponent;
}

/** The multiples of a nice step inside [min, max], about count of them, the step no finer than the series' decimals. */
export function valueTicks(min: number, max: number, count: number, decimals: number): { ticks: number[]; step: number } {
    const step = Math.max(niceStep(max - min, count), 10 ** -decimals);
    const ticks: number[] = [];
    for (let k = Math.ceil(min / step - 1e-9); k * step <= max + step * 1e-9; k++) {
        ticks.push(Number((k * step).toPrecision(12)));
    }
    return { ticks, step };
}

/** The value axis' domain: the extent of the data with a margin, and zero in it when the bars need their base. */
export function valueDomain(min: number, max: number, zero: boolean): [number, number] {
    if (!Number.isFinite(min) || !Number.isFinite(max)) return [0, 1];
    if (zero) {
        min = Math.min(min, 0);
        max = Math.max(max, 0);
    }
    if (min === max) {
        if (min === 0) return [zero ? 0 : -1, 1];
        const margin = Math.abs(min) * 0.01;
        return [min - margin, max + margin];
    }
    const margin = (max - min) * 0.05;
    return [zero && min === 0 ? 0 : min - margin, zero && max === 0 ? 0 : max + margin];
}

const SECOND = 1000;
const MINUTE = 60 * SECOND;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;
const TIME_STEPS = [100, 200, 500, SECOND, 2 * SECOND, 5 * SECOND, 10 * SECOND, 15 * SECOND, 30 * SECOND, MINUTE, 2 * MINUTE,
    5 * MINUTE, 10 * MINUTE, 15 * MINUTE, 30 * MINUTE, HOUR, 2 * HOUR, 3 * HOUR, 6 * HOUR, 12 * HOUR, DAY, 2 * DAY, 7 * DAY,
    14 * DAY, 30 * DAY];

/**
 * The time axis' ticks: the multiples of a round step (a second, five minutes, an hour, a day…) of the local clock,
 * offset being the local time's difference to UTC in milliseconds; the smallest step that gives about count, or fewer.
 */
export function timeTicks(min: number, max: number, count: number, offset: number): { ticks: number[]; step: number } {
    const wanted = (max - min) / (Math.max(1, count) * 1.05); // a window with a margin of a bar is not a step more
    const step = TIME_STEPS.find(s => s >= wanted) ?? TIME_STEPS[TIME_STEPS.length - 1]!;
    const ticks: number[] = [];
    for (let t = Math.ceil((min + offset) / step) * step - offset; t <= max; t += step) ticks.push(t);
    return { ticks, step };
}

const two = (n: number): string => (n < 10 ? '0' : '') + n;

/** A time axis label: the clock to the second, the minute or, from a day of step, the date. */
export function formatTime(time: number, step: number, offset: number): string {
    const d = new Date(time + offset);
    if (step >= DAY) return `${two(d.getUTCMonth() + 1)}-${two(d.getUTCDate())}`;
    const clock = `${two(d.getUTCHours())}:${two(d.getUTCMinutes())}`;
    if (step >= MINUTE) return clock;
    const seconds = `${clock}:${two(d.getUTCSeconds())}`;
    return step >= SECOND ? seconds : `${seconds}.${String(d.getUTCMilliseconds()).padStart(3, '0')}`;
}

/** A value with the decimals its tick step needs, no more than the series have. */
export function formatValue(value: number, step: number, decimals: number): string {
    return value.toFixed(Math.max(0, Math.min(decimals, Math.ceil(-Math.log10(step) - 1e-9))));
}

/** The time axis' domain: the window before the last point, else the points' extent, a margin more for bars. */
export function timeDomain(first: number, last: number, windowMs: number, margin: number): [number, number] {
    if (windowMs > 0) return [last - windowMs, last + margin];
    if (first === last) return [first - SECOND, last + SECOND];
    return [first - margin, last + margin];
}

/** A scale from a domain to a range: the value, or the position. */
export function linear(domain0: number, domain1: number, range0: number, range1: number): (value: number) => number {
    const factor = (range1 - range0) / (domain1 - domain0);
    return value => range0 + (value - domain0) * factor;
}

/** The index in [from, to) whose time is nearest to time. */
export function nearest(times: Float64Array, from: number, to: number, time: number): number {
    let low = from;
    let high = to - 1;
    while (low < high) {
        const middle = (low + high) >> 1;
        if (times[middle]! < time) low = middle + 1;
        else high = middle;
    }
    return low > from && time - times[low - 1]! <= times[low]! - time ? low - 1 : low;
}

/** The smallest value and the largest of the columns in [from, to): NaN is no value. */
export function extent(columns: readonly Float64Array[], from: number, to: number): [number, number] {
    let min = Infinity;
    let max = -Infinity;
    for (const column of columns) {
        for (let i = from; i < to; i++) {
            const v = column[i]!;
            if (v < min) min = v;
            if (v > max) max = v;
        }
    }
    return [min, max];
}

/** The mean time between two points of [from, to), for the width of a bar; the fallback for fewer than two points. */
export function meanGap(times: Float64Array, from: number, to: number, fallback: number): number {
    return to - from < 2 ? fallback : (times[to - 1]! - times[from]!) / (to - from - 1);
}

/** A value with the series' decimals; a dash for no value. */
export function fixed(value: number, decimals: number): string {
    return Number.isNaN(value) ? '–' : value.toFixed(decimals);
}

/** What a screen reader says for the chart: its title, the last value of each series and when. */
export function summary(title: string, names: readonly string[], store: Store, offset: number, decimals: number): string {
    if (store.length === 0) return `${title}: no data`;
    const last = store.end - 1;
    const values = names.map((name, s) => `${name} ${fixed(store.columns[s]![last]!, decimals)}`);
    return `${title}: ${values.join(', ')} at ${formatTime(store.times[last]!, SECOND, offset)}`;
}

/** The cells of the last rows, newest first, for the table a screen reader can walk: a header row, then one per point. */
export function tableRows(names: readonly string[], store: Store, rows: number, offset: number, decimals: number): string[][] {
    const out = [['Time', ...names]];
    for (let i = store.end - 1; i >= Math.max(store.start, store.end - rows); i--) {
        out.push([formatTime(store.times[i]!, SECOND, offset), ...names.map((_, s) => fixed(store.columns[s]![i]!, decimals))]);
    }
    return out;
}
