/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, ServerToClientModel as K } from '#model';
import { about } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import { BaseObject, type Context } from '../runtime/object.ts';

const EXPIRED = 'Fri, 02-Jan-1970 00:00:00 GMT';

/**
 * The cookies, object #0: created by the terminal, which sends the page's cookies to the server first thing. The
 * server then sets and removes cookies through it. In a <pony-app> element, document.cookie is the host page's, not
 * the application's: none is sent and none is written here; the server reads those of the handshake, and writes
 * through a request to the application's origin (HANDLER_COOKIE_REQUEST, ADR-0009).
 */
export class Cookies extends BaseObject {
    private readonly embedded: boolean;

    constructor(context: Context) {
        super(context, 0, -1);
        this.embedded = context.session.embedding !== null;
        const message = about(0);
        message[C.COOKIES] = [...(this.embedded ? new Map<string, string>() : readCookies())].map(([name, value]) => ({ [C.COOKIE_NAME]: name, [C.COOKIE_VALUE]: value }));
        context.send(message);
    }

    override update(reader: Reader): boolean {
        if (reader.key === K.ADD_COOKIE) {
            const name = reader.str ?? '';
            reader.read(); // VALUE
            const value = reader.str ?? '';
            const expires = optional(reader, K.COOKIE_EXPIRE)?.num;
            const domain = optional(reader, K.COOKIE_DOMAIN)?.str;
            const path = optional(reader, K.COOKIE_PATH)?.str;
            const secure = optional(reader, K.COOKIE_SECURE) !== undefined;
            const sameSite = optional(reader, K.COOKIE_SAMESITE)?.str;
            let cookie = `${encodeURIComponent(name)}=${encodeURIComponent(value)}`;
            if (expires) cookie += `;expires=${new Date(expires).toUTCString()}`;
            if (domain) cookie += `;domain=${domain}`;
            if (path) cookie += `;path=${path}`;
            if (secure) cookie += ';secure';
            if (sameSite) cookie += `;SameSite=${sameSite}`;
            this.write(cookie);
            return true;
        }
        if (reader.key === K.REMOVE_COOKIE) {
            let cookie = `${encodeURIComponent(reader.str ?? '')}=`;
            const domain = optional(reader, K.COOKIE_DOMAIN)?.str; // a cookie set with a domain is removed with it
            const path = optional(reader, K.COOKIE_PATH)?.str;
            if (domain) cookie += `;domain=${domain}`;
            if (path) cookie += `;path=${path}`;
            this.write(`${cookie};expires=${EXPIRED}`);
            return true;
        }
        return super.update(reader);
    }

    private write(cookie: string): void {
        if (this.embedded) this.context.warn('A cookie write ignored: the page is the host\'s', this.id);
        else document.cookie = cookie;
    }
}

/** The next model if it is this key, else nothing read. */
function optional(reader: Reader, key: number): Reader | undefined {
    reader.read();
    if (reader.key === key) return reader;
    reader.rewind();
    return undefined;
}

/**
 * The page's cookies, URI-decoded. As GWT's Cookies: read from the last to the first, the first of a name kept
 * (the most specific path).
 */
export function readCookies(): Map<string, string> {
    const cookies = new Map<string, string>();
    const crumbs = document.cookie ? document.cookie.split('; ') : [];
    for (let i = crumbs.length - 1; i >= 0; i--) {
        const crumb = crumbs[i]!;
        const equals = crumb.indexOf('=');
        const name = decode(equals === -1 ? crumb : crumb.substring(0, equals));
        const value = equals === -1 ? '' : decode(crumb.substring(equals + 1));
        cookies.set(name, value);
    }
    return cookies;
}

function decode(text: string): string {
    try {
        return decodeURIComponent(text);
    } catch {
        return text;
    }
}
