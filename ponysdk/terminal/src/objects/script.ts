/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, ServerToClientModel as K } from '#model';
import { about } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import { log } from '../runtime/log.ts';
import { BaseObject, type Context } from '../runtime/object.ts';
import { pageFunction } from '../runtime/page-function.ts';

/**
 * PScript: calls a function of the page by name (FUNCTION_NAME) with FUNCTION_ARGS, after FIXDELAY ms when given. With a
 * COMMAND_ID, the result goes back: a string or a number as text, anything else as "". No source is evaluated (ADR-0015).
 */
export class Script extends BaseObject {
    override update(reader: Reader): boolean {
        if (reader.key !== K.FUNCTION_NAME) return super.update(reader);
        const name = reader.str;
        let args: unknown[] = [];
        if (reader.read() === K.FUNCTION_ARGS) args = reader.array;
        else reader.rewind();
        let commandId = -1;
        if (reader.read() === K.COMMAND_ID) commandId = reader.num;
        else reader.rewind();
        let delay = -1;
        if (reader.read() === K.FIXDELAY) delay = reader.num;
        else reader.rewind();

        const run = () => this.run(name, commandId, () => pageFunction(name)(...args));
        if (delay === -1) run();
        else setTimeout(() => this.context.guard(run), delay);
        return true;
    }

    private run(name: string, commandId: number, call: () => unknown): void {
        let result: unknown;
        try {
            result = call();
        } catch (e) {
            log.error(`Script exception for ${name}`, e);
            if (commandId !== -1) {
                const message = about(this.id);
                message[C.COMMAND_ID] = commandId;
                message[C.ERROR_MSG] = e instanceof Error ? e.message : String(e);
                this.context.send(message);
            }
            return;
        }
        if (commandId === -1) return;
        const message = about(this.id);
        message[C.COMMAND_ID] = commandId;
        message[C.RESULT] = result === null || result === undefined || typeof result === 'object' ? '' : String(result);
        this.context.send(message);
    }
}

/** PFunction: a function of one parameter, args, that other objects call: the page's, by name. */
export class JsFunction extends BaseObject {
    readonly fn: (args: unknown) => unknown;

    static create(context: Context, id: number, type: number, reader: Reader): JsFunction {
        reader.read(); // FUNCTION_NAME
        const name = reader.str;
        return new JsFunction(context, id, type, (args: unknown) => pageFunction(name)(args));
    }

    constructor(context: Context, id: number, type: number, fn: (args: unknown) => unknown) {
        super(context, id, type);
        this.fn = fn;
    }
}
