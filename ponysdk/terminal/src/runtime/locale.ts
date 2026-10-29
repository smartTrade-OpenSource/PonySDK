/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

/**
 * The language a page is in, and what a date is written with in it (docs/decisions/0022-locale.md). The server writes
 * `lang` on the page's root (the body, or the container of a <pony-app> element); the names come from the browser's
 * Intl, so that no text travels. Without a `lang`, English, as GWT's default locale.
 */

export interface Names {
    months: string[];
    monthsShort: string[];
    weekdays: string[];
    weekdaysShort: string[];
    /** The column heads of a month: one letter, as the language writes it. */
    weekdaysNarrow: string[];
    am: string;
    pm: string;
}

/** The week, with the days as Date.getDay() numbers them (0 is Sunday). */
export interface Week {
    first: number;
    weekend: number[];
}

/** Sunday first, as Date.getDay(). */
const WEEKDAYS = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October',
    'November', 'December'];

const ENGLISH: Names = {
    months: MONTHS,
    monthsShort: MONTHS.map(name => name.substring(0, 3)),
    weekdays: WEEKDAYS,
    weekdaysShort: WEEKDAYS.map(name => name.substring(0, 3)),
    weekdaysNarrow: WEEKDAYS.map(name => name.charAt(0)),
    am: 'AM',
    pm: 'PM',
};

/** GWT's default locale: Monday first, the weekend on Saturday and Sunday. */
const DEFAULT_WEEK: Week = { first: 1, weekend: [0, 6] };

const names = new Map<string, Names>();
const weeks = new Map<string, Week>();

/** The `lang` that applies to the node: its own or its nearest ancestor's, through the shadow roots; null for none. */
export function localeOf(from: Node): string | null {
    for (let node: Node | null = from; node !== null; node = node.parentNode ?? (node as ShadowRoot).host ?? null) {
        const lang = node instanceof Element ? node.getAttribute('lang') : null;
        if (lang) return lang;
    }
    return null;
}

/** Whether the node reads from right to left: the computed direction of its element. */
export function isRightToLeft(element: Element): boolean {
    return getComputedStyle(element).direction === 'rtl';
}

/** The names of the locale's Gregorian calendar; English for none, or for a tag the browser does not know. */
export function namesOf(locale: string | null): Names {
    if (locale === null) return ENGLISH;
    let found = names.get(locale);
    if (found === undefined) {
        found = intlNames(locale) ?? ENGLISH;
        names.set(locale, found);
    }
    return found;
}

/** The first day and the weekend of the locale's week, where the browser tells them (Chromium, Safari); else GWT's. */
export function weekOf(locale: string | null): Week {
    if (locale === null) return DEFAULT_WEEK;
    let found = weeks.get(locale);
    if (found === undefined) {
        found = intlWeek(locale);
        weeks.set(locale, found);
    }
    return found;
}

function intlNames(locale: string): Names | null {
    try {
        const at = (options: Intl.DateTimeFormatOptions, date: number): string =>
            new Intl.DateTimeFormat(locale, { calendar: 'gregory', timeZone: 'UTC', ...options }).format(date);
        const months = (month: 'long' | 'short'): string[] => Array.from({ length: 12 }, (_, m) => at({ month }, Date.UTC(2024, m, 15)));
        // 7 January 2024 is a Sunday
        const weekdays = (weekday: 'long' | 'short' | 'narrow'): string[] =>
            Array.from({ length: 7 }, (_, d) => at({ weekday }, Date.UTC(2024, 0, 7 + d)));
        const period = (hour: number): string => new Intl.DateTimeFormat(locale, { hour: 'numeric', hour12: true, timeZone: 'UTC' })
            .formatToParts(Date.UTC(2024, 0, 1, hour)).find(part => part.type === 'dayPeriod')?.value ?? '';
        return {
            months: months('long'),
            monthsShort: months('short'),
            weekdays: weekdays('long'),
            weekdaysShort: weekdays('short'),
            weekdaysNarrow: weekdays('narrow'),
            am: period(3) || ENGLISH.am,
            pm: period(15) || ENGLISH.pm,
        };
    } catch {
        return null; // a tag the browser rejects: the page stays in English
    }
}

interface WeekInfo {
    firstDay: number;
    weekend: number[];
}

function intlWeek(locale: string): Week {
    try {
        const intl = new Intl.Locale(locale) as unknown as { getWeekInfo?: () => WeekInfo };
        if (typeof intl.getWeekInfo !== 'function') return DEFAULT_WEEK; // Firefox: no week information
        const info = intl.getWeekInfo();
        // Intl numbers the days from Monday 1 to Sunday 7
        return { first: info.firstDay % 7, weekend: info.weekend.map(day => day % 7) };
    } catch {
        return DEFAULT_WEEK; // a tag the browser rejects
    }
}
