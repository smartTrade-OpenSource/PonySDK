/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, ServerToClientModel as K } from '#model';
import { about } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import { log } from '../runtime/log.ts';
import { BaseObject, type Context } from '../runtime/object.ts';
import { pageFunction } from '../runtime/page-function.ts';

/** Evaluates in the global scope, as window.eval does. */
const globalEval = eval;

/**
 * PScript: calls a function of the page by name with FUNCTION_ARGS (FUNCTION_NAME), or evaluates JavaScript source
 * (EVAL, which needs 'unsafe-eval'), after FIXDELAY ms when given. With a COMMAND_ID, the result goes back: a string or
 * a number as text, anything else as "".
 */
export class Script extends BaseObject {
    override update(reader: Reader): boolean {
        if (reader.key !== K.EVAL && reader.key !== K.FUNCTION_NAME) return super.update(reader);
        const named = reader.key === K.FUNCTION_NAME;
        const script = reader.str ?? '';
        let args: unknown[] = [];
        if (named && reader.read() === K.FUNCTION_ARGS) args = reader.array;
        else if (named) reader.rewind();
        let commandId = -1;
        if (reader.read() === K.COMMAND_ID) commandId = reader.num;
        else reader.rewind();
        let delay = -1;
        if (reader.read() === K.FIXDELAY) delay = reader.num;
        else reader.rewind();

        const run = () => this.run(script, commandId, named ? () => pageFunction(script)(...args) : () => globalEval(script));
        if (delay === -1) run();
        else setTimeout(() => this.context.guard(run), delay);
        return true;
    }

    private run(script: string, commandId: number, call: () => unknown): void {
        let result: unknown;
        try {
            result = call();
        } catch (e) {
            log.error(`Script exception for ${script}`, e);
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

/** PFunction: a function of one parameter, args, that other objects call: the page's by name, or given as source. */
export class JsFunction extends BaseObject {
    readonly fn: (args: unknown) => unknown;

    static create(context: Context, id: number, type: number, reader: Reader): JsFunction {
        const key = reader.read();
        const value = reader.str ?? '';
        const fn = key === K.FUNCTION_NAME
            ? (args: unknown) => pageFunction(value)(args)
            : new Function('args', value) as (args: unknown) => unknown;
        return new JsFunction(context, id, type, fn);
    }

    constructor(context: Context, id: number, type: number, fn: (args: unknown) => unknown) {
        super(context, id, type);
        this.fn = fn;
    }
}
