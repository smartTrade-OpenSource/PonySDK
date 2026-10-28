/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import { SuggestOracle } from '../src/objects/suggest.ts';
import type { Context } from '../src/runtime/object.ts';

// PMultiWordSuggestOracle's matching, which needs no document

function oracle(...suggestions: string[]): SuggestOracle {
    const o = new SuggestOracle(null as unknown as Context, 1, -1);
    for (const s of suggestions) o.addSuggestion(s);
    return o;
}

const replacements = (o: SuggestOracle, query: string, limit: number): string[] =>
    o.suggest(query, limit).map(s => s.replacement);

test('keeps one more match than the limit, as GWT', () => {
    const o = oracle('Red apple', 'Red cherry', 'Red pepper', 'Rare red');
    assert.deepEqual(replacements(o, 'red', 1), ['Rare red', 'Red apple']);
    assert.deepEqual(replacements(o, 'red', 10), ['Rare red', 'Red apple', 'Red cherry', 'Red pepper']);
});

test('shows the longest of the query words that start at the same place', () => {
    const o = oracle('Red apple');
    assert.deepEqual(o.suggest('re red', 5).map(s => s.display), ['<strong>Red</strong> apple']);
    assert.deepEqual(o.suggest('red re', 5).map(s => s.display), ['<strong>Red</strong> apple']);
});
