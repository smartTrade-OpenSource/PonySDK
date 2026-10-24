/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import { MaskValue } from '../src/objects/mask.ts';

function typed(value: MaskValue, text: string, from = 0): number {
    let position = from;
    for (const c of text) {
        if (!value.fits(position, c)) continue;
        value.remove(position, position + 1);
        position = value.insert(position, c);
    }
    return position;
}

test('shows the mask, its characters past the input only when asked', () => {
    assert.equal(new MaskValue('({{000}}) {{00}}', true, '_').text, '(___) __');
    assert.equal(new MaskValue('({{000}}) {{00}}', false, '_').text, '(_______');
    assert.equal(new MaskValue('{{00}}/{{00}}', true, ' ').length, 5);
});

test('takes digits and letters in their groups, past the mask characters', () => {
    const value = new MaskValue('{{00}}/{{AA}}', true, '_');
    assert.equal(typed(value, '12ab'), 5);
    assert.equal(value.text, '12/ab');
    assert.equal(value.fits(0, 'x'), false);
    assert.equal(value.fits(3, '1'), false);
    const hidden = new MaskValue('{{00}}/{{00}}', false, '_');
    typed(hidden, '1');
    assert.equal(hidden.text, '1____');
    typed(hidden, '2', 1);
    assert.equal(hidden.text, '12/__');
});

test('pushes the next characters when one goes in a taken slot', () => {
    const value = new MaskValue('{{000}}', true, '_');
    typed(value, '12');
    assert.equal(value.insert(0, '9'), 1);
    assert.equal(value.text, '912');
});

test('pulls the next characters back when one goes', () => {
    const value = new MaskValue('{{00}}/{{00}}', true, '_');
    typed(value, '1234');
    assert.equal(value.text, '12/34');
    assert.equal(value.remove(1, 2), true);
    assert.equal(value.text, '13/4_');
    assert.equal(value.insertPosition, 4);
    assert.equal(value.remove(-1, 0), false);
});
