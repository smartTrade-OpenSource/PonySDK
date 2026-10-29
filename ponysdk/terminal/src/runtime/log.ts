/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

/** The terminal's console output, prefixed so that it stands apart from the application's. */
export const log = {
    // The message is an argument, never the format: a '%s' in it is printed as is
    info(message: string): void {
        console.info('[pony] %s', message);
    },
    warn(message: string): void {
        console.warn('[pony] %s', message);
    },
    error(message: string, error?: unknown): void {
        if (error === undefined) console.error('[pony] %s', message);
        else console.error('[pony] %s', message, error);
    },
};

/** An error as one line for the server: name, message and stack. */
export function describe(error: unknown): string {
    if (error instanceof Error) return `${error.name} : ${error.message} : ${error.stack ?? ''}`;
    return String(error);
}
