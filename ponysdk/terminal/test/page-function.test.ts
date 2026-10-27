/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import { pageFunction } from '../src/runtime/page-function.ts';

test('finds a function of the page by its name, when it is called', () => {
    assert.throws(() => pageFunction('ponyTestJoin'), /No function ponyTestJoin in the page/);
    (globalThis as Record<string, unknown>).ponyTestJoin = (...args: unknown[]) => args.join('-');
    try {
        assert.equal(pageFunction('ponyTestJoin')('a', 1), 'a-1');
    } finally {
        delete (globalThis as Record<string, unknown>).ponyTestJoin;
    }
});

test('a name that is not a function is refused', () => {
    assert.throws(() => pageFunction('Infinity'), /No function Infinity/);
});
