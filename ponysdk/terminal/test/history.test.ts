/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import { BrowserHistory, decode } from '../src/runtime/history.ts';
import { MemoryHistory } from '../src/runtime/memory-history.ts';

test('a fragment is decoded, a malformed one kept as written', () => {
    assert.equal(decode('#a%20b%23c'), 'a b#c');
    assert.equal(decode('a%20b'), 'a b');
    assert.equal(decode('#50%'), '50%');
});

test("an embedded application's history: the server's items change it, those asked to fire tell the listeners", () => {
    const history = new MemoryHistory('#start%20page');
    const heard: string[] = [];
    history.onChange(token => heard.push(token));
    assert.equal(history.token, 'start page');

    history.newItem('quiet', false);
    history.newItem('loud', true);
    history.newItem('loud', true); // the same token: nothing
    assert.equal(history.token, 'loud');
    history.fireCurrent();
    assert.deepEqual(heard, ['loud', 'loud']);
});

test("a page's history: the user's navigation fires, the hashchange of a server's item does not", t => {
    const page = globalThis as unknown as Record<string, unknown>;
    const window = new EventTarget();
    const location = { hash: '#first' };
    t.after(() => {
        delete page.window;
        delete page.location;
    });
    Object.assign(page, { window, location });
    const lifetime = new AbortController();
    const history = new BrowserHistory(lifetime.signal);
    const heard: string[] = [];
    history.onChange(token => heard.push(token));
    assert.equal(history.token, 'first');

    history.newItem('a b#c', true);
    assert.equal(location.hash, 'a%20b%23c');
    window.dispatchEvent(new Event('hashchange')); // caused by the item: already the token
    history.newItem('a b#c', true);
    history.newItem('quiet', false);
    location.hash = '#user';
    window.dispatchEvent(new Event('hashchange'));
    history.fireCurrent();
    assert.deepEqual(heard, ['a b#c', 'user', 'user']);

    lifetime.abort(); // the terminal went: its history no longer listens
    location.hash = '#gone';
    window.dispatchEvent(new Event('hashchange'));
    assert.equal(history.token, 'user');
});
