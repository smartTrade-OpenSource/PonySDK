/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

// docs/decisions/0009-embedding-limits.md, decision 10: the page's ponysdk.js registers its addons with the terminal it
// was handed; the terminal of another PonySDK version, another module, creates them from the same table

test('the addon factories are the page\'s, whichever terminal module registered them', async () => {
    const first = await import('../src/runtime/dispatcher.ts');
    const another = '../src/runtime/dispatcher.ts?another-version'; // another URL: another module instance
    const other = await import(another) as typeof first;
    assert.notEqual(other.registerAddOnFactory, first.registerAddOnFactory, 'two module instances');
    const factory = () => ({});
    first.registerAddOnFactory('com.example.Chart', factory as never);
    const dispatcher = Object.create(other.Dispatcher.prototype) as InstanceType<typeof first.Dispatcher>;
    assert.equal(dispatcher.addOnFactory('com.example.Chart'), factory);
});
