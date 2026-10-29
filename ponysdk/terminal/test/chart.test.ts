/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import { ServerToClientModel as K, ServerToClientModelTypeModel, ValueTypeModel as V, WidgetType as W } from '#model';
import { createChart } from '../src/chart.ts';
import { Rows } from '../src/chart-model.ts';
import { Dispatcher, type HistoryPort, type Host } from '../src/runtime/dispatcher.ts';
import { loadingOf } from '../src/runtime/factory.ts';
import { Reader } from '../src/protocol/reader.ts';
import { fixture } from './fixtures.ts';
import { encode } from './frames.ts';

// A chart, from the server's frames to the points the terminal keeps (docs/decisions/0026-chart.md), and the loading of
// the chunk that draws it (docs/decisions/0004-terminal-chunks.md)

const SIGNATURE = 'com.ponysdk.core.ui.chart.PChart';

/** The creation arguments of the addon, and each call of its methods, as the server's frames carry them. */
function calls(): { creation: unknown[]; calls: [string, unknown[] | null][] } {
    const reader = new Reader();
    let creation: unknown[] = [];
    const found: [string, unknown[] | null][] = [];
    for (const frame of fixture('chart').frames) {
        reader.feed(frame);
        while (reader.hasKey()) {
            reader.read();
            const key = reader.key;
            if (key === K.PADDON_CREATION_ARGS) creation = reader.array;
            else if (key === K.PADDON_METHOD) found.push([reader.str, null]);
            else if (key === K.PADDON_ARGUMENTS) found[found.length - 1]![1] = reader.array;
            else if (ServerToClientModelTypeModel[key] === V.ARRAY) reader.array;
        }
    }
    return { creation, calls: found };
}

test('the creation arguments of a chart read back as the server wrote them', () => {
    assert.deepEqual(calls().creation, ['line', 5, 60_000, 3600, 'Prices', 5, 'EURUSD', 'GBPUSD']);
});

test('the points the server wrote decode to the values it was given, a clear starting the deltas again', () => {
    const { creation, calls: found } = calls();
    const rows = new Rows(2, creation[1] as number);
    const out = new Float64Array(2);
    const decoded: number[][] = [];
    for (const [method, args] of found) {
        if (method === 'clear') {
            assert.equal(args, null);
            rows.reset();
        } else {
            decoded.push([rows.read(args!, out), ...out]);
        }
    }
    assert.deepEqual(decoded, fixture('chart').json['points']);
});

test('a chart needs its element and its creation arguments', () => {
    assert.throws(() => createChart(1, [], null, null), /needs its element/);
    assert.throws(() => createChart(1, 'not an array', '2', {} as HTMLElement), /needs its element/);
});

test('the chart is loaded when the first one is created: its block waits, and its factory joins the addons of the page', async () => {
    const reader = new Reader();
    reader.feed(encode([K.FACTORY, SIGNATURE]));
    assert.equal(loadingOf(W.LABEL, reader), null, 'a widget of the core waits for nothing');

    const loading = loadingOf(W.ADDON_COMPOSITE, reader);
    assert.ok(loading instanceof Promise);
    assert.equal(reader.read(), K.FACTORY, 'the reader is back at the FACTORY, for the block to run again');
    reader.rewind();
    assert.equal(loadingOf(W.ADDON, reader), loading, 'a second chart created meanwhile waits for the same loading');

    await loading;
    const dispatcher = new Dispatcher({} as Host, () => null, {} as never, {} as HistoryPort);
    assert.equal(typeof dispatcher.addOnFactory(SIGNATURE), 'function');
    assert.equal(loadingOf(W.ADDON, reader), null, 'loaded: nothing to wait for');

    const other = new Reader();
    other.feed(encode([K.FACTORY, 'com.example.Chart']));
    assert.equal(loadingOf(W.ADDON, other), null, 'an addon of the application is the page\'s to define');
});
