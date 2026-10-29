/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import { describe, log } from '../src/runtime/log.ts';

test('prints with the terminal prefix, the message as an argument', t => {
    const warn = t.mock.method(console, 'warn', () => undefined);
    const error = t.mock.method(console, 'error', () => undefined);
    log.warn('100%s done');
    const cause = new Error('boom');
    log.error('failed');
    log.error('failed with', cause);
    assert.deepEqual(warn.mock.calls.map(c => c.arguments), [['[pony] %s', '100%s done']]);
    assert.deepEqual(error.mock.calls.map(c => c.arguments), [['[pony] %s', 'failed'], ['[pony] %s', 'failed with', cause]]);
});

test('describes an error for the server as one line, anything else as its text', () => {
    const error = new TypeError('bad');
    assert.equal(describe(error), `TypeError : bad : ${error.stack}`);
    const noStack = new Error('plain');
    delete noStack.stack;
    assert.equal(describe(noStack), 'Error : plain : ');
    assert.equal(describe(42), '42');
});
