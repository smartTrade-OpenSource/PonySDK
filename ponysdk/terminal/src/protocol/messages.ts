/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C } from '#model';

/** A JSON message to the server, keyed by ClientToServerModel values. */
export type Message = Record<string, unknown>;

/** A message about one object. */
export function about(objectId: number): Message {
    return { [C.OBJECT_ID]: objectId };
}

/** A key that carries no value: the server only checks its presence. */
export const FLAG = 0;
