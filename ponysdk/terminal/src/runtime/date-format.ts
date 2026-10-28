/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

export const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October',
    'November', 'December'];
/** Sunday first, as Date.getDay(). */
export const WEEKDAYS = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

const NUMERIC = new Set(['y', 'Y', 'd', 'h', 'H', 'k', 'K', 'm', 's', 'S']);

type Part = { literal: string } | { field: string; count: number };

/** A field is numeric when it is written with digits: a month in one or two letters, M or L. */
function numeric(part: Part): boolean {
    return 'field' in part && (NUMERIC.has(part.field) || ((part.field === 'M' || part.field === 'L') && part.count < 3));
}

function pad(value: number, width: number): string {
    return String(value).padStart(width, '0');
}

/** The pattern's fields and literal texts; 'text' is a literal, '' a quote. */
function compile(pattern: string): Part[] {
    const parts: Part[] = [];
    let literal = '';
    for (let i = 0; i < pattern.length;) {
        const c = pattern[i]!;
        if (c === "'") {
            if (pattern[i + 1] === "'") {
                literal += "'";
                i += 2;
                continue;
            }
            // a quoted text, where '' is a quote
            for (i++; i < pattern.length; i++) {
                if (pattern[i] !== "'") literal += pattern[i];
                else if (pattern[i + 1] === "'") literal += pattern[++i];
                else break;
            }
            i++;
            continue;
        }
        if (/[a-zA-Z]/.test(c)) {
            let count = 1;
            while (pattern[i + count] === c) count++;
            if (literal !== '') parts.push({ literal });
            literal = '';
            parts.push({ field: c, count });
            i += count;
            continue;
        }
        literal += c;
        i++;
    }
    if (literal !== '') parts.push({ literal });
    return parts;
}

function zone(date: Date, count: number, gmt: boolean): string {
    const offset = -date.getTimezoneOffset();
    const sign = offset < 0 ? '-' : '+';
    const hours = pad(Math.floor(Math.abs(offset) / 60), 2);
    const minutes = pad(Math.abs(offset) % 60, 2);
    return gmt || count >= 4 ? `GMT${sign}${hours}:${minutes}` : `${sign}${hours}${minutes}`;
}

/**
 * A date pattern of Java's SimpleDateFormat (MM/dd/yyyy...), formatted and parsed as GWT's DateTimeFormat does in its
 * default locale: English names, the browser's time zone. Parsing is lenient, as DateTimeFormat.parse: a day out of
 * range rolls over, a field the pattern lacks is today's (the time is midnight).
 */
export class DateFormat {
    readonly pattern: string;
    private readonly parts: Part[];

    constructor(pattern: string) {
        this.pattern = pattern;
        this.parts = compile(pattern);
    }

    format(date: Date): string {
        let text = '';
        for (const part of this.parts) text += 'literal' in part ? part.literal : this.formatField(date, part.field, part.count);
        return text;
    }

    private formatField(date: Date, field: string, count: number): string {
        switch (field) {
            case 'G': {
                const ad = date.getFullYear() > 0;
                if (count >= 4) return ad ? 'Anno Domini' : 'Before Christ';
                return ad ? 'AD' : 'BC';
            }
            case 'y':
            case 'Y': {
                const year = Math.abs(date.getFullYear());
                return count === 2 ? pad(year % 100, 2) : pad(year, count);
            }
            case 'M':
            case 'L': {
                const month = date.getMonth();
                if (count >= 5) return MONTHS[month]!.charAt(0);
                if (count === 4) return MONTHS[month]!;
                if (count === 3) return MONTHS[month]!.substring(0, 3);
                return pad(month + 1, count);
            }
            case 'd':
                return pad(date.getDate(), count);
            case 'E':
            case 'c': {
                const day = WEEKDAYS[date.getDay()]!;
                if (count >= 5) return day.charAt(0);
                return count === 4 ? day : day.substring(0, 3);
            }
            case 'a':
                return date.getHours() < 12 ? 'AM' : 'PM';
            case 'h':
                return pad(date.getHours() % 12 || 12, count);
            case 'H':
                return pad(date.getHours(), count);
            case 'k':
                return pad(date.getHours() || 24, count);
            case 'K':
                return pad(date.getHours() % 12, count);
            case 'm':
                return pad(date.getMinutes(), count);
            case 's':
                return pad(date.getSeconds(), count);
            case 'S': {
                // fractional seconds, as GWT: the first count digits of the milliseconds
                const millis = pad(date.getMilliseconds(), 3);
                return count <= 3 ? millis.substring(0, count) : millis.padEnd(count, '0');
            }
            case 'Z':
                return zone(date, count, false);
            case 'z':
            case 'v':
                return zone(date, count, true);
            default:
                return field.repeat(count);
        }
    }

    /** The date the text holds, or null when the text does not fit the pattern. */
    parse(text: string): Date | null {
        const today = new Date();
        const fields = {
            year: today.getFullYear(), month: today.getMonth(), day: today.getDate(), hours: 0, minutes: 0, seconds: 0,
            millis: 0, pm: null as boolean | null, offset: null as number | null, bc: false,
        };
        let position = 0;
        for (let i = 0; i < this.parts.length; i++) {
            const part = this.parts[i]!;
            if ('literal' in part) {
                position = matchLiteral(text, position, part.literal);
                if (position < 0) return null;
                continue;
            }
            // abutting numeric fields (yyyyMMdd) take their width; a field alone takes every digit
            const width = numeric(part) && this.parts[i + 1] !== undefined && numeric(this.parts[i + 1]!) ? part.count : 0;
            position = this.parseField(text, position, part, width, fields);
            if (position < 0) return null;
        }
        if (position !== text.length) return null;
        let hours = fields.hours;
        if (fields.pm !== null) hours = (hours % 12) + (fields.pm ? 12 : 0);
        const year = fields.bc ? 1 - fields.year : fields.year;
        const date = new Date(year, fields.month, fields.day, hours, fields.minutes, fields.seconds, fields.millis);
        if (year >= 0 && year < 100) date.setFullYear(year, fields.month, fields.day);
        if (fields.offset !== null) {
            date.setTime(date.getTime() - (fields.offset + date.getTimezoneOffset()) * 60000);
        }
        return Number.isNaN(date.getTime()) ? null : date;
    }

    private parseField(text: string, position: number, part: { field: string; count: number }, width: number,
        fields: { year: number; month: number; day: number; hours: number; minutes: number; seconds: number;
            millis: number; pm: boolean | null; offset: number | null; bc: boolean }): number {
        const { field, count } = part;
        if (!numeric(part)) {
            switch (field) {
                case 'M':
                case 'L': {
                    const found = matchName(text, position, MONTHS);
                    if (found === null) return -1;
                    fields.month = found[0];
                    return found[1];
                }
                case 'E':
                case 'c': {
                    const found = matchName(text, position, WEEKDAYS);
                    return found === null ? -1 : found[1]; // the day of the week does not make the date
                }
                case 'a': {
                    const found = matchName(text, position, ['AM', 'PM']);
                    if (found === null) return -1;
                    fields.pm = found[0] === 1;
                    return found[1];
                }
                case 'G': {
                    const found = matchName(text, position, ['AD', 'BC', 'Anno Domini', 'Before Christ']);
                    if (found === null) return -1;
                    fields.bc = found[0] % 2 === 1;
                    return found[1];
                }
                case 'Z':
                case 'z':
                case 'v': {
                    const zoneMatch = /^(?:GMT|UTC)?([+-])(\d\d):?(\d\d)/.exec(text.substring(position));
                    if (zoneMatch === null) {
                        const utc = /^(?:GMT|UTC|Z)/.exec(text.substring(position));
                        if (utc === null) return -1;
                        fields.offset = 0;
                        return position + utc[0].length;
                    }
                    const minutes = Number(zoneMatch[2]) * 60 + Number(zoneMatch[3]);
                    fields.offset = zoneMatch[1] === '-' ? -minutes : minutes;
                    return position + zoneMatch[0].length;
                }
                default:
                    return matchLiteral(text, position, field.repeat(count));
            }
        }
        let start = position;
        while (text[start] === ' ') start++;
        let negative = false;
        if (field === 'y' && text[start] === '-') {
            negative = true;
            start++;
        }
        let end = start;
        while (end < text.length && text[end]! >= '0' && text[end]! <= '9' && (width === 0 || end - start < width)) end++;
        if (end === start) return -1;
        const digits = text.substring(start, end);
        const value = Number(digits);
        switch (field) {
            case 'y':
            case 'Y':
                fields.year = negative ? -value : count <= 2 && digits.length === 2 ? twoDigitYear(value) : value;
                break;
            case 'M':
            case 'L':
                fields.month = value - 1;
                break;
            case 'd':
                fields.day = value;
                break;
            case 'h':
            case 'H':
            case 'K':
                fields.hours = value;
                break;
            case 'k':
                fields.hours = value === 24 ? 0 : value;
                break;
            case 'm':
                fields.minutes = value;
                break;
            case 's':
                fields.seconds = value;
                break;
            case 'S':
                fields.millis = Math.round(value * 10 ** (3 - digits.length));
                break;
            default:
        }
        return end;
    }
}

/** A two-digit year, within the 80 years before and the 20 after today: as GWT and Java. */
function twoDigitYear(value: number): number {
    const start = new Date().getFullYear() - 80;
    let year = Math.floor(start / 100) * 100 + value;
    if (year < start) year += 100;
    return year;
}

/** The position after the literal, -1 when the text does not have it; a space matches any run of spaces. */
function matchLiteral(text: string, position: number, literal: string): number {
    let p = position;
    for (let i = 0; i < literal.length; i++) {
        const c = literal[i]!;
        if (/\s/.test(c)) {
            while (p < text.length && /\s/.test(text[p]!)) p++;
            continue;
        }
        if (text[p] !== c) return -1;
        p++;
    }
    return p;
}

/** The longest name (or its first three letters) the text has at the position, case aside: its index and end. */
function matchName(text: string, position: number, names: string[]): [number, number] | null {
    const rest = text.substring(position).toLowerCase();
    let best: [number, number] | null = null;
    names.forEach((name, index) => {
        for (const candidate of [name, name.substring(0, 3)]) {
            const lower = candidate.toLowerCase();
            if (rest.startsWith(lower) && (best === null || position + lower.length > best[1])) best = [index, position + lower.length];
        }
    });
    return best;
}
