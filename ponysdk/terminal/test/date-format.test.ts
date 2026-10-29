/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
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

interface DateCase {
    pattern: string;
    text: string;
    /** Year, month (1-12), day, hours, minutes, seconds, milliseconds. */
    date: number[];
}

/** Written by Java's SimpleDateFormat: ./gradlew :ponysdk:terminalFixtures (TerminalFixtures.java). */
function javaCases(): { formatted: DateCase[]; typed: DateCase[] } {
    const file = new URL('../build/fixtures/dates.json', import.meta.url);
    if (!existsSync(file)) throw new Error(`${file.pathname} is missing: run ./gradlew :ponysdk:terminalFixtures`);
    return JSON.parse(readFileSync(file, 'utf8')) as { formatted: DateCase[]; typed: DateCase[] };
}

function local([year, month, day, hours, minutes, seconds, millis]: number[]): Date {
    const date = new Date(2000, 0, 1);
    date.setFullYear(year!, month! - 1, day!);
    date.setHours(hours!, minutes!, seconds!, millis!);
    return date;
}

/** The fields of a date the pattern writes: the others are today's, or midnight, when parsed. */
function written(pattern: string, date: Date): number[] {
    const letters = pattern.replace(/'[^']*'/g, '');
    const has = (re: RegExp): boolean => re.test(letters);
    return [has(/y/) ? date.getFullYear() : -1, has(/M/) ? date.getMonth() + 1 : -1, has(/d/) ? date.getDate() : -1,
        has(/[hHkK]/) ? date.getHours() : -1, has(/m/) ? date.getMinutes() : -1, has(/s/) ? date.getSeconds() : -1,
        has(/S/) ? date.getMilliseconds() : -1];
}

test('formats as Java\'s SimpleDateFormat, and parses back what it formats', () => {
    for (const { pattern, date, text } of javaCases().formatted) {
        const format = new DateFormat(pattern);
        assert.equal(format.format(local(date)), text, `${pattern} ${date.join(',')}`);
        const parsed = format.parse(text);
        assert.notEqual(parsed, null, `${pattern} ${text}`);
        assert.deepEqual(written(pattern, parsed!), written(pattern, local(date)), `${pattern} ${text}`);
    }
});

test('reads what a user types as Java\'s SimpleDateFormat does', () => {
    for (const { pattern, text, date } of javaCases().typed) {
        const parsed = new DateFormat(pattern).parse(text);
        assert.notEqual(parsed, null, `${pattern} ${text}`);
        assert.deepEqual(written(pattern, parsed!), written(pattern, local(date)), `${pattern} ${text}`);
    }
});

test('formats the era, a quote and an unknown letter', () => {
    assert.equal(new DateFormat("''yyyy G").format(date), "'2026 AD");
    assert.equal(new DateFormat('GGGG').format(date), 'Anno Domini');
    const bc = new Date(2000, 0, 1);
    bc.setFullYear(-5);
    assert.equal(new DateFormat('yyyy G').format(bc), '0005 BC');
    assert.equal(new DateFormat('GGGG').format(bc), 'Before Christ');
    assert.equal(new DateFormat('QQ yyyy').format(date), 'QQ 2026');
});

test('parses the era, a negative year and an unknown letter as itself', () => {
    assert.equal(new DateFormat('yyyy G').parse('0005 BC')?.getFullYear(), -4);
    assert.equal(new DateFormat('yyyy GGGG').parse('2026 Anno Domini')?.getFullYear(), 2026);
    assert.equal(new DateFormat('yyyy G').parse('2026 XY'), null);
    assert.equal(new DateFormat('y').parse('-12')?.getFullYear(), -12);
    assert.deepEqual(new DateFormat('QQ dd/MM/yyyy').parse('QQ 05/03/2026'), new Date(2026, 2, 5));
    assert.equal(new DateFormat('QQ yyyy').parse('Q 2026'), null);
});

test('parses the week year it formats', () => {
    const format = new DateFormat('dd/MM/YYYY');
    assert.equal(format.format(date), '05/03/2026');
    assert.deepEqual(format.parse('05/03/2026'), new Date(2026, 2, 5));
});

test('parses a time zone written as GMT, UTC or Z, and refuses another', () => {
    const format = new DateFormat("yyyy-MM-dd'T'HH:mmz");
    for (const zone of ['GMT', 'UTC', 'Z']) assert.equal(format.parse(`2026-03-05T14:07${zone}`)?.getTime(), Date.UTC(2026, 2, 5, 14, 7));
    assert.equal(format.parse('2026-03-05T14:07GMT-02:30')?.getTime(), Date.UTC(2026, 2, 5, 16, 37));
    assert.equal(format.parse('2026-03-05T14:07CET'), null);
});

test('refuses a name it does not know and a date out of range; reads spaces before a number', () => {
    assert.equal(new DateFormat('d MMM yyyy').parse('5 brumaire 2026'), null);
    assert.equal(new DateFormat('EEE d/M/yyyy').parse('Someday 5/3/2026'), null);
    assert.equal(new DateFormat('h:mm a').parse('1:30 XM'), null);
    assert.equal(new DateFormat('yyyy').parse('999999999'), null);
    assert.deepEqual(new DateFormat('d/M/yyyy').parse(' 5/3/2026'), new Date(2026, 2, 5));
});
