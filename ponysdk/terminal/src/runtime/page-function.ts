/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

/**
 * A function of the page that the server calls by name (FUNCTION_NAME, ADR-0015): defined by the application's own
 * scripts, so that no JavaScript source travels and the page needs no 'unsafe-eval'. Looked up at each call: a script
 * loaded later still provides it.
 */
export function pageFunction(name: string): (...args: unknown[]) => unknown {
    const fn = (globalThis as Record<string, unknown>)[name];
    if (typeof fn !== 'function') throw new Error(`No function ${name} in the page`);
    // Called on the window, as window.name(...) would be: alert, scrollTo and the like need it
    return (...args: unknown[]) => (fn as (...a: unknown[]) => unknown).apply(globalThis, args);
}
