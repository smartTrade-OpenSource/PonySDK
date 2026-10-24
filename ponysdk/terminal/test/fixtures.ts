/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { existsSync, readFileSync } from 'node:fs';

/** Frames written by the server write path: ./gradlew :ponysdk:terminalFixtures (TerminalFixtures.java). */
const DIRECTORY = new URL('../build/fixtures/', import.meta.url);

export interface Fixture {
    bytes: Uint8Array;
    /** The bytes cut as the server sent them. */
    frames: Uint8Array[];
    json: Record<string, unknown>;
}

export function fixture(name: string): Fixture {
    const file = new URL(`${name}.bin`, DIRECTORY);
    if (!existsSync(file)) throw new Error(`${file.pathname} is missing: run ./gradlew :ponysdk:terminalFixtures`);
    const data = readFileSync(file);
    const bytes = new Uint8Array(data.buffer, data.byteOffset, data.byteLength);
    const json = JSON.parse(readFileSync(new URL(`${name}.json`, DIRECTORY), 'utf8')) as Record<string, unknown>;
    const frames: Uint8Array[] = [];
    let start = 0;
    for (const end of json['frames'] as number[]) {
        frames.push(bytes.subarray(start, end));
        start = end;
    }
    return { bytes, frames, json };
}
