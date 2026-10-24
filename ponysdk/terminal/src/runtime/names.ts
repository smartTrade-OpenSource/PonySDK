/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

/**
 * The name of a value of a generated model (ServerToClientModel, WidgetType...), for messages: read in the model's own
 * object, so that the bundle carries no table of names (model.ts has them, for the tests).
 */
export function nameOf(model: Readonly<Record<string, number>>, value: number): string | undefined {
    for (const name in model) if (model[name] === value) return name;
    return undefined;
}
