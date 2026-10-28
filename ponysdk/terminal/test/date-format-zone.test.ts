/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import { DateFormat } from '../src/runtime/date-format.ts';

/** The browser's time zone, set for this process: Node reads TZ again when it changes. */
function inZone(zone: string, run: () => void): void {
    const previous = process.env['TZ'];
    process.env['TZ'] = zone;
    try {
        run();
    } finally {
        if (previous === undefined) delete process.env['TZ'];
        else process.env['TZ'] = previous;
    }
}

test('formats the time zone as RFC 822 (Z) or GMT (z, v, ZZZZ)', () => {
    inZone('Asia/Kolkata', () => {
        const date = new Date(Date.UTC(2026, 2, 5, 12, 0));
        assert.equal(new DateFormat('HH:mm Z').format(date), '17:30 +0530');
        assert.equal(new DateFormat('ZZZZ').format(date), 'GMT+05:30');
        assert.equal(new DateFormat('z').format(date), 'GMT+05:30');
        assert.equal(new DateFormat('v').format(date), 'GMT+05:30');
    });
    inZone('America/New_York', () => {
        const date = new Date(Date.UTC(2026, 0, 5, 12, 0));
        assert.equal(new DateFormat('Z').format(date), '-0500');
        assert.equal(new DateFormat('z').format(date), 'GMT-05:00');
    });
});
