/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    extent, fixed, formatTime, formatValue, linear, nearest, niceStep, Rows, meanGap, Store, summary, tableRows,
    timeDomain, timeTicks, valueDomain, valueTicks,
} from '../src/chart-model.ts';

// What the chart computes (src/chart-model.ts, docs/decisions/0026-chart.md): the scales, the ticks, the points kept

const NONE = 0;
const UNLIMITED = Number.MAX_SAFE_INTEGER;

function filled(times: number[], ...columns: number[][]): Store {
    const store = new Store(columns.length, 2);
    times.forEach((time, i) => store.push(time, columns.map(column => column[i]!)));
    return store;
}

test('a store keeps its points in order while it grows and while its front is trimmed', () => {
    const store = new Store(2, 4);
    for (let i = 0; i < 100; i++) store.push(i * 10, [i, -i]);
    assert.equal(store.length, 100);
    assert.equal(store.times[store.start + 99], 990);
    store.trim(NONE, 10); // the last 10 points
    assert.deepEqual([store.length, store.times[store.start], store.columns[1]![store.start]], [10, 900, -90]);
    // the front freed more than half: the next points move the rest down instead of growing the arrays
    const capacity = store.times.length;
    for (let i = 100; i < 300; i++) {
        store.push(i * 10, [i, -i]);
        store.trim(NONE, 10);
    }
    assert.equal(store.times.length, capacity, 'no growth: the points moved down');
    assert.deepEqual([store.length, store.times[store.start], store.times[store.end - 1]], [10, 2900, 2990]);
});

test('a store drops what is older than its window, and keeps the last point whatever the window', () => {
    const store = filled([0, 1000, 2000, 3000, 4000], [1, 2, 3, 4, 5]);
    store.trim(2500, UNLIMITED); // from 1500: 2000, 3000, 4000
    assert.deepEqual([store.length, store.times[store.start]], [3, 2000]);
    store.trim(1, UNLIMITED);
    assert.equal(store.length, 1);
    assert.equal(store.times[store.start], 4000);
    new Store(1).trim(1000, 10); // nothing to trim
    store.clear();
    assert.deepEqual([store.length, store.start], [0, 0]);
});

test('a row is the time since the previous one and the change of each series in units; null leaves a series where it was', () => {
    const rows = new Rows(2, 2);
    const out = new Float64Array(2);
    assert.equal(rows.read([1_000_000, 10850, null], out), 1_000_000);
    assert.deepEqual([...out.map(v => Number.isNaN(v) ? -1 : v)], [108.5, -1], 'the second series has no value yet');
    assert.equal(rows.read([100, -5, 20000], out), 1_000_100);
    assert.deepEqual([...out], [108.45, 200]);
    rows.read([50, null, null], out);
    assert.deepEqual([...out], [108.45, 200], 'unchanged');
    rows.reset();
    assert.equal(rows.read([7, 1, 1], out), 7);
    assert.deepEqual([...out], [0.01, 0.01], 'deltas from zero again');
    assert.equal(rows.scale, 100);
});

test('a malformed row is refused, not decoded into a wrong point', () => {
    const rows = new Rows(2, 0);
    const out = new Float64Array(2);
    assert.throws(() => rows.read([1, 2], out), /2 elements for 2 series/);
    assert.throws(() => rows.read(['1', 2, 3], out), /Not a number/);
    assert.throws(() => rows.read([1, null, true], out), /Not a number/);
});

test('ticks are the multiples of a 1, 2 or 5 step', () => {
    assert.equal(niceStep(100, 5), 20);
    assert.equal(niceStep(1, 10), 0.1);
    assert.equal(niceStep(7, 5), 1);
    assert.equal(niceStep(0.3, 5), 0.05);
    assert.equal(niceStep(12, 4), 5);
    assert.equal(niceStep(0.0004, 0), 0.0005, 'a count below one is one');
    assert.deepEqual(valueTicks(0, 100, 5, 4), { ticks: [0, 20, 40, 60, 80, 100], step: 20 });
    assert.deepEqual(valueTicks(-3.2, 3.2, 6, 4).ticks, [-3, -2, -1, 0, 1, 2, 3]);
    // 0.1 + 0.2 is not 0.3, and an axis label must not say so
    assert.deepEqual(valueTicks(0, 0.5, 5, 4).ticks, [0, 0.1, 0.2, 0.3, 0.4, 0.5]);
    assert.deepEqual(valueTicks(1.0851, 1.0859, 4, 5).ticks, [1.0852, 1.0854, 1.0856, 1.0858]);
    // a series of whole numbers has no tick between two of them
    assert.deepEqual(valueTicks(51.52, 52.48, 4, 0), { ticks: [52], step: 1 });
    assert.deepEqual(valueTicks(0, 0.003, 3, 2).ticks, [0]);
});

test('the value axis fits the data with a margin, bars from zero', () => {
    const [low, high] = valueDomain(10, 20, false);
    assert.ok(Math.abs(low - 9.5) < 1e-9 && Math.abs(high - 20.5) < 1e-9);
    assert.deepEqual(valueDomain(5, 25, true).map(v => Math.round(v * 100) / 100), [0, 26.25]);
    assert.deepEqual(valueDomain(-10, -2, true).map(v => Math.round(v * 100) / 100), [-10.5, 0]);
    assert.deepEqual(valueDomain(-4, 4, true).map(v => Math.round(v * 100) / 100), [-4.4, 4.4]);
    assert.deepEqual(valueDomain(100, 100, false), [99, 101], 'a flat line still has a scale');
    assert.deepEqual(valueDomain(0, 0, false), [-1, 1]);
    assert.deepEqual(valueDomain(0, 0, true), [0, 1]);
    assert.deepEqual(valueDomain(Infinity, -Infinity, false), [0, 1], 'no data');
});

test('time ticks are round times of the local clock', () => {
    const hour = 3_600_000;
    // 60 s over 6 ticks: every 10 s
    const seconds = timeTicks(0, 60_000, 6, 0);
    assert.equal(seconds.step, 10_000);
    assert.deepEqual(seconds.ticks, [0, 10_000, 20_000, 30_000, 40_000, 50_000, 60_000]);
    // the local clock is an hour ahead: a tick at each local 5 minutes
    const local = timeTicks(0, 20 * 60_000, 4, hour);
    assert.equal(local.step, 5 * 60_000);
    assert.deepEqual(local.ticks.map(t => (t + hour) % (5 * 60_000)), [0, 0, 0, 0, 0]);
    assert.equal(timeTicks(0, 1000, 10, 0).step, 100);
    assert.equal(timeTicks(0, 4 * 86_400_000, 4, 0).step, 86_400_000);
    assert.equal(timeTicks(0, 10 * 365 * 86_400_000, 2, 0).step, 30 * 86_400_000, 'the largest step is the last');
    assert.deepEqual(timeTicks(NaN, 1, 4, 0).ticks, []);
});

test('a time label shows what the step tells apart', () => {
    const t = Date.UTC(2026, 9, 29, 14, 5, 9, 45);
    assert.equal(formatTime(t, 500, 0), '14:05:09.045');
    assert.equal(formatTime(t, 1000, 0), '14:05:09');
    assert.equal(formatTime(t, 60_000, 0), '14:05');
    assert.equal(formatTime(t, 86_400_000, 0), '10-29');
    assert.equal(formatTime(t, 1000, 2 * 3_600_000), '16:05:09', 'in the local clock');
});

test('a value label has the decimals its step needs, no more than the series have', () => {
    assert.equal(formatValue(1.0852, 0.0002, 5), '1.0852');
    assert.equal(formatValue(20, 20, 4), '20');
    assert.equal(formatValue(0.5, 0.1, 4), '0.5');
    assert.equal(formatValue(1.08521, 0.00001, 3), '1.085', 'the series stop at 3');
    assert.equal(formatValue(0.05, 0.01, 2), '0.05');
});

test('the time axis shows the window before the last point, else the points, a margin for bars', () => {
    assert.deepEqual(timeDomain(0, 1000, 60_000, 0), [-59_000, 1000]);
    assert.deepEqual(timeDomain(0, 1000, 60_000, 50), [-59_000, 1050]);
    assert.deepEqual(timeDomain(0, 1000, 0, 0), [0, 1000]);
    assert.deepEqual(timeDomain(0, 1000, 0, 50), [-50, 1050]);
    assert.deepEqual(timeDomain(500, 500, 0, 0), [-500, 1500], 'one point is not an empty axis');
});

test('a linear scale maps a domain to a range, and back with the inverse', () => {
    const x = linear(10, 20, 100, 300);
    assert.deepEqual([x(10), x(15), x(20)], [100, 200, 300]);
    const y = linear(0, 1, 50, 0);
    assert.equal(y(0.5), 25);
    assert.equal(linear(100, 300, 10, 20)(x(13)), 13);
});

test('the point nearest to a time, from either side', () => {
    const times = Float64Array.from([0, 100, 200, 400, 800]);
    assert.equal(nearest(times, 0, 5, -50), 0);
    assert.equal(nearest(times, 0, 5, 40), 0);
    assert.equal(nearest(times, 0, 5, 60), 1);
    assert.equal(nearest(times, 0, 5, 300), 2, 'halfway: the earlier');
    assert.equal(nearest(times, 0, 5, 301), 3);
    assert.equal(nearest(times, 0, 5, 5000), 4);
    assert.equal(nearest(times, 1, 4, 0), 1, 'only the points from..to count');
    assert.equal(nearest(times, 3, 4, 0), 3);
});

test('the extent and the mean gap of the points kept', () => {
    const a = Float64Array.from([5, 3, NaN, 9]);
    const b = Float64Array.from([4, 12, 1, NaN]);
    assert.deepEqual(extent([a, b], 0, 4), [1, 12], 'NaN is no value');
    assert.deepEqual(extent([a, b], 0, 2), [3, 12]);
    assert.deepEqual(extent([], 0, 0), [Infinity, -Infinity]);
    assert.equal(meanGap(Float64Array.from([0, 100, 150, 400]), 0, 4, 7), 400 / 3);
    assert.equal(meanGap(Float64Array.from([0, 100, 150, 400]), 2, 4, 7), 250);
    assert.equal(meanGap(Float64Array.from([0]), 0, 1, 7), 7, 'one point has no gap');
});

test('what a screen reader gets: the last values and the last rows, newest first', () => {
    const empty = new Store(2);
    assert.equal(summary('Prices', ['EURUSD', 'GBPUSD'], empty, 0, 4), 'Prices: no data');
    const store = filled([Date.UTC(2026, 0, 1, 10, 0, 0), Date.UTC(2026, 0, 1, 10, 0, 1), Date.UTC(2026, 0, 1, 10, 0, 2)],
        [1.0851, 1.0852, 1.0853], [NaN, 1.27, 1.2701]);
    assert.equal(summary('Prices', ['EURUSD', 'GBPUSD'], store, 0, 4), 'Prices: EURUSD 1.0853, GBPUSD 1.2701 at 10:00:02');
    assert.deepEqual(tableRows(['EURUSD', 'GBPUSD'], store, 2, 0, 2),
        [['Time', 'EURUSD', 'GBPUSD'], ['10:00:02', '1.09', '1.27'], ['10:00:01', '1.09', '1.27']]);
    assert.equal(tableRows(['EURUSD', 'GBPUSD'], store, 10, 0, 2).length, 4, 'no more rows than points');
    assert.deepEqual(tableRows(['EURUSD', 'GBPUSD'], store, 10, 0, 2)[3], ['10:00:00', '1.09', '–'], 'a dash for no value');
    assert.equal(fixed(NaN, 2), '–');
});
