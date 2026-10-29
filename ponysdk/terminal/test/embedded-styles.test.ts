/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';
import { pageSelector } from '../src/embedded-styles.ts';

// docs/decisions/0009-embedding-limits.md, decision 4: a page's stylesheet in a <pony-app> element

test('in a shadow root, :root and html are the element, body its container', () => {
    assert.equal(pageSelector(':root', ':host'), ':host');
    assert.equal(pageSelector('html, body', ':host'), ':host, [data-pony-body]');
    assert.equal(pageSelector('body, table td, select', ':host'), '[data-pony-body], table td, select');
    assert.equal(pageSelector('html body > .a', ':host'), ':host [data-pony-body] > .a');
    assert.equal(pageSelector('body.dark .a', ':host'), '[data-pony-body].dark .a');
});

test('a compound on the root goes into :host()', () => {
    assert.equal(pageSelector('html.dark .a', ':host'), ':host(.dark) .a');
    assert.equal(pageSelector(':root[data-theme="x y"]:hover', ':host'), ':host([data-theme="x y"]:hover)');
    assert.equal(pageSelector(':is(html.dark) .a', ':host'), ':is(:host(.dark)) .a');
});

test('an escaped quote does not end a string', () => {
    assert.equal(pageSelector('[title="a \\" body"] body', ':host'), '[title="a \\" body"] [data-pony-body]');
});

test('an escaped quote in a compound on the root does not end its string', () => {
    assert.equal(pageSelector('html[title="a \\" b"] .a', ':host'), ':host([title="a \\" b"]) .a');
});

test('in the page, the root is the element', () => {
    assert.equal(pageSelector(':root', 'pony-app'), 'pony-app');
    assert.equal(pageSelector('html.dark body .a', 'pony-app'), 'pony-app.dark [data-pony-body] .a');
});

test('names that are not the page stay', () => {
    for (const selector of ['.body', '#html', 'tbody td', 'body-text', '.a:not(.b)', '[title="a body"]', 'a[href$=html]',
        '.x\\:root', 'my-html']) {
        assert.equal(pageSelector(selector, ':host'), null, selector);
    }
});
