/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import { DateFormat } from '../src/runtime/date-format.ts';

const date = new Date(2026, 2, 5, 14, 7, 9, 45); // Thursday 5 March 2026, 14:07:09.045

test('formats the fields of a SimpleDateFormat pattern', () => {
    assert.equal(new DateFormat('dd/MM/yyyy').format(date), '05/03/2026');
    assert.equal(new DateFormat('MM/dd/yy').format(date), '03/05/26');
    assert.equal(new DateFormat('d MMM yyyy').format(date), '5 Mar 2026');
    assert.equal(new DateFormat('EEEE d MMMM').format(date), 'Thursday 5 March');
    assert.equal(new DateFormat('EEE, h:mm a').format(date), 'Thu, 2:07 PM');
    assert.equal(new DateFormat('dd/MM/yyyy HH:mm:ss.SSS').format(date), '05/03/2026 14:07:09.045');
    assert.equal(new DateFormat("yyyy-MM-dd'T'HH:mm 'o''clock'").format(date), "2026-03-05T14:07 o'clock");
    assert.equal(new DateFormat('k K').format(new Date(2026, 0, 1, 0, 0)), '24 0');
});

test('parses what it formats', () => {
    for (const pattern of ['dd/MM/yyyy', 'MM/dd/yyyy HH:mm:ss', 'd MMMM yyyy', 'EEE d MMM yyyy h:mm a', 'yyyyMMdd', 'yyyy-MM-dd HH:mm:ss.SSS']) {
        const format = new DateFormat(pattern);
        assert.equal(format.format(format.parse(format.format(date))!), format.format(date), pattern);
    }
});

test('parses numbers of any width, and abutting fields by their width', () => {
    assert.deepEqual(new DateFormat('dd/MM/yyyy').parse('5/3/2026'), new Date(2026, 2, 5));
    assert.deepEqual(new DateFormat('yyyyMMdd').parse('20260305'), new Date(2026, 2, 5));
    assert.deepEqual(new DateFormat('d MMM yyyy').parse('5 march 2026'), new Date(2026, 2, 5));
    assert.deepEqual(new DateFormat('h:mm a').parse('12:30 AM')?.getHours(), 0);
    assert.deepEqual(new DateFormat('h:mm a').parse('1:30 PM')?.getHours(), 13);
});

test('reads a two-digit year within 80 years before and 20 after today', () => {
    const year = new Date().getFullYear();
    const near = (year + 5) % 100;
    const far = (year + 30) % 100;
    assert.equal(new DateFormat('yy').parse(String(near).padStart(2, '0'))?.getFullYear(), year + 5);
    assert.equal(new DateFormat('yy').parse(String(far).padStart(2, '0'))?.getFullYear(), year + 30 - 100);
    assert.equal(new DateFormat('yyyy').parse('26')?.getFullYear(), 26);
});

test('is lenient, as GWT: a day out of range rolls over', () => {
    assert.deepEqual(new DateFormat('dd/MM/yyyy').parse('32/01/2026'), new Date(2026, 1, 1));
});

test('refuses a text that does not fit', () => {
    const format = new DateFormat('dd/MM/yyyy');
    assert.equal(format.parse('05-03-2026'), null);
    assert.equal(format.parse('05/03/2026 extra'), null);
    assert.equal(format.parse('05/03'), null);
    assert.equal(format.parse(''), null);
});

test('takes a time zone', () => {
    const parsed = new DateFormat("yyyy-MM-dd'T'HH:mmZ").parse('2026-03-05T14:07+0100');
    assert.equal(parsed?.getTime(), Date.UTC(2026, 2, 5, 13, 7));
});
