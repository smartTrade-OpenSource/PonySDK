/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    ClientToServerModel as C,
    HandlerModelNames,
    ServerToClientModel as K,
    ServerToClientModelTypeModel,
    ValueTypeModel as V,
    WidgetType as W,
    WidgetTypeNames,
} from '#model';
import type { Message } from '../src/protocol/messages.ts';
import { Reader } from '../src/protocol/reader.ts';
import { Dispatcher, type HistoryPort, type Host } from '../src/runtime/dispatcher.ts';
import { BaseObject, type Context, type ObjectFactory, type TerminalObject } from '../src/runtime/object.ts';
import { fixture } from './fixtures.ts';

function value(reader: Reader): unknown {
    switch (ServerToClientModelTypeModel[reader.key]) {
        case V.NULL:
            return null;
        case V.BOOLEAN:
            return reader.bool;
        case V.STRING:
            return reader.str;
        case V.ARRAY:
            return reader.array;
        default:
            return reader.number;
    }
}

/** Records what the dispatcher asks of it, and reads the extra models of an add or a handler. */
class Recorded extends BaseObject {
    private readonly log: string[];

    constructor(context: Context, id: number, type: number, log: string[]) {
        super(context, id, type);
        this.log = log;
    }

    override update(reader: Reader): boolean {
        this.log.push(`#${this.id} ${Reader.keyName(reader.key)}=${JSON.stringify(value(reader))}`);
        return true;
    }

    override add(reader: Reader, child: TerminalObject): void {
        this.log.push(`add #${child.id} to #${this.id}${this.extra(reader)}`);
    }

    override remove(reader: Reader, child: TerminalObject): void {
        this.log.push(`remove #${child.id} from #${this.id}${this.extra(reader)}`);
    }

    override addHandler(reader: Reader, handler: number): void {
        this.log.push(`#${this.id} handler ${HandlerModelNames[handler]}${this.extra(reader)}`);
    }

    /** Models up to END, which stays for the dispatcher. */
    private extra(reader: Reader): string {
        let extra = '';
        while (reader.read() !== K.END) extra += ` ${Reader.keyName(reader.key)}=${JSON.stringify(value(reader))}`;
        reader.rewind();
        return extra;
    }
}

interface Run {
    log: string[];
    sent: Message[];
}

function run(name: string): Run {
    const log: string[] = [];
    const sent: Message[] = [];
    const host: Host = {
        send: message => sent.push(message),
        contextCreated: () => true,
        contextResumed: () => undefined,
        contextDestroyed: () => undefined,
        streamRequested: () => undefined,
    };
    const history: HistoryPort = { token: '', newItem: () => undefined, fireCurrent: () => undefined };
    const factory: ObjectFactory = (type, context, id) => {
        log.push(`create ${WidgetTypeNames[type]} #${id}`);
        if (type === W.BROWSER) context.windows.mainWindowId = id;
        return new Recorded(context, id, type, log);
    };
    const dispatcher = new Dispatcher(host, factory, { contextId: 1, tabindexOnlyFormField: false, hostPageBaseURL: '' }, history);
    for (const frame of fixture(name).frames) dispatcher.receive(frame);
    return { log, sent };
}

const runs = ['page-v1', 'page-v2', 'page-v3'].map(run);

test('protocols 1, 2 and 3 apply the same operations, in the same order', () => {
    assert.deepEqual(runs[1]!.log, runs[0]!.log);
    assert.deepEqual(runs[2]!.log, runs[0]!.log);
});

test('the page is built, updated and partly removed', () => {
    const log = runs[2]!.log;
    const count = (pattern: RegExp) => log.filter(line => pattern.test(line)).length;
    assert.equal(count(/^create LABEL /), 12);
    assert.equal(count(/^create BUTTON /), 3);
    assert.equal(count(/^create FLOW_PANEL /), 4);
    assert.equal(count(/ handler HANDLER_DOM_CLICK/), 3);
    assert.equal(count(/ TEXT="cell \d\.\d updated"$/), 12);
    assert.equal(count(/ ADD_STYLE_NAME="even"$/), 6);
    assert.equal(count(/^remove /), 1 + 5);
    // every object is added once, after its creation
    const created = new Set<string>();
    for (const line of log) {
        const create = /^create \w+ (#\d+)/.exec(line);
        if (create) created.add(create[1]!);
        const add = /^add (#\d+) /.exec(line);
        if (add) assert.ok(created.has(add[1]!), `${line} before its creation`);
    }
});

test('nothing is reported to the server', () => {
    for (const { sent } of runs) {
        assert.deepEqual(sent.filter(m => C.ERROR_MSG in m || C.WARN_MSG in m), []);
    }
});
