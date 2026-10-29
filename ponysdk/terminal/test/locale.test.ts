/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
import { test } from 'node:test';

import { DateFormat } from '../src/runtime/date-format.ts';
import { localeOf, namesOf, weekOf } from '../src/runtime/locale.ts';

// docs/decisions/0022-locale.md: the page's language, and what a date is written with in it

/** What localeOf reads of a node: an element's lang, its parent, a shadow root's host. */
class FakeElement {
    readonly lang: string | null;
    readonly parentNode: object | null;

    constructor(lang: string | null, parentNode: object | null = null) {
        this.lang = lang;
        this.parentNode = parentNode;
    }

    getAttribute(name: string): string | null {
        return name === 'lang' ? this.lang : null;
    }
}

class FakeShadowRoot {
    readonly parentNode = null;
    readonly host: FakeElement;

    constructor(host: FakeElement) {
        this.host = host;
    }
}

(globalThis as unknown as { Element: unknown }).Element = FakeElement;

test('the language is the nearest lang, through the shadow roots', () => {
    const page = new FakeElement('fr');
    const body = new FakeElement(null, page);
    assert.equal(localeOf(body as unknown as Node), 'fr');
    assert.equal(localeOf(new FakeElement('ar', page) as unknown as Node), 'ar');
    const host = new FakeElement('en-GB', page);
    const container = new FakeElement(null, new FakeShadowRoot(host));
    assert.equal(localeOf(container as unknown as Node), 'en-GB', 'a <pony-app> element: its host page');
});

test('no lang, or an empty one, is no language', () => {
    assert.equal(localeOf(new FakeElement(null, new FakeElement('')) as unknown as Node), null);
});

test('English without a locale, as GWT', () => {
    const names = namesOf(null);
    assert.equal(names.months[2], 'March');
    assert.equal(names.monthsShort[2], 'Mar');
    assert.equal(names.weekdays[0], 'Sunday');
    assert.equal(names.weekdaysShort[4], 'Thu');
    assert.equal(names.weekdaysNarrow[4], 'T');
    assert.equal(names.am, 'AM');
    assert.deepEqual(weekOf(null), { first: 1, weekend: [0, 6] });
});

test('the names of a language are the browser\'s', () => {
    const french = namesOf('fr');
    assert.equal(french.months[2], 'mars');
    assert.equal(french.months[11], 'décembre');
    assert.equal(french.weekdays[4], 'jeudi');
    assert.equal(french.weekdaysNarrow[1], 'L');
    const arabic = namesOf('ar');
    assert.equal(arabic.months[2], 'مارس');
    assert.equal(arabic.weekdays[0], 'الأحد');
    assert.equal(namesOf('fr'), french, 'computed once');
});

test('a tag the browser rejects leaves the page in English', () => {
    assert.equal(namesOf('not a tag').months[0], 'January');
    assert.deepEqual(weekOf('not a tag'), { first: 1, weekend: [0, 6] });
});

test('the week of a locale: its first day and its weekend, where the browser tells them', () => {
    assert.equal(weekOf('fr').first, 1);
    assert.equal(weekOf('en-US').first, 0);
    assert.deepEqual([...weekOf('en-US').weekend].sort(), [0, 6]);
    assert.equal(weekOf('fr'), weekOf('fr'), 'computed once');
});

test('a browser without week information keeps GWT\'s week', () => {
    const prototype = Intl.Locale.prototype;
    const original = Object.getOwnPropertyDescriptor(prototype, 'getWeekInfo')!;
    try {
        Reflect.deleteProperty(prototype, 'getWeekInfo');
        assert.deepEqual(weekOf('de'), { first: 1, weekend: [0, 6] });
    } finally {
        Object.defineProperty(prototype, 'getWeekInfo', original);
    }
});

const date = new Date(2026, 2, 5, 14, 7, 9, 45); // Thursday 5 March 2026, 14:07:09.045

test('a date in French, written and read back', () => {
    const format = new DateFormat('EEEE d MMMM yyyy', 'fr');
    assert.equal(format.format(date), 'jeudi 5 mars 2026');
    assert.deepEqual(format.parse('jeudi 5 mars 2026'), new Date(2026, 2, 5));
    assert.equal(new DateFormat('EEE d MMM yyyy', 'fr').format(date), 'jeu. 5 mars 2026');
    assert.deepEqual(new DateFormat('d MMM yyyy', 'fr').parse('5 MARS 2026'), new Date(2026, 2, 5));
    assert.equal(new DateFormat('EEEEE', 'fr').format(date), 'J', 'narrow');
    assert.equal(new DateFormat('MMMMM', 'fr').format(date), 'm', 'the first letter');
});

test('a day period in a language', () => {
    assert.equal(new DateFormat('h:mm a', 'ar').format(date), '2:07 م');
    assert.deepEqual(new DateFormat('h:mm a', 'ar').parse('1:30 م')?.getHours(), 13);
});

test('the digits an Arabic user types are read', () => {
    const format = new DateFormat('dd/MM/yyyy', 'ar');
    assert.deepEqual(format.parse('٠٥/٠٣/٢٠٢٦'), new Date(2026, 2, 5));
    assert.deepEqual(format.parse('۰۵/۰۳/۲۰۲۶'), new Date(2026, 2, 5), 'Persian');
});

interface LocalizedCase {
    locale: string;
    pattern: string;
    /** Year, month (1-12), day, hours, minutes, seconds, milliseconds. */
    date: number[];
    text: string;
}

test('formats as Java\'s SimpleDateFormat does in the language, and parses back what it formats', () => {
    const file = new URL('../build/fixtures/dates.json', import.meta.url);
    if (!existsSync(file)) throw new Error(`${file.pathname} is missing: run ./gradlew :ponysdk:terminalFixtures`);
    const cases = (JSON.parse(readFileSync(file, 'utf8')) as { localized: LocalizedCase[] }).localized;
    assert.ok(cases.length > 0);
    for (const { locale, pattern, date: [year, month, day, hours, minutes, seconds, millis], text } of cases) {
        const format = new DateFormat(pattern, locale);
        const written = new Date(2000, 0, 1);
        written.setFullYear(year!, month! - 1, day!);
        written.setHours(hours!, minutes!, seconds!, millis!);
        assert.equal(format.format(written), text, `${locale} ${pattern} ${text}`);
        assert.notEqual(format.parse(text), null, `${locale} ${pattern} ${text}`);
    }
});
