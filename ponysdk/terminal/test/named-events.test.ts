/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import { readFields } from '../src/runtime/named-events.ts';

test('reads the fields at their paths, as the server reads them', () => {
    const event = new CustomEvent('pick', { detail: { day: 12, tags: ['a'] } });
    Object.defineProperty(event, 'target', { value: { value: 'abc', checked: true, size: Number.NaN } });
    assert.deepEqual(readFields(event, ['type', 'detail', 'target.value', 'target.checked', 'target.size', 'missing.path']), {
        type: 'pick',
        detail: '{"day":12,"tags":["a"]}', // an object as JSON text
        'target.value': 'abc',
        'target.checked': true,
        'target.size': null, // not a JSON number
        'missing.path': null,
    });
});
