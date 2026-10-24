/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import { ServerToClientModel as K, ServerToClientModelTypeModel, ValueTypeModel as V } from '#model';
import { INCOMPLETE, Reader } from '../src/protocol/reader.ts';
import { fixture } from './fixtures.ts';

interface Model {
    key: string;
    value: unknown;
}

/** The value of the last model read, from the field its type uses. */
function value(reader: Reader): unknown {
    switch (ServerToClientModelTypeModel[reader.key]) {
        case V.BOOLEAN:
            return reader.bool;
        case V.BYTE:
        case V.SHORT:
        case V.INTEGER:
        case V.UINT31:
            return reader.int;
        case V.LONG:
        case V.DOUBLE:
        case V.FLOAT:
            return reader.num;
        case V.STRING:
            return reader.str;
        case V.ARRAY:
            return reader.array;
        default:
            return null;
    }
}

function readBlock(reader: Reader, into: Model[]): void {
    do {
        reader.read();
        into.push({ key: Reader.keyName(reader.key), value: value(reader) });
    } while (reader.key !== K.END);
}

/** Reads every complete block of each message, as the terminal does; the rest waits for the next message. */
function readMessages(messages: Uint8Array[]): Model[] {
    const reader = new Reader();
    const models: Model[] = [];
    for (const message of messages) {
        reader.feed(message);
        while (reader.hasKey() && reader.blockEnd() !== INCOMPLETE) readBlock(reader, models);
    }
    assert.equal(reader.hasKey(), false, 'bytes left unread');
    return models;
}

function chunks(bytes: Uint8Array, size: number): Uint8Array[] {
    const messages: Uint8Array[] = [];
    for (let start = 0; start < bytes.length; start += size) messages.push(bytes.subarray(start, start + size));
    return messages;
}

for (const name of ['values-plain', 'values-dictionary']) {
    const { bytes, frames, json } = fixture(name);
    const expected = json['expected'] as Model[];

    test(`${name}: every value type and string encoding reads back as written`, () => {
        assert.deepEqual(readMessages([bytes]), expected);
    });

    test(`${name}: a block split across messages is read once complete`, () => {
        assert.deepEqual(readMessages(frames), expected);
        assert.deepEqual(readMessages(chunks(bytes, 4096)), expected);
        assert.deepEqual(readMessages(chunks(bytes, 997)), expected);
    });

    test(`${name}: dictionary entries added by skipped blocks are kept`, () => {
        const reader = new Reader();
        reader.feed(bytes);
        const read: Model[] = [];
        const wanted: Model[] = [];
        let index = 0;
        for (let block = 0; reader.hasKey(); block++) {
            const start = index;
            while (expected[index]!.key !== 'END') index++;
            index++;
            if (block % 2 === 0) {
                reader.skipBlock();
            } else {
                readBlock(reader, read);
                wanted.push(...expected.slice(start, index));
            }
        }
        assert.deepEqual(read, wanted);
    });
}

test('a model is read again after rewind()', () => {
    const { bytes } = fixture('values-plain');
    const reader = new Reader();
    reader.feed(bytes);
    const first = reader.read();
    const offset = reader.offset;
    reader.rewind();
    assert.equal(reader.offset, 0);
    assert.equal(reader.read(), first);
    assert.equal(reader.offset, offset);
});

test('slice() hands bytes over and keeps their dictionary entries', () => {
    const { bytes, json } = fixture('values-dictionary');
    const expected = json['expected'] as Model[];
    const reader = new Reader();
    reader.feed(bytes);
    const half = Math.floor(expected.length / 2);
    let blocks = 0;
    for (let i = 0; i < half; i++) if (expected[i]!.key === 'END') blocks++;
    let end = 0;
    for (let i = 0; i < blocks; i++) {
        end = reader.blockEnd();
        reader.offset = end;
    }
    const handedOver = reader.slice(0, end);
    assert.equal(reader.offset, end);
    assert.equal(handedOver.byteLength, end);
    const rest: Model[] = [];
    while (reader.hasKey()) readBlock(reader, rest);
    const wanted = expected.slice(expected.findIndex((_, i) => {
        let count = 0;
        for (let j = 0; j < i; j++) if (expected[j]!.key === 'END') count++;
        return count === blocks;
    }));
    assert.deepEqual(rest, wanted);
});
